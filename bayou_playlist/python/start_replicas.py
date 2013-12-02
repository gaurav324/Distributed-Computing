import os
import shlex, subprocess
import socket
import readline # optional, will allow Up/Down/History in the console
import thread
import time

from optparse import OptionParser

execute_command = """java -classpath %(root)s/bin -DCONFIG_NAME="%(root)s/intial_config.properties" -DLOG_FOLDER="/tmp" ut.distcomp.replica.Replica %(process_no)s"""

#java -classpath ./bin -DLOG_FOLDER=/tmp -DCONFIG_NAME=./intial_config.properties ut.distcomp.replica.Replica  0

process_no_pid_map = {}
proc_count = -1
def start_process(opts, args):
    global process_no_pid_map
    global proc_count

    process_no_pid_map = {}
    process_no_tuple_map = {}

    f_client = open(opts.root +  "/intial_config_new.properties","w")
    f = open(opts.root +  "/intial_config.properties")
    f.readline()
    content = f.readline()
    lines = f.readlines()
    process_number = {}

    i=0
    
    for line in lines:
        if line.startswith("port"):
            port = int(line.split("=")[1])
            continue
        elif line.startswith("proc"):
	    process_number[i] = str(line.split("=")[1].strip())
	    print process_number[i]
	    continue
	elif line.startswith("host"):
            host = line.split("=")[1].strip()
        else:
            continue
	
        process_no_tuple_map[process_number[i]] = (host, port)
        i += 1
    f_client.write(str(process_no_tuple_map))
    f.close()
    f_client.close() 
    # Total number of process.
    proc_count = int(content.split("=")[1])
    print "Total process to spawn: ", proc_count

    for proc_no in range(proc_count):
       
        default_message_count = -1
        default_sending_process = -1
        
        print process_number[proc_no]
        command = execute_command % {'root' : opts.root, 
                                     'process_no' : process_number[proc_no],
                                    }
        print "Going to execute: ", command
        args = shlex.split(command)
        process_no_pid_map[proc_no] = subprocess.Popen(args);
    
    process_no_socket_map = {}
    time.sleep(3)
    return (process_no_pid_map, process_no_socket_map)

def getopts():
    """
    Parse the command line options.

    """
    parser = OptionParser()

    parser.add_option("--delay",
                      help="This would ensure a delay(secs) in addition to timeouts.",
                      default=0)

    parser.add_option("--root",
                      help="Location where you have copied the project.")


    opts,args = parser.parse_args()

    return opts, args


if __name__ == "__main__":
    opts, args = getopts()
    #delay = float(opts.delay)
    start_process(opts, args);
 	
    
    from IPython import embed
    embed()
