package ut.paxos.downloaded;

import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Process extends Thread {
	ProcessId me;
	Queue<PaxosMessage> inbox = new Queue<PaxosMessage>();
	Env env;
	
	protected Logger logger;
	protected String logFolder;
	
	public static final Lock pingPongLock = new ReentrantLock();
	public static final Condition pingPongCondition = pingPongLock.newCondition();
	
	public Process(String logFolder, Env env, ProcessId me, Logger xlogger) {
		this.logFolder = logFolder;
		this.env = env;
		this.me = me;
		if (xlogger != null) {
			this.logger= xlogger;
		} else {
			try {
				Handler fh;
				logger = Logger.getLogger(me.name);
				
				Formatter formatter = new Formatter() {
		            @Override
		            public String format(LogRecord arg0) {
		                StringBuilder b = new StringBuilder();
		                b.append("[");
		                b.append(arg0.getSourceClassName());
		                b.append("-");
		                b.append(arg0.getSourceMethodName());
		                b.append(" ");
		                b.append("] ");
		                b.append(arg0.getMillis() / 1000);
		                b.append(" || ");
		                b.append("[Thread:");
		                b.append(arg0.getThreadID());
		                b.append("] || ");
		                b.append(arg0.getLevel());
		                b.append(" || ");
		                b.append(arg0.getMessage());
		                b.append(System.getProperty("line.separator"));
		                return b.toString();
		            }
	
		        };
				logger.setUseParentHandlers(false);
				Logger globalLogger = Logger.getLogger("global");
				Handler[] handlers = globalLogger.getHandlers();
				for(Handler handler : handlers) {
				    globalLogger.removeHandler(handler);
				}
				fh = new FileHandler(logFolder + "/" + me + ".log");
				fh.setLevel(Level.FINEST);
				fh.setFormatter(formatter);
				logger.addHandler(fh);
				
		        LogManager lm = LogManager.getLogManager();
		        lm.addLogger(logger);
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void body() {
		
	}

	public void run(){
		body();
		env.removeProc(me);
	}

	PaxosMessage getNextMessage(){
		return inbox.bdequeue();
	}

	void sendMessage(ProcessId dst, PaxosMessage msg){
		env.sendMessage(dst, msg);
	}

	void deliver(PaxosMessage msg){
		inbox.enqueue(msg);
	}
}
