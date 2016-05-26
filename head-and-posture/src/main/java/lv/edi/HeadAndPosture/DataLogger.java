package lv.edi.HeadAndPosture;

import android.media.MediaScannerConnection;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import lv.edi.SmartWearProcessing.Segment;
import lv.edi.SmartWearProcessing.SensorDataProcessing;

/**
 * Created by Richards on 18.08.2015..
 */
public class DataLogger {
    Timer timer;
    private File logFileFolder;
    private File logFile;
    private boolean isLogging = false;
    private HeadTiltProcessingService headTiltProcessingService;
    private PostureProcessingService postureProcessingService;
    private int timeInterval;
    private PrintWriter writer;
    private int activeActivity=0;
    private final String logFileHeader = "Data Head angle [o]," +
                                         "Data Posture max Distance [cm]," +
                                         "Head Over Threshold, " +
                                         "Posture over Threshold, " +
                                         "Acitve Head (0) or Active Posture (1), " +
                                         "Head Threshold," +
                                         "Posture Threshold [cm]," +
                                         "Sampling Rate [Hz]";

    public DataLogger(File logFileFolder, HeadTiltProcessingService headTiltProcessingService, PostureProcessingService postureProcessingService, float sampleRate){
        this.headTiltProcessingService=headTiltProcessingService;
        this.postureProcessingService=postureProcessingService;
        timeInterval = (int)(1/sampleRate*1000);

        this.logFileFolder=logFileFolder;


    }

    public void startLogSession(float samplingRate) throws FileNotFoundException {
        timeInterval = (int)(1/samplingRate*1000);

        Calendar c = Calendar.getInstance();
        String logFileName = "loggedData-"+c.get(Calendar.DAY_OF_MONTH)+"-"+(c.get(Calendar.MONTH)+1)+"-"+c.get(Calendar.YEAR)+"--"+c.get(Calendar.HOUR_OF_DAY)+"-"+c.get(Calendar.MINUTE)+".csv";
        Log.d("LOGGING", logFileName);
        logFile = new File(logFileFolder.toString()+"/"+logFileName);
        writer = new PrintWriter(logFile);
        writer.println(logFileHeader);
        Log.d("LOGGING", "head threshold " + headTiltProcessingService.getThreshold());
        writer.println(headTiltProcessingService.getVerticalAngle() + "," +
                postureProcessingService.getMaxDistance() + "," +
                headTiltProcessingService.isOverThreshold() + "," +
                postureProcessingService.isOverThreshold() + "," +
                activeActivity + "," +
                headTiltProcessingService.getThreshold() + "," + postureProcessingService.getThreshold() + "," + samplingRate);
        timer = new Timer();
        isLogging = true;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                writer.println(headTiltProcessingService.getVerticalAngle()+","+
                        postureProcessingService.getMaxDistance()+","+
                        headTiltProcessingService.isOverThreshold()+","+
                        postureProcessingService.isOverThreshold()+","+
                        activeActivity);
            }
        }, 0, timeInterval);

    }


    public File stopLogSession(){
        timer.cancel();
        writer.close();
        return logFile;

    }

    public void setActiveActivity(int activeActivity){
        this.activeActivity=activeActivity;
    }
}
