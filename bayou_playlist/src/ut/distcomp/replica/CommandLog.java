package ut.distcomp.replica;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class CommandLog {
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
	public void add(Command cmd) {
		this.cmds.add(cmd);
	}
	
	// Get the maximum CSN.
	public int getMaxCSN() {
		// For non-primary replicas, it would return INTMAX.
		return cmds.get(cmds.size() - 1).CSN;
	}
	
	// First sort all the commands and write them to file.
	public void writeToFile() {
		sort();
		try {
			PrintWriter writer = new PrintWriter(this.commandLogFileName, "UTF-8");
			for(Command cmd: this.cmds) {
				writer.write(cmd.toString());
				writer.write("\n");
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	// Create a comparator and sort the cmds.
	public void sort() {
		Collections.sort(this.cmds, comparator);
	}
}