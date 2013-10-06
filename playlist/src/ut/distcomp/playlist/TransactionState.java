package ut.distcomp.playlist;

public class TransactionState {
	public enum STATE {
		RESTING, WAIT_DECISION,
		DECISION_RECEIVED, UNCERTAIN, COMMITABLE, 
		WAIT_ACK, ACK_RECEIVED, COMMIT, ABORT,
		RECOVERING, STATE_ENQUIRY_WAIT
	};
}