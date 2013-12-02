package ut.distcomp.replica;

public class AddRetireOperation extends Operation {
	OperationType type;
	String process_id;
	String host;
	String port;
	
	public AddRetireOperation() {
		
	}
	
	public AddRetireOperation(OperationType type, String process_id, String host, String port) {
		this.type = type;
		this.process_id = process_id;
		this.host = host;
		this.port = port;
	}
	
	public static Operation operationFromString(String str) {
		AddRetireOperation op = new AddRetireOperation();
		String[] strSplit = str.split(Operation.SEPARATOR);
		
		OperationType type = OperationType.valueOf(strSplit[0]);
		if (!(type == OperationType.ADD_NODE || type == OperationType.RETIRE_NODE)) {
			return Operation.operationFromString(str);
		}
		op.type = type;
		op.process_id = strSplit[1];
		
		if (type == OperationType.ADD_NODE) {
			op.host = strSplit[2];
			op.port = strSplit[3];
		}
		
		return op;
	}
	
	@Override
	public String toString() {
		StringBuilder operation = new StringBuilder();
		operation.append(type);
		operation.append(Operation.SEPARATOR);
		operation.append(process_id);
		operation.append(Operation.SEPARATOR);
		if (host == null) {
			operation.append("");
		} else {
			operation.append(host);
			operation.append(Operation.SEPARATOR);
			operation.append(port);
		}
		
		return operation.toString();
	}
}