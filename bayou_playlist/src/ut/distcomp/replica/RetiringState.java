package ut.distcomp.replica;

public enum RetiringState {
	NOT_RETIRING, // Normal not retiring state.
	REQUESTED_RETIRE, // Asked other replicas to wait so that I can retire.
	CANNOT_RETIRE, // Cannot handle retire messages now, because someone else is retiring.
	RETIRE_REQUEST_APPROVED // When some replica approves my request, that I can retire.
}
