/**
 * This code may be modified and used for non-commercial 
 * purposes as long as attribution is maintained.
 * 
 * @author: Isaac Levy
 */

package ut.distcomp.communication;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

public class ListenServer extends Thread {

	public volatile boolean killSig = false;
	final int port;
	final String procNum;
	final List<IncomingSock> socketList;
	final Config conf;
	final ServerSocket serverSock;
	ConcurrentLinkedQueue<String> queue;

	protected ListenServer(Config conf, List<IncomingSock> sockets, 
			ConcurrentLinkedQueue<String> queue) {
		this.conf = conf;
		this.socketList = sockets;
		this.queue = queue;

		procNum = conf.procNum;
		port = conf.ports.get(procNum);
		try {
			serverSock = new ServerSocket(port);
			conf.logger.info(String.format(
					"Server %s: Server connection established", procNum));
		} catch (IOException e) {
			String errStr = String.format(
					"Server %s: [FATAL] Can't open server port %d", procNum,
					port);
			conf.logger.log(Level.SEVERE, errStr);
			throw new Error(errStr);
		}
	}

	public void run() {
		while (!killSig) {
			try {
				IncomingSock incomingSock = new IncomingSock(
						serverSock.accept(), conf, queue);
				socketList.add(incomingSock);
				incomingSock.start();
				conf.logger.info(String.format(
						"Server %s: New incoming connection accepted from %s",
						procNum, incomingSock.sock.getInetAddress()
								.getHostName()));
			} catch (IOException e) {
				if (!killSig) {
					conf.logger.log(Level.INFO, String.format(
							"Server %s: Incoming socket failed", procNum), e);
				}
			}
		}
	}

	protected void cleanShutdown() {
		killSig = true;
		try {
			serverSock.close();
		} catch (IOException e) {
			conf.logger.log(Level.INFO,String.format(
					"Server %s: Error closing server socket", procNum), e);
		}
	}
}
