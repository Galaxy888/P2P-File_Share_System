package unimelb.bitbox;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import unimelb.bitbox.protocol.Protocol;
import unimelb.bitbox.util.HostPort;

import javax.crypto.KeyGenerator;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Client {


    public static void main( String[] args ) throws NoSuchAlgorithmException {

        HostPort severHostPort=null;
        HostPort peerHostPort=null;
        String command=null;
        String identity=null;


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
            if(argsBean.getPeer()!=null){
                peerHostPort = new HostPort(argsBean.getPeer());
            }
//
            identity = argsBean.getIdentity();
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
//        System.out.println("Peer: " + peerHostPort);
        System.out.println("identity: " + identity);


        Protocol protocol = new Protocol();

        String authRequest = protocol.generateAuthRequestMessage(identity);

        System.out.println(authRequest);
        String response = null;



        try{
            System.out.println(severHostPort.host+severHostPort.port);

            Socket socket = new Socket(severHostPort.host, severHostPort.port);


            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

            response = ClientSendReceiveMessage(input, output, authRequest);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }











    }




    public static String ClientSendReceiveMessage(BufferedReader input, BufferedWriter output, String SendMessage) {


        try{
            // Output Stream
            output.write(SendMessage + "\n");
            output.flush();

            while(true){
                String message = input.readLine();
                System.out.println("Client receive: "+ message);
                return message;

            }


        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {

        }
        return null;
    }












}
