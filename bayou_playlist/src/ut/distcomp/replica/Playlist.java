package ut.distcomp.replica;

import java.util.Hashtable;

public class Playlist {
	Hashtable<String, String> playList = new Hashtable<String, String>();

	public Playlist() {
	}
	
	public void performOperation(Operation op) throws SongNotFoundException {
		switch (op.type) {
			case ADD:
				add(op.song, op.url);
			case DELETE:
				delete(op.song);
			case EDIT:
				edit(op.song, op.url);
		}
	}
	
	public synchronized void add(String song, String url) {
		this.playList.put(song, url);
	}
	
	public synchronized void delete(String song) throws SongNotFoundException {
		if (this.playList.containsKey(song)) {
			this.playList.remove(song);
			return;
		}
		
		throw new SongNotFoundException("Could not find song: " + song);
	}
	
	public synchronized void edit(String song, String newUrl) throws SongNotFoundException {
		if (this.playList.containsKey(song)) {
			this.playList.put(song, newUrl);
			return;
		}
		
		throw new SongNotFoundException("Could not find song: " + song);
	}
}

class SongNotFoundException extends Exception {
	public SongNotFoundException(String ex) {
		super(ex);
	}
}
