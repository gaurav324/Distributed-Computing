package ut.paxos.downloaded;
public class PValue {
	BallotNumber ballot_number;
	int slot_number;
	Command command;

	public PValue(BallotNumber ballot_number, int slot_number,
											Command command){
		this.ballot_number = ballot_number;
		this.slot_number = slot_number;
		this.command = command;
	}
	
	public boolean equals(Object o) {
		PValue other = (PValue) o;
		return ballot_number == other.ballot_number && slot_number == other.slot_number;
		//return client.equals(other.client) && out == other.out && op.equals(other.op);
	}

	public String toString(){
		return "PV(" + ballot_number + ", " + slot_number + ", " + command + ")";
	}
}
