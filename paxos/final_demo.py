import socket
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM); s.connect(("localhost", 4000))

import time

from IPython import embed
embed()
