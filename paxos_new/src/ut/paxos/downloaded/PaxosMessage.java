package ut.paxos.downloaded;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.*;

public class PaxosMessage {
	ProcessId src;
}

class P1aMessage extends PaxosMessage {
	BallotNumber ballot_number;
	int slot_no; ReadRequestMessage readReq;
	P1aMessage(ProcessId src, BallotNumber ballot_number, int slot_no, ReadRequestMessage readReq){
		this.src = src; this.ballot_number = ballot_number;
		// This is for read-only requests.
		this.slot_no = slot_no;
		this.readReq = readReq;
}	}
class P1bMessage extends PaxosMessage {
	BallotNumber ballot_number; Set<PValue> accepted;
	P1bMessage(ProcessId src, BallotNumber ballot_number, Set<PValue> accepted) {
		this.src = src; this.ballot_number = ballot_number; this.accepted = accepted;
}	}
class P2aMessage extends PaxosMessage {
	BallotNumber ballot_number; int slot_number; Command command;
	P2aMessage(ProcessId src, BallotNumber ballot_number, int slot_number, Command command){
		this.src = src; this.ballot_number = ballot_number;
		this.slot_number = slot_number; this.command = command;
}	}
class P2bMessage extends PaxosMessage {
	BallotNumber ballot_number; int slot_number;
	P2bMessage(ProcessId src, BallotNumber ballot_number, int slot_number){
		this.src = src; this.ballot_number = ballot_number; this.slot_number = slot_number;
}	}
class PreemptedMessage extends PaxosMessage {
	BallotNumber ballot_number;
	PreemptedMessage(ProcessId src, BallotNumber ballot_number){
		this.src = src; this.ballot_number = ballot_number;
}	}
class AdoptedMessage extends PaxosMessage {
	BallotNumber ballot_number; Set<PValue> accepted;
	AdoptedMessage(ProcessId src, BallotNumber ballot_number, Set<PValue> accepted){
		this.src = src; this.ballot_number = ballot_number; this.accepted = accepted;
}	}
class DecisionMessage extends PaxosMessage {
	ProcessId src; int slot_number; Command command;
	public DecisionMessage(ProcessId src, int slot_number, Command command){
		this.src = src; this.slot_number = slot_number; this.command = command;
}	}
class RequestMessage extends PaxosMessage {
	Command command;
	public RequestMessage(ProcessId src, Command command){
		this.src = src ; this.command = command;
}	}
class ProposeMessage extends PaxosMessage {
	int slot_number; Command command;
	public ProposeMessage(ProcessId src, int slot_number, Command command){
		this.src = src; this.slot_number = slot_number; this.command = command;
}	}

class RequestHeartBeat extends PaxosMessage {
	public RequestHeartBeat(ProcessId src) {
		this.src = src;
	}
}

class HeartBeat extends PaxosMessage {
	public HeartBeat(ProcessId src) {
		this.src = src;
	}
}

class ReadRequestMessage extends PaxosMessage {
	Command command;
	public ReadRequestMessage(ProcessId src,  Command command) {
		this.src = src;
		this.command = command;
	}
}

class ReadRequestAttachMessage extends PaxosMessage {
	int slot_number; // Slot number to which this read request is to be attached.
	Command command;
	public ReadRequestAttachMessage(int slot_number, Command command) {
		this.slot_number = slot_number;
		this.command = command;
	}
}