package ut.paxos.downloaded;
import java.util.*;

import ut.paxos.bank.Message;

public class Acceptor extends Process {
	BallotNumber ballot_number = null;
	Set<PValue> accepted = new HashSet<PValue>();

	public Acceptor(Env env, ProcessId me, String logFolder) {
		super(logFolder, env, me, null);
		this.env = env;
		this.me = me;
		env.addProc(me, this);
	}

	public void body(){
		System.out.println("Here I am: " + me);
		for (;;) {
			PaxosMessage msg = getNextMessage();

			if (msg instanceof P1aMessage) {
				P1aMessage m = (P1aMessage) msg;

				if (ballot_number == null ||
						ballot_number.compareTo(m.ballot_number) < 0) {
					ballot_number = m.ballot_number;
				}
				// custom code for storing what all read requests to be 
				// attached in the following messages.
				if (m.readReq != null) {
					boolean flag = true;
					for (PValue pv : accepted) {
						if (pv.ballot_number == ballot_number && pv.slot_number == m.slot_no) {
						//if (pv.slot_number == m.slot_no) {
							Command pvCommand = pv.command;
							logger.info(me + " || " + "Updated PValues readOnlyCommand set :" + pv.toString());
							pvCommand.addReadonlyCommand((Command)m.readReq.command);
							flag = false;
						}
					}
					if (flag) {
						Command cmd = new Command(null, -1, null);
						cmd.addReadonlyCommand((Command)m.readReq.command);
						PValue pv = new PValue(ballot_number, m.slot_no, cmd);
						logger.info(me + " || " + "Accepted :" + pv.toString());
						accepted.add(pv);
					}
				}
				sendMessage(m.src, new P1bMessage(me, ballot_number, new HashSet<PValue>(accepted)));
			}
			else if (msg instanceof P2aMessage) {
				P2aMessage m = (P2aMessage) msg;

				if (ballot_number == null ||
						ballot_number.compareTo(m.ballot_number) <= 0) {
					ballot_number = m.ballot_number;
					
					// Search for the pValue, if it is already there, then go and update
					// the command field. It is possible that last time for a read request
					// we could have created a dummy kind of request.
					boolean flag = true;
					for (PValue pv : accepted) {
						if (pv.ballot_number == ballot_number && pv.slot_number == m.slot_number) {
							Command pvCommand = pv.command;
							pvCommand.client = m.command.client;
							pvCommand.op = m.command.op;
							pvCommand.req_id = m.command.req_id;
							//m.command.hiddenReadOnlyRequest = new ArrayList<Message>(pvCommand.hiddenReadOnlyRequest);
							//pvCommand.addReadonlyMessage((Message)m.readReq.command.op);
							flag = false;
						}
					}
					if (flag) {
						accepted.add(new PValue(ballot_number, m.slot_number, m.command));
					}
					//accepted.add(new PValue(ballot_number, m.slot_number, m.command));
				}
				sendMessage(m.src, new P2bMessage(me, ballot_number, m.slot_number));
			}
		}
	}
}
