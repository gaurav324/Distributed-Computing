package ut.distcomp.replica;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import ut.distcomp.communication.Config;
import ut.distcomp.communication.NetController;
import ut.distcomp.util.Queue;

public class Replica {
	// Process Id attached to this replica. This is not the system ProcessID, but just an identifier to identify the server.
	static String processId;
		
	// Instance of the config associated with this replica.
	static Config config;
	
	// Event queue for storing all the messages from the wire. This queue would be passed to the NetController.
	final ConcurrentLinkedQueue<String> queue;

	// This is the container which would have data about all the commands stored.
	final CommandLog cmds = new CommandLog();
	
	public Replica(String processId) {
		this.processId = processId;
		
		try {
			Handler fh = new FileHandler(System.getProperty("LOG_FOLDER") + "/" + processId + ".log");
			fh.setLevel(Level.FINEST);
			
			config = new Config(System.getProperty("CONFIG_NAME"), fh);		
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		// Start NetController and start receiving messages from other servers.
		this.queue = new Queue<String>();
		NetController controller = new NetController(processId, config, queue);
	}
	
	public void startReceivingMessages() {
		Thread th = new Thread() {
			public void run() {
				while(true) {
					String message = queue.poll();
					
					Message message = Message.parseMsg(msg);
				}
			}
		};
		th.start();
	}
	
	public static void main(String[] args) {
		// Create your replica instance.
		Replica replica = new Replica(args[0]);
		
		// XXX There would be more to it, like getting yourself added to the system and getting retired. 
		
		// Start receiving messages.
		replica.startReceivingMessages();
			
		//System.out.println("Hello, world.");
	}
}
