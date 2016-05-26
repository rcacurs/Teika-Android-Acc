package lv.edi.HeadTilt;

import android.util.Log;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import lv.edi.SmartWearProcessing.Sensor;
import lv.edi.SmartWearProcessing.Filter;
import lv.edi.SmartWearProcessing.SensorDataProcessing;

/**
 * Created by Richards on 19/06/2015.
 * Class provides processing service for head tilt application
 * processing is done at specified time interval
 */
public class HeadTiltProcessingService{

    private Sensor sensor;
    private float[] referenceState = new float[3];
    private float[] currentSens1 = new float[3];
    private float[] previousSens1 = new float[3];
    private float[] crossVertical = new float[3];
    private float[] crossHorizontal = new float[3];
    private float[] crossVerticalN = new float[3];
    private float[] crossHorizontalN = new float[3];
    private float absoluteAccChange = 0;
    private int timeInterval=10;
    private boolean isProcessing=false;
    private boolean isStateSaved=false;
    private boolean isOverThreshold = false;
    private Timer timer;
    private ProcessingEventListener listener;
    private boolean isXZplane=false;
    private float XX;
    private float YY;
    private float threshold = 0.7f;
    private float iconSize = 0.1f;
    private float previousR = 0;
    private long startTime;
    private long currentTime;
    private int goodFrameCount=0;
    private int badFrameCount=0;

    // locators for processing
    float[] tempSens = new float[3];
    float[] tempRef = new float[3];

    Filter filterX;
    Filter filterY;
    Filter filterZ;

    /**
     * @param - Sensor object from which input data is taken. must be not null!
     */
    public HeadTiltProcessingService(Sensor sensor){
        this.sensor=sensor;
        filterX = new Filter();
        filterY = new Filter();
        filterZ = new Filter();
    }
    /**
     * Allows specifying time interval at which computation is done
     * @param sensor sensor object from which input data is taken. must be not null!
     * @param timeInterval time interval in ms for computation period
     */
    public HeadTiltProcessingService(Sensor sensor, int timeInterval){
        this(sensor);
        this.timeInterval=timeInterval;
    }

    public HeadTiltProcessingService(Sensor sensor, int timeInterval, float threshold){
        this(sensor, timeInterval);
        this.threshold = threshold;
    }
    /**
     * Constructor allowing to set threshold value for feedback trigger
     * @param sensor sensor object from which input data is taken. must be not null!
     * @param timeInterval time interval in ms for computation period
     * @param threshold treshold value. float value in range 0-1.0f
     * @param iconSize size of icon, relative to view width
     *
     */
    public HeadTiltProcessingService(Sensor sensor, int timeInterval, float threshold, float iconSize){
        this(sensor, timeInterval);
        this.threshold = threshold;
        this.iconSize = iconSize;
    }

    /** setter method for thrreshold setting. when distance of object exceeds threshold
     * isOverThreshold flag is raised;
     *
     * @param threshold
     */
    public void setThreshold(float threshold){
        this.threshold = threshold;
    }

    public void setIconRadius(float iconRadius){
        this.iconSize = iconRadius;
    }
    /**
     * Sets accelerometer data for reference state
     * @param reference float[3] array containing reference sensor data
     */
    public void setReference(float[] reference){
        if(reference.length==3){
            referenceState[0]=reference[0];
            referenceState[1]=reference[1];
            referenceState[2]=reference[2];

            isStateSaved=true;

            if(referenceState[0]>Math.abs(referenceState[1])){
                isXZplane=true;
            } else{
                isXZplane=false;
            }
        }
    }

    /**
     * sets event listener for processing events
     */
    public void setProcessingEventListener(ProcessingEventListener listener){
        this.listener = listener;
    }

    /** starts processing service
     *
     */
    public void startProcessing(){
        filterX.clear();
        filterY.clear();
        filterZ.clear();
        timer = new Timer();
        isProcessing = true;
        startTime=System.currentTimeMillis();
        currentTime=System.currentTimeMillis();
        goodFrameCount=0;
        badFrameCount=0;
        previousSens1=sensor.getAccRawNorm();

        timer.scheduleAtFixedRate(new TimerTask(){
            long counter=0;
            @Override
            public void run(){
                currentSens1 = sensor.getAccRawNorm();
                Log.d("PROCESSING_SERVICE", "acc data "+currentSens1[0]+" "+currentSens1[1]+" "+currentSens1[2]);
//                currentSens1[0] = filterX.filter(currentSens1[0]);
//                currentSens1[1] = filterY.filter(currentSens1[1]);
//                currentSens1[2] = filterZ.filter(currentSens1[2]);

                Log.d("PROCESSING_SERVICE", "acc data after filter: "+currentSens1[0]+" "+currentSens1[1]+" "+currentSens1[2]);


                absoluteAccChange = (float)Math.sqrt(Math.pow(currentSens1[0]-previousSens1[0],2.0)+
                                                     Math.pow(currentSens1[1]-previousSens1[1],2.0)+
                                                     Math.pow(currentSens1[2]-previousSens1[2],2.0));
                Log.d("PROCESSING_SERVICE", ""+absoluteAccChange);
                previousSens1[0]=currentSens1[0];
                previousSens1[1]=currentSens1[1];
                previousSens1[2]=currentSens1[2];



                if (listener != null) {
                    currentTime = System.currentTimeMillis();
                    ProcessingResult result = new ProcessingResult(absoluteAccChange, YY, (currentTime - startTime) / 1000, isOverThreshold, getGoodPercentage());
                    listener.onProcessingResult(result);
                    result = null;
                    Log.d("PROCESSING_SERVICE", "SENDING TO LISTENER");
                }
                Log.d("PROCESSING_SERVICE", "XX: "+XX+" YY: "+YY);
            }
        }, 0, timeInterval);

    }

    /** stops processing
     *
     */
    public void stopProcessing(){
        if(timer!=null){
            isProcessing = false;
            timer.cancel();
        }
    }

    /**
     *
     * @return true if curren service is running
     */
    public boolean isProcessing(){
        return isProcessing;
    }

    /**
     *
     * @return returns true if this process has saved reference accelerometer vector
     */
    public boolean isStateSaved(){
        return isStateSaved;
    }

    /**
     * Returns if currently is over threshold
     * @return
     */
    public boolean isOverThreshold(){return isOverThreshold;}

    /**
     * Returns current duration of processing session
     * @return long number of seconds since session start
     */
    public long getSessionDuration(){
        return (currentTime-startTime)/1000;
    }

    /**
     * Gives fraciont of session when head tilt was good (in threshold range)
     * @return float percentage of good tim
     */
    public float getGoodPercentage(){
        float result;
        try {
            result = ((float)(goodFrameCount)) / (goodFrameCount + badFrameCount)*100;
            Log.d("PROCESSING_SERVICE", "goodTimePercentage = "+result);
        } catch(ArithmeticException ex){
            result = 100;
            Log.d("PROCESSING_SERVICE", "goodTimePercentage = "+result);
        }
        Log.d("PROCESSING_SERVICE", "goodTimePercentage = "+result);
        if(Float.isNaN(result)){
            Log.d("PROCESSING_SERVICE", "goodTimePercentage = "+result);
            result=100;
            Log.d("PROCESSING_SERVICE", "goodTimePercentage = "+result);
        }
        return result;
    }

}
