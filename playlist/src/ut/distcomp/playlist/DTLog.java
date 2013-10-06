package ut.distcomp.playlist;

import java.io.*;
//import java.io.FileNotFoundException;
import java.util.*;
import java.lang.*;
import java.sql.Timestamp;
import java.util.Date;

import ut.distcomp.playlist.TransactionState.STATE;

public class DTLog {
	public static final String Log_SEPARATOR = " %% ";
	public static final String UpSet_SEPARATOR = ", ";	 // to separate upProcess
	
	Process process;
	String folder;
	
	public DTLog(Process process) {
		this.process = process;
		this.folder = System.getProperty("LOG_FOLDER");
	}
	
	//Function To write log to the file	
	public void write(STATE state, String command) {
		int process_id = process.processId;
		Hashtable<Integer,Long>upProcess = process.upProcess;
		
		final File Log_folder = new File(folder); // creates a new folder to store log files
	
		if(!Log_folder.exists()){
			
		   if(Log_folder.mkdirs()){
			   //System.out.println("first process to log and create directory");
		   }else{
			   System.out.println("Failed to create a Log directory. Something went wrong");
		   }
		 }
	
		//Write log to the process file.
		if(Log_folder.canWrite()){
			try {
				final File myFile = new File(Log_folder + "/" + process_id + ".DTlog");
				
				if(!myFile.exists()){ // if file doesn't exist create it
					myFile.createNewFile();
				}
			
				FileWriter log_writer = new FileWriter(myFile.getAbsoluteFile(), true);
				BufferedWriter log_buf = new BufferedWriter(log_writer);
				
				StringBuilder log_str = new StringBuilder();
				
				/** get the time stamp**/
				java.util.Date date= new java.util.Date();
				Timestamp timeStamp = new Timestamp(date.getTime());
				log_str.append(timeStamp + Log_SEPARATOR);
				
				/**Get all the keys of the hashtable ie all the process_id of up process**/
				Set<Integer> set_of_upProcess = upProcess.keySet();
				
				/** Making string out of setKeys of upProcess, seperated by ,**/
				
				log_str.append("[");
				for(Integer i : set_of_upProcess){
					log_str.append(i + UpSet_SEPARATOR);
				}
				log_str.append("]");
				log_str.append(Log_SEPARATOR);
				
				/*Append msg to the log_str*/
				log_str.append(state.toString() + Log_SEPARATOR);
				
				log_str.append(command + Log_SEPARATOR);
				
				log_buf.write(log_str + "\n");  // this will append new log to the existing file.
	            log_buf.close();
			} catch(IOException ex) {
				ex.printStackTrace();
			}
		} else {
			System.out.println("Application doesn't have permissions to write to this folder.");
		}
	}

	
	/** Functions to read DT Logger **/
	public STATE getLoggedState(int process_id) {
		final File Log_folder = new File(folder);
   	    final File myFile = new File(Log_folder + "/" + process_id + ".DTlog"); //get my file from the log 
        String msg = null;
        if (myFile.exists() && myFile.length() != 0) {
	   	    if (myFile.canRead()) {
	   	    	try {
		   	    	FileReader log_reader = new FileReader(myFile);
		   	    	BufferedReader log_buf = new BufferedReader(log_reader);
		   		    String str=null,temp;
		   		    while ((temp=log_buf.readLine()) != null) {
		   		    	str=temp;
		   		    }
		   		    String[] msg_splits = str.split(Log_SEPARATOR);
		   		    System.out.println(msg_splits[0] + " "  + msg_splits[1] + " " + msg_splits[2] + " " + msg_splits[3]);
		   		    msg = msg_splits[2];
	   	    	} catch(IOException ex) {
	   	    		ex.printStackTrace();
	   	    	}
	   	    }
        }
        if (msg == null) {
        	return null;
        }
        if (msg.equals(STATE.RESTING.toString())) {
        	return STATE.RESTING;
        } else if (msg.equals(STATE.UNCERTAIN.toString())) {
        	return STATE.UNCERTAIN;
        } else if (msg.equals(STATE.ABORT.toString())) {
        	return STATE.ABORT;
        } else if (msg.equals(STATE.COMMIT.toString())) {
        	return STATE.COMMIT;
        } 
        
        return null;
	}

	/** Functions to read DT Logger **/
	public String getLoggedCommand(int process_id) {
		final File Log_folder = new File(folder);
   	    final File myFile = new File(Log_folder + "/" + process_id + ".DTlog"); //get my file from the log 
        String msg = null;
        if (myFile.exists() && myFile.length() != 0) {
	   	    if (myFile.canRead()) {
	   	    	try {
		   	    	FileReader log_reader = new FileReader(myFile);
		   	    	BufferedReader log_buf = new BufferedReader(log_reader);
		   		    String str=null,temp;
		   		    while ((temp=log_buf.readLine()) != null) {
		   		    	str=temp;
		   		    }
		   		    String[] msg_splits = str.split(Log_SEPARATOR);
		   		    msg = msg_splits[3];
	   	    	} catch(IOException ex) {
	   	    		ex.printStackTrace();
	   	    	}
	   	    }
        }
        return msg;
	}
	
	public Set<Integer> LastUpProcessSet(int process_id) {
		final File Log_folder = new File(folder);
   	    final File myFile = new File(Log_folder+"/Log_"+process_id+".txt"); //get my file from the log 
        String upProcess_str=null;
        Set<Integer> up_set = new HashSet<Integer>(); // to return the set of up process
        
        if (myFile.exists() && myFile.length() != 0){
	   	    if(myFile.canRead()){
	   	    	try {
		   	    	FileReader log_reader = new FileReader(myFile);
		   	    	BufferedReader log_buf = new BufferedReader(log_reader);
		   		    String str=null,temp;
		   		    while((temp=log_buf.readLine())!=null){
		   		    	str=temp;
		   		    }
		   		    String[] msg_splits = str.split(Log_SEPARATOR);
		   		    upProcess_str = msg_splits[1];
		   	    	
		   		    String[] upStr_split = upProcess_str.split(UpSet_SEPARATOR);
		   		    for(String upStr: upStr_split){
		   		    	up_set.add(Integer.parseInt(upStr));
		   		    }
	   	    	} catch (IOException ex) {
	   	    		ex.printStackTrace();
	   	    	}
	   		 }
        }
	   
   	 return up_set;
	}
}
