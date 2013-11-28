import os
import shlex, subprocess
import socket 
import time 
import readline 
import thread

from multiprocessing import Process

from optparse import OptionParser


#s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
#s.connect(("localhost", 4000))


def execute_file(filename,PortNumber):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect(("localhost",PortNumber))
    with open(filename) as fp:
	for line in fp:
	    cmd = line.split(" ")
	    if cmd[0]!='\n':
		time.sleep(float(cmd[0]))
		print ('Executing "'+cmd[1].rstrip('\n')+'"')
		s.send(cmd[1].rstrip('\n'))
		l = cmd[1].split("--")
		if('INQUIRY' in l):
		    response = s.recv(6553600)
		    print response
   # print 'executing', filename 

def getopts():
    """
    Parse the command line options

    """    
    parser = OptionParser()
    
    parser.add_option("--TimeDelay",
		      help="To Introduce a time Delay(sec) on clients.",
		      default=0)
    parser.add_option("--Port",
		      help="Specify the port number to be used",
		      default=4001)
    parser.add_option("--FileNames",
		      help="Enter files names: File1==File2==File3",
		      default="File1")
	
    opts,args = parser.parse_args()
    return opts, args


if __name__=="__main__":

    opts, args = getopts()
    Files = opts.FileNames.split('==');
    for File in Files:
	p = Process(target=execute_file,args=(File,opts.Port,))
	p.start()
	#p.join()
			
