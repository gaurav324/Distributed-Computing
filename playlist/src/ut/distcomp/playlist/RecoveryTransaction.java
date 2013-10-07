package ut.distcomp.playlist;

import ut.distcomp.playlist.TransactionState.STATE;

public class RecoveryTransaction extends Transaction {
	STATE otherState;
	
	boolean commitFlag = false;
	boolean abortFlag = true;
	
	boolean isTotalFailure = true;
		
	public RecoveryTransaction(Process process, Message msg) {
		super(process, msg);
		
		this.state = STATE.RECOVERING;
		this.otherState = STATE.RESTING;
		
		this.BUFFER_TIMEOUT = 2000;
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
		while(state != STATE.COMMIT || state != STATE.ABORT ) {

			// Get connected properly.
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
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
						
						if (isTotalFailure) {
							// We would now wait for either COMMIT/ABORT message.
							// We would have to be super smart here.
						}
					}
				};
				th.start();
			} else if (otherState == STATE.STATE_ENQUIRY_WAIT) {
				if (message.type == MessageType.STATE_COMMIT) {
					commitFlag = true;
					process.dtLogger.write(STATE.COMMIT, command);
					state = STATE.COMMIT;
					process.config.logger.info("Received: " +  message.toString());
					process.config.logger.info("Transaction committed.");
					break;
				} else if (message.type == MessageType.STATE_ABORT) {
					abortFlag = true;
					process.dtLogger.write(STATE.ABORT, command);
					state = STATE.ABORT;
					process.config.logger.info("Received: " +  message.toString());
					process.config.logger.info("Transaction aborted.");
					break;
				} else if (message.type != MessageType.STATE_RECOVERING) {
					isTotalFailure = false;
				} 
			} else { // We are indefinitely waiting for a decision.
				if (message.type == MessageType.COMMIT) {
					commitFlag = true;
					process.dtLogger.write(STATE.COMMIT, command);
					state = STATE.COMMIT;
					process.config.logger.info("Received: " +  message.toString());
					process.config.logger.info("Transaction committed.");
					break;
				} else if (message.type == MessageType.ABORT) {
					abortFlag = true;
					process.dtLogger.write(STATE.ABORT, command);
					state = STATE.ABORT;
					process.config.logger.info("Received: " +  message.toString());
					process.config.logger.info("Transaction aborted.");
					break;
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
}
