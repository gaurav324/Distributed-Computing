/**
 * This code may be modified and used for non-commercial 
 * purposes as long as attribution is maintained.
 * 
 * @author: Isaac Levy
 */

package ut.distcomp.communication;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import ut.distcomp.replica.InputPacket;
import ut.distcomp.util.Queue;

public class IncomingSock extends Thread {
	final static String MSG_SEP = "&";
	Socket sock;
	InputStream in;
	OutputStream out;
	private volatile boolean shutdownSet;
	Queue<InputPacket> queue;
	int bytesLastChecked = 0;
	Config conf;
	
	protected IncomingSock(Socket sock, Config conf, 
			Queue<InputPacket> queue) throws IOException {
		this.sock = sock;
		in = new BufferedInputStream(sock.getInputStream());
		out = new BufferedOutputStream(sock.getOutputStream());
		this.queue = queue;
		this.conf = conf;
	}
	
	protected List<InputPacket> getMsgs() {
		List<InputPacket> msgs = new ArrayList<InputPacket>();
		InputPacket tmp;
		conf.logger.log(Level.INFO, "Queue size" + queue.size());
		while((tmp = queue.poll()) != null)
			msgs.add(tmp);
		return msgs;
	}
	
	public void run() {
		while (!shutdownSet) {
			try {
				int avail = in.available();
				if (avail == bytesLastChecked) {
					sleep(10);
				} else {
					in.mark(avail);
					byte[] data = new byte[avail];
					in.read(data);
					String dataStr = new String(data);
					int curPtr = 0;
					int curIdx;
					while ((curIdx = dataStr.indexOf(MSG_SEP, curPtr)) != -1) {
						InputPacket packet = new InputPacket(dataStr.substring(curPtr, curIdx), out);
						if (packet.msg.startsWith("pause")) {
							conf.logger.info("Going to pause the process.");
							Queue.pausedFlag = true;
						} else if (packet.msg.startsWith("continue")) {
							conf.logger.info("Going to resume the process.");
							Queue.pausedFlag = false;
							queue.offer(packet);
						} else {
							queue.offer(packet);
						}
						String x = dataStr.substring(curPtr, curIdx);
						curPtr = curIdx + 1;
					}
					in.reset();
					in.skip(curPtr);
					bytesLastChecked = avail - curPtr;
				}
			} catch (IOException e) {
				conf.logger.log(Level.INFO, "Exception" + e.toString());
				e.printStackTrace();
			} catch (InterruptedException e) {
				conf.logger.log(Level.INFO, "Exception" + e.toString());
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
