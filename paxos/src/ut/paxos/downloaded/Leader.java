package ut.paxos.downloaded;
import java.util.*;

public class Leader extends Process {
	ProcessId[] acceptors;
	ProcessId[] replicas;
	BallotNumber ballot_number;
	boolean active = false;
	String PingPong;
	Map<Integer, Command> proposals = new HashMap<Integer, Command>();

	// Testing purposes.
	ProcessId[] leaders;
	boolean pingPongSet;
	boolean failureDetectionSet;
	boolean isPingingSeniorLeader;
	boolean isSeniorLeaderAlive;
	int waitBeforeHeartBeatPeriod = 0;
	
	public Leader(Env env, ProcessId me, ProcessId[] acceptors,
										ProcessId[] replicas, ProcessId[] leaders, String logFolder, 
										String pingPong, String failureDetection) {
		super(logFolder, env, me, null);
		ballot_number = new BallotNumber(0, me);
		this.acceptors = acceptors;
		this.replicas = replicas;
		this.leaders = leaders;
		env.addProc(me, this);
		
		this.pingPongSet = Boolean.parseBoolean(pingPong);
		this.failureDetectionSet = Boolean.parseBoolean(failureDetection);
		//assert(this.pingPongSet == this.failureDetectionSet);
		this.isPingingSeniorLeader = false;
		this.isSeniorLeaderAlive = false;
	}

	public void body() {
		System.out.println("Here I am: " + me);

		new Scout(env, new ProcessId("scout:" + me + ":" + ballot_number),
			me, acceptors, ballot_number, logFolder, logger);
		

		for (;;) {
			PaxosMessage msg = getNextMessage();
			if (pingPongSet && !isPingingSeniorLeader) {
				//logger.info(me + ". Trying to acquire ping pong lock.");
				pingPongLock.lock();
			}
			if (msg instanceof ProposeMessage) {
				ProposeMessage m = (ProposeMessage) msg;
				logger.info(me + " || Received: " + m.command.toString());
				if (!proposals.containsKey(m.slot_number)) {
					proposals.put(m.slot_number, m.command);
					if (active) {
						new Commander(env,
							new ProcessId("commander:" + me + ":" + ballot_number + ":" + m.slot_number),
							me, acceptors, replicas, ballot_number, m.slot_number, m.command, logFolder, logger);
					}
				}
			} else if (msg instanceof RequestHeartBeat) {
				logger.info(me + " || Received: " + msg.toString());
				waitBeforeHeartBeatPeriod += 200;
				try {
					sleep(waitBeforeHeartBeatPeriod);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				sendMessage(msg.src, new HeartBeat(this.me));
			} else if (msg instanceof HeartBeat) {
				logger.info(me + " || Received: " + msg.toString());
				if (isPingingSeniorLeader) {
					this.isSeniorLeaderAlive = true;
				}
			} else if (msg instanceof AdoptedMessage) {
				AdoptedMessage m = (AdoptedMessage) msg;
				logger.info(me + " || Received: " + m.toString());

				if (pingPongSet) {
					logger.info(me + ". Going to signal other leaders.");
					pingPongCondition.signal();
					try {
						sleep(1);
						logger.info(me + ". Going to wait on ping pong condition.");
						pingPongLock.lock();
						pingPongCondition.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (ballot_number.equals(m.ballot_number)) {
					Map<Integer, BallotNumber> max = new HashMap<Integer, BallotNumber>();
					for (PValue pv : m.accepted) {
						BallotNumber bn = max.get(pv.slot_number);
						if (bn == null || bn.compareTo(pv.ballot_number) < 0) {
							max.put(pv.slot_number, pv.ballot_number);
							proposals.put(pv.slot_number, pv.command);
						}
					}

					for (int sn : proposals.keySet()) {
						logger.info(me + " || Sending commander with: " + proposals.get(sn));
						new Commander(env,
							new ProcessId("commander:" + me + ":" + ballot_number + ":" + sn),
							me, acceptors, replicas, ballot_number, sn, proposals.get(sn), logFolder, logger);
					}
					active = true;
				}
			}

			else if (msg instanceof PreemptedMessage) {
				final PreemptedMessage m = (PreemptedMessage) msg;
				logger.info(me + " || Received: " + m.toString());
				if (failureDetectionSet) {
					active = false;
					Thread t = new Thread() {
						public void run() {
							while(true) {
								RequestHeartBeat request = new RequestHeartBeat(Leader.this.me);
								sendMessage(m.ballot_number.leader_id, request);
								isPingingSeniorLeader = true;
								isSeniorLeaderAlive = false;
								
								try {
									sleep(2000);
								} catch (InterruptedException e) {}
								if (isSeniorLeaderAlive) {
									continue;
								} else {
									isPingingSeniorLeader = false;
									if (ballot_number.compareTo(m.ballot_number) < 0) {
										ballot_number = new BallotNumber(m.ballot_number.round + 1, me);
										logger.info(me + " || Sending scout with: " + ballot_number.toString());
										new Scout(env, new ProcessId("scout:" + me + ":" + ballot_number),
											me, acceptors, ballot_number, logFolder, logger);
										active = false;
									}
									break;
								}
							}
						}
					};
					t.start();
				} else {
					isPingingSeniorLeader = false;
					if (ballot_number.compareTo(m.ballot_number) < 0) {
						ballot_number = new BallotNumber(m.ballot_number.round + 1, me);
						logger.info(me + " || Sending scout with: " + ballot_number.toString());
						new Scout(env, new ProcessId("scout:" + me + ":" + ballot_number),
							me, acceptors, ballot_number, logFolder, logger);
						active = false;
					}
				}
			}

			else {
				System.err.println("Leader: unknown msg type");
			}
		}
	}
}
