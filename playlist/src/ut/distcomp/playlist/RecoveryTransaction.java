package ut.distcomp.playlist;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ut.distcomp.playlist.TransactionState.STATE;

public class RecoveryTransaction extends Transaction {
	STATE otherState;
	
	boolean isTotalFailure = true;
	
	Set<Integer> upProcessSet;
	Hashtable<Integer, Set<Integer>> allUpSets;
		
	public RecoveryTransaction(Process process, Message msg) {
		super(process, msg);
		
		this.state = STATE.RECOVERING;
		this.otherState = STATE.RESTING;
		
		upProcessSet = process.dtLogger.getLastUpProcessSet(process.processId);
		allUpSets = new Hashtable<Integer, Set<Integer>>();
		
		this.BUFFER_TIMEOUT = 5000;
		this.DECISION_TIMEOUT = process.delay + this.BUFFER_TIMEOUT;
	}
	
	/**
	 * Broadcast the STATE_ENQUIRY to all the active process.
	 *
	 * If all the replies are STATE.RECOVERING, it means a total failure is going on.
	 * 
	 * If even one of the process replies COMMIT or ABORT, we are done.
	 * Otherwise, it would mean that transaction is still going on and we 
	 * should eventually get a result.
	 * 
	 * However, it may happen that all the processes which are running currently
	 * may die immediately after responding. Therefore, we might not get a result
	 * for now. But, once other processes start coming up they would enquire my
	 * state and someone would be running TOTAL FAILURE case.
	 * 
	 * Then that total failure case would notify all that total failure has occurred 
	 * and they keep on checking periodically that we are out of total failure or not.
     */
	
	@Override
	public void run() {
		lock.lock();

		// Get connected properly.
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		while(state != STATE.COMMIT || state != STATE.ABORT ) {
			if (otherState == STATE.RESTING) {
				Message msg = new Message(this.process.processId, MessageType.STATE_ENQUIRY, command);
				
				otherState = STATE.STATE_ENQUIRY_WAIT;
				process.config.logger.info("Sending: " + msg.toString());
				process.controller.broadCastMsgs(msg.toString());
				
				Thread th = new Thread() {
					public void run() {
						try {
							Thread.sleep(DECISION_TIMEOUT);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						lock.lock();
						if (isTotalFailure) {
							if (isIntersectionPresent()) {
								process.dtLogger.write(STATE.ABORT, command);
								state = STATE.ABORT;
								process.config.logger.info("All process present in the intersection set are UP.");
								process.config.logger.info("Transaction aborted.");
								process.notifyTransactionComplete();
							} else {
								try {
									// WAIT for another 5 seconds and then re-try.
									lock.unlock();
									Thread.sleep(5000);
									lock.lock();
									otherState = STATE.RESTING;
									boolean isTotalFailure = true;
									nextMessageArrived.signal();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						allUpSets.clear();
						lock.unlock();
					}
				};
				th.start();
			} else if (otherState == STATE.STATE_ENQUIRY_WAIT) {
				if (message.type == MessageType.STATE_COMMIT) {
					process.dtLogger.write(STATE.COMMIT, command);
					state = STATE.COMMIT;
					process.config.logger.info("Received: " +  message.toString());
					process.config.logger.info("Transaction committed.");
					process.notifyTransactionComplete();
					break;
				} else if (message.type == MessageType.STATE_ABORT) {
					process.dtLogger.write(STATE.ABORT, command);
					state = STATE.ABORT;
					process.config.logger.info("Received: " +  message.toString());
					process.config.logger.info("Transaction aborted.");
					process.notifyTransactionComplete();
					break;
				} else if (message.type != MessageType.STATE_RECOVERING) { 
					// If I get any message, abort or commit = recovery complete.
					// If I get resting or uncertain, it also means that someone
					// is running the transaction.
					process.config.logger.info("Received: " +  message.toString());
					isTotalFailure = false;
				} else if (message.type == MessageType.COMMIT) {
						process.dtLogger.write(STATE.COMMIT, command);
						state = STATE.COMMIT;
						process.config.logger.info("Received: " +  message.toString());
						process.config.logger.info("Transaction committed.");
						process.notifyTransactionComplete();
						break;
				} else if (message.type == MessageType.ABORT) {
						process.dtLogger.write(STATE.ABORT, command);
						state = STATE.ABORT;
						process.config.logger.info("Received: " +  message.toString());
						process.config.logger.info("Transaction aborted.");
						process.notifyTransactionComplete();
						break;
				} else {
					process.config.logger.info("Received: " + message.toString());
					
					Set<Integer> upProcessSetRecived = new HashSet<Integer>();
					String payload = message.payLoad.substring(1, message.payLoad.length() - 1);
					String[] payload_split = payload.split(DTLog.UpSet_SEPARATOR);
					for(String proc_no: payload_split){
						upProcessSetRecived.add(Integer.parseInt(proc_no));
		   		    }
					
					allUpSets.put(message.process_id, upProcessSetRecived);
				}
			} 
			
			// Start waiting for the next message to come.
			try {
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
	
	private boolean isIntersectionPresent() {
		Set<Integer> intersection = new HashSet<Integer>(upProcessSet);
		intersection.add(process.processId);
		
		for (Map.Entry<Integer, Set<Integer>> entry : allUpSets.entrySet())
		{
			Set<Integer> temp = entry.getValue();
			temp.add((Integer) entry.getKey());
			
			intersection.retainAll(temp);
		}

		boolean isIntersectionPresent = true;
		allUpSets.put(process.processId, upProcessSet);
		for (Integer i : intersection) {
			if (!allUpSets.containsKey(i)) {
				isIntersectionPresent = false;
			}
		}
		
		return isIntersectionPresent;
	}
	
	public String getUpStates() {
		return upProcessSet.toString();
	}
}
