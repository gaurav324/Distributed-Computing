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
		EDIT,
		
		// TERMINATION related messages.
		UR_SELECTED,
		STATE_REQ,
		STATE_VALUE,
		
		// RECOVERY SPECIFIC MESSAGE.
		STATE_ENQUIRY,
		STATE_UNDECIDED,
		STATE_COMMIT,
		STATE_ABORT,
		STATE_RECOVERING,
		
		// Misc.
		PRINT_STATE,
		
		//To kill any process
		DIE,
};