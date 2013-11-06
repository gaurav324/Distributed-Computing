/**
 * This code may be modified and used for non-commercial 
 * purposes as long as attribution is maintained.
 * 
 * @author: Isaac Levy
 */

package ut.paxos.downloaded;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import ut.paxos.bank.Message;

public class IncomingSocket extends Thread {
	final static String MSG_SEP = "&";
	Socket sock;
	InputStream in;
	PrintWriter out;
	ProcessId pid;
	private volatile boolean shutdownSet;
	//private final ConcurrentLinkedQueue<String> queue;
	//Queue<String> queue;
	int bytesLastChecked = 0;
	int cid = 0;
	Replica[] replicas;
	
	protected IncomingSocket(Socket sock, Replica[] replicas) throws IOException {
		this.sock = sock;
		in = new BufferedInputStream(sock.getInputStream());
		out = new PrintWriter(sock.getOutputStream());
		pid = new ProcessId(this.getName());

		// This was the culprit function.
		//sock.shutdownOutput();
		this.replicas = replicas;		
	}
	synchronized void sendMessage(Replica p, PaxosMessage msg){
		if (p != null) {
			p.deliver(msg);
		}
	}
	
	public void run() {
		while (!shutdownSet) {
			try {
				int avail = in.available();
				if (avail == bytesLastChecked) {
					sleep(1000);
				} else {
					in.mark(avail);
					byte[] data = new byte[avail];
					in.read(data);
					String dataStr = new String(data);
					int curPtr = 0;
					int curIdx;
					while ((curIdx = dataStr.indexOf(MSG_SEP, curPtr)) != -1) {
						String x = dataStr.substring(curPtr, curIdx);
						System.out.println("Received: " + x);
						try{
							int delay = 0;
							if (x.startsWith("delay=")) {
								String[] b = x.split("--");
								StringBuilder sb = new StringBuilder();
								for (int i=1; i < b.length; ++i) {
									sb.append(b[i]);
									sb.append("--");
								}
								delay = Integer.parseInt(b[0].split("=")[1]);
								x = sb.toString();
							}
							Message msg = Message.parseMsg(x);
						
							for (int r = 0; r < replicas.length; r++) {
								if (delay > 0) {
									Thread.sleep(delay);
								}
								sendMessage(replicas[r],
										new RequestMessage(pid, new Command(out, cid, msg)));	
							}
							cid += 1;
						}catch(Exception ex) {
							ex.printStackTrace();
						}
						curPtr = curIdx + 1;
					}
					in.reset();
					in.skip(curPtr);
					bytesLastChecked = avail - curPtr;
				}
			} catch (IOException e) {
//				conf.logger.log(Level.INFO, "Exception" + e.toString());
				e.printStackTrace();
			} catch (InterruptedException e) {
//				conf.logger.log(Level.INFO, "Exception" + e.toString());
				e.printStackTrace();
			}
		}
		
		shutdown();
	}
	
	public void cleanShutdown() {
		shutdownSet = true;
	}
	
	protected void shutdown() {
		try { in.close(); } catch (IOException e) {}
		
		try { 
			sock.shutdownInput();
			sock.close(); }			
		catch (IOException e) {}
	}
}