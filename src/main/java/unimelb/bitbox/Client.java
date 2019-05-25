package unimelb.bitbox;

//import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import sun.misc.BASE64Encoder;
import unimelb.bitbox.protocol.Protocol;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;


public class Client {


    public static void main( String[] args ) throws GeneralSecurityException, IOException {

        HostPort severHostPort=null;
        HostPort peerHostPort=null;
        String command=null;
        String identity=null;

        String AES128=null;


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
            identity = argsBean.getIdentity();

        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            //Print the usage to help the user understand the arguments expected
            //by the program
            parser.printUsage(System.err);
        }

//        System.out.println("Command: " + command);
//        System.out.println("Server: " + severHostPort);
//        System.out.println("Peer: " + peerHostPort);
//        System.out.println("identity: " + identity);



//////////////
//        String privKeyPEM=readFile("/Users/yuguo/.ssh/id_rsa");
//        privKeyPEM= privKeyPEM.replace("-----BEGIN RSA PRIVATE KEY-----", "").replace("\n", "");
//        // Remove the first and last lines
//        privKeyPEM = privKeyPEM.replace("-----END RSA PRIVATE KEY-----", "");
//        System.out.println(privKeyPEM);
//
//
//        // Base64 decode the data
//        byte [] encoded = Base64.decode(privKeyPEM);
//
//        // PKCS8 decode the encoded RSA private key
//        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
//        KeyFactory kf = KeyFactory.getInstance("RSA");
//        PrivateKey privKey = kf.generatePrivate(keySpec);
//
//        // Display the results
//        System.out.println(privKey);
////////////////


        // privateKey is in format: PKCS#8
        PrivateKey privateKey = (new PrivateKeyReader("/Users/yuguo/.ssh/id_rsa")).getPrivateKey();
        System.out.println(privateKey);
//        System.out.println("private key PKCS8："+new BASE64Encoder().encodeBuffer(privateKey.getEncoded()));

        Cipher pipher = Cipher.getInstance("RSA");

        // re-initialise the cipher to be in decrypt mode
        pipher.init(Cipher.DECRYPT_MODE, privateKey );

        // decrypt message
//        byte[] decrypt = pipher.doFinal(krypteradAESNyckel);



        Protocol protocol = new Protocol();

        String authRequest = protocol.generateAuthRequestMessage(identity);
//        System.out.println(authRequest);
//        String authResponse = null;

        try{
            System.out.println(severHostPort.host+severHostPort.port);

            Socket socket = new Socket(severHostPort.host, severHostPort.port);


            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

            String authResponse = ClientSendReceiveMessage(input, output, authRequest);


//            System.out.println("authResponse : "+ authResponse);

            String authResponseProcessed = process(authResponse);
            System.out.println("authResponseProcessed : "+ authResponseProcessed);

            if(!authResponseProcessed.equals("false")){

                String secretKey = decryptToGetSecretKey(authResponseProcessed,privateKey);
                System.out.println(secretKey);

                String commandJson = processCmd(command,peerHostPort);
                System.out.println("commandJson: "+ commandJson);

                String commandJsonEncryptSecretKey = encryptMessageSecretKey(commandJson,secretKey);

                String commandJsonEncryptSecretKeyJson = protocol.generatePayload(commandJsonEncryptSecretKey);

                String responseEncryptSecretKey = ClientSendReceiveMessage(input, output, commandJsonEncryptSecretKeyJson);

                // payload

                String responseProcessedWithoutPayload = process(responseEncryptSecretKey);

                String responseDecrypt = decryptMessageSecretKey(responseProcessedWithoutPayload, secretKey);


                String responseProcessed = process(responseDecrypt);

                System.out.println(responseProcessed);

            }

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



    public static String decryptToGetSecretKey(String message, PrivateKey privateKey){

        return message;

    }


//    public static String encryptMessageSecretKey(String message, String secretKey){
////        return message;
////    }
////    public static String decryptMessageSecretKey(String message, String secretKey){
////        return message;
////    }


    public static String encryptMessageSecretKey( String str, String key ) throws NoSuchAlgorithmException, NoSuchPaddingException /*throws Exception*/, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException{

        //SecretKey publicKey1 = new SecretKeySpec(key.getBytes(), "AES");
        SecretKey publicKey1 = new SecretKeySpec(Base64.getDecoder().decode(key), "AES");

        System.out.println("String, key: "+str+"   "+key);

        // AES encrypt
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE,  publicKey1);
        byte[] result = cipher.doFinal(str.getBytes());
        String outStr = Base64.getEncoder().encodeToString(result);
        return outStr;
    }
    public static String decryptMessageSecretKey(String str, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        //SecretKey publicKey1 = new SecretKeySpec(key.getBytes(), "AES");
        SecretKey publicKey1 = new SecretKeySpec(Base64.getDecoder().decode(key), "AES");

        //Base64 dncode
        byte[] content = Base64.getDecoder().decode(str);
        //AES decrypt
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE,  publicKey1);
        byte[] result = cipher.doFinal(content);
        String outStr = new String(result);
        return outStr;
    }

    public static String process(String inputMessage) throws NoSuchAlgorithmException, IOException{
        String response = "";
        Protocol protocol = new Protocol();
        boolean status;
        String AES128;

        try {
            Document inputMessageDoc = Document.parse(inputMessage);
            if(inputMessageDoc.getString("payload")!=null){
                return inputMessageDoc.getString("payload");

            }
            else {
                String command = inputMessageDoc.getString("command");

                switch (command) {
                    case "AUTH_RESPONSE":
                        status = inputMessageDoc.getBoolean("status");

                        if (status) {
                            /*String abs = inputMessageDoc.getString("AES128");
                            System.out.println(abs);
                            byte[] AES = Base64.getDecoder().decode(abs);
                            System.out.println(AES);
                            AES128 = new String(AES);*/
                            AES128 = inputMessageDoc.getString("AES128");
                            return AES128;
                        } else{
                            return "false";
                        }

                    case "LIST_PEERS_RESPONSE":
//                        return inputMessageDoc.getString("command");

                        ArrayList<Document> peersList = (ArrayList<Document>) inputMessageDoc.get("peers");

                        if(peersList.size()!=0){
                            for (Document peer : peersList) {
                                HostPort hostport = new HostPort(peer);
                                response += hostport + "\t";
                            }
                            return response;
                        }else{
                            return "No peer connected";
                        }






                    case "CONNECT_PEER_RESPONSE":

                        return inputMessageDoc.getString("message");

                    case "DISCONNECT_PEER_REQUEST":

                        return inputMessageDoc.getString("message");


                }
            }


        }catch (java.util.InputMismatchException e) {
//            log.info("invalid command");
            e.printStackTrace();
//    		this.CloseConnection();
        }
            return response;
        }



    static String readFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }





    public static String processCmd(String cmd, HostPort peerHostPort){

        String jsonMessage = "";
        Protocol protocol = new Protocol();

        if(cmd.equals("list_peers")){
            jsonMessage = protocol.generateListPeersRequestMessage();

        }else if(cmd.equals("connect_peer")){
            jsonMessage =protocol.generateConnectPeerRequestMessage(peerHostPort);

        }else if(cmd.equals("disconnect_peer")){
            jsonMessage =protocol.generateDisconnectPeerRequestMessage(peerHostPort);
        }

        return jsonMessage;

    }







}
