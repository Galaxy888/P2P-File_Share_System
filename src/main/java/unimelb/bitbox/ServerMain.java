package unimelb.bitbox;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import unimelb.bitbox.protocol.Protocol;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain implements FileSystemObserver {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;
	private String newEvent = null;
	private Eventmanager manager;


	public ServerMain(Eventmanager manager) throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);
		this.manager = manager;
	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		Protocol protocol = new Protocol();
		manager.newevent.offer(protocol.genergateFileSystemEventMessage(fileSystemEvent));
	}


	public String getNewEvent() {
		return newEvent;
	}
}
