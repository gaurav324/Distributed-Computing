package ut.paxos.downloaded;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import ut.paxos.bank.Message;

public class Command {
	//ProcessId client;
	IncomingSocket client;
	int req_id;
	Object op;
	
	ArrayList<Command> hiddenReadOnlyRequest;

//	public Command(ProcessId client, int req_id, Object op){
//		this.client = client;
//		this.req_id = req_id;
//		this.op = op;
//	}

	public Command(Command c) {
		this.client = c.client; // This acts as a client.
		this.req_id = c.req_id;
		this.op = c.op;
		
		this.hiddenReadOnlyRequest = new ArrayList<Command>(c.hiddenReadOnlyRequest); 
	}
	public Command(IncomingSocket client, int req_id, Object op){
		//this.client = client;
		this.client = client; // This acts as a client.
		this.req_id = req_id;
		this.op = op;
		
		this.hiddenReadOnlyRequest = new ArrayList<Command>();
	}
	
	public boolean equals(Object o) {
		Command other = (Command) o;
		return client == other.client && req_id == other.req_id && op.equals(other.op);
		//return client.equals(other.client) && out == other.out && op.equals(other.op);
	}
	
	public void addReadonlyCommand(Command op2) {
		hiddenReadOnlyRequest.add(op2);
	}

	public String toString() {
		if (hiddenReadOnlyRequest != null) {
			return "Command(" + client + ", " + req_id + ", " + op + ", " + hiddenReadOnlyRequest.size() + ")" ;
		}
		else {
			return "Command(" + client + ", " + req_id + ", " + op + ")";
	//}
		}
	}
}
	
