package ut.distcomp.replica;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
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
	
	// Am I the primary server.
	static boolean isPrimary;
		
	// Instance of the config associated with this replica.
	static Config config;
	
	// Event queue for storing all the messages from the wire. This queue would be passed to the NetController.
	final Queue<InputPacket> queue;

	// This is the container which would have data about all the commands stored.
	final CommandLog cmds;
	
	// This is where we maintain all the playlist.
	static final Playlist playlist = new Playlist();
	
	// Controller instance used to send messages to all the replicas.
	final NetController controller;
	
	static RetiringState retiringState = RetiringState.NOT_RETIRING;
	
	// When we request some nodes to retire, we maintain a count that how
	// many have replied.
	HashMap<String, InetAddress> retireRequestMap;
	
	// ProcessId of the server, for which this replica has stopped accepting
	// requests for more retires.
	String parentRetiringProcessId;
	
	public Replica(String processId) {
		this.processId = processId;
		this.cmds = new CommandLog(this.processId);
		
		try {
			Handler fh = new FileHandler(System.getProperty("LOG_FOLDER") + "/" + processId + ".log");
			fh.setLevel(Level.FINEST);
			
			config = new Config(System.getProperty("CONFIG_NAME"), fh);		
			
			if (this.processId.equals("0")) {
				isPrimary = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		// Start NetController and start receiving messages from other servers.
		this.queue = new Queue<InputPacket>();
		controller = new NetController(processId, config, queue);
	}
	
	public void startReceivingMessages() {
		Thread th = new Thread() {
			public void run() {
				while(true) {
					InputPacket msgPacket = queue.poll();
					String msg = msgPacket.msg;
					config.logger.fine("Trying to parse: " + msg);
					Message message = Message.parseMsg(msg);
		
					switch (message.type) {
						case OPERATION: {
							Operation op = Operation.operationFromString(message.payLoad);
							config.logger.info("Received: " + op.toString());
							try {
								// Update your memory.
								playlist.performOperation(op);
								
								// Update the log file. Therefore update list of commands in the memory first.
								int CSN = Integer.MAX_VALUE;
								if (isPrimary) {
									CSN = cmds.getMaxCSN();
									CSN += 1;
								}
								try {
									Thread.sleep(1000);
								} catch (Exception ex) {
									ex.printStackTrace();
								}
								long acceptStamp = System.currentTimeMillis()/1000;
								Command cmd = new Command(CSN, acceptStamp, processId, op);
								cmds.add(cmd);
								cmds.writeToFile(controller);
								
								String reply = acceptStamp + "==" + processId;
								try {
									msgPacket.out.write(reply.getBytes());
									msgPacket.out.flush();
								} catch (IOException e) {
									e.printStackTrace();
								}								
								
								// Once a message is written to the log file.
								// We can transfer that update to all the adjoining replicas.
								updateNeighbors(null);
							} catch (SongNotFoundException e) {
								System.out.println(e.getMessage());
								config.logger.warning(e.getMessage());
								try {
									msgPacket.out.write(e.getMessage().getBytes());
									msgPacket.out.flush();
								} catch (IOException ex) {
									ex.printStackTrace();
								}
								e.printStackTrace();
							}
							break;
						}
						case BECOME_PRIMARY:
						case ENTROPY: {
							config.logger.info("Received command set from: " + message.process_id);
							ArrayList<Command> entropyCmds = CommandLog.deSerializeCommands(message.payLoad);
							if (entropyCmds == null) {
								config.logger.warning("Could not parse command log coming from: " + message.process_id);
								break;
							}
							//System.out.println(entropyCmds.toString());
							boolean success = false; 
							for (Command cmd : entropyCmds) {
								// Add each of this command to my own set.
								success = cmds.add(cmd);
							}
							// If there is even atleast one addition, we would forward command to all other replicas
							// except myself and the process you sent this file.
							if (success) {
								cmds.writeToFile(controller);
								if (isPrimary) {
									// If primary received new messages, it must have assigned the CSN also.
									// Therefore, we need to tell this back to the sender.
									updateNeighbors(null);
								} else {
									// Otherwise, tell all except send sender.
									updateNeighbors(message.process_id);
								}
							}
							if (message.type == MessageType.BECOME_PRIMARY) {
								isPrimary = true;
							}
							break;
						}
						case READ: {
							config.logger.info("Replying back to the read.");
							String[] payLoad = message.payLoad.split("==");
							if (!payLoad[0].equals("X")) {
								long acceptStamp = Long.parseLong(payLoad[0]);
								String server_id = payLoad[1];
								
								if (!server_id.equals(processId)) {
									boolean entryFound = false;
									for (Command cmd : cmds.cmds) {
										if (cmd.acceptStamp == acceptStamp && cmd.serverId.equals(server_id)) {
											entryFound = true;
											break;
										}
									}
									if (!entryFound) {
										try {
											msgPacket.out.write("NO".getBytes());
											msgPacket.out.flush();
										} catch (IOException e) {
											e.printStackTrace();
										}
										break;
									}
								}
							}
							
							try {
								msgPacket.out.write(playlist.toString().getBytes());
								msgPacket.out.flush();
							} catch (IOException e) {
								e.printStackTrace();
							}
							break;
						}
						case DISCONNECT: {
							config.logger.info("Going to disconnect from the given servers.");
							String nodeToDisconnect = null;
							if (!message.payLoad.equals("X")) {
								nodeToDisconnect = message.payLoad;
								controller.disconnect(nodeToDisconnect);
							} else {
								controller.forgetAll();
							}
							break;
						}
						case CONNECT: {
							config.logger.info("Going to connect to the given set of servers.");
							String nodeToConnect = null;
							if (!message.payLoad.equals("X")) {
								nodeToConnect = message.payLoad;
								controller.connect(nodeToConnect);
							} else {
								controller.connectAll();
							}
							updateNeighbors(null);
							break;
						}
						case RETIRE: {
							if (retiringState == RetiringState.CANNOT_RETIRE) {
								try {
									msgPacket.out.write("Sorry cannot retire as in the middle of some other retiring".getBytes());
									msgPacket.out.flush();
								} catch (IOException e) {
									e.printStackTrace();
								}
								break;
							}
							config.logger.info("Going to retire.");
							
							if (controller.outSockets.size() == 1) {
								try {
									msgPacket.out.write("Cannot retire, as isolated from everyone.".getBytes());
									msgPacket.out.flush();
								} catch (IOException e) {
									e.printStackTrace();
								}
								break;
							}
							// Now we can update our state to requested retire.
							retiringState = RetiringState.REQUESTED_RETIRE;
							
							// Will ask all the neighbors to stop retiring and be my retire-GodFather.
							config.logger.info("Sending message to all neighbors to stop retiring.");
							Message stopRetireRequestMsg = new Message(processId, MessageType.STOP_RETIRING, "X");
							retireRequestMap = new HashMap<String, InetAddress>(config.addresses); 
							retireRequestMap.remove(processId);
							config.logger.info("Going to wait for : " + retireRequestMap.size() + " retire request responses.");
							controller.broadCastMsgs(stopRetireRequestMsg.toString(), null);
							break;
						}
						case STOP_RETIRING: {
							if (retiringState == RetiringState.CANNOT_RETIRE || retiringState == RetiringState.REQUESTED_RETIRE) {
								// It means I have already accepted somebody's request to stop retiring.
								Message m = new Message(processId, MessageType.STOP_RETIRING_REJECTED, "X");
								// Tell the sender, that I cannot wait for him.
								controller.sendMsg(message.process_id, m.toString());
								break;
							}
							// Otherwise, stop retiring and inform the sender.
							retiringState = RetiringState.CANNOT_RETIRE;
							
							Message m = new Message(processId, MessageType.STOP_RETIRING_APPROVED, "X");
							// Tell the sender, that I am waiting for him.
							parentRetiringProcessId = message.process_id;
							controller.sendMsg(message.process_id, m.toString());
							break;
						}
						case STOP_RETIRING_REJECTED: {
							if (retiringState == RetiringState.REQUESTED_RETIRE) {
								retireRequestMap.remove(message.process_id);
								// If everyone has said that, they cannot retire for me. I would simply request again.
								if (retireRequestMap.size() == 0) {
									// Will ask all the neighbors to stop retiring and be my retire-GodFather.
									config.logger.info("Sending message to all neighbors to stop retiring .");
									Message stopRetireRequestMsg = new Message(processId, MessageType.STOP_RETIRING, "X");
									retireRequestMap = new HashMap<String, InetAddress>(config.addresses); 
									retireRequestMap.remove(processId);
									config.logger.info("Going to wait for : " + retireRequestMap.size() + " retire request responses.");
									controller.broadCastMsgs(stopRetireRequestMsg.toString(), null);
									break;
								}
							} else {
								config.logger.info("Ignoring STOP_RETIRING_REJECTED.");
							}
							break;
						}
						case STOP_RETIRING_APPROVED: {
							if (retiringState == RetiringState.REQUESTED_RETIRE) {
								retiringState = RetiringState.RETIRE_REQUEST_APPROVED;
								
								// Tell the rest of the servers, to be free and enjoy.
								Message setFreeMessage = new Message(processId, MessageType.SET_FREE, "X");
								
								// Except the server, which sent us approved message.
								HashMap<String, Boolean> exceptProcess = new HashMap<String, Boolean>();
								exceptProcess.put(message.process_id, true);
								controller.broadCastMsgs(setFreeMessage.toString(), exceptProcess);
								
								// Do an Anti-Entropy Session with the same process.
								String payload = cmds.serializeCommands();
								
								Message antiEntropyMessage;
								if (isPrimary) {
									// This message would do two things, one of becoming primary and another of 
									antiEntropyMessage = new Message(processId, MessageType.BECOME_PRIMARY, payload);
								} else {
									antiEntropyMessage = new Message(processId, MessageType.ENTROPY, payload);
								}
								// Pass on your message.
								controller.sendMsg(message.process_id, antiEntropyMessage.toString());
								
								// Set the other server free. 
								controller.sendMsg(message.process_id, setFreeMessage.toString());
								
								try {
									Thread.sleep(1000);
								} catch (Exception ex) {
									ex.printStackTrace();
								}
								config.logger.info("Have performed anti-entropy and now exiting gracefully.");
								System.exit(0);
							}
							break;
						} 
						case SET_FREE: {
							// If the process which locked me is letting me free, then Hurray !!
							if (parentRetiringProcessId.equals(message.process_id)) {
								retiringState = RetiringState.NOT_RETIRING;
								parentRetiringProcessId = null;
							}
							controller.outSockets.remove(message.process_id);

							// Update the log file. Therefore update list of commands in the memory first.
							int CSN = Integer.MAX_VALUE;
							if (isPrimary) {
								CSN = cmds.getMaxCSN();
								CSN += 1;
							}
							try {
								Thread.sleep(1000);
							} catch (Exception ex) {
								ex.printStackTrace();
							}
							long acceptStamp = System.currentTimeMillis()/1000;
							AddRetireOperation op = new AddRetireOperation(OperationType.RETIRE_NODE, message.process_id, null, null);
							Command cmd = new Command(CSN, acceptStamp, processId, op);
							cmds.add(cmd);
							cmds.writeToFile(controller);
							
							String reply = acceptStamp + "==" + processId;
							try {
								msgPacket.out.write(reply.getBytes());
								msgPacket.out.flush();
							} catch (IOException e) {
								e.printStackTrace();
							}								
							
							// Once a message is written to the log file.
							// We can transfer that update to all the adjoining replicas.
							updateNeighbors(null);
						    break;
						}
						case JOIN: {
							// Reply back with combination of serverId and currentTimeStamp.
							long acceptStamp = System.currentTimeMillis()/1000;
							String newId = processId + "_" + acceptStamp;
							try {
								msgPacket.out.write(newId.getBytes());
								msgPacket.out.flush();
							} catch (IOException e) {
								e.printStackTrace();
							}
							String host = message.payLoad.split(Operation.SEPARATOR)[0];
							String port = message.payLoad.split(Operation.SEPARATOR)[1];
							AddRetireOperation op = new AddRetireOperation(OperationType.ADD_NODE, newId, host, port);
							
							// Update the log file. Therefore update list of commands in the memory first.
							int CSN = Integer.MAX_VALUE;
							if (isPrimary) {
								CSN = cmds.getMaxCSN();
								CSN += 1;
							}
							try {
								Thread.sleep(1000);
							} catch (Exception ex) {
								ex.printStackTrace();
							}
							Command cmd = new Command(CSN, acceptStamp, processId, op);
							cmds.add(cmd);
							cmds.writeToFile(controller);
							
							String reply = acceptStamp + "==" + processId;
							try {
								msgPacket.out.write(reply.getBytes());
								msgPacket.out.flush();
							} catch (IOException e) {
								e.printStackTrace();
							}								
							
							// Once a message is written to the log file.
							// We can transfer that update to all the adjoining replicas.
							updateNeighbors(null);
						    break;
						}
					}
				}
			}

			private void updateNeighbors(String exceptProcess) {
				String payload = cmds.serializeCommands();
				HashMap<String, Boolean> exceptMap = new HashMap<String, Boolean>();
				exceptMap.put(exceptProcess, true);
				if (payload != null) {
					Message msg = new Message(processId, MessageType.ENTROPY, payload);
					config.logger.fine("Going to broadcast command set: " + msg.toString());
					controller.broadCastMsgs(msg.toString(), exceptMap);
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
