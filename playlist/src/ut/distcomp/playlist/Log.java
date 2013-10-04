package ut.distcomp.playlist;

import java.io.*;
//import java.io.FileNotFoundException;
import java.util.*;

public class Log {

// To write log to the file	
	public void WriteToLog(int process_id,String msg) throws IOException {
		final File Log_folder = new File(System.getProperty("user.dir"), "Process_logs");
		if(!Log_folder.exists()){
		   if(Log_folder.mkdirs()){
			   System.out.println("first process to log and create directory");
		   }else{
			   System.out.println("Failed to create a Log directory. Something went wrong");
		   }
		 }
	//Write log to the process file.
		if(Log_folder.canWrite()){
		
			final File myFile = new File(Log_folder, "Log_"+ process_id + ".txt");
			
			if(!myFile.exists()){ // if file doesn't exist create it
					myFile.createNewFile();
			}
		
			FileWriter log_writer;
				log_writer = new FileWriter(myFile.getName(), true);
				log_writer.write(msg+"\n");  // this will append new log to the existing file.
			
		}else{
			System.out.println("Application doesn't have permissions to write to this folder");
		}
	}

// To retrieve log from the file
     public List<String> ReadFromLog(int process_id) throws IOException {
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
    	 
    	return log_list;	 
   }
}