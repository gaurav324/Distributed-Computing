package ut.paxos.downloaded;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

public class Command {
	//ProcessId client;
	IncomingSocket client;
	int req_id;
	Object op;

//	public Command(ProcessId client, int req_id, Object op){
//		this.client = client;
//		this.req_id = req_id;
//		this.op = op;
//	}

	public Command(IncomingSocket client, int req_id, Object op){
		//this.client = client;
		this.client = client; // This acts as a client.
		this.req_id = req_id;
		this.op = op;
	}
	
	public boolean equals(Object o) {
		Command other = (Command) o;
		return client == other.client && req_id == other.req_id && op.equals(other.op);
		//return client.equals(other.client) && out == other.out && op.equals(other.op);
	}

	public String toString(){
		return "Command(" + client + ", " + req_id + ", " + op + ")";
	}
}
