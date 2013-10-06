package ut.distcomp.playlist;

public enum MessageType {
		HEARTBEAT, // Everyone
		
		//  Messages from Coordinator to Process.
		VOTE_REQ,
		PRE_COMMIT, 
		COMMIT, 
		ABORT, 
		
		// Messages from Process to Coordinator.
		YES, 
		NO, 
		ACK,
		
		// Message to Coordinator from outside.
		ADD,
		DELETE,
		UPDATE,
		
		// TERMINATION related messages.
		UR_SELECTED,
		STATE_REQ,
		STATE_VALUE,
};