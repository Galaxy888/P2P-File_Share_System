package unimelb.bitbox;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import unimelb.bitbox.protocol.Protocol;
import unimelb.bitbox.protocol.ProtocolInterface;
import unimelb.bitbox.util.*;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.ServerMain;


public class Peer implements ProtocolInterface
{
    private static Logger log = Logger.getLogger(Peer.class.getName());
    public static int syncInterval = Integer.parseInt(Configuration.getConfigurationValue("syncInterval")) *1000;
    //    private static ArrayList<String> fileSystemEventsMessage;

    //    public static BlockingQueue<String> events = new LinkedBlockingQueue<>();
//    private static ArrayList<String> newFileSystemEventsMessage;
    public static int eventnum = 0;

    public static void main( String[] args )throws IOException, NumberFormatException,Exception

    {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();
        Eventmanager eventmanager = new Eventmanager(Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections")));

        eventmanager.start();
//        new ServerMain();
        ServerMain serverMain = new ServerMain(eventmanager);

        // current peer
        String host = Configuration.getConfigurationValue("advertisedName");
        int port =  Integer.parseInt(Configuration.getConfigurationValue("port"));

        // peers
        String peers = Configuration.getConfigurationValue("peers");

        log.info("This peer port: "+ Configuration.getConfigurationValue("port"));

        P2PClient p2pClient = new P2PClient(peers,host,port,serverMain,eventmanager);
        p2pClient.start();

        P2PServer p2pServer = new P2PServer(port,host,serverMain, eventmanager);

        p2pServer.start();

    }



}
