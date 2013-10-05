import os
import shlex, subprocess
import socket
import readline # optional, will allow Up/Down/History in the console
import thread
import time

from optparse import OptionParser

execute_command = """java -classpath %(root)s/playlist/src:%(root)s/playlist/bin -DCONFIG_NAME="%(root)s/playlist/src/config.properties" -DLOG_FOLDER="/tmp" -DDELAY="%(delay)s" ut.distcomp.playlist.Process %(process_no)s
"""

def start_process(opts, args):
    f = open("../config.properties")
    content = f.readline()
    lines = f.readlines()

    process_no_tuple_map = {}
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
        i += 1

    f.close()
    
    # Total number of process.
    proc_count = int(content.split("=")[1])
    print "Total process to spawn: ", proc_count

    # A map from process number to process Id.
    process_no_pid_map = {}
    for proc_no in range(proc_count):
        command = execute_command % {'root' : opts.root, 
                                     'process_no' : proc_no,
                                     'delay' : str(int(opts.delay) * 1000)
                                    }
        print "Going to execute: ", command
        args = shlex.split(command)
        process_no_pid_map[proc_no] = subprocess.Popen(args);
    
    process_no_socket_map = {}
    time.sleep(3)
    for no, tup in process_no_tuple_map.iteritems():
        s = socket.socket()
        print tup
        s.connect(tup)
        process_no_socket_map[no] = s

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

    parser.add_option("--demo",
                      help="View demo. You have total X demos.")

    opts,args = parser.parse_args()

    return opts, args

def killAll(pid_map):
    for pid, proc in pid_map.iteritems():
        proc.kill()

if __name__ == "__main__":
    opts, args = getopts()

    # A list of processes is here.
    proc, conn = start_process(opts, args)
    
    import atexit
    atexit.register(killAll, pid_map=proc)

    if (opts.demo == str(1)):
        print "We would start a transaction and then kill one process (non-coordinator) before sending a YES.\n"
        print "Please monitor logs\n"

        time.sleep(float(opts.delay))
        conn[0].send("11--ADD--tumhiho=http://Aashiqui&");
        
        # Waiting for vote-request to reach.
        time.sleep(float(opts.delay))

        # Extra buffer to ensure the process has received VOTE-REQ.
        time.sleep(1)

        # Before that responds let us kill that.
        print "Killing Process 1\n"
        proc[1].kill()
    
    from IPython import embed
    embed()
