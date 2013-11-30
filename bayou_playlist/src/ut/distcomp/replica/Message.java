package ut.distcomp.replica;

public class Message {
	public static final String SEPARATOR = "--";
	
	// Process ID of the server sending the message. For clients it would be -1 or something like that.
	public final String process_id;
	
	// Type of the message.
	public final MessageType type;
	
	// Payload which would be different for different type of messages.
	public final String payLoad;
	
	Message(String process_id, MessageType type, String payLoad) {
		this.process_id = process_id; 
		this.type = type;
		this.payLoad = payLoad;
	}
	
	public static Message parseMsg(String msg) {
		String[] parts = msg.split(Message.SEPARATOR);

		String type = parts[1];
		MessageType msgType = MessageType.valueOf(type);
		
		String payload = parts[2];
		Message new_msg = new Message(parts[0], msgType, payload);
		return new_msg;
	}
	
	public String toString() {
		StringBuilder value = new StringBuilder();
		value.append(this.process_id);
		value.append(Message.SEPARATOR);
		value.append(this.type);
		value.append(Message.SEPARATOR);
		value.append(this.payLoad);
		
		return value.toString();
	}
}
