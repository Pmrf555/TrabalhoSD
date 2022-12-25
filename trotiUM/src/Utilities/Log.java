package Utilities;

import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;

public class Log {
    public static final String EVENT =   "EVENT  ";
    public static final String ERROR =   "ERROR  ";
    public static final String WARNING = "WARNING";
    public static final String INFO =    "INFO   ";
    public static final String DEBUG =   "DEBUG  ";

    private static final AtomicInteger counter = new AtomicInteger(1);
    private static final Console console = System.console();
    private static final FileOutputStream logFile;

    static {
        FileOutputStream tmp = null;
        String file = "./../data/logmein.log";
        File tmpDir = new File(file);
        boolean exists = tmpDir.exists() && !tmpDir.isDirectory();
        if(!exists){
            file = file.replace("../", "/");
        }
        try {
            tmp = new FileOutputStream(file,true);
        } catch (IOException e) {
            System.err.println("Error creating log file");
        }
        logFile = tmp;
    }

    private static String getTimestamp(){
        String pattern = "yyyy-MM-dd'.'HH:mm:ss.SSS";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(new java.util.Date());
    }

    public static String formated(String type,String message){
        String formated = String.format("|%s|%s->%s|\n",getTimestamp(),StringUtils.padString(String.format("Log#%d %s",counter.getAndIncrement(),type),16),StringUtils.padString(message,64));
        //console.printf("%s",formated);
        Log.to(formated);
        return formated;
    }

    public static void all(String type,String message,boolean toscreen){
        console.printf("%s",formated(type, message));
    }

    public static void line(String line){
        to(line);
    }

    public static void all(String type,String message){
        formated(type, message);
    }
    
    public static void to(String message){
        try {
            logFile.write(message.getBytes());
        } catch (IOException e) {
            System.err.println("Error writing to log file");
        }
    }

}
