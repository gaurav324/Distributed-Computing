import os
import shlex, subprocess
import socket
import readline # optional, will allow Up/Down/History in the console
import thread
import time

from optparse import OptionParser

execute_command = """java -classpath %(root)s/playlist/src:%(root)s/playlist/bin -DCONFIG_NAME="%(root)s/playlist/src/config.properties" -DLOG_FOLDER="/tmp" -DDELAY="%(delay)s" -DPartialPreCommit="1" ut.distcomp.playlist.Process %(process_no)s
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
                      help="""1. PARTICIPANT FAILURE AND RECOVERY. BEFORE SENDING YES/NO.
                              2. COORDINATOR FAILURE AND RECOVERY. AFTER SENDING VOTE-REQUEST.
                              3. CASCADE COORDINATOR FAILURE.""")

    opts,args = parser.parse_args()

    return opts, args

def killAll(pid_map):
    for pid, proc in pid_map.iteritems():
        proc.kill()

def start_listening():
    port = 5000
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind(('localhost', port))
    s.listen(5)
    
    while 1:
        client, address = s.accept()
        data = client.recv(1024)
        client.close()
        break
    return data

if __name__ == "__main__":
    opts, args = getopts()

    # A list of processes is here.
    proc, conn = start_process(opts, args)
    
    import atexit
    atexit.register(killAll, pid_map=proc)

    delay = float(opts.delay)

    # PARTICIPANT FAILURE and RECOVERY.
    if (opts.demo == str(1)):
        print "We would start a transaction and then kill one process (non-coordinator) before sending a YES.\n"
        print "Please monitor logs\n"

        time.sleep(delay)
        conn[0].send("11--ADD--tumhiho=http://Aashiqui&");
        
        # Waiting for vote-request to reach.
        time.sleep(delay)

        # Extra buffer to ensure the process has received VOTE-REQ.
        time.sleep(1)

        # Before that responds let us kill that.
        print "Killing Process 1\n"
        proc[1].kill()

    # COORDINATOR FAILURE AND RECOVERY.
    if (opts.demo == str(2)):
        print "We would start a transaction and then kill one coordinator after sending VOTE-REQ.\n"
        print "Please monitor logs\n"
    
        time.sleep(delay)
        conn[0].send("11--ADD--tumhiho=http://Aashiqui&")

        # Waiting for coordinator to dispatch vote-request.
        time.sleep(delay)

        # Extra buffer to ensure the process has received VOTE-REQ.
        time.sleep(1)

        # Kill the coordinator.
        print "Killing the coordinator."
        proc[0].kill()

    if (opts.demo == str(3)):
       print "We would start a transaction and then kill two coordinators after sending VOTE-REQ.\n"
       print "Please monitor logs\n"

       time.sleep(delay)
       conn[0].send("11--ADD--tumhiho=http://Aashiqui&")

       # Waiting for coordinator to dispatch vote-request.
       time.sleep(delay)

       # Extra buffer to ensure the process has received VOTE-REQ.
       time.sleep(1)

       # Kill the coordinator.
       print "Killing the coordinator."
       proc[0].kill()

       # Waiting until normal process time-out on pre-commit/abort message.
       time.sleep(delay + 1)

       # Someone would elect a new co-ordinator and inform him. Would wait so that it dispatches         # the UR_SELECTED message.
       time.sleep(delay)

       # Before new coordinator sends the state request.
       proc[1].kill()
   

    #time.sleep(5)
    
    #conn[0].send("11--ADD--tumhiho=http://Aashiqui&")
 
    # Open a port to listen.
    #data = start_listening()
    #print data

    from IPython import embed
    embed()
