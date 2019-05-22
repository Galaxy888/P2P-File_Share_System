package unimelb.bitbox;
import org.kohsuke.args4j.Option;

public class CmdLineArgs {


    @Option(required = true, name = "-c", usage = "Command")
    private  String command;


    @Option(required = false, name = "-s", usage = "Server")
    private String server;

    @Option(required = false, name = "-p", usage = "Peer")
    private String peer;

    public String getCommand() {
        return command;
    }

    public String getServer() {
        return server;
    }

    public String getPeer() {
        return peer;
    }


}
