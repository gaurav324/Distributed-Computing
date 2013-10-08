package ut.distcomp.playlist;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public class StateRecovery {

	public StateRecovery() {
		
	}
	
	public static void updateStateFile(Process process) {
		String folder = System.getProperty("LOG_FOLDER");
		final File Log_folder = new File(System.getProperty("LOG_FOLDER")); // creates a new folder to store log files
		
		if(!Log_folder.exists()){
			if(Log_folder.mkdirs()){
			   //System.out.println("first process to log and create directory");
			} else {
			   System.out.println("Failed to create a Log directory. Something went wrong");
			}
		 }
		
		//Write log to the process file.
		if(Log_folder.canWrite()) {
			final File myFile = new File(Log_folder + "/" + process.processId + ".Songs");
			
			try {
				FileOutputStream fos = new FileOutputStream(myFile);
				ObjectOutputStream oos = new ObjectOutputStream(fos);

				oos.writeObject(process.playList);
				oos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static Hashtable<String, String> readStateFile(Process process) {
		String folder = System.getProperty("LOG_FOLDER");
		final File Log_folder = new File(System.getProperty("LOG_FOLDER")); // creates a new folder to store log files

		Hashtable<String, String> playList = new Hashtable<String, String>();
		Hashtable<String, String> temp = null;
		
		try {
			final File myFile = new File(Log_folder + "/" + process.processId + ".Songs");
			
			FileInputStream fos = new FileInputStream(myFile);
			ObjectInputStream oos = new ObjectInputStream(fos);

			temp = (Hashtable<String, String>) oos.readObject();
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (temp == null) {
			return playList;
		} else {
			return temp;
		}
	}
}
