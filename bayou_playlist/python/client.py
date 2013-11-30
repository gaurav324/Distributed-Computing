import os
import shlex, subprocess
import socket
import readline # optional, will allow Up/Down/History in the console
import thread
import time

from optparse import OptionParser


process_no_tuple_map = {}
process_no_pid_map = {}
proc_count = -1
my_replicaId = 0
my_latest_write = "X"
my_Id = -1;


def read_config():
    global process_no_tuple_map
    global process_no_pid_map
    global proc_count
    global my_replicaId #if my replicaId doesn't respond we need to assign a new replica.
    global my_Id

    #my_Id = opts.client_ID
    #my_replicaId = opts.my_replicaId
    process_no_pid_map = {}
    process_no_tuple_map = {}

    f = open("/Users/Gill/Documents/Distri_projects/distri-projects/bayou_playlist/intial_config.properties")
    content = f.readline()
    lines = f.readlines()

    i=0
    for line in lines:
        if line.startswith("port"):
            port = int(line.split("=")[1])
            continue
        elif line.startswith("host"):
            host = line.split("=")[1].strip()
        else:
            continue
        process_no_tuple_map[i] = (host, port)
	#print process_no_tuple_map[i]
        i += 1

    f.close()
    
    # Total number of process.
    #proc_count = int(content.split("=")[1])
    #print "Total process to spawn: ", proc_count

    
    #process_no_socket_map = {}
    #my_replicaActive = 1

    #time.sleep(3)
    #for no, tup in process_no_tuple_map.iteritems():
	


def getopts():
    """
    Parse the command line options.

    """
    parser = OptionParser()

    parser.add_option("--client_ID",
		      help="unique client ID.",
		      default=0)		
			
    parser.add_option("--my_replicaId",
		      help="This the ID of the replica to which this client should contact.",
		      default=0)

    parser.add_option("--delay",
                      help="This would ensure a delay(secs) in addition to timeouts.",
                      default=0)

    parser.add_option("--root",
                      help="Location where you have copied the project.")

    opts,args = parser.parse_args()

    return opts, args

def test_func():

    print "hello i m working\n"


def command(cmd):
    read_config()
    global my_latest_write
    global my_replicaId
    command = cmd.split("@")
     
    valid_commands = ["ADD", "DELETE", "EDIT"]
    if(command[0] == "READ"):
	tup = process_no_tuple_map[int(my_replicaId)]
	s = socket.socket()
	print tup
	s.connect(tup)
	
	cmd_str = ("-1--" + command[0] + "--" + my_latest_write + "&")

	print cmd_str
	response = s.send(cmd_str)
	print response
	if(response == "NO"):
	    print "Cannot establish session with my_relica" + my_replicaId
	    for no, tup in process_no_tuple_map.iteritems():
		if(no != int(my_replicaId)):
		    s = socket.socket()
		    print tup
		    s.connect(tup)
		    cmd_str = ("-1--" + command[0] + "--" + my_latest_write + "&")
		    s.send(cmd_str)
		    response = s.recv(1024) #will recieve latest write from the 
		    if(reponse == "NO"):
			continue
		    else:
			my_replicaId = str(no)
			print "koi mil gaya\n"
    
	    			
	print command[0]

    elif(command[0] in valid_commands):
	tup = process_no_tuple_map[int(my_replicaId)]
	s = socket.socket()
	print tup
	s.connect(tup)
	cmd_str = ("-1--OPERATION--" + command[0] + "==" + command[1] + "&")

	print cmd_str
	s.send(cmd_str)
	my_latest_write = s.recv(1024)
	print my_latest_write
    else:
	print "Not a valid command\n"	
	
	 



if __name__ == "__main__":
    opts, args = getopts()
    #delay = float(opts.delay)
   
    my_replicaId = opts.my_replicaId
    #proc, conn = connect_replica(opts, args);
   
        #conn[0].send("11--ADD--tumhiho=http://Aashiqui&")
 
	
    
    from IPython import embed
    embed()
