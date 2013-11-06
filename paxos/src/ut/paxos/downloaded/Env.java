package ut.paxos.downloaded;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;

import ut.paxos.bank.State;

public class Env {
	static Map<ProcessId, Process> procs = new HashMap<ProcessId, Process>();
	public final static int nAcceptors = 3, nReplicas = 8, nLeaders = 2, nRequests = 10;
	public static ProcessId[] acceptors;
	public static ProcessId[] replicas;
	public static ProcessId[] leaders;

	public static ArrayList<IncomingSocket> socketList = new ArrayList<IncomingSocket>();
			
    synchronized void sendMessage(ProcessId dst, PaxosMessage msg){
        Process p = procs.get(dst);
        if (p != null) {
                p.deliver(msg);
        }
    }
    
	synchronized void addProc(ProcessId pid, Process proc){
		procs.put(pid, proc);
		proc.start();
	}

	synchronized void removeProc(ProcessId pid){
		procs.remove(pid);
	}

	void run(String[] args) throws FileNotFoundException, IOException{
		acceptors = new ProcessId[nAcceptors];
		replicas = new ProcessId[nReplicas];
		leaders = new ProcessId[nLeaders];

		for (int i = 0; i < nAcceptors; i++) {
			acceptors[i] = new ProcessId("acceptor:" + i);
			Acceptor acc = new Acceptor(this, acceptors[i]);
		}
		for (int i = 0; i < nReplicas; i++) {
			replicas[i] = new ProcessId("replica:" + i);
			State appState = new State(args[0]);
			Replica repl = new Replica(this, appState, replicas[i], leaders);
		}
		for (int i = 0; i < nLeaders; i++) {
			leaders[i] = new ProcessId("leader:" + i);
			Leader leader = new Leader(this, leaders[i], acceptors, replicas);
		}

//		for (int i = 1; i < nRequests; i++) {
//			ProcessId pid = new ProcessId("client:" + i);
//			for (int r = 0; r < nReplicas; r++) {
//				sendMessage(replicas[r],
//					new RequestMessage(pid, new Command(pid, 0, "operation " + i)));
//			}
//		}
	}

	public static void main(String[] args) throws FileNotFoundException, IOException{
		new Env().run(args);
		
		// Start server on the given port.
		final int port = Integer.parseInt(args[1]);
		ServerSocket serverSock;
		try {
			serverSock = new ServerSocket(port);
			System.out.println("Request handler started.");
		} catch (IOException e) {
			String errStr = String.format(
					"Server can't open server port %d", port);
			System.out.println(errStr);
			throw new Error(errStr);
		}
		
		HashMap<Integer, Queue> clientQueues = new HashMap<Integer , Queue>();
		
		Replica[] replicaProcess = new Replica[nReplicas];
		for (int i = 0; i < nReplicas; i++) {
			replicaProcess[i] = (Replica)procs.get(replicas[i]);
		}

		try {
			while(true) {
				System.out.println("Still waiting..\n");
				IncomingSocket incomingSock = new IncomingSocket(
						serverSock.accept(), replicaProcess);
				socketList.add(incomingSock);
				incomingSock.start();
				System.out.println("New incoming connection accepted from " + incomingSock.sock.getInetAddress());
//				conf.logger.info(String.format(
//						"Server %d: New incoming connection accepted from %s",
//						procNum, incomingSock.sock.getInetAddress()
//								.getHostName()));
			}
		} catch (IOException e) {
//				if (!killSig) {
//					conf.logger.log(Level.INFO, String.format(
//							"Server %d: Incoming socket failed", procNum), e);
		} finally {
			try {
				serverSock.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
