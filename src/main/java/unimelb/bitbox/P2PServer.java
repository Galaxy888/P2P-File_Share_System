package unimelb.bitbox;

import unimelb.bitbox.util.HostPort;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class P2PServer extends Thread {
    private static Logger log = Logger.getLogger(P2PServer.class.getName());

    // Declare the port number
    private int port;
    private String host;

    private HostPort hostPort;


    private ServerMain serverMain;
    private Eventmanager manager;

    
    

    // Identifies the user number connected
    public static int counterPeerNum = 0;
    public static ArrayList<HostPort> connectedHostPorts = new ArrayList<HostPort>();

    private ServerSocket serverSocket;

    private Set<P2PServerThread> serverThreads = new HashSet<P2PServerThread>();

    public P2PServer(int port, String host, ServerMain serverMain, Eventmanager manager) throws IOException, NoSuchAlgorithmException{
        this.port = port;
        this.host = host;
        serverSocket = new ServerSocket(port);
        hostPort = new HostPort(host,port);
        this.serverMain = serverMain;
        this.manager = manager;


    }

    public void run(){
        try {
            while (true){

                Socket client = serverSocket.accept();
//                counterPeerNum++;

                // Start a new thread for a connection
                BlockingQueue<String> serverqueue1 = new LinkedBlockingQueue<>();
                manager.serverqueue.add(serverqueue1);
                P2PServerThread serverThread = new P2PServerThread(client, this, serverMain,serverqueue1);
                serverThreads.add(serverThread);
                serverThread.start();

            }

        }catch (IOException e) {
            e.printStackTrace();}
    }


    public Set<P2PServerThread> getServerThreads() {
        return serverThreads;
    }


    public HostPort getHostPort() {
        return hostPort;
    }


}
