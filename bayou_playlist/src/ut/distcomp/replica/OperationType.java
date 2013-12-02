package ut.distcomp.replica;

import java.io.Serializable;

public enum OperationType implements Serializable {
	ADD, DELETE, EDIT, RETIRE_NODE, ADD_NODE;
}
