package unimelb.bitbox;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

//import org.omg.Messaging.SYNC_WITH_TRANSPORT;
import unimelb.bitbox.protocol.Protocol;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

public class P2PClient extends Thread{

	private static Logger log = Logger.getLogger(P2PClient.class.getName());

	private String host;
	private int port;
	private String allPeers;
	private ServerMain serverMain;
	private Eventmanager manager;



	public P2PClient(String allPeers, String host, int port, ServerMain serverMain, Eventmanager eventmanager) throws IOException
	{
		this.allPeers = allPeers;
		this.host = host;
		this.port = port;
		this.serverMain = serverMain;
		this.manager = eventmanager;
	}

	public void run()
	{
		String[] peers = allPeers.split(",");
		Protocol protocol = new Protocol();
		String connectedhost=null;
		HostPort currenthostport;

		int connectedport=0;
		Queue<HostPort> queue = new LinkedList<HostPort>();
		for(int i=0; i<peers.length;i++){
			String peerHost = peers[i].split(":")[0];
			int peerPort = Integer.parseInt(peers[i].split(":")[1]);
			HostPort hostPort = new HostPort(peerHost,peerPort);
			queue.offer(hostPort);
		}
//		System.out.println("queue: "+ queue);

		while (!queue.isEmpty())
		{
			currenthostport = queue.poll();
			HostPort localHostPort = new HostPort(host,port);
			String handshake = protocol.generateHandshakeRequestMessage(localHostPort);
			String response = null;

			try {
				Socket socket = new Socket(currenthostport.host, currenthostport.port);

				BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
				BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

				response = Client(input, output, handshake);

				Document inputMessageDoc = Document.parse(response);
				String command = inputMessageDoc.getString("command");
				if (command.equals("CONNECTION_REFUSED"))
				{
					ArrayList<Document> connectedpeers = (ArrayList<Document>) inputMessageDoc.get("peers");
					for (Document connectedpeer : connectedpeers) {
						HostPort hostport1 = new HostPort(connectedpeer);
						queue.offer(hostport1);
					}

				}else if (command.equals("HANDSHAKE_RESPONSE"))
				{
					connectedhost = currenthostport.host;
					connectedport = currenthostport.port;
					log.info("connectedhostport:  "+ connectedhost+ "  "+connectedport);
					P2PClientThread clientThread = new P2PClientThread(socket,this, serverMain, connectedhost,connectedport,input, output, manager);
					clientThread.start();
					break;
				}
			} catch (IOException e) {
				log.info("Connection fail");
//				e.printStackTrace();
			}


		}

	}






	public String Client(BufferedReader input, BufferedWriter output,String SendMessage) {


		try{
			// Output Stream
			output.write(SendMessage + "\n");
			output.flush();

			while(true){
				String message = input.readLine();
				return message;

			}


		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {

		}
		return null;
	}

}
