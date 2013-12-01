package ut.distcomp.replica;

import java.io.OutputStream;

public class InputPacket {
	public String msg;
	OutputStream out;
	
	public InputPacket(String msg, OutputStream out) {
		this.msg = msg;
		this.out = out;
	}
}
