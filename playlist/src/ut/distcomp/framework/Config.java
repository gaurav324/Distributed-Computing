/**
 * This code may be modified and used for non-commercial 
 * purposes as long as attribution is maintained.
 * 
 * @author: Isaac Levy
 */

package ut.distcomp.framework;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Config {

	/**
	 * Loads config from a file.  Optionally puts in 'procNum' if in file.
	 * See sample file for syntax
	 * @param filename
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Config(String filename, Handler fh) throws FileNotFoundException, IOException {
		logger = Logger.getLogger("NetFramework");
		
		logger.setUseParentHandlers(false);
		
		Logger globalLogger = Logger.getLogger("global");
		Handler[] handlers = globalLogger.getHandlers();
		for(Handler handler : handlers) {
		    globalLogger.removeHandler(handler);
		}
		
		Formatter formatter = new Formatter() {

            @Override
            public String format(LogRecord arg0) {
                StringBuilder b = new StringBuilder();
//                b.append("[");
//                b.append(arg0.getSourceClassName());
//                b.append("-");
//                b.append(arg0.getSourceMethodName());
//                b.append(" ");
//                b.append("] ");
                b.append(arg0.getLevel());
                b.append("-");
                b.append(arg0.getMessage());
                b.append(System.getProperty("line.separator"));
                return b.toString();
            }

        };
		fh.setFormatter(formatter);
		logger.addHandler(fh);
		
        LogManager lm = LogManager.getLogManager();
        lm.addLogger(logger);

		//logger.setLevel(Level.FINEST);

		Properties prop = new Properties();
		prop.load(new FileInputStream(filename));
		numProcesses = loadInt(prop,"NumProcesses");
		addresses = new InetAddress[numProcesses];
		ports = new int[numProcesses];
		for (int i=0; i < numProcesses; i++) {
			ports[i] = loadInt(prop, "port" + i);
			addresses[i] = InetAddress.getByName(prop.getProperty("host" + i).trim());
		}
		if (prop.getProperty("procNum") != null) {
			procNum = loadInt(prop,"procNum");
		} else {
			logger.info("procNum not loaded from file");
		}
	}
	
	private int loadInt(Properties prop, String s) {
		return Integer.parseInt(prop.getProperty(s.trim()));
	}
	
	/**
	 * Default constructor for those who want to populate config file manually
	 */
	public Config() {
	}

	/**
	 * Array of addresses of other hosts.  All hosts should have identical info here.
	 */
	public InetAddress[] addresses;
	

	/**
	 * Array of listening port of other hosts.  All hosts should have identical info here.
	 */
	public int[] ports;
	
	/**
	 * Total number of hosts
	 */
	public int numProcesses;
	
	/**
	 * This hosts number (should correspond to array above).  Each host should have a different number.
	 */
	public int procNum;
	
	/**
	 * Logger.  Mainly used for console printing, though be diverted to a file.
	 * Verbosity can be restricted by raising level to WARN
	 */
	public Logger logger;
}
