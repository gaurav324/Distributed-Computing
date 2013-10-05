package ut.distcomp.playlist;

import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ut.distcomp.playlist.TransactionState.STATE;

/**
 * This class would be used to track the state of the current running
 * transaction in the non-coordinator processes.
 * 
 * @author gnanda
 *
 */
public class Transaction implements Runnable {
	public int DECISION_TIMEOUT = 2000;
	
	// Reference to the process starting this transaction.
	Process process;
	
	// Message which is to be synchronized.
	String command; 

	// Next message to be processed.
	Message message;
	
	// State of the current process. {RESTING, UNCERTAIN, COMMITABLE, COMMIT, ABORT}
	STATE state = STATE.RESTING;
	
	/* 
	 * XXXX FLAGS TO BE CONTROLLED FROM OUTSIDE XXXX
	 */

	// This variable is for telling whether a process wants to 
	// accept this transaction or not.
	public boolean decision = true;
	
	// This is to determine whether to send an abort decision to coordinator or not.
	public boolean sendAbort = true;
	
	protected final Lock lock = new ReentrantLock();
	protected final Condition nextMessageArrived = lock.newCondition();
	
	public Transaction(Process process, Message message) {
		this.process = process;
		this.command = message.payLoad;
		this.message = message;
		this.DECISION_TIMEOUT = DECISION_TIMEOUT + process.delay;
	}
	
	@Override
	public void run() {
		lock.lock();
		while(state != STATE.COMMIT && state != STATE.ABORT) {
			
			if (state == STATE.RESTING) {
				// If we have come here, it means that we just received a VOTE-REQ.
				// If we don't like the song, we will simply abort.
				if (!decision) {
					process.dtLogger.write(STATE.ABORT);
					state = STATE.ABORT;
					process.config.logger.warning("Transaction aborted. Not ready for this message.");
					if(sendAbort) {
						process.config.logger.info("Received: " + message.toString());
						Message msg = new Message(process.processId, MessageType.NO, " ");
						Process.waitTillDelay();
						process.config.logger.info("Going to send No.");
						process.controller.sendMsg(process.coordinatorProcessNumber, msg.toString());
					}
				} else {
					// Send coordinator a YES.
					process.dtLogger.write(STATE.UNCERTAIN);
					state = STATE.UNCERTAIN;
					process.config.logger.info("Received: " + message.toString());
					Message msg = new Message(process.processId, MessageType.YES, " ");
					Process.waitTillDelay();
					process.config.logger.info("Going to send Yes.");
					process.controller.sendMsg(process.coordinatorProcessNumber, msg.toString());
					process.config.logger.warning("Update state to UNCERTAIN after sending a YES.");
					
					// Timeout if all the process don't reply back with a Yes or No.
					Thread th = new Thread() {
						public void run(){
							try {
								Thread.sleep(DECISION_TIMEOUT);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							lock.lock();
							if (state == STATE.UNCERTAIN) {
								electCordinator();
								// RUN TERMINATION PROTOCOL.
							}
							lock.unlock();
						}
					};
					th.start();
				}
			} else if (state == STATE.UNCERTAIN) {
				if (message.type == MessageType.ABORT) {
					process.dtLogger.write(STATE.ABORT);
					state = STATE.ABORT;
					process.config.logger.info("Transaction aborted. Co-ordinator sent an abort." );
				}
				else if (message.type == MessageType.PRE_COMMIT) {
					process.config.logger.info("Received: " + message.toString());
					process.config.logger.warning("Updated state to COMMITABLE.");
					state = STATE.COMMITABLE;
					
					Message msg = new Message(process.processId, MessageType.ACK, " ");
					Process.waitTillDelay();
					process.config.logger.info("Going to send Acknowledgment.");
					process.controller.sendMsg(process.coordinatorProcessNumber, msg.toString());
					
					// Timeout if all the process don't reply back with a Yes or No.
					Thread th = new Thread() {
						public void run(){
							try {
								Thread.sleep(DECISION_TIMEOUT);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							lock.lock();
							if (state == STATE.COMMITABLE) {
								// RUN TERMINATION PROTOCOL.
								electCordinator();
							}
							lock.unlock();
						}
					};
					th.start();
				} else {
					process.config.logger.warning("Was expecting either an ABORT or PRE_COMMIT." +
							"However, received a: " + message.type);
				}
			} else if (state == STATE.COMMITABLE) {
				process.config.logger.info("Received: " + message.toString());
				if (message.type == MessageType.COMMIT) {
					process.dtLogger.write(STATE.COMMIT);
					state = STATE.COMMIT;
					process.config.logger.info("Transaction Committed.");
				} else {
					process.config.logger.warning("Was expecting only a COMMIT message." +
							"However, received a: " + message.type);
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

	private void electCordinator() {
		Integer[] keys = (Integer[]) process.upProcess.keySet().toArray(new Integer[0]);
		Arrays.sort(keys);
		
		System.out.println(this.process.processId + " is electing new coordinator");
		System.out.println("New coordinator is " + keys[0]);
	}

	public void update(Message message) {
		lock.lock();
		
		this.message = message;
		nextMessageArrived.signal();
		
		lock.unlock();
	}

}
