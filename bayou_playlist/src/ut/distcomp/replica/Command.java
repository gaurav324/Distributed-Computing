package ut.distcomp.replica;

public class Command {
	int CSN;
	int acceptStamp;
	String serverId;
	Operation operation;
	
	public static final String SEPARATOR = " || ";
	
	public Command(int CSN, int acceptStamp, String serverId, Operation operation) {
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
		command.append(SEPARATOR);
		command.append(this.acceptStamp);
		command.append(SEPARATOR);
		command.append(this.serverId);
		command.append(SEPARATOR);
		command.append(this.operation.toString());
		
		return command.toString();
	}
	
	public static Command fromString(String command) {
		String[] commandSplit = command.split(SEPARATOR);
		
		try {
			Command newCmd = new Command(Integer.parseInt(commandSplit[0]), 
					Integer.parseInt(commandSplit[1]), 
					commandSplit[2],
					Operation.operationFromString(commandSplit[3]));
			return newCmd;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return null;
	}
}
