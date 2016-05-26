package lv.edi.YesNo;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import lv.edi.SmartWearProcessing.Filter;
import lv.edi.SmartWearProcessing.Sensor;
import lv.edi.SmartWearProcessing.SensorDataProcessing;
import static java.lang.Math.*;

/**
 * Created by Richards on 18.11.2015..
 */
public class YesNoProcessingService {
    private Sensor sensor;
    private YesNoView ynView;
    private boolean isProcessing=false;
    private Timer timer;
    private long timerPeriod=33;
    private Filter filterX;
    private Filter filterY;
    private Filter filterZ;
    private float[] referenceState = new float[3];
    private float[] currentSens1 = new float[3];
    private float[] tempSens = new float[3];
    private float[] tempRef = new float[3];
    private float[] crossVertical = new float[3];
    private float[] crossVerticalN = new float[3];
    private float[] crossHorizontal = new float[3];
    private float[] crossHorizontalN = new float[3];
    private float XX;
    private float YY;
    private float markerRelPos[] = new float[2];
    private boolean isXZplane=true;
    private float radius;
    private final int YES=1;
    private final int NO=-1;
    private final int NONE=0;
    private int prevState=NONE;
    private Vibrator v;
    private MediaPlayer mpYes;
    private MediaPlayer mpNo;
    private long lastTransitionTime=0;
    private long deltaTime=0;
    private long acceptTimeThreshold=3000;
    private float yesProgress=0;
    private float noProgress=0;

    float testAngleZ=0;

    public YesNoProcessingService(Sensor sensor){
        filterX = new Filter();
        filterY = new Filter();
        filterZ = new Filter();
        mpYes = MediaPlayer.create(YesNoApplication.getAppContext(), R.raw.yes);
        mpNo = MediaPlayer.create(YesNoApplication.getAppContext(), R.raw.no);
        v = (Vibrator) YesNoApplication.getAppContext().getSystemService(Context.VIBRATOR_SERVICE);
        mpYes.setLooping(false);
        mpYes.setVolume(1.0f, 1.0f);
        mpNo.setLooping(false);
        mpNo.setVolume(1.0f, 1.0f);
        this.sensor=sensor;

    }
    public void setYesNoView(YesNoView ynView){
        this.ynView = ynView;
    }

    public void start(long period){
        filterX.clear();
        filterY.clear();
        filterZ.clear();

        timer = new Timer();
        referenceState[0]=sensor.getAccRawNormX();
        referenceState[1]=sensor.getAccRawNormY();
        referenceState[2]=sensor.getAccRawNormZ();


        if(referenceState[0]>Math.abs(referenceState[1])){
            isXZplane=true;
        } else{
            isXZplane=false;
        }

        isProcessing=true;


        timer.scheduleAtFixedRate(new TimerTask() {
         @Override
                public void run(){
                    markerRelPos = computeMarkerPosition();
                    if(ynView!=null){                           // TODO repace with real data
                        //testAngleZ += Math.PI/180;
                       // float[] testPos=new float[2];
                       // testPos[0]=(float)Math.cos(testAngleZ);
                        //testPos[1]=0.15f;

                        //markerRelPos=testPos;
                        int curState = detectRegion();
                        if((prevState==NONE)&&(curState==YES)) {
                            lastTransitionTime=System.currentTimeMillis();

                            v.vibrate(50);
                        }
                        if(curState==YES){
                            deltaTime = System.currentTimeMillis() - lastTransitionTime;
                            yesProgress = (float)deltaTime/ acceptTimeThreshold;
                            if(deltaTime>acceptTimeThreshold){
                                Log.d("PROCESSING_SERV_feedb", "YES_TRIGGER ");
                                //mpYes.seekTo(0);
                                mpYes.start();
                                Log.d("PROCESSING_SERV_feedb", "YES_TRIGGER "+mpYes.toString());
                                lastTransitionTime=System.currentTimeMillis();
                            }
                        }

                        if((prevState==YES)&&(curState==NONE)) {
                            v.vibrate(100);
                            deltaTime=0;
                            yesProgress=0;
                        }

                        if((prevState==NONE)&&(curState==NO)) {
                            lastTransitionTime=System.currentTimeMillis();
                            v.vibrate(50);
                        }
                        if(curState==NO){
                            deltaTime = System.currentTimeMillis() - lastTransitionTime;
                            noProgress = (float)deltaTime/ acceptTimeThreshold;
                            if(deltaTime>acceptTimeThreshold){
                                //mpNo.seekTo(0);
                                mpNo.start();
                                lastTransitionTime=System.currentTimeMillis();
                            }
                        }

                        if((prevState==NO)&&(curState==NONE)) {
                            v.vibrate(100);
                            deltaTime=0;
                            noProgress=0;
                        }
                        prevState=curState;
                        ynView.updateData(markerRelPos, yesProgress, noProgress);
                    }


             Log.d("PROCESSING_SERVICE", "XX: "+XX+" YY: "+YY);

            }
        }, 0, period);
    }

    public void stop(){
        if(isProcessing) {
            isProcessing = false;
            timer.cancel();
            timer = null;
        }
    }

    boolean isProcessing(){
        return isProcessing;
    }

    float[] computeMarkerPosition(){
        currentSens1 = sensor.getAccRawNorm();
        currentSens1[0] = filterX.filter(currentSens1[0]);
        currentSens1[1] = filterY.filter(currentSens1[1]);
        currentSens1[2] = filterZ.filter(currentSens1[2]);

        if(isXZplane){
            tempSens[0]=currentSens1[0];
            tempSens[1]=0;
            tempSens[2]=currentSens1[2];

            tempRef[0]=referenceState[0];
            tempRef[1]=0;
            tempRef[2]=referenceState[2];

            SensorDataProcessing.crossProduct(tempRef, tempSens, crossVertical);

        } else{
            tempSens[0]=0;
            tempSens[1]=currentSens1[1];
            tempSens[2]=currentSens1[2];

            tempRef[0]=0;
            tempRef[1]=referenceState[1];
            tempRef[2]=referenceState[2];

            SensorDataProcessing.crossProduct(tempRef, tempSens, crossVertical);
        }
        crossVerticalN = Arrays.copyOf(crossVertical, crossVertical.length);
        SensorDataProcessing.normalizeVector(crossVerticalN);

        tempSens[0]=currentSens1[0];
        tempSens[1]=currentSens1[1];
        tempSens[2]=0;

        tempRef[0]=referenceState[0];
        tempRef[1]=referenceState[1];
        tempRef[2]=0;

        SensorDataProcessing.crossProduct(tempSens, tempRef, crossHorizontal);
        crossHorizontalN = Arrays.copyOf(crossHorizontal, crossHorizontal.length);
        SensorDataProcessing.normalizeVector(crossHorizontalN);

        SensorDataProcessing.absVector(crossHorizontal);
        SensorDataProcessing.absVector(crossVertical);

        float XX=-(float)(Math.asin(SensorDataProcessing.dotProduct(crossHorizontal, crossHorizontalN))*180/Math.PI)/45; // may include sensitivity multiplier
        float YY=(float)(Math.asin(SensorDataProcessing.dotProduct(crossVertical, crossVerticalN))*180/Math.PI)/45;      // may include sensitivity mult in futuree

        float[] res = new float[2];
        res[0]=XX;
        res[1]=YY;
        return res;
    }

    public void setRadius(float radius){
        this.radius=radius;
        if(ynView!=null){
            ynView.setRadius(radius);
        }
    }

    public int detectRegion(){
        int res = NONE;
        if(sqrt(pow(markerRelPos[0]-0.5f,2)+pow(markerRelPos[1],2)) <=  radius/2) { // radius/2 because radius is set relative to 1/4 of witdh of the screen
            res = YES;
            Log.d("PROCESSING_REGION", "REGION YES");
        }

        if(sqrt(pow(markerRelPos[0]+0.5f,2)+pow(markerRelPos[1],2)) <=  radius/2) { // radius/2 because radius is set relative to 1/4 of witdh of the screen
            res = NO;
            Log.d("PROCESSING_REGION", "REGION NO");
        }
        return res;
    }

    public void setAcceptTimeThreshold(long acceptTimeThreshold){
        this.acceptTimeThreshold=acceptTimeThreshold;
    }

}
