package ut.paxos.downloaded;

import java.io.IOException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import ut.paxos.bank.Account;
import ut.paxos.bank.Client;
import ut.paxos.bank.Message;
import ut.paxos.bank.MessageType;
import ut.paxos.bank.State;

public class Replica extends Process {
	public ut.paxos.bank.State appState;
	ProcessId[] leaders;
	int slot_num = 1;
	Map<Integer /* slot number */, Command> proposals = new HashMap<Integer, Command>();
	Map<Integer /* slot number */, Command> decisions = new HashMap<Integer, Command>();

	// Testing variables.
	// myLeader variable is for testing the network partition thing and nothing else.
	Integer myLeader;
	
	// Variable to store when to execute a read only command.
	HashMap<Integer, ArrayList<Command>> readOnlyToExecute = new HashMap<Integer, ArrayList<Command>>();
	public Replica(Env env, ut.paxos.bank.State appState, ProcessId me, ProcessId[] leaders, String logFolder,
			Integer myLeader) {
		super(logFolder, env, me, null);
		this.appState = appState;
		this.leaders = leaders;
		env.addProc(me, this);
		
		this.myLeader = myLeader;
	}

	void propose(Command c) {
		Message msg = (Message)c.op;
		// Send read-only commands only to all the leaders without assigning any slot number.
		if (msg.type == MessageType.INQUIRY) {
			for (ProcessId ldr: leaders) {
				logger.info(me + " || " + "proposing: " + msg.type + "--" + msg.payLoad + " to Leader:" + ldr.name);
				sendMessage(ldr, new ReadRequestMessage(me, c));
			}
		} else if (!decisions.containsValue(c)) {
			for (int s = 1;; s++) {
				if (!proposals.containsKey(s) && !decisions.containsKey(s)) {
					proposals.put(s, c);
					System.out.println(me + "|| SLOT: " + s + " || " + "proposing: " + msg.type + "--" + msg.payLoad);
					for (ProcessId ldr: leaders) {
						if (ldr.name.equals("leader_" + myLeader) || myLeader == null) {
							logger.info(me + "|| SLOT: " + s + " || " + "proposing: " + msg.type + "--" + msg.payLoad + " to Leader:" + ldr.name);
							sendMessage(ldr, new ProposeMessage(me, s, c));
						}
					}
					break;
				}
			}
		}
	}

	void perform(Command c){
		for (int s = 1; s < slot_num; s++) {
			if (c.equals(decisions.get(s))) {
				slot_num++;
				return;
			}
		}
		Message msg = (Message)c.op;
		try {
			System.out.println( me + " || SLOT: " + slot_num + " || " + "performing: " + msg.type + "--" + msg.payLoad);
			logger.info( me + " || SLOT: " + slot_num + " || " + "performing: " + msg.type + "--" + msg.payLoad);
			
			Client x = this.appState.clients.get(msg.clientName);
			if (x == null) {
				throw new Exception("Not a valid client: " + msg.clientName);
			}
				
			String [] msgSplit = msg.payLoad.split("=");
			Account acc = x.accounts.get(Integer.parseInt(msgSplit[0]));
			if (acc == null) {
				throw new Exception("Not a valid account number " + msgSplit[0] + " for client: " + x.clientName);
			}
			
			// If this is a dummy message.
			for (Command cc: c.hiddenReadOnlyRequest) {
				try {
					Message readMsg = (Message)cc.op;
					cc.client.callBack(cc, me + " : " + acc.inquiry());
					System.out.println(acc.inquiry());
				} catch (Exception ex) {
					cc.client.callBack(c, ex.getMessage());
					ex.printStackTrace();
				}
			}
			
			switch(msg.type) {
				case DEPOSIT:
					if (msgSplit.length != 2) {
						c.client.callBack(c, "Not a valid command: " + msg.toString());
						System.out.println("Not a valid command: " + msg.toString());
						break;
					}
					acc.deposit(Double.parseDouble(msgSplit[1]));
					c.client.callBack(c, me + " || Deposited: " + msgSplit[1]);
					break;
				case WITHDRAW:
					if (msgSplit.length != 2) {
						c.client.callBack(c, "Not a valid command: " + msg.toString());
						System.out.println("Not a valid command: " + msg.toString());
						break;
					}
					acc.withDraw(Double.parseDouble(msgSplit[1]));
					c.client.callBack(c, me + " || Withdrew: " + msgSplit[1]);
					break;
				case TRANSFER:
					if (msgSplit.length != 4) {
						c.client.callBack(c, "Not a valid command: " + msg.toString());
						System.out.println("Not a valid command: " + msg.toString());
						break;
					}
					String toClientString = msgSplit[1];
					int toAccNumber = Integer.parseInt(msgSplit[2]);
					Client toClient = this.appState.clients.get(toClientString);
					if (toClient == null) {
						c.client.callBack(c, "Not a valid client: " + toClientString);
						System.out.println("Not a valid client: " + toClientString);
						break;
					}
					Account toAccount = toClient.accounts.get(toAccNumber);
					if (toAccount == null) {
						throw new Exception("Not a valid account number " + toAccount + " for client: " + toClientString);
					}
					acc.transfer(toAccount, Double.parseDouble(msgSplit[3]));
					c.client.callBack(c, me + " || Transfered: " + msgSplit[3] + " to " + toClientString + ":" + toAccNumber);
					break;
//				case INQUIRY:
//					if (msgSplit.length != 1) {
//						c.client.callBack(c, "Not a valid command: " + msg.toString());
//						System.out.println("Not a valid command: " + msg.toString());
//						break;
//					}
//					c.client.callBack(c, me + " : " + acc.inquiry());
//					System.out.println(acc.inquiry());
//					System.out.println(this.proposals.toString());
//					break;
			}
		} catch(Exception ex) {
			c.client.callBack(c, ex.getMessage());
			ex.printStackTrace();
		}
		
		slot_num++;
	}

	public void body(){
		System.out.println("Here I am: " + me);
		for (;;) {
			PaxosMessage msg = getNextMessage();

			if (msg instanceof RequestMessage) {
				RequestMessage m = (RequestMessage) msg;				
				propose(m.command);
			}
//			} else if (msg instanceof ReadRequestAttachMessage) {
//				ReadRequestAttachMessage m = (ReadRequestAttachMessage) msg;
//				ArrayList<Command> cmds = readOnlyToExecute.get(m.slot_number);
//				if (cmds == null) {
//					cmds = new ArrayList<Command>();
//				}
//				// We have already executed command for this slot number.
//				// Therefore, we can safely return the state from here.
//				// If someone has already returned a higher value, this would
//				// be ignored by the client.
//				if (slot_num == m.slot_number) {
//					try {
//						Message actualMsg = (Message)m.command.op;
//						Client x = this.appState.clients.get(actualMsg.clientName);
//						if (x == null) {
//							throw new Exception("Not a valid client: " + actualMsg.clientName);
//						}
//							
//						String [] msgSplit = actualMsg.payLoad.split("=");
//						Account acc = x.accounts.get(Integer.parseInt(msgSplit[0]));
//						if (acc == null) {
//							throw new Exception("Not a valid account number " + msgSplit[0] + " for client: " + x.clientName);
//						}
//						
//						logger.info("Slot number was equal to the read request attach message. Going to execute it.");
//						logger.info(me + "|| SLOT: " + slot_num + " || " + "performing: " 
//								+ actualMsg.type + "--" + actualMsg.payLoad);
//						m.command.client.callBack(m.command, me + " : " + acc.inquiry());
//						System.out.println(acc.inquiry());
//						System.out.println(this.proposals.toString());
//					}
//					catch (Exception ex) {
//						m.command.client.callBack(m.command, ex.getMessage());
//						ex.printStackTrace();
//					}
//				} else if (slot_num < m.slot_number) {
//					cmds.add(m.command);
//				} else {
//					// If this comamnd is for 10 slot num and we are at 12, 11th command must have had the same
//					// read only, and it would have executed this command.
//				}
//				// Second thing you check is that if a command has 
//				// a message in itself. If so, then we would have 
//				// to execute that message itself.
//			}
			else if (msg instanceof DecisionMessage) {				
				DecisionMessage m = (DecisionMessage) msg;
				if (decisions.containsKey(m.slot_number)) {
					Command x = decisions.get(m.slot_number);
					// This means this is for read-only purpose.
					if (m.command.client == null) {
						x.hiddenReadOnlyRequest = m.command.hiddenReadOnlyRequest;
					} else {
						x.client = m.command.client;
						x.op = m.command.op;
						x.req_id = m.command.req_id;
						if (m.command.hiddenReadOnlyRequest != null) {
							x.hiddenReadOnlyRequest = m.command.hiddenReadOnlyRequest;
						}
					}
				} else {
					decisions.put(m.slot_number, m.command);
				}
				
				
				for (;;) {
					Command c = decisions.get(slot_num);
					if (c == null) {
						break;
					}
					// If this is a dummy message.
					if (c.client == null) {
						for (Command cc: c.hiddenReadOnlyRequest) {
							try {
								Message readMsg = (Message)cc.op;
								Client x = this.appState.clients.get(readMsg.clientName);
								if (x == null) {
									throw new Exception("Not a valid client: " + readMsg.clientName);
								}
								String [] msgSplit = readMsg.payLoad.split("=");
								if (msgSplit.length != 1) {
									c.client.callBack(c, "Not a valid command: " + msg.toString());
									System.out.println("Not a valid command: " + msg.toString());
									break;
								}
								Account acc = x.accounts.get(Integer.parseInt(msgSplit[0]));
								if (acc == null) {
									throw new Exception("Not a valid account number " + msgSplit[0] + " for client: " + x.clientName);
								}
								cc.client.callBack(cc, me + " : " + acc.inquiry());
								System.out.println(acc.inquiry());
							} catch (Exception ex) {
								cc.client.callBack(c, ex.getMessage());
								ex.printStackTrace();
							}
						}
						break;
					}
					
					Command c2 = proposals.get(slot_num);
					if (c2 != null && !c2.equals(c)) {
						propose(c2);
					}
					perform(c);
//					// This is where we are sure that we can execute any 
//					// corresponding read only commands.
//					ArrayList<Command> cmds = readOnlyToExecute.get(m.slot_number);
//					if (cmds != null) {
//						for (Command cc: cmds) {
//							try {
//								Message readMsg = (Message)cc.op;
//								Client x = this.appState.clients.get(readMsg.clientName);
//								if (x == null) {
//									throw new Exception("Not a valid client: " + readMsg.clientName);
//								}
//								String [] msgSplit = readMsg.payLoad.split("=");
//								if (msgSplit.length != 1) {
//									c.client.callBack(c, "Not a valid command: " + msg.toString());
//									System.out.println("Not a valid command: " + msg.toString());
//									break;
//								}
//								Account acc = x.accounts.get(Integer.parseInt(msgSplit[0]));
//								if (acc == null) {
//									throw new Exception("Not a valid account number " + msgSplit[0] + " for client: " + x.clientName);
//								}
//								cc.client.callBack(cc, me + " : " + acc.inquiry());
//								System.out.println(acc.inquiry());
//							} catch (Exception ex) {
//								cc.client.callBack(c, ex.getMessage());
//								ex.printStackTrace();
//							}
//						}
//					}
				}
			}
			else {
				System.err.println("Replica: unknown msg type");
			}
		}
	}
}
