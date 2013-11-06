package ut.paxos.bank;

public class Account {
	// Account number.
	private int accNumber;
	
	// Balance amount in the account.
	private double balance;
	
	public Account(int accNumber) {
		this.accNumber = accNumber;
		this.balance = 0.0;
	}

	public void deposit(double money) {
		this.balance += money;
	}
	
	public void withDraw(double money) throws LowBalanceException {
		if (this.balance - money < 0.0) {
			throw new LowBalanceException("There is not sufficient money in the account number: " + this.accNumber);
		}
		
		this.balance -= money;
	}
	
	public void transfer(Account to, double money) throws LowBalanceException {
		if (this.balance - money < 0.0) {
			throw new LowBalanceException("There is not sufficient money in the account number: " + this.accNumber);
		}
		
		// No need to have synchronization or something, as only one thread can operate on this account.
		this.balance -= money;
		to.deposit(money);
	}
	
	public double inquiry() {
		return balance;
	}
}
