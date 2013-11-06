package ut.paxos.bank;

public class LowBalanceException extends Exception {
	public LowBalanceException(String message) {
		super(message);
	}
}
