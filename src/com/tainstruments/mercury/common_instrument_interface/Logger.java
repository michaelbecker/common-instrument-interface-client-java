package com.tainstruments.mercury.common_instrument_interface;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


/**
 *  Package visibility
 */
class Logger implements Runnable {

    private boolean loggingEnabled;
    private final Object logLock;
    private final Queue<LogMessage> messageQueue;
    private final Object messageAvailable;
    private final String touchLogFileName = "/tmp/CIILOG";;
    private final String logPath = "/tmp";

    static private Logger instance;


    /**
     *  Private ctor, we are a singleton.
     */
    private Logger() {
        messageAvailable = new Object();
        logLock = new Object();
        messageQueue = new LinkedList<>();
        File f = new File(touchLogFileName);
        if (f.exists()){
            loggingEnabled = true;
        }
        else{
            loggingEnabled = true;
        }
    }

    
    static public Logger getInstance() {

        if (instance == null) {
            
            instance = new Logger();
            Thread thread = new Thread(instance, "Cii-Logger-Thread");
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY + 1);
            thread.start();
        }
        
        return instance;
    }

    
    private class LogMessage {

        private final Date timestamp;
        private byte[] data;
        private String message;

        
        @Override
        public String toString(){

            StringBuilder s = new StringBuilder(timestamp.toString() + " ");
            if (message != null){
                s.append(message);
                s.append(" ");
            }
            if (data != null){
                s.append(Arrays.toString(data));
            }

            return s.toString();
        }

        
        public LogMessage(String message, byte[] buffer, int dataLength){
            timestamp = new Date();
            if (message != null){
                this.message = message;
            }
            if (buffer != null){
                data = Arrays.copyOf(buffer, dataLength);
            }
        }

    }
    

    @Override
    public void run(){

        boolean isMessageAvailable;
        boolean wasEnabled = false;
        BufferedWriter writer = null;

        while(true){

            synchronized (messageAvailable){
                try {
                    messageAvailable.wait(1000);
                } catch (InterruptedException ex)
                {}
            }

            File f = new File(touchLogFileName);
            loggingEnabled = f.exists();

            //
            //  Turning it on, create a file.
            //
            if (loggingEnabled && !wasEnabled) {

                Calendar now = Calendar.getInstance();
                
                String filename =   logPath + "/" +
                                    (now.get(Calendar.YEAR)) + "_" +
                                    now.get(Calendar.MONTH) + "_" +
                                    now.get(Calendar.DAY_OF_MONTH) + "___" +
                                    now.get(Calendar.HOUR_OF_DAY) + "_" +
                                    now.get(Calendar.MINUTE) + "_" +
                                    now.get(Calendar.SECOND) + 
                                    ".log";

                File logFile=new File(filename);

                try {
                    writer = new BufferedWriter(new FileWriter(logFile));
                } catch(IOException ex) {
                    System.out.println("Log Writer Failed making writer " + ex);
                    writer = null;
                }
            }
            //
            //  Turning it off, close the file.
            //
            else if (!loggingEnabled && wasEnabled) {
                if (writer != null){
                    try{
                        writer.flush();
                        writer.close();
                    }catch(IOException ex){
                        System.out.println("Log Writer Close Exception " + ex);
                    }
                }
            }

            wasEnabled = loggingEnabled;

            if (!loggingEnabled) {
                continue;
            }

            synchronized(logLock) {

                isMessageAvailable = !messageQueue.isEmpty();
                
                while (isMessageAvailable) {
                    
                    LogMessage msg = messageQueue.poll();
                    try {
                        //System.out.println(msg.toString());
                        writer.write(msg.toString() + "\n");
                    } catch (IOException ex) {
                        System.out.println("Log Writer Failed writing " + ex);
                    }

                    isMessageAvailable = !messageQueue.isEmpty();
                }
            }

            try {
                writer.flush();
            } catch (IOException ex) {
                System.out.println("Log Writer Failed flushing " + ex);
            }
        }
    }


    public void Log(String message, byte [] buffer, int dataLength){
        
        if (loggingEnabled){

            LogMessage msg = new LogMessage(message, buffer, dataLength);

            synchronized(logLock){
                messageQueue.add(msg);
            }

            synchronized(messageAvailable){
                messageAvailable.notify();
            }
        }
    }


    public static void main(String argv[]){
        System.out.println("Unit Test of logging");

        Logger logger = Logger.getInstance();

        logger.Log("Hi from a log string", null, 0);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
        }

        
        for (int i = 0; i<100; i++){
            logger.Log("LogStr " + i, null, 0);
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
        }

        for (int i = 1000; i<1100; i++){
            logger.Log("LogStr " + i, null, 0);
        }

        
        try {
            Thread.sleep(30000);
        } catch (InterruptedException ex) {
        }

        System.out.println("Exiting Unit Test");
    }
}
