package lv.edi.HeadAndPosture;

import android.util.Log;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import lv.edi.SmartWearProcessing.Segment;
import lv.edi.SmartWearProcessing.Sensor;
import lv.edi.SmartWearProcessing.SensorDataProcessing;

/**
 * Created by Richards on 17.08.2015..
 */
public class PostureProcessingService {

    private int timeInterval = 20;

    private Vector<Vector<Segment>> savedStateSegments;
    private Vector<Vector<Segment>> savedStateSegmentsInitial;
    private Vector<Vector<Segment>> currentStateSegments;
    private Vector<Vector<Sensor>> sensors;
    private Vector<Vector<Float>> distances;

    private boolean isProcessing;
    private int referenceRow;
    private int referenceCol;
    private float threshold;
    private float maxDistance;
    private Timer timer;
    private boolean isStateSaved = false;
    private ProcessingEventListener listener;
    private boolean isOverThreshold;

    /**
     * constructor for posture processing service
     */
    public PostureProcessingService(
                                    Vector<Vector<Segment>>currenStateSegments,
                                    Vector<Vector<Sensor>> sensors,
                                    int refRow, int refCol,
                                    float threshold){
        this.currentStateSegments = currenStateSegments;
        this.sensors = sensors;
        this.referenceRow = refRow;
        this.referenceCol = refCol;
        this.threshold = threshold;
    }

    /**
     * Save reference posture
     * @param savedStateSegments
     * @param savedStateSegmentsInitial
     */
    public void setReferenceState(Vector<Vector<Segment>>savedStateSegments, Vector<Vector<Segment>>savedStateSegmentsInitial){
        this.savedStateSegments = savedStateSegments;
        this.savedStateSegmentsInitial = savedStateSegmentsInitial;
        isStateSaved = true;

    }

    /**
     * register processing result event listener
     * @param listener
     */
    public void setProcessingResultEventListener(ProcessingEventListener listener){
        this.listener = listener;
    }

    /**
     * allocates array where to store distances
     * @param distances
     */
    public void setDistancesArray(Vector<Vector<Float>> distances){
        this.distances = distances;
    }

    public boolean isStateSaved(){
        return isStateSaved;
    }
    /** starts processing service
     *
     */

    public void startProcessing(float samplingFrequency){
        timeInterval = (int)(1/samplingFrequency*1000);
        timer = new Timer();
        isProcessing = true;
        timer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                Segment.setAllSegmentOrientations(currentStateSegments, sensors);
                Segment.setSegmentCenters(currentStateSegments, (short) referenceRow, (short) referenceCol);
               // Log.d("PROCESSING ", " "+savedStateSegmentsInitial);
                Segment.compansateCentersForTilt(savedStateSegmentsInitial, currentStateSegments, savedStateSegments, referenceRow, referenceCol);
                if(distances!=null) {
                    Segment.compareByDistances(savedStateSegments, currentStateSegments, distances);
                }
               // Log.d("PROCESSING", "POSTURE_PROCESSIN_PROCESSED");
                maxDistance = SensorDataProcessing.getMaxDistanceFromDistances(distances);
                Log.d("PROCESSING", "maxDistance "+maxDistance+" ");
                isOverThreshold = (maxDistance>threshold);

                ProcessingResult result = new ProcessingResult(maxDistance, isOverThreshold); // TODO add exact value;
                if(listener!=null){
                    listener.onProcessingResult(result);
                }
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

    public boolean isProcessing(){
        return isProcessing;
    }

    public float getMaxDistance(){
        return maxDistance;
    }
    public boolean isOverThreshold(){
        return isOverThreshold;
    }

    public float getThreshold(){
        return threshold;
    }
    public void setThreshold(float threshold){
        this.threshold = threshold;
    }

}
