package ut.distcomp.util;

import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.*;

import ut.distcomp.replica.InputPacket;

public class Queue<InputPacket> extends ConcurrentLinkedQueue<InputPacket> {
	private final Lock lock = new ReentrantLock();
	private final Condition notEmpty = lock.newCondition();

	public Queue() {
		super();
	}
	
	public boolean offer(InputPacket packet) {
		lock.lock();
		try {
			boolean status = super.offer(packet);
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
			while (this.size() == 0) {
				try {
					notEmpty.await();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
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
