package unimelb.bitbox;

import unimelb.bitbox.protocol.Protocol;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import static unimelb.bitbox.Peer.syncInterval;

public class P2PClientThread extends Thread{

    private static Logger log = Logger.getLogger(P2PClientThread.class.getName());

    private P2PClient client;
    private Socket socket;
    protected BufferedReader input = null;
    protected BufferedWriter output = null;
    private String host;
    private int port;
    private ServerMain serverMain;
    private Eventmanager manager;
    Timer t;

    public P2PClientThread(Socket socket, P2PClient client, ServerMain serverMain, String host,int port, BufferedReader input,  BufferedWriter output, Eventmanager manager) throws IOException {
        this.socket = socket;
        this.client = client;
        this.serverMain=serverMain;
        this.host = host;
        this.port=port;
        this.manager = manager;
        this.input = input;
        this.output = output;

        t = new Timer();
    }

    public void run(){
//        while (true){
        t.scheduleAtFixedRate(
                new TimerTask()
                {
                    public void run()
                    {
                        ArrayList<FileSystemManager.FileSystemEvent> fileSystemEvents = serverMain.fileSystemManager.generateSyncEvents();
                        Protocol protocol = new Protocol();
//        String fileSystemEventsMessage =  protocol.genergateFileSystemEventsMessage(fileSystemEvents);
                        ArrayList<String> fileSystemEventsMessage = protocol.genergateFileSystemEventsMessage(fileSystemEvents);
//                        System.out.println(fileSystemEventsMessage.size());
                        for (String event: fileSystemEventsMessage){
                            //eventqueue.offer(event);
                            manager.eventqueue1.offer(event);

                        }
//                            System.out.println(eventqueue);
                    }
                },
                0,      // run first occurrence immediatetly
                syncInterval);




        try {
            Thread t = new Thread(() -> clientToServer());
            t.start();


            Thread t2 = new Thread(() -> clientToServerInput());
            t2.start();

        } catch (Exception e) {
            log.info("Stopped");
            e.printStackTrace(); }
    }


    private  void clientToServer(){
        try {
            while(true)
            {
                String message = manager.eventqueue1.take();

                output.write(message+ "\n");
                log.info("sending: "+ message);
                output.flush();

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clientToServerInput(){
        try {
            while(true)
            {
                String inputMessage = input.readLine();
                log.info("Receive: "+ inputMessage);
                String response = process(inputMessage);

                if(!response.equals("")){
                    manager.eventqueue1.offer(response);
                }
            }
        } catch (IOException e) {
//            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
        }

    }

    private String process(String inputMessage) throws NoSuchAlgorithmException, IOException {
        String response = "";
        Protocol protocol = new Protocol();
        HostPort hostport=new HostPort(host,port);
        String md5 = " ", host = " ", pathName = " ", content= " ";
        long fileSize = 0, time = 0, position = 0;
        int port;
        try {
            long block_size = Long.parseLong(Configuration.getConfigurationValue("blockSize").trim());
            Document inputMessageDoc = Document.parse(inputMessage);
            String command = inputMessageDoc.getString("command");
//            System.out.println("command: " + command);
            switch(command) {
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

                                    break;
                                }

                                if (!this.serverMain.fileSystemManager.checkShortcut(pathName)) {
                                    long readLength;
                                    if (fileSize==0) {
                                        response= protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "file loader ready", true);

                                        this.serverMain.fileSystemManager.checkWriteComplete(pathName);
                                        break;

                                    } else if (fileSize <= block_size) {
                                        readLength = fileSize;
                                    } else {
                                        readLength = block_size;
                                    }
//                                    log.info("file_byte request sent");
                                    response = protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "file loader ready", true);


                                    response= protocol.genergateFileBytesRequest(fileDescriptorFCReq, pathName, 0, readLength);

                                }else {
                                    this.serverMain.fileSystemManager.cancelFileLoader(pathName);
//                                    log.info("shortcut");

                                    response= protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "file loader ready", true);


                                }

                            } catch (Exception e) {
                                response= protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "there was a problem creating the file", false);

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

                                    response = protocol.genergateFileBytesRequest(fileDescriptorFCReq, pathName, 0, readLength);

                                } else {
                                    this.serverMain.fileSystemManager.cancelFileLoader(pathName);
                                    response = protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "file loader ready", true);
                                }

                            } catch (Exception e) {
                                log.warning("file create failed");
                            }

                        } else {

                            response= protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "file loader ready", true);


                        }
                    } else {
//                        System.out.println("path not safe");
                        response= protocol.generateFileCreateResponseMessage(fileDescriptorFCReq, pathName, "unsafe pathname", false);

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
//                                    System.out.println("file_byte request sent");
                                    response = protocol.genergateFileBytesRequest(fileDescriptorFMReq, pathName, 0, readLength);


                                } else {
                                    this.serverMain.fileSystemManager.cancelFileLoader(pathName);
//                                    System.out.println("shortcut");
                                    response = protocol.generateFileModifyResponseMessage(fileDescriptorFMReq, pathName, "file loader ready", true);

                                }


                            } catch (Exception e) {
                                e.printStackTrace();
                                this.CloseConnection();
                                break;
                            }
                        } else {

                            response= protocol.generateFileModifyResponseMessage(fileDescriptorFMReq, pathName, "pathname does not exist", true);

                        }
                    } else {
//                        System.out.println("not safe path");
                        response= protocol.generateFileModifyResponseMessage(fileDescriptorFMReq, pathName, "unsafe pathname given", false);


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

                        log.info("size checked, byte response sent" );
                    } else {
                        log.info("The read length is bigger than file size");
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
                                }

                            } catch (Exception e) {
                                response = protocol.generateFileDeleteResponseMessage(fileDescriptorFDReq, pathName, "couldn't delete file", false);
                                e.printStackTrace();
                            }
                        } else {
//                            System.out.println("file does not exist");
                            response= protocol.generateFileDeleteResponseMessage(fileDescriptorFDReq, pathName, "file does not exist", false);
                        }
                    } else {
//                        System.out.println("not safe path");
                        response= protocol.generateFileDeleteResponseMessage(fileDescriptorFDReq, pathName, "unsafe pathname given", false);
                    }
                    break;


                case "DIRECTORY_DELETE_REQUEST":

                    pathName = inputMessageDoc.getString("pathName");
                    if (this.serverMain.fileSystemManager.isSafePathName(pathName)) {
                        if (this.serverMain.fileSystemManager.dirNameExists(pathName)) {
                            if(this.serverMain.fileSystemManager.deleteDirectory(pathName)) {
//                                System.out.println("directory deleted");
                                response= protocol.generateDirectoryDeleteResponseMessage(pathName, "directory deleted", true);
                            } else {
//                                System.out.println("directory delete fail");
                                response= protocol.generateDirectoryDeleteResponseMessage(pathName, "there was a problem deleting the directory", false);
                            }

                        }else {
                            response= protocol.generateDirectoryDeleteResponseMessage(pathName, "pathname does not exist", false);
                        }
                    } else {
                        response= protocol.generateDirectoryDeleteResponseMessage(pathName, "unsafe pathname given", false);

                    }


                    break;

                case "DIRECTORY_CREATE_REQUEST":
                    pathName = inputMessageDoc.getString("pathName");
                    if (this.serverMain.fileSystemManager.isSafePathName(pathName)) {
                        if (!this.serverMain.fileSystemManager.dirNameExists(pathName)) {
                            if(this.serverMain.fileSystemManager.makeDirectory(pathName)) {

                                response= protocol.generateDirectoryCreateResponseMessage(pathName, "directory created", true);
                            } else {
                                log.warning("directory delete fail");
                                response= protocol.generateDirectoryCreateResponseMessage(pathName, "there was a problem creating the directory", false);
                            }

                        }else {
                            response= protocol.generateDirectoryCreateResponseMessage(pathName, "pathname already exists", false);
                        }
                    } else {
                        response= protocol.generateDirectoryCreateResponseMessage(pathName, "unsafe pathname given", false);

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

                default:

                    log.info("invalid command");
                    this.CloseConnection();

            }

        } catch (java.util.InputMismatchException e) {
            log.info("invalid command");
//            e.printStackTrace();
            this.CloseConnection();

        }
        log.info("Sending to " + hostport + " "+response);
        return response;
    }

    private void CloseConnection() {
//        P2PServer.counterPeerNum--;
//		P2PServer.connectedHostPorts.remove()
//        System.out.println("Closing connection, Now have: " + P2PServer.counterPeerNum + " connections");

        try {
            this.input.close();
        } catch (Exception e) {
        }

        try {
            this.output.close();
        } catch (Exception e) {

        }

        try {
            this.socket.close();
        } catch (Exception e) {

        }
    }

}
