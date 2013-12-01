package ut.distcomp.util;

import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.*;

import ut.distcomp.replica.InputPacket;

public class Queue<InputPacket> extends ConcurrentLinkedQueue<InputPacket> {
	private final Lock lock = new ReentrantLock();
	private final Condition notEmpty = lock.newCondition();
	public static boolean pausedFlag = false;

	public Queue() {
		super();
	}
	
	public boolean offer(ut.distcomp.replica.InputPacket packet) {
		lock.lock();
		try {
			if (packet.msg.startsWith("continue")) {
				notEmpty.signal();
				return true;
			}
			boolean status = super.offer((InputPacket) packet);
			notEmpty.signal();
			return status;
		}
		finally {
			lock.unlock();
		}
	}
	
	public InputPacket poll() {
		lock.lock();
		try {
			while (this.size() == 0 || pausedFlag) {
				try {
					notEmpty.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			InputPacket result = super.poll();
			return result;
		} finally {
			lock.unlock();
		}
	}
}
