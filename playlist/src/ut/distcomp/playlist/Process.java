package ut.distcomp.playlist;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import ut.distcomp.framework.Config;
import ut.distcomp.framework.NetController;
import ut.distcomp.framework.Queue;

public class Process {
	// Heartbeat pumping time gap in milli seconds. 
	public static final int HEARTBEAT_PUMP_TIME = 500;
	
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
	
	// Map of the UP Processes. ProcessId to time last updated.
	Hashtable<Integer, Long> upProcess;
	
	public Process(int processId) {
		this.processId = processId;
		this.configName = System.getProperty("CONFIG_NAME");
		this.upProcess = new Hashtable<Integer, Long>();
		
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
		
		// Start receiving messages from other process.
		me.startReceivingMessages();
		
		// Start clearing Up Processes from which a hearBeat 
		// was not received in a delta amount of time.
		me.startClearingDeadProcess();
			
	}
	
	public void pumpHeartBeat() {
		final Message heartBeat = new Message(this.processId, MessageType.HEARTBEAT, " ");
		 
        Thread th = new Thread() {
        	public void run() {
        		// XXXX
        		// Wait for 5 seconds initially to let everyone come up.
        		try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
        		while(true) {
        			controller.sendMsgs(heartBeat.toString());
        			try {
						Thread.sleep(HEARTBEAT_PUMP_TIME);
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
			Thread th = new Thread() {
	        	public void run() {
	        		while(true) {
		        		String msg = queue.poll();
		    			Message message = Message.parseMsg(msg);
		    			
		    			switch(message.type) {
		    				case HEARTBEAT: {
		    					updateProcessList(message);
		    				} // End of Heartbeat case.
		    			}
	        		}
	        	}
	        };
	        
	        th.start();
	}
	
	// Update the list processes.
	public void updateProcessList(Message message) {
		if (!upProcess.containsKey(message.process_id)) {
			System.out.println(String.format("Adding %d to the upProcess list", message.process_id));
		}
		upProcess.put(message.process_id, System.currentTimeMillis());
	}
	
	// Clears the processes which are dead/non-responsive.
	public void startClearingDeadProcess() {
        Thread th = new Thread() {
        	public void run() {		
        		while(true) {
	        		try {
						Thread.sleep(HEARTBEAT_PUMP_TIME);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	        		for (Iterator<Map.Entry<Integer, Long>> i = upProcess.entrySet().iterator(); i.hasNext(); ) {
	        	        Map.Entry<Integer, Long> entry = i.next();
	        	        
	        	        if (System.currentTimeMillis() - entry.getValue() > (HEARTBEAT_PUMP_TIME + 500)) {
	        	            i.remove();
	        	            config.logger.warning(String.format("Process %d seems to dead.", entry.getKey()));
	        	        }
	        	    }
        		}
        	}
        };
        
        th.start(); // Start the thread.
	}
}

