package ut.distcomp.playlist;

public class TransactionState {
	public enum STATE {
		RESTING, WAIT_DECISION, UNCERTAIN, COMMITABLE, WAIT_ACK, COMMIT, ABORT
	};
}