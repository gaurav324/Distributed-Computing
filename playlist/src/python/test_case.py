import os
import shlex, subprocess
import socket
import readline # optional, will allow Up/Down/History in the console
import thread
import time

from optparse import OptionParser

execute_command = """java -classpath %(root)s/playlist/src:%(root)s/playlist/src -DCONFIG_NAME="%(root)s/playlist/src/config.properties" -DLOG_FOLDER="/tmp" -DDELAY="%(delay)s" -DPartialPreCommit="%(partial_pre_commit)s" -DPartialCommit="%(partial_commit)s" -DExtraCredit="%(extra_credit)s" -DDeathAfter="%(death_message_count)s=%(sending_process)s" ut.distcomp.playlist.Process %(process_no)s
"""

process_no_tuple_map = {}
process_no_pid_map = {}
proc_count = -1
def start_process(opts, args):
    global process_no_tuple_map
    global process_no_pid_map
    global proc_count

    process_no_pid_map = {}
    process_no_tuple_map = {}

    f = open("../config.properties")
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
        i += 1

    f.close()
    
    # Total number of process.
    proc_count = int(content.split("=")[1])
    print "Total process to spawn: ", proc_count

    # A map from process number to process Id.
    partial_pre_commit = opts.partial_pre_commit 
    partial_commit = opts.partial_commit
    extra_credit = opts.extra_credit

    (dying_process, message_count, sending_process) = opts.deathAfter.split("=")
    
    for proc_no in range(proc_count):
       
        default_message_count = -1
        default_sending_process = -1
        
        if (proc_no != 0):
            partial_pre_commit = -1
            partial_commit = -1
	    extra_credit = -1
        
        if str(proc_no) == dying_process:
            default_message_count = message_count
            default_sending_process = sending_process
         
        command = execute_command % {'root' : opts.root, 
                                     'process_no' : proc_no,
                                     'delay' : str(int(opts.delay) * 1000),
                                     'partial_pre_commit' : partial_pre_commit,
                                     'partial_commit' : partial_commit,
                                     'extra_credit':extra_credit,
                                     'death_message_count' : str(default_message_count),
                                     'sending_process' : str(default_sending_process),
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

    parser.add_option("--extra_credit",
                      help="This would illustrate us the bug introduced by not writing PRE-COMMIT to the log file.",
                      default=-1
                     )

    parser.add_option("--deathAfter",
                       help="Pass To=n=FROM format. where To = process which would die;  n=no of message and From=sending process. This would not include heartbeats.",
                       default="-1=-1=-1"
                     )
 
    parser.add_option("--demo",
                      help="""0. Everything worlks fine.
                              1. PARTICIPANT FAILURE AND RECOVERY. BEFORE SENDING YES/NO.
                              2. COORDINATOR FAILURE AND RECOVERY. AFTER SENDING VOTE-REQUEST.
                              3. COORDINATOR FAILURE AND RECOVERY. AFTER SENDING PRE-COMMIT.
                              4. CASCADE CO-ORDINATOR FAILURE - 2.
                              5. Partial PreCommit.
                              6. Partial Commit.
                              7. Extra Credit Error.
			      8. Future Coordinator Failure.
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
        
	opts.extra_credit = -1
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
        time.sleep(delay + 1)

        # Before that responds let us kill that.
        print "Killing Process 1\n"
        proc[1].kill()

        # Let us re-start after 30 secs.
        time.sleep(30)
        command = execute_command % {'root' : opts.root, 
                                     'process_no' : 1,
                                     'delay' : str(int(opts.delay) * 1000),
                                     'partial_pre_commit' : -1,
                                     'partial_commit' : -1,
				                     'extra_credit' : -1,
                                     'death_message_count' : -1,
                                     'sending_process' : -1
                                    }
        
        # Start the process.
        print "Going to execute: ", command
        args = shlex.split(command)
        process_no_pid_map[1] = subprocess.Popen(args);

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

        # Let us restart the coordinator in 40 sec.
        time.sleep(30)
	print "Starting coordinator again to Recover."
        command = execute_command % {'root' : opts.root, 
                                     'process_no' : 0,
                                     'delay' : str(int(opts.delay) * 1000),
                                     'partial_pre_commit' : -1,
                                     'partial_commit' : -1,
                                     'extra_credit' : -1,
                                     'death_message_count' : -1,
                                     'sending_process' : -1
                                    }
        
        # Start the process.
        print "Going to execute: ", command
        args = shlex.split(command)
        process_no_pid_map[0] = subprocess.Popen(args);

    # COORDINATOR FAILURE AND RECOVERY. AFTER SENDING PRE-COMMIT.
    if (opts.demo == str(3)):
        print "We would start a transaction and then kill one coordinator after sending PRE_COMMIT"
        print "Please monitor logs\n"
    
        # A list of processes is here.
        proc, conn = start_process(opts, args)
 
        time.sleep(5)
        conn[0].send("11--ADD--tumhiho=http://Aashiqui&")

        # Waiting for coordinator to dispatch vote-request.
        time.sleep(delay + 1)

        # Waiting to send YES/NO message.
        time.sleep(delay + 1)

        # Waiting until normal process time-out on pre-commit/abort message.
        time.sleep(delay + 2)

        # Kill the coordinator.
        print "Killing the coordinator."
        proc[0].kill()

        # Let us restart the coordinator in 30 sec.
        time.sleep(30)
	print "Let us start Coordinator again to Recover."
        command = execute_command % {'root' : opts.root, 
                                     'process_no' : 0,
                                     'delay' : str(int(opts.delay) * 1000),
                                     'partial_pre_commit' : -1,
                                     'partial_commit' : -1,
                                     'extra_credit' : -1,
                                     'death_message_count' : -1,
                                     'sending_process' : -1
                                    }
        
        # Start the process.
        print "Going to execute: ", command
        args = shlex.split(command)
        process_no_pid_map[0] = subprocess.Popen(args);

    # Kill two coordinators, one after another.
    if (opts.demo == str(4)):
        print "We would start a transaction and then kill two coordinators one after the another.\n"
        print "Please monitor logs\n"

        # A list of processes is here.
        proc, conn = start_process(opts, args)
 
        time.sleep(5)
        conn[0].send("11--ADD--tumhiho=http://Aashiqui&")

        # Waiting for coordinator to dispatch vote-request.
        time.sleep(delay + 1)

        # Kill the coordinator.
        print "Killing the coordinator."
        proc[0].kill()

        # Waiting to send YES/NO message.
        time.sleep(delay + 1)

        # Waiting until normal process time-out on pre-commit/abort message.
        time.sleep(delay + 2)

        # Someone would elect a new co-ordinator and inform him. Would wait so that it dispatches # the UR_SELECTED message.
        time.sleep(delay + 1)

        # Before new coordinator sends the state request.
        print "Killing the new coordinator."
        proc[1].kill()

        # Let us restart the coordinator in 30 sec.
        time.sleep(30)
        command = execute_command % {'root' : opts.root, 
                                     'process_no' : 0,
                                     'delay' : str(int(opts.delay) * 1000),
                                     'partial_pre_commit' : -1,
                                     'partial_commit' : -1,
				                     'extra_credit' :-1,
                                     'death_message_count' : -1,
                                     'sending_process' : -1
                                    }
        
        # Start the process.
        print "Going to execute: ", command
        args = shlex.split(command)
        process_no_pid_map[0] = subprocess.Popen(args);

        # Let us restart the coordinator in 30 sec.
        command = execute_command % {'root' : opts.root, 
                                     'process_no' : 1,
                                     'delay' : str(int(opts.delay) * 1000),
                                     'partial_pre_commit' : -1,
                                     'partial_commit' : -1,
                                     'extra_credit' : -1,
                                     'death_message_count' : -1,
                                     'sending_process' : -1
                                    }
        
        # Start the process.
        print "Going to execute: ", command
        args = shlex.split(command)
        process_no_pid_map[1] = subprocess.Popen(args);        
    
    if (opts.demo == str(5)):
        print "Coordinator would crash after partial pre-commit of 1 message."
        print "Please monitor logs."

        opts.partial_pre_commit = 1
           
        # A list of processes is here.
        proc, conn = start_process(opts, args)

        time.sleep(5)
        conn[0].send("11--ADD--tumhiho=http://Aashiqui&")

        # Restart the coordinator after 60 secs.
        time.sleep(60)
        command = execute_command % {'root' : opts.root, 
                                     'process_no' : 0,
                                     'delay' : str(int(opts.delay) * 1000),
                                     'partial_pre_commit' : -1,
                                     'partial_commit' : -1,
                                     'extra_credit' : -1,
                                     'death_message_count' : -1,
                                     'sending_process' : -1
                                    }
        
        # Start the process.
        print "Going to execute: ", command
        args = shlex.split(command)
        process_no_pid_map[1] = subprocess.Popen(args);       
        
    if (opts.demo == str(6)):
        print "Coordinator would crash after partial commit of 1 message."
        print "Please monitor logs."

        opts.partial_commit = 1
           
        # A list of processes is here.
        proc, conn = start_process(opts, args)

        time.sleep(5)
        conn[0].send("11--ADD--tumhiho=http://Aashiqui&")

        # Restart the coordinator after 60 secs.
        time.sleep(30)
	print "Restarting the Coordinator to recover"
        command = execute_command % {'root' : opts.root, 
                                     'process_no' : 0,
                                     'delay' : str(int(opts.delay) * 1000),
                                     'partial_pre_commit' : -1,
                                     'partial_commit' : -1,
                                     'extra_credit' : -1,
                                     'death_message_count' : -1,
                                     'sending_process' : -1
                                    }
        
        # Start the process.
        print "Going to execute: ", command
        args = shlex.split(command)
        process_no_pid_map[1] = subprocess.Popen(args);  

    if (opts.demo == str(7)):
        print "Coordinator would crash after writing commit to log and all other process die and recover (FOR EXTRA CREDIT!!)."
        print "Please monitor logs."

        opts.extra_credit = 1
           
        # A list of processes is here.
        proc, conn = start_process(opts, args)

        time.sleep(5)
        conn[0].send("11--ADD--tumhiho=http://Aashiqui&")

        time.sleep(40 + int(opts.delay) * 8)

        # Restart the coordinator after 10 secs.
        for i in range(proc_count):
            if i == 0:
                continue
            else:
                command = execute_command % { 'root' : opts.root, 
                                              'process_no' : i,
                                              'delay' : str(int(opts.delay) * 1000),
                                              'partial_pre_commit' : -1,
                                              'partial_commit' : -1,
       				                          'extra_credit' : -1,
                                              'death_message_count' : -1,
                                              'sending_process' : -1
                                            }
	
                # Start the process.
                print "Going to execute: ", command
                args = shlex.split(command)
                process_no_pid_map[i] = subprocess.Popen(args);  

    # Future Coordinator Failure.
    if (opts.demo == str(8)):
        print "We would start a transaction and then kill the future coordinator to be elected and then the present coordinator.\n"
        print "Please monitor logs\n"


        # A list of processes is here.
        proc, conn = start_process(opts, args)
 
        time.sleep(5)
        conn[0].send("11--ADD--tumhiho=http://Aashiqui&")

        # Waiting for coordinator to dispatch vote-request.
        time.sleep(delay + 1)

        # Kill the coordinator.
        print "Killing the future coordinator."
        proc[1].kill()

        # Sleeping before killing present coordinator.
        time.sleep(1)
        proc[0].kill()

        # Waiting until normal process time-out on pre-commit/abort message.
        time.sleep(delay + 2)

        # Someone would elect a new co-ordinator and inform him. Would wait so that it dispatches # the UR_SELECTED message.
        time.sleep(delay + 1)

	
    
    from IPython import embed
    embed()
