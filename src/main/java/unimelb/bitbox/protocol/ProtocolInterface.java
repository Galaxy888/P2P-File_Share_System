package unimelb.bitbox.protocol;

public interface ProtocolInterface {
    public static String INVALID_PROTOCOL = "INVALID_PROTOCOL";
    public static String CONNECTION_REFUSED = "CONNECTION_REFUSED";
    public static String HANDSHAKE_REQUEST = "HANDSHAKE_REQUEST";
    public static String HANDSHAKE_RESPONSE = "HANDSHAKE_RESPONSE";
    public static String FILE_CREATE_REQUEST = "FILE_CREATE_REQUEST";
    public static String FILE_CREATE_RESPONSE = "FILE_CREATE_RESPONSE";
    public static String FILE_DELETE_REQUEST = "FILE_DELETE_REQUEST";
    public static String FILE_DELETE_RESPONSE = "FILE_DELETE_RESPONSE";
    public static String FILE_MODIFY_REQUEST = "FILE_MODIFY_REQUEST";
    public static String FILE_MODIFY_RESPONSE = "FILE_MODIFY_RESPONSE";
    public static String DIRECTORY_CREATE_REQUEST = "DIRECTORY_CREATE_REQUEST";
    public static String DIRECTORY_CREATE_RESPONSE = "DIRECTORY_CREATE_RESPONSE";
    public static String DIRECTORY_DELETE_REQUEST = "DIRECTORY_DELETE_REQUEST";
    public static String DIRECTORY_DELETE_RESPONSE = "DIRECTORY_DELETE_RESPONSE";
    public static String FILE_BYTES_REQUEST = "FILE_BYTES_REQUEST";
    public static String FILE_BYTES_RESPONSE = "FILE_BYTES_RESPONSE";

    public static String AUTH_REQUEST = "AUTH_REQUEST";

    public static String FIELD_COMMAND = "command";
    public static String FIELD_MESSAGE = "message";
    public static String FIELD_PEERS = "peers";
    public static String FIELD_HOSTPORT = "hostPort";
    public static String FIELD_FILEDESCRIPTOR = "fileDescriptor";
    public static String FIELD_PATHNAME = "pathName";
    public static String FIELD_STATUS = "status";
    public static String FIELD_POSITION = "position";
    public static String FIELD_LENGTH = "length";
    public static String FIELD_CONTETN = "content";

}
