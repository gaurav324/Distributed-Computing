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
my_latest_write = "X"
my_Id = "-1";
root = "/"
sockets = {}
execute_command = """java -classpath %(root)s/bin -DCONFIG_NAME="%(root)s/intial_config.properties" -DLOG_FOLDER="/tmp" ut.distcomp.replica.Replica %(process_no)s"""

def ensure_connect(replicaId, dispose=False):
    global sockets
    if (dispose):
        sockets = {}
    if replicaId not in sockets:
        s = socket.socket()
        sockets[replicaId] = s
        sockets[replicaId].connect(process_no_tuple_map[replicaId])
    return sockets[replicaId]

def join(replicaId = my_replicaId):
    global process_no_tuple_map

    # Initially send a mesage to the replicaId.
    replicaId = str(replicaId)
    s = ensure_connect(replicaId)

    f = open(opts.root + "/intial_config.properties", "r")
    lines = f.readlines()
    f.close()

    procno = int(lines[-1].split("=")[0][-1]) + 1
    portno = int(lines[-3].split("=")[1]) + 1

    s.send(my_Id + "--JOIN--localhost==" + str(portno) +  "&")
    newId = s.recv(1024)
    
    lines[1] = "NumProcesses=" + str(procno + 1)

    lines.append("port" + str(procno) + "=" + str(portno))
    lines.append("proc" + str(procno) + "=" + str(newId))
    lines.append("host" + str(procno) + "=" + "localhost")
    
    # Adding to process_no_tuple_map.
    process_no_tuple_map[str(newId)] = ("localhost", portno)
    
    f = open(opts.root + "/intial_config.properties", "w")
    for line in lines:
        f.write(str(line.strip()))
        f.write("\n")
    f.close()
    
    # Editing intial_config_new.properties
    f_client = open(opts.root +  "/intial_config_new.properties","w")
    f_client.write(str(process_no_tuple_map))
    f_client.close()
    command = execute_command % {'root' : opts.root, 
                                 'process_no' : newId,
                                }
    print "Starting new replica by running: " + command + " &"
    args = shlex.split(command)
    subprocess.Popen(args);

def command(cmd_type, command, replicaId=-1):
    global my_latest_write
    global my_replicaId

    if (replicaId == -1):
        replicaId = my_replicaId

    read_config()
    valid_cmd_types = ["ADD", "DELETE", "EDIT"]

    tup = process_no_tuple_map[replicaId]
    s = ensure_connect(replicaId)
    if cmd_type in valid_cmd_types:
        cmd_str = str(my_Id) + "--OPERATION--" + cmd_type + "==" + command + "&"

        print "Sending: " + cmd_str + " to " + replicaId
        s.send(cmd_str)
        my_latest_write = s.recv(1024)
        print "Response: " + my_latest_write
        return
    else:
        if (cmd_type == "READ"):
            cmd_str = str(my_Id) + "--READ--" + my_latest_write + "&"
            print "Sending: " + cmd_str + " to " + replicaId
            s.send(cmd_str)
            response = s.recv(1024)
            print "Response: " + response
            if (response == "NO"):
                print "Could not find my read in " + replicaId + ". Going to contact others."
                for no, tup in process_no_tuple_map.iteritems():
                    if (no != replicaId):
                        s = ensure_connect(no)
                        s.send(cmd_str)
                        response = s.recv(1024)
                        if (response == "NO"):
                            continue
                        else:
                            my_replicaId = str(no)
                            print "Response: " + response
                            print "Response found in: " + my_replicaId + ". All the next commands would be sent to this server."
                            break
        else:
            print "Not a valid command\n"   

def isolate(x=my_replicaId):
    read_config()
    x = str(x)
    for i in process_no_tuple_map:
        if i == x:
            s = ensure_connect(i)
            cmd_str = str(my_Id) + "--DISCONNECT--X&"
            s.send(cmd_str)
            print "Sending: " + cmd_str + " to " + i
        else:
            s = ensure_connect(i)
            cmd_str = str(my_Id) + "--DISCONNECT--" + str(x) + "&"
            s.send(cmd_str)
            print "Sending: " + cmd_str + " to " + i

def reconnect(x=my_replicaId):
    read_config()
    x = str(x)
    for i in process_no_tuple_map:
        if i == x:
            s = ensure_connect(i)
            cmd_str = str(my_Id) + "--CONNECT--X&"
            s.send(cmd_str)
            print "Sending: " + cmd_str + " to " + i
        else:
            s = ensure_connect(i)
            cmd_str = str(my_Id) + "--CONNECT--" + str(x) + "&"
            s.send(cmd_str)
            print "Sending: " + cmd_str + " to " + i


def breakConnection(i, j):
    read_config()
    i = str(i); j = str(j)
    s1 = ensure_connect(i)
    s2 = ensure_connect(j)
    
    cmd_str1 = str(my_Id) + "--DISCONNECT--" + j + "&"
    s1.send(cmd_str1)    
    print "Sending: " + cmd_str1 + " to " + i

    cmd_str2 = str(my_Id) + "--DISCONNECT--" + i + "&"
    s2.send(cmd_str2)    
    print "Sending: " + cmd_str2 + " to " + j
    
def recoverConnection(i, j):
    read_config()
    i = str(i); j = str(j)
    s1 = ensure_connect(i)
    s2 = ensure_connect(j)
    
    cmd_str1 = str(my_Id) + "--CONNECT--" + j + "&"
    s1.send(cmd_str1)    
    print "Sending: " + cmd_str1 + " to " + i

    cmd_str2 = str(my_Id) + "--CONNECT--" + i + "&"
    s2.send(cmd_str2)    
    print "Sending: " + cmd_str2 + " to " + j
    
def printLog(x="ALL"):
    read_config()
    if(x == "ALL"):
        for i in sorted(process_no_tuple_map):
            logFile = (opts.logFolder + "replicaLogs/" + str(i) + ".log")
            print "Logs for: " + str(i) + ".log"
            os.system('cat %(logFile)s'% locals())
            print "\n"

    else:
        logFile = (opts.logFolder + "replicaLogs/" + str(x) + ".log")
        print str(x) + ".log"
        os.system('cat %(logFile)s'% locals())
        print "\n"

def Pause(x = "ALL"):
    read_config()
    cmd_str = "pause&"
    x = str(x)
    if(x == "ALL"):
        for i in process_no_tuple_map:
            s = ensure_connect(str(i))
            s.send(cmd_str)
            print "Sending: " + cmd_str + " to " + i
    else:
        s = ensure_connect(str(x))
        s.send(cmd_str)
        print "Sending: " + cmd_str + " to " + x
        
def Continue(x = "ALL"):
    read_config()
    cmd_str = "continue&"
    x = str(x)
    if(x == "ALL"):
        for i in process_no_tuple_map:
            s = ensure_connect(str(i))
            s.send(cmd_str)
            print "Sending: " + cmd_str + " to " + i
    else:
        s = ensure_connect(str(x))
        s.send(cmd_str)
        print "Sending: " + cmd_str + " to " + x

def leave(x):
    read_config()
    x = str(x)
    global process_no_tuple_map
    global my_replicaId
    
    s = ensure_connect(x)
    cmd_str = str(my_Id) + "--RETIRE--X&"
    s.send(cmd_str)
    print "Sending: " + cmd_str + " to " + x

    response = s.recv(1024)
    if (not response.startswith("NO")):
        del process_no_tuple_map[x]
        for processId in process_no_tuple_map:
            my_replicaId = processId
            break
        f = open(opts.root +  "/intial_config_new.properties","w")
        f.write(str(process_no_tuple_map))
        f.close()
    else:
        print "Replica " + x + " rejected the leave request."

def read_config():
    global process_no_tuple_map
    process_no_tuple_map = {}

    f = open(opts.root + "/intial_config_new.properties")
    lines = f.readlines()
    f.close()
    
    process_no_tuple_map = eval(lines[0])
 
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

if __name__ == "__main__":
    global my_replicaId

    opts, args = getopts()
    read_config()
    print "Initial list of servers: " + str(process_no_tuple_map)

    my_replicaId = opts.my_replicaId
    my_Id = opts.client_ID

    for processId in process_no_tuple_map:
        my_replicaId = processId
        break

    from IPython import embed
    embed()

