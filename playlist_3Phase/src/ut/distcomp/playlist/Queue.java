package ut.distcomp.playlist;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.*;

public class Queue<String> extends ConcurrentLinkedQueue<String> {
	private final Lock lock = new ReentrantLock();
	private final Condition notEmpty = lock.newCondition();

	public Queue() {
		super();
	}
	
	public boolean offer(String str) {
		lock.lock();
		try {
			boolean status = super.offer(str);
			notEmpty.signal();
			return status;
		}
		finally {
			lock.unlock();
		}
	}
	
	public String poll() {
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
			String result = super.poll();
			return result;
		} finally {
			lock.unlock();
		}
	}
}
