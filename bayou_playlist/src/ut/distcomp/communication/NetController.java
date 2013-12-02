/**
 * This code may be modified and used for non-commercial 
 * purposes as long as attribution is maintained.
 * 
 * @author: Isaac Levy
 */

/**
* The sendMsg method has been modified by Navid Yaghmazadeh to fix a bug regarding to send a message to a reconnected socket.
*/

package ut.distcomp.communication;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import ut.distcomp.replica.InputPacket;
import ut.distcomp.replica.Replica;
import ut.distcomp.util.Queue;

/**
 * Public interface for managing network connections.
 * You should only need to use this and the Config class.
 * @author ilevy
 *
 */
public class NetController {
	public final Config config;
	private final List<IncomingSock> inSockets;
	public final HashMap<String, OutgoingSock> outSockets;
	private final ListenServer listener;
	
	public NetController(String processId, Config config, Queue<InputPacket> queue) {
		this.config = config;
		this.config.procNum = processId;

		inSockets = Collections.synchronizedList(new ArrayList<IncomingSock>());
		outSockets = new HashMap<String, OutgoingSock>();
		
		for (String process: config.addresses.keySet()) {
			outSockets.put(process, null);
		}

		listener = new ListenServer(config, inSockets, queue);
		listener.start();
	}
	
	// Establish outgoing connection to a process.
	private synchronized void initOutgoingConn(String proc) throws IOException {
		if (outSockets.get(proc) != null)
			throw new IllegalStateException("proc " + proc + " not null");
		
		outSockets.put(proc, new OutgoingSock(new Socket(config.addresses.get(proc), config.ports.get("" + proc))));
		config.logger.info(String.format("Server %s: Socket to %s established", 
				config.procNum, proc));
	}
	
	public synchronized void sendMsgs(Set<String> processes, String msg) {//, int partial_count) {
		for(String processNo: processes) {	
			config.logger.info("Sending: " + msg + " to " + processNo);
			sendMsg(processNo, msg);
		}
	}
	
	public synchronized void broadCastMsgs(String msg, HashMap<String, Boolean> exceptProcess)
	{
		Set<String> keySet = new HashSet<String>(outSockets.keySet());
		for (String processId: keySet) {
			if (processId.equals(this.config.procNum) || (exceptProcess != null && exceptProcess.containsKey(processId))) {
				continue;
			}
			sendMsg(processId, msg);
		}
	}
	
	public synchronized boolean sendMsg(String process, String msg) {
		config.logger.info("Sending Message to " + process + ":	" + msg);
		try {
			if (outSockets.get(process) == null)
				initOutgoingConn(process);
			outSockets.get(process).sendMsg(msg);
		} catch (IOException e) { 
			OutgoingSock sock = outSockets.get(process);
			if (sock != null) {
				sock.cleanShutdown();
				outSockets.remove(process);
				try{
					initOutgoingConn(process);
					sock = outSockets.get(process);
					sock.sendMsg(msg);	
				} catch(IOException e1){
					if (sock != null) {
						sock.cleanShutdown();
						outSockets.remove(process);
					}
					//config.logger.info(String.format("Server %d: Msg to %d failed.",
                    //    config.procNum, process));
        		    //config.logger.log(Level.FINE, String.format("Server %d: Socket to %d error",
                    //    config.procNum, process), e);
                    return false;
				}
				return true;
			}
			//config.logger.info(String.format("Server %d: Msg to %d failed.", 
			//	config.procNum, process));
			config.logger.log(Level.FINE, String.format("Server %s: Socket to %s error", 
				config.procNum, process), e);
			return false;
		}
		return true;
	}
	
	/**
	 * Return a list of msgs received on established incoming sockets
	 * @return list of messages sorted by socket, in FIFO order. *not sorted by time received*
	 */
	public synchronized List<InputPacket> getReceivedMsgs() {
		List<InputPacket> objs = new ArrayList<InputPacket>();
		config.logger.log(Level.INFO, "Looking for messages.");
		synchronized(inSockets) {
			ListIterator<IncomingSock> iter  = inSockets.listIterator();
			while (iter.hasNext()) {
				IncomingSock curSock = iter.next();
				try {
					objs.addAll(curSock.getMsgs());
				} catch (Exception e) {
					config.logger.log(Level.INFO, 
							"Server " + config.procNum + " received bad data on a socket", e);
					curSock.cleanShutdown();
					iter.remove();
				}
			}
		}
		
		return objs;
	}
	/**
	 * Shuts down threads and sockets.
	 */
	public synchronized void shutdown() {
		listener.cleanShutdown();
        if(inSockets != null) {
		    for (IncomingSock sock : inSockets)
			    if(sock != null)
                    sock.cleanShutdown();
        }
		if(outSockets != null) {
            for (OutgoingSock sock : outSockets.values())
			    if(sock != null)
                    sock.cleanShutdown();
        }
		
	}

	public void disconnect(String nodeToDisconnect) {
		Replica.disconnectedNodes.put(nodeToDisconnect, true);
		outSockets.remove(nodeToDisconnect);
	}
	
	public void connect(String nodeToConnect) {
		if (!outSockets.containsKey(nodeToConnect)) {
			Replica.disconnectedNodes.remove(nodeToConnect);
			outSockets.put(nodeToConnect, null);
		}
	}
	
	public void forgetAll() {
		for (String pid : outSockets.keySet()) {
			Replica.disconnectedNodes.put(pid, true);
		}
		outSockets.clear();
	}
	
	public void connectAll() {
		for (String node: this.config.addresses.keySet()) {
			connect(node);
		}
		Replica.disconnectedNodes.clear();
	}

}
