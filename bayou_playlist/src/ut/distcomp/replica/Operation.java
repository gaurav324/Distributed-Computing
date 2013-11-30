package ut.distcomp.replica;

import java.io.Serializable;

public class Operation implements Serializable {
	public static final String SEPARATOR = "==";
	
	OperationType type;
	String song;
	String url;
	
	public static Operation operationFromString(String str) {
		Operation op = new Operation();
		String[] strSplit = str.split(Operation.SEPARATOR);
		
		OperationType type = OperationType.valueOf(strSplit[0]);
		op.type = type;
		op.song = strSplit[1];
		
		if (type == OperationType.ADD || type == OperationType.EDIT) {
			op.url = strSplit[2];
		}
		
		return op;
	}
	
	@Override
	public String toString() {
		StringBuilder operation = new StringBuilder();
		operation.append(type);
		operation.append(Operation.SEPARATOR);
		operation.append(song);
		operation.append(Operation.SEPARATOR);
		if (url == null) {
			operation.append("");
		} else {
			operation.append(url);
		}
		
		return operation.toString();
	}
}