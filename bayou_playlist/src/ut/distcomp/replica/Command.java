package ut.distcomp.replica;

import java.io.Serializable;
import java.util.Comparator;

public class Command implements Serializable {
	int CSN;
	long acceptStamp;
	String serverId;
	Operation operation;
	
	public static final String SEPARATOR = "~~";
	
	public Command(int CSN, long acceptStamp, String serverId, Operation operation) {
		this.CSN = CSN;
		this.acceptStamp = acceptStamp;
		this.serverId = serverId;
		this.operation = operation;
	}
	
	public void setSequenceNumber(int CSN) {
		this.CSN = CSN;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
	    if (other == this) return true;
	    if (!(other instanceof Command))return false;
		Command c = (Command) other;
		
		if (c.CSN == this.CSN && 
				c.acceptStamp == this.acceptStamp && 
				c.serverId.equals(this.serverId)) { /** && 
				c.operation.equals(this.operation)) { */ //Ignoring this probably because we dont need to compare the operation also.
			return true;
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		StringBuilder command = new StringBuilder();
		command.append(this.CSN);
		command.append(Command.SEPARATOR);
		command.append(this.acceptStamp);
		command.append(Command.SEPARATOR);
		command.append(this.serverId);
		command.append(Command.SEPARATOR);
		String opString;
		if (this.operation instanceof AddRetireOperation) {
			opString = ((AddRetireOperation)this.operation).toString();
		} else {
			opString = this.operation.toString();
		}
		command.append(opString);
		
		return command.toString();
	}
	
	public static Command fromString(String command) {
		String[] commandSplit = command.split(Command.SEPARATOR);
		Replica.config.logger.fine("Going to construct Command from: " + command);

		try {
			Command newCmd = new Command(Integer.parseInt(commandSplit[0]), 
					Integer.parseInt(commandSplit[1]), 
					commandSplit[2],
					AddRetireOperation.operationFromString(commandSplit[3]));
			return newCmd;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return null;
	}
}

class CommandComparator implements Comparator<Command>
{
    public int compare(Command c1, Command c2)
    {
        if (c1.CSN != c2.CSN) {
        	return c2.CSN > c1.CSN ? -1 : 1;
        } else if (c1.acceptStamp != c2.acceptStamp) {
        	return c2.acceptStamp > c1.acceptStamp ? -1 : 1;
        } else if (c1.serverId != c2.serverId) {
        	return c1.serverId.compareTo(c2.serverId);
        }
        
        return 0;
    }
}
