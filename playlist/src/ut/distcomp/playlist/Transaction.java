package ut.distcomp.playlist;

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
	public final int DECISION_TIMEOUT = 2000;
	
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
	}
	
	@Override
	public void run() {
		lock.lock();
		while(state != STATE.COMMIT && state != STATE.ABORT) {
			
			if (state == STATE.RESTING) {
				// If we have come here, it means that we just received a VOTE-REQ.
				// If we don't like the song, we will simply abort.
				if (!decision) {
					state = STATE.ABORT;
					process.config.logger.warning("Transaction aborted. Not ready for this message.");
					if(sendAbort) {
						Message msg = new Message(process.processId, MessageType.NO, " ");
						process.controller.sendMsg(process.coordinatorProcessNumber, msg.toString());
					}
				} else {
					// Send coordinator a YES.
					state = STATE.UNCERTAIN;
					Message msg = new Message(process.processId, MessageType.YES, " ");
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
								// RUN TERMINATION PROTOCOL.
							}
							lock.unlock();
						}
					};
					th.start();
				}
			} else if (state == STATE.UNCERTAIN) {
				if (message.type == MessageType.ABORT) {
					state = STATE.COMMIT;
					process.config.logger.info("Transaction aborted. Co-ordinator sent an abort." );
				}
				else if (message.type == MessageType.PRE_COMMIT) {
					state = STATE.COMMITABLE;
					
					Message msg = new Message(process.processId, MessageType.ACK, " ");
					process.controller.sendMsg(process.coordinatorProcessNumber, msg.toString());
					process.config.logger.warning("Update state to COMMITABLE after sending ACK.");
					
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
				if (message.type == MessageType.COMMIT) {
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

	public void update(Message message) {
		lock.lock();
		
		this.message = message;
		System.out.println("Received: " + this.message);
		nextMessageArrived.signal();
		
		lock.unlock();
	}

}
