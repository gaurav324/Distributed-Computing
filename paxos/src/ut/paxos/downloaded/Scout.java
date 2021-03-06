package ut.paxos.downloaded;
import java.util.*;
import java.util.logging.Logger;

public class Scout extends Process {
	ProcessId leader;
	ProcessId[] acceptors;
	BallotNumber ballot_number;
	
	// This is for read-only requests.
	int slot_no;
	ReadRequestMessage readReq;
	public Scout(Env env, ProcessId me, ProcessId leader,
			ProcessId[] acceptors, BallotNumber ballot_number, String logFolder, Logger logger,
			int slot_no, ReadRequestMessage readReq) {
		super(logFolder, env, me, logger);
		this.acceptors = acceptors;
		this.leader = leader;
		this.ballot_number = ballot_number;
		
		// This is for read-only requests.
		this.slot_no = slot_no;
		this.readReq = readReq;
		
		env.addProc(me, this);
	}

	public void body(){
		P1aMessage m1 = new P1aMessage(me, ballot_number, this.slot_no, this.readReq);
		Set<ProcessId> waitfor = new HashSet<ProcessId>();
		for (ProcessId a: acceptors) {
			sendMessage(a, m1);
			waitfor.add(a);
		}

		Set<PValue> pvalues = new HashSet<PValue>();
		while (2 * waitfor.size() >= acceptors.length) {
			PaxosMessage msg = getNextMessage();

			if (msg instanceof P1bMessage) {
				P1bMessage m = (P1bMessage) msg;

				int cmp = ballot_number.compareTo(m.ballot_number);
				if (cmp != 0) {
					sendMessage(leader, new PreemptedMessage(me, m.ballot_number));
					return;
				}
				if (waitfor.contains(m.src)) {
					waitfor.remove(m.src);
					pvalues.addAll(m.accepted);
				}
			}
			else {
				System.err.println("Scout: unexpected msg");
			}
		}

		sendMessage(leader, new AdoptedMessage(me, ballot_number, pvalues));
	}
}
