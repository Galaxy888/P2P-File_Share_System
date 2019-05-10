package unimelb.bitbox.protocol;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.EVENT;
import unimelb.bitbox.util.FileSystemManager.FileDescriptor;
import unimelb.bitbox.util.HostPort;

import java.awt.*;
import java.util.ArrayList;

import static unimelb.bitbox.util.FileSystemManager.EVENT.*;

public class Protocol implements ProtocolInterface{


    public String genergateFileSystemEventMessage(FileSystemManager.FileSystemEvent fileSystemEvent){
        // string message in json form
        Document eventDoc = new Document();

        EVENT event = fileSystemEvent.event;
        FileDescriptor fileDescriptor = fileSystemEvent.fileDescriptor;
        String pathName = fileSystemEvent.pathName;

        if (event==FILE_CREATE){
            eventDoc.append("command",FILE_CREATE_REQUEST);
            eventDoc.append("fileDescriptor",fileDescriptor.toDoc());
            eventDoc.append("pathName",pathName);
        }else if(event==FILE_DELETE){
            eventDoc.append("command", FILE_DELETE_REQUEST);
            eventDoc.append("fileDescriptor",fileDescriptor.toDoc());
            eventDoc.append("pathName",pathName);

        }else if(event==FILE_MODIFY){
            eventDoc.append("command", FILE_MODIFY_REQUEST);
            eventDoc.append("fileDescriptor",fileDescriptor.toDoc());
            eventDoc.append("pathName",pathName);


        }else if(event==DIRECTORY_CREATE){
            eventDoc.append("command", DIRECTORY_CREATE_REQUEST);
            eventDoc.append("pathName",pathName);

        }else if(event==DIRECTORY_DELETE){
            eventDoc.append("command", DIRECTORY_DELETE_REQUEST);
            eventDoc.append("pathName",pathName);
        }

        return eventDoc.toJson();
    }


    public ArrayList<String> genergateFileSystemEventsMessage(ArrayList<FileSystemManager.FileSystemEvent> fileSystemEvents){

        ArrayList<String> eventsJson = new ArrayList<String>();

        for (int i = 0; i < fileSystemEvents.size(); i++) {

            String eventJson = genergateFileSystemEventMessage(fileSystemEvents.get(i));
            eventsJson.add(eventJson);
        }
        return eventsJson;
    }


    public String genergateFileBytesRequest(Document fileDescriptor, String pathName, long position, long length){
        // string message in json form
        Document eventDoc = new Document();

        eventDoc.append("command",FILE_BYTES_REQUEST);
        eventDoc.append("fileDescriptor", fileDescriptor);
        eventDoc.append("pathName", pathName);
        eventDoc.append("position", position);
        eventDoc.append("length", length);

        return eventDoc.toJson();
    }



    public String generateFileCreateResponseMessage(Document fileDescriptor, String pathName, String message, boolean status) {

        String command = FILE_CREATE_RESPONSE;
        // string message in json form
        Document messageJson = new Document();
        messageJson.append("command",command);
        messageJson.append("fileDescriptor",fileDescriptor);
        messageJson.append("pathName",pathName);
        messageJson.append("message",message);
        messageJson.append("status",status);
        // return message string
        return messageJson.toJson();
    }





    public String generateFileDeleteResponseMessage(Document fileDescriptor, String pathName, String message, boolean status) {

        String command = FILE_DELETE_RESPONSE;
        // string message in json form
        Document messageJson = new Document();
        messageJson.append("command",command);
        messageJson.append("fileDescriptor",fileDescriptor);
        messageJson.append("pathName",pathName);
        messageJson.append("message",message);
        messageJson.append("status",status);
        // return message string
        return messageJson.toJson();
    }


    public String generateFileBytesResponseMessage(Document fileDescriptor, String pathName, long position, long length, String content,  String message, boolean status) {

        String command = FILE_BYTES_RESPONSE;

        Document messageJson = new Document();
        messageJson.append("command", command);
        messageJson.append("fileDescriptor", fileDescriptor);
        messageJson.append("pathName", pathName);
        messageJson.append("position", position);
        messageJson.append("length", length);
        messageJson.append("content", content);
        messageJson.append("message", message);
        messageJson.append("status", status);
        return messageJson.toJson();

    }


    public String generateFileModifyResponseMessage(Document fileDescriptor, String pathName, String message, boolean status) {

        String command = FILE_MODIFY_RESPONSE;
        // string message in json form
        Document messageJson = new Document();
        messageJson.append("command",command);
        messageJson.append("fileDescriptor",fileDescriptor);
        messageJson.append("pathName",pathName);
        messageJson.append("message",message);
        messageJson.append("status",status);
        // return message string
        return messageJson.toJson();
    }



    public String generateDirectoryCreateResponseMessage(String pathName, String message, boolean status) {

        String command = DIRECTORY_CREATE_RESPONSE;

        Document messageJson = new Document();
        messageJson.append("command", command);
        messageJson.append("pathName", pathName);
        messageJson.append("message", message);
        messageJson.append("status", status);
        return messageJson.toJson();
    }


    public String generateDirectoryDeleteResponseMessage(String pathName, String message, boolean status) {

        String command = DIRECTORY_DELETE_RESPONSE;

        Document messageJson = new Document();
        messageJson.append("command", command);
        messageJson.append("pathName", pathName);
        messageJson.append("message", message);
        messageJson.append("status", status);
        return messageJson.toJson();
    }



    public String generateHandshakeRequestMessage(HostPort hostPort) {
        //get fields required for HANDSHAKE_REQUEST protocol
        String command = HANDSHAKE_REQUEST;
        // string message in json form
        Document messageJson = new Document();
        messageJson.append("command", command);
        messageJson.append("hostPort", hostPort.toDoc());
        // return message string
        return messageJson.toJson();
    }

    public String generateHandshakeResponseMessage(HostPort hostPort) {
        //get fields required for HANDSHAKE_RESPONSE protocol
        String command = HANDSHAKE_RESPONSE;
        // string message in json form
        Document messageJson = new Document();
        messageJson.append("command", command);
        messageJson.append("hostPort", hostPort.toDoc());
        // return message string
        return messageJson.toJson();
    }

    public String generateConncetionRefusedMessage(ArrayList<HostPort> hostPorts) {
        //get fields required for HANDSHAKE_RESPONSE protocol
        String command = CONNECTION_REFUSED;
        // string message in json form
        Document messageJson = new Document();
        messageJson.append("command", command);
        messageJson.append("message", "connection limit reached");
        ArrayList<Document> peersDocs = new ArrayList<Document>();
        for (HostPort peer: hostPorts){
            peersDocs.add(peer.toDoc());
        }
        messageJson.append("peers", peersDocs);
        // return message string
        return messageJson.toJson();
    }



}
