package unimelb.bitbox;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {


    public static void main( String[] args ){

        HostPort severHostPort=null;
        HostPort peerHostPort=null;
        String command=null;


        //Object that will store the parsed command line arguments
        CmdLineArgs argsBean = new CmdLineArgs();

        //Parser provided by args4j
        CmdLineParser parser = new CmdLineParser(argsBean);

        try {

            //Parse the arguments
            parser.parseArgument(args);

            //After parsing, the fields in argsBean have been updated with the given
            //command line arguments

            command = argsBean.getCommand();
            severHostPort = new HostPort(argsBean.getServer());
            peerHostPort = new HostPort(argsBean.getPeer());
//            System.out.println("Command: " + argsBean.getCommand());
//            System.out.println("Server: " + argsBean.getServer());
//            System.out.println("Peer: " + argsBean.getPeer());


        } catch (CmdLineException e) {

            System.err.println(e.getMessage());

            //Print the usage to help the user understand the arguments expected
            //by the program
            parser.printUsage(System.err);
        }



        System.out.println("Command: " + command);
        System.out.println("Server: " + severHostPort);
        System.out.println("Peer: " + peerHostPort);



        try{
            System.out.println(severHostPort.host+severHostPort.port);

            Socket socket = new Socket(severHostPort.host, severHostPort.port);


            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }








}
