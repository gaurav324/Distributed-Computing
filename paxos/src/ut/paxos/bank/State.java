package ut.paxos.bank;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class State {
	
	public State(String filename) throws FileNotFoundException, IOException {
		Properties prop = new Properties();
		prop.load(new FileInputStream(filename));
		
		numClients = loadInt(prop, "NumClients");
		for (int i=0; i < numClients; i++) {
			String[] clientAccNumbers = prop.getProperty("client" + i).split(",");
			
			HashMap<Integer, Account> accounts = new HashMap<Integer, Account>();
			for(int j=1; j < clientAccNumbers.length; j++) {
				Integer accNo = Integer.parseInt(clientAccNumbers[j]);
				//System.out.println("Adding: " + accNo);
				accounts.put(accNo, new Account(accNo)); 
			}
			Client client = new Client(clientAccNumbers[0], accounts);
			//System.out.println("AddingClient: " + clientAccNumbers[0]);
			clients.put(clientAccNumbers[0], client);
		}
		
		// Code to check whether property file is read properly.
//		Iterator it = clients.entrySet().iterator();
//	    while (it.hasNext()) {
//	        Map.Entry pairs = (Map.Entry)it.next();
//	        System.out.println("Client: " + pairs.getKey());
//	        Client x = (Client)pairs.getValue();
//	        
//	        Iterator it1 = x.accounts.entrySet().iterator();
//	        while(it1.hasNext()) {
//	        	Map.Entry pairs1 = (Map.Entry)it1.next();
//	        	System.out.println("Account number is: " + pairs1.getKey());
//	        }
//	    }
	}
	
	// Helper function.
	private int loadInt(Properties prop, String s) {
		return Integer.parseInt(prop.getProperty(s.trim()));
	}
	
	// Total number of clients;
	public int numClients;
	
	// Map from client name to the client object.
	public final HashMap<String,Client> clients = new HashMap<String, Client>();
}
