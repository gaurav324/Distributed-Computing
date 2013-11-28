package ut.distcomp.playlist;

public class Message {
	public static final String SEPARATOR = "--";
	
	public final MessageType type;
	public final int process_id;
	public final String payLoad;
	
	Message(int process_id, MessageType type, String payLoad) {
		this.process_id = process_id; 
		this.type = type;
		this.payLoad = payLoad;
	}
	
	public static Message parseMsg(String msg) {
		String[] parts = msg.split(SEPARATOR);

		int process_id = Integer.parseInt(parts[0]);
		
		String type = parts[1];
		MessageType msgType = MessageType.valueOf(type);
		
		String payload = parts[2];
		Message new_msg = new Message(process_id, msgType, payload);
		return new_msg;
	}
	
	public String toString() {
		StringBuilder value = new StringBuilder();
		value.append(this.process_id);
		value.append(this.SEPARATOR);
		value.append(this.type);
		value.append(this.SEPARATOR);
		value.append(this.payLoad);
		
		return value.toString();
	}
}
