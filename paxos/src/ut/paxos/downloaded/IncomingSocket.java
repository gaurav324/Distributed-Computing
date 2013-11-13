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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import ut.paxos.bank.Message;

public class IncomingSocket extends Thread {
	final static String MSG_SEP = "&";
	Socket sock;
	InputStream in;
	ProcessId pid;
	private volatile boolean shutdownSet;
	int bytesLastChecked = 0;
	
	final Queue<String> messages;
	int cid = 0;
	PrintWriter out;
	final Replica[] replicas;
	
	private final Lock lock = new ReentrantLock();
	private final Condition sendNewMessage = lock.newCondition();
	
	//Time out client to send command again.
	int ClientTimeOut = 2000;
	boolean ReceivedResponse = false;
	
	Message msg;
	
	protected IncomingSocket(Socket sock, Replica[] replicasx) throws IOException {
		this.sock = sock;
		in = new BufferedInputStream(sock.getInputStream());
		out = new PrintWriter(sock.getOutputStream());
		pid = new ProcessId(this.getName());

		// This was the culprit function.
		//sock.shutdownOutput();
		this.replicas = replicasx;
		this.messages = new Queue<String>();
		
		Thread t = new Thread() {
			public void run() {
				while(true) {
					lock.lock();
					try{
						String x = messages.bdequeue();
						System.out.println("Received: " + x);
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
						 msg = Message.parseMsg(x);
					
						//for (int r = 0; r < replicas.length; r++) {
						 for (int r = 0; r < 1; r++) {
							if (delay > 0) {
								Thread.sleep(delay);
							}
							sendMessage(replicas[r],
									new RequestMessage(pid, new Command(IncomingSocket.this, cid, msg)));	
						}
						
						//if time out on replica send same command again.  
						Thread t = new Thread() {
							public void run(){
								while(true) {
									try {
										sleep(ClientTimeOut);
									}catch(InterruptedException e){}
									if(ReceivedResponse)
										break;
									else {
										//logger.info("Client timed out sending again.");
										System.out.print("Client timed out sending again\n");
										for (int r = 0; r < replicas.length; r++) {
											sendMessage(replicas[r],
													new RequestMessage(pid, new Command(IncomingSocket.this, cid, msg)));	
										}
										continue;
									}
									
								}
							}
						
						};
						t.start();
						
						sendNewMessage.await();
					}catch(Exception ex) {
						ex.printStackTrace();
					} finally {
						lock.unlock();
					}
				}
			}
		};
		t.start();
	}
	
	public void callBack(Command c, String reply) {
		lock.lock();
		try {
			if (c.req_id == cid) {
				System.out.println("Called back with " + reply);
				out.write(reply + "\n");
				out.flush();
				sendNewMessage.signal();
				cid += 1;
				ReceivedResponse = true;
			}
		} finally {
			lock.unlock();
		}
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
						messages.enqueue(x); 
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