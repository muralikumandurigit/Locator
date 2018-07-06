package LocatorServer;

import java.io.IOException;
import java.util.logging.*;

public class MyLogger {
    static private FileHandler fileTxt;
    static private FileHandler sockfileTxt;

    static private SimpleFormatter formatterTxt;
    static int limit = 1000000; // 1 Mb

    static private FileHandler fileHTML;
    static private Formatter formatterHTML;

    static public void setup() throws IOException {

        // get the global logger to configure it
        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

        Logger socketlogger = Logger.getLogger("SOCKETLOGGER");

        String logDir = "E:\\Murali\\Logs\\";
        // suppress the logging output to the console
        Logger rootLogger = Logger.getLogger("Locator Logger");
        rootLogger.addHandler(new FileHandler());
        Handler[] handlers = rootLogger.getHandlers();
        if (handlers[0] instanceof ConsoleHandler) {
            rootLogger.removeHandler(handlers[0]);
        }

        logger.setLevel(Level.INFO);
        socketlogger.setLevel(Level.INFO);

        fileTxt = new FileHandler(logDir + "Logging.txt",limit,200,true);
        fileHTML = new FileHandler(logDir+"Logging.html",limit,200,true);

        sockfileTxt = new FileHandler(logDir+"Soc.txt",limit,2000,true);

        // create a TXT formatter
        formatterTxt = new SimpleFormatter();
        fileTxt.setFormatter(formatterTxt);
        logger.addHandler(fileTxt);

        sockfileTxt.setFormatter(formatterTxt);
        socketlogger.addHandler(sockfileTxt);

        // create an HTML formatter
        formatterHTML = new MyHtmlFormatter();
        fileHTML.setFormatter(formatterHTML);
        logger.addHandler(fileHTML);


    }
}
