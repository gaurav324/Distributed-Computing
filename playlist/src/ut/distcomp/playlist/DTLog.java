package ut.distcomp.playlist;

import java.io.*;
//import java.io.FileNotFoundException;
import java.util.*;
import java.lang.*;
import java.sql.Timestamp;
import java.util.Date;

public class DTLog {
	public static final String Log_SEPARATOR = "%%";
	public static final String UpSet_SEPARATOR = "|";	 // to separate upProcess
	
//	String folder;
//	
//	DTLog(folder) {
//		this.folder = folder;
//	}
	
	//Function To write log to the file	
	public static void WriteToLog(int process_id, String msg, Hashtable<Integer,Long>upProcess) throws IOException {
		final File Log_folder = new File("Process_logs"); // creates a new folder to store log files
	
		if(!Log_folder.exists()){
			
		   if(Log_folder.mkdirs()){
			   System.out.println("first process to log and create directory");
		   }else{
			   System.out.println("Failed to create a Log directory. Something went wrong");
		   }
		 }
	
		//Write log to the process file.
		if(Log_folder.canWrite()){
			
			final File myFile = new File(Log_folder + "/Log_"+ process_id + ".txt");
			
			if(!myFile.exists()){ // if file doesn't exist create it
				myFile.createNewFile();
			}
		
			FileWriter log_writer = new FileWriter(myFile.getAbsoluteFile(), true);
			BufferedWriter log_buf = new BufferedWriter(log_writer);
			
			StringBuilder log_str = new StringBuilder();
			
			/** get the time stamp**/
			java.util.Date date= new java.util.Date();
			Timestamp timeStamp = new Timestamp(date.getTime());
			log_str.append(timeStamp+Log_SEPARATOR);
			
			
			
			/**Get all the keys of the hashtable ie all the process_id of up process**/
			Set<Integer> set_of_upProcess = upProcess.keySet();
			
			/**making string out of setKeys of upProcess, seperated by |**/
			for(Integer i : set_of_upProcess){
				log_str.append(i+UpSet_SEPARATOR);
			}
			log_str.append(Log_SEPARATOR);
			
			/*Append msg to the log_str*/
			log_str.append(msg+Log_SEPARATOR);
			
			log_buf.write(log_str+"\n");  // this will append new log to the existing file.
            log_buf.close();			
		}else{
			System.out.println("Application doesn't have permissions to write to this folder");
		}
	}

	
	/** functions to read DT Logger**/
	public static String LastLogMsg(int process_id) throws IOException {
		final File Log_folder = new File("Process_logs");
   	    final File myFile = new File(Log_folder+"/Log_"+process_id+".txt"); //get my file from the log 
        String msg_return=null;
        if(myFile.exists() && myFile.length()!=0){
	   	    if(myFile.canRead()){
	   	    	FileReader log_reader = new FileReader(myFile);
	   	    	BufferedReader log_buf = new BufferedReader(log_reader);
	   		    String str=null,temp;
	   		    while((temp=log_buf.readLine())!=null){
	   		    	str=temp;
	   		    }
	   		    String[] msg_splits = str.split(Log_SEPARATOR);
	   		    msg_return = msg_splits[2];
	   	    	
	   	    }
        }else{
        	msg_return = "Nothing in the log";
        }
	   	 System.out.println(msg_return);   
   	 return msg_return;
	}
	
	
	public static Set<Integer> LastUpProcessSet(int process_id) throws IOException {
		final File Log_folder = new File("Process_logs");
   	    final File myFile = new File(Log_folder+"/Log_"+process_id+".txt"); //get my file from the log 
        String upProcess_str=null;
        Set<Integer> up_set = new HashSet<Integer>(); // to return the set of up process
        
        if(myFile.exists() && myFile.length()!=0){
	   	    if(myFile.canRead()){
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
	   		 }
        }
	   
   	 return up_set;
	}
// To retrieve log from the file
     public static List<String> ReadFromLog(int process_id) throws IOException {
    	 final File Log_folder = new File(System.getProperty("user.dir"), "Process_logs");
    	 final File myFile = new File(Log_folder,"Log_"+process_id+".txt"); //get my file from the log
    	 List<String> log_list = new ArrayList<String>(); // to return the list of logs
    	 if(myFile.canRead()){
    		 FileReader log_reader = new FileReader(myFile);// throws FileNotFoundException;
    		 BufferedReader log_buf = new BufferedReader(log_reader);
    		 String str;
    		 while((str = log_buf.readLine())!=null){
    			 log_list.add(str);
    		 }
    	}
    	 else{System.out.println("wt the hell\n");}
    	 
    	return log_list;	 
   }
}
