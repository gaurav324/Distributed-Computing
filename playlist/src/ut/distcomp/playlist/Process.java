package ut.distcomp.playlist;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import ut.distcomp.framework.Config;
import ut.distcomp.framework.NetController;
import ut.distcomp.playlist.Transaction;
import ut.distcomp.playlist.TransactionState.*;

public class Process {
	// Heartbeat pumping time gap in milli seconds. 
	public static final int HEARTBEAT_PUMP_TIME = 5000;
	
	// Maintain your own playlist.
	Hashtable<String, String> playList;
	
	// Current process Id.
	int processId;
	
	// Manage your connections.
	NetController controller;
	
	// Location of the config File.
	String configName;
	
	// Instance that would read the configuration file.
	static Config config;

	// Event queue for storing all the messages from the wire.
	final ConcurrentLinkedQueue<String> queue;
	
	// Map of the UP Processes. ProcessId to time last updated.
	Hashtable<Integer, Long> upProcess;
	
	// Current transaction if any-running currently. At one time, we would 
	// only serve one single transaction like (Add, Delete or Update).
	Transaction activeTransaction;
	
	// This variable contains the process number of the coordinator.
	int coordinatorProcessNumber;
	
	//// VARIABLES FOR INTERACTION WITH THE SYSTEM ////
	static int delay;
	
	public Process(int processId) {
		this.processId = processId;
		this.configName = System.getProperty("CONFIG_NAME");
		delay = Integer.parseInt(System.getProperty("DELAY"));
		this.upProcess = new Hashtable<Integer, Long>();
		this.coordinatorProcessNumber = 0;
		
		try {
			Handler fh = new FileHandler("/tmp/" + processId + ".log");
			fh.setLevel(Level.FINEST);
			
			config = new Config(this.configName, fh);
			
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
        			controller.broadCastMsgs(heartBeat.toString());
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
		    					break;
		    				} // End of Heartbeat case.
		    				case ADD:
		    				case DELETE:
		    				case UPDATE: {
		    					if (coordinatorProcessNumber == processId) {
		    						if (activeTransaction != null) {
			    						config.logger.warning("A transaction is already running. Ignoring this request.");
			    						break;
		    						}
		    						startNewTransaction(message);
		    					} else {
		    						config.logger.warning("I am not the coordiantor. Don't send me: " + message.type);
		    					}
		    					break;
		    				} // End of ADD/DELTE/UPDATE case.
		    				case VOTE_REQ: {
		    					if (coordinatorProcessNumber == processId) {
		    						config.logger.warning(message.type + " sent by: " + message.process_id);
		    						config.logger.warning("There is something wrong. Coordiantor is not supposed to get " + message.type);
		    					} else {
		    						if (activeTransaction == null) {
		    							startNewTransaction(message);
		    						} else {
		    							config.logger.warning(message.type + " sent by: " + message.process_id);
			    						config.logger.warning("I should not get a VOTE-REQ if transaction is already running.");
		    						}
		    					}
		    					break;
		    				}
		    				case PRE_COMMIT:
		    				case COMMIT:
		    				case ABORT: {
		    					if (coordinatorProcessNumber == processId) {
		    						config.logger.warning(message.type + " sent by: " + message.process_id);
		    						config.logger.warning("There is something wrong. Coordiantor is not supposed to get " + message.type);
		    						break;
		    					} else {
		    						if (activeTransaction != null) {
		    							activeTransaction.update(message);
		    						} else {
		    							config.logger.warning(message.type + " sent by: " + message.process_id);
			    						config.logger.warning("I should not get a " +  message.type + " if transaction is not running.");
		    						}
		    						break;
		    					}
		    				} // End of messages received by the normal process.
		    				case YES:
		    				case NO:
		    				case ACK: {
		    					if (coordinatorProcessNumber != processId) {
		    						config.logger.warning(message.type + " sent by: " + message.process_id);
		    						config.logger.warning("There is something wrong. I am not coorindator. Coordinator should get " + message.type);
		    						break;
		    					} else {
		    						if (activeTransaction != null) {
		    							activeTransaction.update(message);
		    						} else {
		    							config.logger.warning(message.type + " sent by: " + message.process_id);
			    						config.logger.warning("I should not get a " +  message.type + " if transaction is not running.");
		    						}
		    					}
		    					break;
		    				}
		    			}
	        		}
	        	}
	        };
	        
	        th.start();
	}
	
	// Update the list processes.
	public void updateProcessList(Message message) {
		if (!upProcess.containsKey(message.process_id)) {
			//config.logger.info(String.format("Adding %d to the upProcess list", message.process_id));
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
	        	            //config.logger.warning(String.format("Process %d seems to dead. Clearing from up list.", entry.getKey()));
	        	        }
	        	    }
        		}
        	}
        };
        
        th.start(); // Start the thread.
	}
	
	public void startNewTransaction(Message message) {
		if (coordinatorProcessNumber == processId) {
			activeTransaction = new CoordinatorTransaction(this, message);
		} else {
			activeTransaction = new Transaction(this, message);
		}
		
		Thread thread = new Thread(activeTransaction);
		thread.start();
	}
	
	public void notifyTransactionComplete() {
		System.out.println("Transaction is complete. We are going to: " + activeTransaction.state);
	}
	
	public static void waitTillDelay() {
		try {
			config.logger.info("Waiting for " + Process.delay / 1000 + " secs.");
			Thread.sleep(Process.delay);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

