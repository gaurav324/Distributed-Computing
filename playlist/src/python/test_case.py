import os
import shlex, subprocess
import socket
import readline # optional, will allow Up/Down/History in the console
import thread
import time

from optparse import OptionParser

execute_command = """java -classpath %(root)s/playlist/src:%(root)s/playlist/bin -DCONFIG_NAME="%(root)s/playlist/src/config.properties" -DLOG_FOLDER="/tmp" -DDELAY="%(delay)s" -DPartialPreCommit="%(partial_pre_commit)s" -DPartialCommit="%(partial_commit)s" ut.distcomp.playlist.Process %(process_no)s
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
    partial_pre_commit = opts.partial_pre_commit 
    partial_commit = opts.partial_commit
    for proc_no in range(proc_count):
        if (proc_no != 0):
            partial_pre_commit = -1
            partial_commit = -1

        command = execute_command % {'root' : opts.root, 
                                     'process_no' : proc_no,
                                     'delay' : str(int(opts.delay) * 1000),
                                     'partial_pre_commit' : partial_pre_commit,
                                     'partial_commit' : partial_commit,
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

    import atexit
    atexit.register(killAll, pid_map=process_no_pid_map)

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

    parser.add_option("--partial_pre_commit",
                      help="This would kill the first coordinator after sending first X pre_commits.",
                      default=-1
                     )

    parser.add_option("--partial_commit",
                      help="This would kill the first coordinator after the first X commits.",
                      default=-1
                     )
    parser.add_option("--demo",
                      help="""0. Everything worlks fine.
                              1. PARTICIPANT FAILURE AND RECOVERY. BEFORE SENDING YES/NO.
                              2. COORDINATOR FAILURE AND RECOVERY. AFTER SENDING VOTE-REQUEST.
                              3. COORDINATOR FAILURE AND RECOVERY. AFTER SENDING PRE-COMMIT.
                              4. CASCADE CO-ORDINATOR FAILURE - 2.
                              5. Partial PreCommit.
                              6. Partial Commit.
                           """)

    opts,args = parser.parse_args()

    return opts, args

def killAll(pid_map):
    for pid, proc in pid_map.iteritems():
        proc.kill()

if __name__ == "__main__":
    opts, args = getopts()
    delay = float(opts.delay)

    # Normal case. No body dies.
    if (opts.demo == str(0)):
        
        # A list of processes is here.
        proc, conn = start_process(opts, args)

        time.sleep(5)
        conn[0].send("11--ADD--tumhiho=http://Aashiqui&")
 
    # PARTICIPANT FAILURE AND RECOVERY. BEFORE SENDING YES/NO.
    if (opts.demo == str(1)):
        print "We would start a transaction and then kill one process (non-coordinator) before sending a YES.\n"
        print "Please monitor logs\n"

        # A list of processes is here.
        proc, conn = start_process(opts, args)
 
        time.sleep(5)
        conn[0].send("11--ADD--tumhiho=http://Aashiqui&");
        
        # Waiting for vote-request to reach.
        time.sleep(delay)

        # Before that responds let us kill that.
        print "Killing Process 1\n"
        proc[1].kill()

    # COORDINATOR FAILURE AND RECOVERY. AFTER SENDING VOTE-REQUEST.
    if (opts.demo == str(2)):
        print "We would start a transaction and then kill one coordinator after sending VOTE-REQ."
        print "Please monitor logs"
    
        # A list of processes is here.
        proc, conn = start_process(opts, args)
 
        time.sleep(5)
        conn[0].send("11--ADD--tumhiho=http://Aashiqui&")

        # Waiting for coordinator to dispatch vote-request.
        time.sleep(delay + 1)
        
        # Kill the coordinator.
        print "Killing the coordinator."
        proc[0].kill()

    # COORDINATOR FAILURE AND RECOVERY. AFTER SENDING PRE-COMMIT.
    if (opts.demo == str(3)):
        print "We would start a transaction and then kill one coordinator after sending PRE_COMMIT"
        print "Please monitor logs\n"
    
        # A list of processes is here.
        proc, conn = start_process(opts, args)
 
        time.sleep(5)
        conn[0].send("11--ADD--tumhiho=http://Aashiqui&")

        # Waiting for coordinator to dispatch vote-request.
        time.sleep(delay + 0.5)

        # Waiting to send YES/NO message.
        time.sleep(delay + 0.5)

        # Waiting until normal process time-out on pre-commit/abort message.
        time.sleep(delay + 2)

        # Kill the coordinator.
        print "Killing the coordinator."
        proc[0].kill()


    if (opts.demo == str(4)):
        print "We would start a transaction and then kill two coordinators one after the another.\n"
        print "Please monitor logs\n"

        # A list of processes is here.
        proc, conn = start_process(opts, args)
 
        time.sleep(5)
        conn[0].send("11--ADD--tumhiho=http://Aashiqui&")

        # Waiting for coordinator to dispatch vote-request.
        time.sleep(delay)

        # Kill the coordinator.
        print "Killing the coordinator."
        proc[0].kill()

        # Waiting to send YES/NO message.
        time.sleep(delay)

        # Waiting until normal process time-out on pre-commit/abort message.
        time.sleep(delay + 2)

        # Someone would elect a new co-ordinator and inform him. Would wait so that it dispatches # the UR_SELECTED message.
        time.sleep(delay)

        # Before new coordinator sends the state request.
        print "Killing the new coordinator."
        proc[1].kill()

    if (opts.demo == str(5)):
        print "Coordinator would crash after partial pre-commit of 1 message."
        print "Please monitor logs."

        opts.partial_pre_commit = 1
           
        # A list of processes is here.
        proc, conn = start_process(opts, args)

        time.sleep(5)
        conn[0].send("11--ADD--tumhiho=http://Aashiqui&")
        
    if (opts.demo == str(6)):
        print "Coordinator would crash after partial commit of 1 message."
        print "Please monitor logs."

        opts.partial_commit = 1
           
        # A list of processes is here.
        proc, conn = start_process(opts, args)

        time.sleep(5)
        conn[0].send("11--ADD--tumhiho=http://Aashiqui&")

    from IPython import embed
    embed()
