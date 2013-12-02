package ut.distcomp.replica;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import ut.distcomp.communication.NetController;

public class CommandLog {
	static final String COMMAND_SEPARATOR = "#";
	final String commandLogFileName;
	
	ArrayList<Command> cmds = new ArrayList<Command>();
	final CommandComparator comparator = new CommandComparator(); 
	
	// Empty Constructor.
	public CommandLog(String processId) {
		this.commandLogFileName = System.getProperty("LOG_FOLDER") + "/replicaLogs/" + processId + ".log";
	}
	
	// Create a copy of the list of commands.
	public CommandLog(ArrayList<Command> cmds, String processId) {
		this.commandLogFileName = System.getProperty("LOG_FOLDER") + "/replicaLogs/" + processId + ".log";
		this.cmds = new ArrayList<Command>(cmds);
	}
	
	// Add a command to the list of commands.
	public boolean add(Command cmd) {
		for (Command c : cmds) {
			if (c.acceptStamp == cmd.acceptStamp && c.serverId.equals(cmd.serverId)) {
				if (cmd.CSN != Integer.MAX_VALUE && c.CSN != cmd.CSN) {
					c.CSN = cmd.CSN;
					return true;
				}
				return false;
			}
		}
		if (Replica.isPrimary) {
			cmd.CSN = getMaxCSN() + 1;
		}
		
		this.cmds.add(cmd);
		return true;
	}
	
	// Get the maximum CSN.
	public int getMaxCSN() {
		// Only primary would query this.
		if (cmds.size() == 0) {
			return -1;
		}
		int maxCSN = -1;
		for (Command cmd : cmds) {
			if (cmd.CSN != Integer.MAX_VALUE && cmd.CSN > maxCSN) {
				maxCSN = cmd.CSN; 
			}
		}
		return maxCSN;
	}
	
	// First sort all the commands and write them to file.
	public void writeToFile(NetController controller) {
		sort();
		Replica.playlist.clear();
		try {
			PrintWriter writer = new PrintWriter(this.commandLogFileName, "UTF-8");
			for(Command cmd: this.cmds) {
				if (cmd.operation instanceof AddRetireOperation) {
					AddRetireOperation op = (AddRetireOperation)cmd.operation;
					if (op.type == OperationType.RETIRE_NODE) {
						controller.outSockets.remove(op.process_id);
					} else {
						if (!Replica.disconnectedNodes.containsKey(op.process_id)) {
							controller.outSockets.put(op.process_id, null);
							try {
								controller.config.ports.put(op.process_id, Integer.parseInt(op.port));
								controller.config.addresses.put(op.process_id, InetAddress.getByName(op.host));
							} catch (UnknownHostException e) {
								e.printStackTrace();
							}
						}
					}
				} else {
					Replica.playlist.performOperation(cmd.operation);
				}
				writer.write(cmd.toString());
				writer.write("\n");
			}
			writer.close();
		} catch (SongNotFoundException e) {
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	// Create a comparator and sort the cmds.
	public void sort() {
		Collections.sort(this.cmds, comparator);
	}
	
	public String serializeCommands() {
		StringBuilder builder = new StringBuilder();
		for (Command cmd: this.cmds) {
			builder.append(cmd.toString());
			builder.append(CommandLog.COMMAND_SEPARATOR);
		}
		if (builder.length() > 2) {
			return builder.substring(0, builder.length() - 1).toString();
		}
		return "X";
	}
	
	public static ArrayList<Command> deSerializeCommands(String cmdString) {
		String[] cmds = cmdString.split(CommandLog.COMMAND_SEPARATOR);
	
		ArrayList<Command> cmdList = new ArrayList<Command>();
		if (cmds[0].equals("X")) {
			return cmdList;
		}
		for (String cmd: cmds) {
			Command c = Command.fromString(cmd);
			if (c != null) {
				cmdList.add(c);
			}
		}
		
		return cmdList;
	}
}