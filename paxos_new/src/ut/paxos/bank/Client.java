package ut.paxos.bank;

import java.util.HashMap;

public class Client {
	// Identifier to represent a client.
	public String clientName;
	
	// Map that stores account number to the account object.
	public HashMap<Integer, Account> accounts;
	
	// Constructor.
	public Client(String clientName, HashMap<Integer, Account> accounts) {
		this.clientName = clientName;
		this.accounts = accounts;
	}
}
