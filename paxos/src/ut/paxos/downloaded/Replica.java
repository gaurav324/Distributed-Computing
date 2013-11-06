package ut.paxos.downloaded;

import java.util.*;

import ut.paxos.bank.Account;
import ut.paxos.bank.Client;
import ut.paxos.bank.Message;
import ut.paxos.bank.State;


public class Replica extends Process {
	public ut.paxos.bank.State appState;
	ProcessId[] leaders;
	int slot_num = 1;
	Map<Integer /* slot number */, Command> proposals = new HashMap<Integer, Command>();
	Map<Integer /* slot number */, Command> decisions = new HashMap<Integer, Command>();

	public Replica(Env env, ut.paxos.bank.State appState, ProcessId me, ProcessId[] leaders){
		this.env = env;
		this.appState = appState;
		this.me = me;
		this.leaders = leaders;
		env.addProc(me, this);
	}

	void propose(Command c){
		if (!decisions.containsValue(c)) {
			for (int s = 1;; s++) {
				if (!proposals.containsKey(s) && !decisions.containsKey(s)) {
					proposals.put(s, c);
					Message msg = (Message)c.op;
					System.out.println( me + "|| SLOT: " + slot_num + " || " + "proposing: " + msg.type + "--" + msg.payLoad);
					for (ProcessId ldr: leaders) {
						sendMessage(ldr, new ProposeMessage(me, s, c));
					}
					break;
				}
			}
		}
	}

	void perform(Command c){
		for (int s = 1; s < slot_num; s++) {
			if (c.equals(decisions.get(s))) {
				slot_num++;
				return;
			}
		}
		Message msg = (Message)c.op;
		try {
			System.out.println( me + " || SLOT: " + slot_num + " || " + "performing: " + msg.type + "--" + msg.payLoad);
			
			Client x = this.appState.clients.get(msg.clientName);
			if (x == null) {
				throw new Exception("Not a valid client: " + msg.clientName);
			}
				
			String [] msgSplit = msg.payLoad.split("=");
			Account acc = x.accounts.get(Integer.parseInt(msgSplit[0]));
			if (acc == null) {
				throw new Exception("Not a valid account number " + msgSplit[0] + " for client: " + x.clientName);
			}
			switch(msg.type) {
				case DEPOSIT:
					if (msgSplit.length != 2) {
						c.out.println("Not a valid command: " + msg.toString());
						System.out.println("Not a valid command: " + msg.toString());
						break;
					}
					acc.deposit(Double.parseDouble(msgSplit[1]));
					break;
				case WITHDRAW:
					if (msgSplit.length != 2) {
						c.out.println("Not a valid command: " + msg.toString());
						System.out.println("Not a valid command: " + msg.toString());
						break;
					}
					acc.withDraw(Double.parseDouble(msgSplit[1]));
					break;
				case TRANSFER:
					if (msgSplit.length != 4) {
						c.out.println("Not a valid command: " + msg.toString());
						System.out.println("Not a valid command: " + msg.toString());
						break;
					}
					String toClientString = msgSplit[1];
					int toAccNumber = Integer.parseInt(msgSplit[2]);
					Client toClient = this.appState.clients.get(toClientString);
					if (toClient == null) {
						c.out.println("Not a valid client: " + toClientString);
						System.out.println("Not a valid client: " + toClientString);
						break;
					}
					Account toAccount = toClient.accounts.get(toAccNumber);
					if (toAccount == null) {
						throw new Exception("Not a valid account number " + toAccount + " for client: " + toClientString);
					}
					acc.transfer(toAccount, Double.parseDouble(msgSplit[3]));
					break;
				case INQUIRY:
					if (msgSplit.length != 1) {
						c.out.println("Not a valid command: " + msg.toString());
						System.out.println("Not a valid command: " + msg.toString());
						break;
					}
					c.out.println(me + " : " + acc.inquiry());
					System.out.println(acc.inquiry());
					System.out.println(this.proposals.toString());
					break;
			}
			c.out.flush();
		} catch(Exception ex) {
			c.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		
		slot_num++;
	}














	public void body(){
		System.out.println("Here I am: " + me);
		for (;;) {
			PaxosMessage msg = getNextMessage();

			if (msg instanceof RequestMessage) {
				RequestMessage m = (RequestMessage) msg;
				propose(m.command);
			}

			else if (msg instanceof DecisionMessage) {
				DecisionMessage m = (DecisionMessage) msg;
				decisions.put(m.slot_number, m.command);
				for (;;) {
					Command c = decisions.get(slot_num);
					if (c == null) {
						break;
					}
					Command c2 = proposals.get(slot_num);
					if (c2 != null && !c2.equals(c)) {
						propose(c2);
					}
					perform(c);
				}
			}
			else {
				System.err.println("Replica: unknown msg type");
			}
		}
	}
}
