package ut.paxos.bank;

import ut.paxos.downloaded.Command;

public class Message {
	public static final String SEPARATOR = "--";
	
	public final String clientName;
	public final MessageType type;
	public final String payLoad;
	
	Message(String clientName, MessageType type, String payLoad) {
		this.clientName = clientName;
		this.type = type;
		this.payLoad = payLoad;
	}
	
	public static Message parseMsg(String msg) {
		String[] parts = msg.split(SEPARATOR);
		
		String clientName = parts[0];
		
		String type = parts[1];
		MessageType msgType = MessageType.valueOf(type);
		
		String payload = parts[2];
		Message new_msg = new Message(clientName, msgType, payload);
		return new_msg;
	}
	
	public String toString() {
		StringBuilder value = new StringBuilder();
		value.append(this.clientName);
		value.append(this.SEPARATOR);
		value.append(this.type);
		value.append(this.SEPARATOR);
		value.append(this.payLoad);
		
		return value.toString();
	}
	
	public boolean equals(Message o) {
		return type.equals(o.type) && payLoad.equals(o.payLoad);
	}
}