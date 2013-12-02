package ut.distcomp.replica;

public enum MessageType {
	// Messages that define valid Operations.
	OPERATION,
	
	// Entropy.
	ENTROPY,
	
	// Read.
	READ,
	
	// DISCONNECT and CONNECT.
	DISCONNECT,
	CONNECT,
	
	// RETIRE.
	RETIRE,
	STOP_RETIRING, // When someone receives this message, this server would not respond to any retire request.
	STOP_RETIRING_REJECTED,
	STOP_RETIRING_APPROVED, // When someone gets a stop_retiring request, it sends back approval.
	BECOME_PRIMARY, // When someone receives this, they would get a request to become primary and do entropy also.
	SET_FREE, // When someone gets this message, then can retire now.
	
	// JOIN.
	JOIN,
}
