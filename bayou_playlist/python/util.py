def Isolate(i):
    """
    Isolate the given server Id from all the machines.

    """
    s1 = socket.socket(socket.AF_INET, socket.SOCK_STREAM); 
    s1.connect(("localhost", 4001));

    

# XXX YET TO IMPLEMENT.
def printLog():
    """
    Print all the log files.
    
    """
    pass

def printLog(id):
    """
    Print the log file for the given Id.

    """
    pass
