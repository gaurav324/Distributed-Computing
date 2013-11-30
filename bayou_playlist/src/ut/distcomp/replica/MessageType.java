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
}
