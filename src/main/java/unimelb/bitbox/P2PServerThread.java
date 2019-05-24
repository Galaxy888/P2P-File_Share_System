package unimelb.bitbox;

import unimelb.bitbox.protocol.Protocol;
import unimelb.bitbox.protocol.ProtocolInterface;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.kohsuke.args4j.Config;

import static unimelb.bitbox.Peer.syncInterval;


public class P2PServerThread extends Thread implements ProtocolInterface {
	private static Logger log = Logger.getLogger(P2PServerThread.class.getName());

	private P2PServer server;
	private Socket socket;
	protected BufferedReader input = null;
	protected BufferedWriter output = null;
	private HostPort hostport;

	private ServerMain serverMain;
	private boolean isConnected;

	private BlockingQueue<String> events;
	
	
	private String[] authorized_keys = Configuration.getConfigurationValue("authorized_keys").split(",");


	
	
	Timer t;


	public P2PServerThread(Socket socket, P2PServer server, ServerMain serverMain, BlockingQueue<String> events1) {
		this.socket = socket;
		this.server = server;
		this.serverMain=serverMain;
		this.events=events1;
		this. isConnected =false;
		this.t= new Timer();
		try{
			input = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "UTF-8"));
			output = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream(), "UTF-8"));
		} catch (Exception e) {
//			e.printStackTrace();
//        	this.CloseConnection();
		}
	}

	public void run(){
//		System.out.println("run");
		try {
//			System.out.println("Received Message: ");
			//System.out.println(input.readUTF());
			while (true){
				Thread t = new Thread(() -> serveToClient());
				t.start();

				String inputMessage = input.readLine();
				log.info("inputMessage： "+inputMessage);
				process(inputMessage);

			}
		} catch (Exception e) {
			log.warning("Exception detected");
//    			this.CloseConnection();
//    			break;
		}
		if (isConnected) {
//			System.out.println("isConnected");

			t.scheduleAtFixedRate(
					new TimerTask() {
						public void run() {
							ArrayList<FileSystemManager.FileSystemEvent> fileSystemEvents = serverMain.fileSystemManager.generateSyncEvents();
							Protocol protocol = new Protocol();
//        String fileSystemEventsMessage =  protocol.genergateFileSystemEventsMessage(fileSystemEvents);
							ArrayList<String> fileSystemEventsMessage = protocol.genergateFileSystemEventsMessage(fileSystemEvents);
//                        System.out.println(fileSystemEventsMessage.size());
							for (String event : fileSystemEventsMessage) {
								events.offer(event);

							}
						}
					},
					0,      // run first occurrence immediatetly
					syncInterval);
		}


//    	}


	}


	private  void serveToClient(){
		try {
			while(true)
			{
				String message = events.take();
				output.write(message + "\n");
				log.info("message: "+message);
				output.flush();


			}
		} catch (InterruptedException e) {
//			e.printStackTrace();
		} catch (IOException e) {
//			e.printStackTrace();
		}

	}



	private void process(String inputMessage) throws Exception {
		String response = "";
		Protocol protocol = new Protocol();
		//HostPort hostport;
		String md5 = " ", host = " ", pathName = " ", content= " ";
		long fileSize = 0, time = 0, position = 0;
		int port;
		try {
			long block_size = Long.parseLong(Configuration.getConfigurationValue("blockSize").trim());
			Document inputMessageDoc = Document.parse(inputMessage);
			String command = inputMessageDoc.getString("command");
//			System.out.println("command: " + command);
			switch(command) {
			
				case "HANDSHAKE_REQUEST":
					Document hostportinput = (Document) inputMessageDoc.get("hostPort");
					hostport = new HostPort(hostportinput);
					//host = hostport.host;
					//port = hostport.port;
					if (P2PServer.counterPeerNum < Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"))) {
						P2PServer.counterPeerNum++;
						P2PServer.connectedHostPorts.add(hostport);
						log.info("receive HANDSHAKE_REQUEST from " + hostport);

						response= protocol.generateHandshakeResponseMessage(server.getHostPort());
						this.isConnected= true;

						events.offer(response);
						ArrayList<FileSystemManager.FileSystemEvent> fileSystemEvents = serverMain.fileSystemManager.generateSyncEvents();
						ArrayList<String> fileSystemEventsMessage = protocol.genergateFileSystemEventsMessage(fileSystemEvents);
						for (String event: fileSystemEventsMessage){
							events.offer(event);
						}

					} else {

						response= protocol.generateConncetionRefusedMessage(P2PServer.connectedHostPorts);
						events.offer(response);
					}
					break;

				case "FILE_CREATE_REQUEST":
					Document fileDescriptorFCReq = (Document) inputMessageDoc.get("fileDescriptor");
					md5 = fileDescriptorFCReq.getString("md5");
					time = fileDescriptorFCReq.getLong("lastModified");
					fileSize = fileDescriptorFCReq.getLong("fileSize");

					pathName = inputMessageDoc.getString("pathName");
					if (this.serverMain.fileSystemManager.isSafePathName(pathName)) {
						if (!this.serverMain.fileSystemManager.fileNameExists(pathName)) {
							try {
								if (!this.serverMain.fileSystemManager.createFileLoader(pathName, md5, fileSize, time)) {
									response = protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "unknown problem", false);
									events.offer(response);
									break;
								}

								if (!this.serverMain.fileSystemManager.checkShortcut(pathName)) {
									long readLength;
									if (fileSize==0) {
										response= protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "file loader ready", true);
										events.offer(response);
										this.serverMain.fileSystemManager.checkWriteComplete(pathName);
										break;

									} else if (fileSize <= block_size) {
										readLength = fileSize;
									} else {
										readLength = block_size;
									}
//									System.out.println("file_byte request sent");
									response = protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "file loader ready", true);
									events.offer(response);

									response= protocol.genergateFileBytesRequest(fileDescriptorFCReq, pathName, 0, readLength);
									events.offer(response);
								}else {
									this.serverMain.fileSystemManager.cancelFileLoader(pathName);
//									System.out.println("shortcut");

									response= protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "file loader ready", true);
									events.offer(response);

								}

							} catch (Exception e) {
								response= protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "there was a problem creating the file", false);
								events.offer(response);
								e.printStackTrace();
							}
						} else if (!this.serverMain.fileSystemManager.fileNameExists(pathName, md5)) {
							try {
								if (!serverMain.fileSystemManager.modifyFileLoader(pathName, md5, time)){
									response = protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "Unknown error", false);
									break;
								}
								if (!this.serverMain.fileSystemManager.checkShortcut(pathName)) {
									long readLength;
									if (fileSize <= block_size){
										readLength = fileSize;

									} else {
										readLength = block_size;
									}
									response = protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "Success", true);
									events.offer(response);
									response = protocol.genergateFileBytesRequest(fileDescriptorFCReq, pathName, 0, readLength);
									events.offer(response);
								} else {
									this.serverMain.fileSystemManager.cancelFileLoader(pathName);
									response = protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "file loader ready", true);
								}

							} catch (Exception e) {
								log.warning("file create failed");
							}

						} else {

							response= protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "file loader ready", true);
							events.offer(response);

						}
					} else {
//						System.out.println("path not safe");
						response= protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "unsafe pathname", false);
						events.offer(response);
					}
					break;


				case "FILE_MODIFY_REQUEST":
					Document fileDescriptorFMReq = (Document) inputMessageDoc.get("fileDescriptor");
					md5 = fileDescriptorFMReq.getString("md5");
					time = fileDescriptorFMReq.getLong("lastModified");
					fileSize = fileDescriptorFMReq.getLong("fileSize");
					pathName = inputMessageDoc.getString("pathName");
					if (this.serverMain.fileSystemManager.isSafePathName(pathName)) {
						if (!this.serverMain.fileSystemManager.fileNameExists(pathName)) {
							response = protocol.generateFileModifyResponseMessage(fileDescriptorFMReq, pathName, "pathname does not exist", false);
							events.offer(response);
						} else if (!this.serverMain.fileSystemManager.fileNameExists(pathName, md5)) {
							try {
								if (!this.serverMain.fileSystemManager.modifyFileLoader(pathName, md5, time)) {
									break;
								}

								if (!this.serverMain.fileSystemManager.checkShortcut(pathName)) {
									long readLength;
									if (fileSize <= block_size) {
										readLength = fileSize;
									} else {
										readLength = block_size;
									}
//									System.out.println("file_byte request sent");
									response = protocol.genergateFileBytesRequest(fileDescriptorFMReq, pathName, 0, readLength);
									events.offer(response);

								} else {
									this.serverMain.fileSystemManager.cancelFileLoader(pathName);
//									System.out.println("shortcut");
									response = protocol.generateFileModifyResponseMessage(fileDescriptorFMReq, pathName, "file loader ready", true);
									events.offer(response);
								}


							} catch (Exception e) {
								e.printStackTrace();
//                                this.CloseConnection();
								break;
							}
						} else {

							response= protocol.generateFileModifyResponseMessage(fileDescriptorFMReq, pathName, "pathname does not exist", true);
							events.offer(response);
						}
					} else {
//						System.out.println("not safe path");
						response= protocol.generateFileModifyResponseMessage(fileDescriptorFMReq, pathName, "unsafe pathname given", false);
						events.offer(response);

					}
					break;

				case "FILE_BYTES_RESPONSE":
					Document fileDescriptorFBRes = (Document) inputMessageDoc.get("fileDescriptor");
					md5 = fileDescriptorFBRes.getString("md5");
					time = fileDescriptorFBRes.getLong("lastModified");
					fileSize = fileDescriptorFBRes.getLong("fileSize");
					pathName = inputMessageDoc.getString("pathName");
					content = inputMessageDoc.getString("content");
					position = inputMessageDoc.getLong("position");
					ByteBuffer src = ByteBuffer.wrap(java.util.Base64.getDecoder().decode(content));

					this.serverMain.fileSystemManager.writeFile(pathName, src, position);


					if (!this.serverMain.fileSystemManager.checkWriteComplete(pathName)) {
						long readLength;
						if (position + block_size  <= fileSize) {
							readLength = block_size;
						} else {
							readLength = fileSize - position  ;

						}
						response= protocol.genergateFileBytesRequest(fileDescriptorFBRes, pathName, position+block_size, readLength);
						events.offer(response);
					} else {

						log.info("file transfer complete");

					}


					break;


				case "FILE_BYTES_REQUEST":
					Document fileDescriptorFBReq = (Document) inputMessageDoc.get("fileDescriptor");
					md5 = fileDescriptorFBReq.getString("md5");
					time = fileDescriptorFBReq.getLong("lastModified");
					fileSize = fileDescriptorFBReq.getLong("fileSize");
					pathName = inputMessageDoc.getString("pathName");
					long length = inputMessageDoc.getLong("length");
					position = inputMessageDoc.getLong("position");

					if (position + length <= fileSize) {
						byte[] byteContent = serverMain.fileSystemManager.readFile(md5, position, length).array();
						content = java.util.Base64.getEncoder().encodeToString(byteContent);
						response = protocol.generateFileBytesResponseMessage(fileDescriptorFBReq, pathName, position, length, content,"successful read", true );
						events.offer(response);
//						System.out.println("size checked, byte response sent" );
					} else {
						log.warning("The read length is bigger than file size");
//            		    this.CloseConnection();
					}


					break;





				case "FILE_DELETE_REQUEST":
					Document fileDescriptorFDReq = (Document) inputMessageDoc.get("fileDescriptor");
					md5 = fileDescriptorFDReq.getString("md5");
					time = fileDescriptorFDReq.getLong("lastModified");
					fileSize = fileDescriptorFDReq.getLong("fileSize");
					pathName = inputMessageDoc.getString("pathName");
					if (this.serverMain.fileSystemManager.isSafePathName(pathName)) {
						if (this.serverMain.fileSystemManager.fileNameExists(pathName,md5)) {
							try {
								if(this.serverMain.fileSystemManager.deleteFile(pathName, time, md5)) {
									response= protocol.generateFileDeleteResponseMessage(fileDescriptorFDReq, pathName, "file deleted", true);
									events.offer(response);
								}

							} catch (Exception e) {
								response = protocol.generateFileDeleteResponseMessage(fileDescriptorFDReq, pathName, "couldn't delete file", false);
								events.offer(response);
								e.printStackTrace();
							}
						} else {
//							System.out.println("file does not exist");
							response= protocol.generateFileDeleteResponseMessage(fileDescriptorFDReq, pathName, "file does not exist", false);
							events.offer(response);
						}
					} else {
//						System.out.println("not safe path");
						response= protocol.generateFileDeleteResponseMessage(fileDescriptorFDReq, pathName, "unsafe pathname given", false);
						events.offer(response);
					}
					break;


				case "DIRECTORY_DELETE_REQUEST":

					pathName = inputMessageDoc.getString("pathName");
					if (this.serverMain.fileSystemManager.isSafePathName(pathName)) {
						if (this.serverMain.fileSystemManager.dirNameExists(pathName)) {
							if(this.serverMain.fileSystemManager.deleteDirectory(pathName)) {
//								System.out.println("directory deleted");
								response= protocol.generateDirectoryDeleteResponseMessage(pathName, "directory deleted", true);
								events.offer(response);
							} else {
//								System.out.println("directory delete fail");
								response= protocol.generateDirectoryDeleteResponseMessage(pathName, "there was a problem deleting the directory", false);
								events.offer(response);
							}

						}else {
							response= protocol.generateDirectoryDeleteResponseMessage(pathName, "pathname does not exist", false);
							events.offer(response);
						}
					} else {
						response= protocol.generateDirectoryDeleteResponseMessage(pathName, "unsafe pathname given", false);
						events.offer(response);

					}


					break;

				case "DIRECTORY_CREATE_REQUEST":
					pathName = inputMessageDoc.getString("pathName");
					if (this.serverMain.fileSystemManager.isSafePathName(pathName)) {
						if (!this.serverMain.fileSystemManager.dirNameExists(pathName)) {
							if(this.serverMain.fileSystemManager.makeDirectory(pathName)) {

								response= protocol.generateDirectoryCreateResponseMessage(pathName, "directory created", true);
								events.offer(response);
							} else {
//								System.out.println("directory delete fail");
								response= protocol.generateDirectoryCreateResponseMessage(pathName, "there was a problem creating the directory", false);
								events.offer(response);
							}

						}else {
							response= protocol.generateDirectoryCreateResponseMessage(pathName, "pathname already exists", false);
							events.offer(response);
						}
					} else {
						response= protocol.generateDirectoryCreateResponseMessage(pathName, "unsafe pathname given", false);
						events.offer(response);

					}

					break;

				case "DIRECTORY_CREATE_RESPONSE":
					log.info("received from " + hostport +" "+ command);
					break;

				case "DIRECTORY_DELETE_RESPONSE":
					log.info("received from " + hostport +" "+ command);
					break;

				case "FILE_CREATE_RESPONSE":
					log.info("received from " + hostport +" "+ command);
					break;

				case "FILE_MODIFY_RESPONSE":
					log.info("received from " + hostport +" "+ command);
					break;

				case "FILE_DELETE_RESPONSE":
					log.info("received from " + hostport +" "+ command);
					break;

                case "CONNECTION_REFUSED":
                    log.warning("connection refused");
                    break;

                case "INVALID_PROTOCOL":
                    log.warning("invalid protocol");
                    break;
                
                case "AUTH_REQUEST":
                	System.out.println("recieved auth_request");
                	String identity = inputMessageDoc.getString("identity");
                	System.out.println("identity: "+identity);
                	for (String keys: authorized_keys) {
                		System.out.println("key: " + keys);
                		if (keys.split(" ")[2].equals(identity)) {
                			String clientPublicKey = keys.split(" ")[1];

              
                	        PublicKey pubkey = getPublicKey(clientPublicKey);
                	        System.out.println("public key generate ");
                	        System.out.println(pubkey);
           
                			
              		
                			//create key AES
                			KeyGenerator gen = KeyGenerator.getInstance("AES");
                			gen.init(128);
                		    SecretKey AES = gen.generateKey();
                		    
                		    // get the raw key bytes
                		    System.out.println("secret key: "+AES);
                		    byte[] symmetriskNyckel = AES.getEncoded();
                		    
                		    //encrypt AES key with RSA
                		    Cipher pipher = Cipher.getInstance("RSA");
                		    pipher.init(Cipher.ENCRYPT_MODE, pubkey);
                		    byte[] krypteradAESNyckel= pipher.doFinal(symmetriskNyckel);
                		    System.out.println("get raw key byte: "+krypteradAESNyckel);
                		    
                		    String secretKeyEncoded = Base64.getEncoder().encodeToString(krypteradAESNyckel);
                		    System.out.println("secret key encode with pubkey: "+ secretKeyEncoded);
                		    
                		}
                	}
                	break;
                	

				default:				
					log.info("invalid command");

			}

		} catch (java.util.InputMismatchException e) {
			log.info("invalid command");
			e.printStackTrace();
//    		this.CloseConnection();

		}
		log.info("Sending to " + hostport + " "+response);
//    	return response;
	}
	
	
	
	
	
	public static PublicKey getPublicKey(String key) throws InvalidKeySpecException, NoSuchAlgorithmException {
		KeyFactory kf = KeyFactory.getInstance("RSA");

        X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(key));

        PublicKey pubKey = kf.generatePublic(keySpecX509);
        
		return pubKey;


	}

		


}
