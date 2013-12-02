import os
import shlex, subprocess
import socket
import readline # optional, will allow Up/Down/History in the console
import thread
import time

from optparse import OptionParser


process_no_tuple_map = {}
proc_count = -1
my_replicaId = 0
my_latest_write = "X==X"
my_Id = "-1";
root = "/"
sockets = {}
execute_command = """java -classpath %(root)s/bin -DCONFIG_NAME="%(root)s/intial_config.properties" -DLOG_FOLDER="/tmp" ut.distcomp.replica.Replica %(process_no)s"""

def join():
    global sockets 
    global process_no_tuple_map

    # Initially send a mesage to the my_replicaId.
    if my_replicaId not in sockets:
	s = socket.socket()
	sockets[my_replicaId] = s
    sockets[my_replicaId].connect(process_no_tuple_map[my_replicaId])

    f = open(opts.root + "/intial_config.properties", "r")
    lines = f.readlines()
    f.close()

    procno = int(lines[-1].split("=")[0][-1]) + 1
    portno = int(lines[-3].split("=")[1]) + 1

    sockets[my_replicaId].send(my_Id + "--JOIN--localhost==" + str(portno) +  "&")
    newId = s.recv(1024)
    
    lines[1] = "NumProcesses=" + str(procno + 1)

    lines.append("port" + str(procno) + "=" + str(portno))
    lines.append("proc" + str(procno) + "=" + str(newId))
    lines.append("host" + str(procno) + "=" + "localhost")
    
    #adding to process_no_tuple_map
    process_no_tuple_map[str(newId)] = ("localhost", portno)
    
    f = open(opts.root + "/intial_config.properties", "w")
    for line in lines:
	f.write(str(line.strip()))
	f.write("\n")
    f.close()
    
    #editing intial_config_new.properties
    f_client = open(opts.root +  "/intial_config_new.properties","w")
    f_client.write(str(process_no_tuple_map))
    f_client.close()
    command = execute_command % {'root' : opts.root, 
				 'process_no' : newId,
                                }
    print "Starting new replica by running: " + command + " &"
    args = shlex.split(command)
    subprocess.Popen(args);

def read_config():
    global process_no_tuple_map
    process_no_tuple_map = {}

    f = open(opts.root + "/intial_config_new.properties")
    lines = f.readlines()
    f.close()
    
    process_no_tuple_map = eval(lines[0])
    print process_no_tuple_map
 
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

    parser.add_option("--logFolder",
                      help="Location where all the log files are kept.")


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
	tup = process_no_tuple_map[my_replicaId]
	s = socket.socket()
	print tup
	s.connect(tup)
	
	cmd_str = ("-1--" + command[0] + "--" + my_latest_write + "&")

	print cmd_str
	s.send(cmd_str)
	response = s.recv(1024)
	#print response
	if(response == "NO"):
	    print "Cannot establish session with my_relica: " + my_replicaId + "\n"
	    print "Looking for another replica to execute READ\n"
	    for no, tup in process_no_tuple_map.iteritems():
		if(no != my_replicaId):
		    s = socket.socket()
		    print tup
		    s.connect(tup)
		    cmd_str = ("-1--" + command[0] + "--" + my_latest_write + "&")
		    s.send(cmd_str)
		    response = s.recv(1024) #will recieve latest write from the 
		    if(response == "NO"):
			continue
		    else:
			my_replicaId = str(no)
			print response 
			print "New ReplicaId = " + my_replicaId 
			break   
    
	    			
	#print command[0]

    elif(command[0] in valid_commands):
	tup = process_no_tuple_map[my_replicaId]
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
	
	 
def isolate(x):
    read_config()
    for i in process_no_tuple_map:
	if(i == x):
	    s = socket.socket()
	    tup = process_no_tuple_map[i]
	    s.connect(tup)
	    cmd_str =  ("-1--DISCONNECT--X&")
	    s.send(cmd_str)
	    print cmd_str
	else:
	    s = socket.socket()
	    tup = process_no_tuple_map[i]
	    s.connect(tup)
	    cmd_str =  ("-1--DISCONNECT--" + str(x) + "&")
	    s.send(cmd_str)
	    print cmd_str

	
def reconnect(x):
    read_config()
    for i in process_no_tuple_map:
	if(i == x):
	    s = socket.socket()
	    tup = process_no_tuple_map[i]
	    s.connect(tup)
	    cmd_str =  ("-1--CONNECT--X&")
	    s.send(cmd_str)
	    print cmd_str
	else:
	    s = socket.socket()
	    tup = process_no_tuple_map[i]
	    s.connect(tup)
	    cmd_str =  ("-1--CONNECT--" + str(x) + "&")
	    s.send(cmd_str)
	    print cmd_str

	    

def breakConnection(i,j):
    read_config()
    #telling i to disconnect from j
    s = socket.socket()
    tup = process_no_tuple_map[i]
    s.connect(tup)
    cmd_str =  ("-1--DISCONNECT--" + str(j) + "&")
    s.send(cmd_str)    
    print cmd_str + "\t"

    #telling j to disconnect from i
    s = socket.socket()
    tup = process_no_tuple_map[j]
    s.connect(tup)
    cmd_str =  ("-1--DISCONNECT--" + str(i) + "&")
    s.send(cmd_str)    
    print cmd_str


def recoverConnection(i,j):
    read_config()
    #telling i to disconnect from j
    s = socket.socket()
    tup = process_no_tuple_map[i]
    s.connect(tup)
    cmd_str =  ("-1--CONNECT--" + str(j) + "&")
    s.send(cmd_str)    
    print cmd_str + "\t"

    #telling j to disconnect from i
    s = socket.socket()
    tup = process_no_tuple_map[j]
    s.connect(tup)
    cmd_str =  ("-1--CONNECT--" + str(i) + "&")
    s.send(cmd_str)    
    print cmd_str

    
def printLog(x):
    read_config()
    if(x == "ALL"):
	for i in process_no_tuple_map:
	    logFile = (opts.logFolder + "replicaLogs/" + str(i) + ".log")
	    print str(i) + ".log"
	    os.system('cat %(logFile)s'% locals())
	    print "\n"

    else:
	logFile = (opts.logFolder + "replicaLogs/" + str(x) + ".log")
	print str(x) + ".log"
	os.system('cat %(logFile)s'% locals())
	print "\n"


def Pause(x):
    read_config()
    if(x == "ALL"):
	for i in process_no_tuple_map:
	    s = socket.socket()
	    tup = process_no_tuple_map[i]
	    s.connect(tup)
	    cmd_str =  ("pause&")
	    s.send(cmd_str)
	    print cmd_str
    else:
	s = socket.socket()
	tup = process_no_tuple_map[x]
	s.connect(tup)
	cmd_str = ("pause&")
	s.send(cmd_str)
	print cmd_str
	    
def Continue(x):
	read_config()
	if(x == "ALL"):
	    for i in process_no_tuple_map:
		s = socket.socket()
		tup = process_no_tuple_map[i]
		s.connect(tup)
		cmd_str =  ("continue&")
		s.send(cmd_str)
		print cmd_str
	else:
	    s = socket.socket()
	    tup = process_no_tuple_map[x]
	    s.connect(tup)
	    cmd_str = ("continue&")
	    s.send(cmd_str)
	    print cmd_str


def leave(x):
    read_config()
    global process_no_tuple_map

    #sending message to i to retire
    s = socket.socket()
    tup = process_no_tuple_map[x]
    s.connect(tup)
    cmd_str = "leave&"
    s.send(cmd_str)
    print cmd_str
    response = s.recv(1024)
    #response = "YES"
    if(response != "NO"):
	del process_no_tuple_map[x]
	f = open(opts.root +  "/intial_config_new.properties","w")
	#for i in process_no_tuple_map:
	 #   print process_no_tuple_map[i]
	 #   print i
	 #   f.write(str(process_no_tuple_map[i]))
	  #  f.write("\n")
	f.write(str(process_no_tuple_map))   
	f.close()	
    else:
	print "Could not leave node: "+ x + "\n"

if __name__ == "__main__":
    opts, args = getopts()
    read_config()
    
    my_replicaId = opts.my_replicaId
    my_Id = opts.client_ID
    
    from IPython import embed
    embed()
