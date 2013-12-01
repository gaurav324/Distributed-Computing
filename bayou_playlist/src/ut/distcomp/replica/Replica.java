package ut.distcomp.replica;

import java.io.FileNotFoundException;
import java.io.IOException;
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
								cmds.writeToFile();
								
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
								cmds.writeToFile();
								if (isPrimary) {
									// If primary received new messages, it must have assigned the CSN also.
									// Therefore, we need to tell this back to the sender.
									updateNeighbors(null);
								} else {
									// Otherwise, tell all except send sender.
									updateNeighbors(message.process_id);
								}
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
