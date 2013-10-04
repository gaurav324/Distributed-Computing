package ut.distcomp.playlist;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.Processor;

import ut.distcomp.playlist.TransactionState.STATE;

public class CoordinatorTransaction extends Transaction {
	private final int DECISION_TIMEOUT = 2000;
	
	// Set of processes from where I am expecting an response.
	Set<Integer> processWaitSet;
	
	// Set of processes which sent me a positive response.
	Set<Integer> positiveResponseSet;
	
	private boolean abortFlag = false;
	private String reasonToAbort;
	
	public CoordinatorTransaction(Process process, Message message) {
		super(process, message);
		processWaitSet = new HashSet<Integer>();
		positiveResponseSet = new HashSet<Integer>();
	}
	
	@Override
	public void run() {
		lock.lock();
		// WaitSize > 0 is being checked because we have to collect all the responses because aborting/commiting.
		while((state != STATE.COMMIT && state != STATE.ABORT) || processWaitSet.size() > 0) {
			
			if(state == STATE.RESTING) {
				// If we have come here, it means that we just received a new Transaction request.
				Message msg = new Message(process.processId, MessageType.VOTE_REQ, command);
				processWaitSet.addAll(process.upProcess.keySet());
				process.controller.sendMsgs(processWaitSet, msg.toString());
				
				// Update your state to waiting for all the decisions to arrive.
				state = STATE.WAIT_DECISION;
				
				// Timeout if all the process don't reply back with a Yes or No.
				Thread th = new Thread() {
					public void run(){
						try {
							Thread.sleep(DECISION_TIMEOUT);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (processWaitSet.size() > 0 || abortFlag) {
							if (processWaitSet.size() > 0) {
								reasonToAbort = "Did not get a reply from some processes.";
							}
							abortTransaction();
						}
					}
				};
				th.start();
			} // End of STATE.RESTING
			else if (state == STATE.WAIT_DECISION) {
				if (message.type == MessageType.YES) {
					processWaitSet.remove(message.process_id);
					positiveResponseSet.add(message.process_id);
					if (processWaitSet.size() == 0) {
						process.config.logger.info("Successfully got all the YES replies.");
					}
				} else if (message.type == MessageType.NO) {
					abortFlag = true;
					processWaitSet.remove(message.process_id);
					process.config.logger.info("Got a no from " + message.process_id);
					reasonToAbort = "Process " + message.process_id + " sent a NO !!";
				} else {
					process.config.logger.warning("Co-ordinator was waiting for YES/NO." + 
							" However got a " + message.type + ".");
				}
				
			} // End of STATE.WAIT_DECISION.
			
			try {
				// Wait until some other message is arrived.
				nextMessageArrived.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		lock.unlock();
	}
	
	public void update(Message message) {
		lock.lock();
		
		this.message = message;
		nextMessageArrived.signal();
		
		lock.unlock();
	}
	
	public void abortTransaction() {
		state = STATE.ABORT;
		process.config.logger.warning("Transaction aborted: " + reasonToAbort);
		
		Message msg = new Message(process.processId, MessageType.ABORT, command);
		process.controller.sendMsgs(positiveResponseSet, msg.toString());
		
		processWaitSet.clear();
		positiveResponseSet.clear();
	}
}
