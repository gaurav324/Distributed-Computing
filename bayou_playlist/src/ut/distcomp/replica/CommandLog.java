package ut.distcomp.replica;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class CommandLog {
	String commandLogFileName = System.getProperty("LOG_FOLDER") + "/replicaLogs/" +  Replica.processId + ".log";
	
	ArrayList<Command> cmds = new ArrayList<Command>();
	
	// Empty Constructor.
	public CommandLog() {
		
	}
	
	public CommandLog(ArrayList<Command> cmds) {
		this.cmds = new ArrayList<Command>(cmds);
	}
	
	public void writeToFile() {
		try {
			PrintWriter writer = new PrintWriter(this.commandLogFileName, "UTF-8");
			for(Command cmd: this.cmds) {
				writer.write(cmd.toString());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
}
