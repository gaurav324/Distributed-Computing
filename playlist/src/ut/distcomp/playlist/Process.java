package ut.distcomp.playlist;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import ut.distcomp.framework.Config;
import ut.distcomp.framework.NetController;
import ut.distcomp.framework.Queue;

public class Process {
	// Maintain your own playlist.
	Hashtable<String, String> playList;
	
	// Current process Id.
	int processId;
	
	// Manage your connections.
	NetController controller;
	
	// Location of the config File.
	String configName;
	
	// Instance that would read the configuration file.
	Config config;

	// Event queue for storing all the messages from the wire.
	final ConcurrentLinkedQueue<String> queue;
	
	// State of the current process. {RESTING, UNCERTAIN, COMMITABLE, DECIDED}
	ProcessState current_state;
	
	public Process(int processId) {
		this.processId = processId;
		this.configName = System.getProperty("CONFIG_NAME");
		
		try {
			this.config = new Config(this.configName);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.queue = new Queue<String>();
		this.controller = new NetController(this.processId, this.config, this.queue);
	}
	
	public static void main(String[] args) {
		
		// First argument would be the process number.
		Process me = new Process(Integer.parseInt(args[0]));
		
		// Start sending HeartBeats.
		me.pumpHeartBeat();
		
		me.startReceivingMessages();

//		while(true) {
//			try {
//				Thread.sleep(2000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			System.out.println(me.queue.poll());
//		}
			
	}
	
	public void pumpHeartBeat() {
		 final Message heartBeat = new Message(this.processId, MessageType.HEARTBEAT, " ");
		 
        Thread th = new Thread() {
        	public void run() {
        		while(true) {
        			controller.sendMsgs(heartBeat.toString());
        			try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
        	}
        };
        
	    th.start();
	}
	
	public void startReceivingMessages() {
		while(true) {
			String msg = this.queue.poll();
			
			Message message = Message.parseMsg(msg);
			
			switch(message.type) {
				case HEARTBEAT: System.out.println(message.toString());
			}
		}
	}
}

