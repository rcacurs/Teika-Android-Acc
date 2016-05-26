package lv.edi.HeadAndPosture;



/**
 * Created by Richards on 10/07/2015.
 */
public class ProcessingResult {
    static final int RESULT_HEAD = 0;
    static final int RESULT_POSTURE = 1;
    private int resultType=0;
    private float relativeX=0;                // represents computed X coordinate
    private float relativeY=0;                // represents computed Y coordinate
    private long elapsedTimeSeconds;          // represents elapsed time in seconds for session
    private boolean isOverThreshold=false;    // represents flag for if is over threshold
    private float goodTimePercent=100.0f;
    private float maxDistance = 0.0f;
    private float headVerticalAngle = 0.0f;   // vertical angle of head degrees

    /**
     * Construct result object that is computed with ProcessingService class for head tilt processing
     * @param relativeX - relative X coordinate for object
     * @param relativeY - relative Y coordinate
     * @param elapsedTimeSeconds - elapsed time in seconds for session
     * @param isOverThreshold - flag that shows if head tilt is over threshold
     * @param goodTimePercent - shows what percentage of the session head tilt was good
     */
    public ProcessingResult(float relativeX, float relativeY, long elapsedTimeSeconds, boolean isOverThreshold, float goodTimePercent){
        this(RESULT_HEAD);
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        this.elapsedTimeSeconds = elapsedTimeSeconds;
        this.isOverThreshold = isOverThreshold;
        this.goodTimePercent = goodTimePercent;

    }

    public ProcessingResult(int resultType){
        this.resultType=resultType;
    }
    //
    public ProcessingResult(float maxDistance){
        this(RESULT_POSTURE);
        this.maxDistance=maxDistance;
    }

    public ProcessingResult(float maxDistance, boolean isOverThreshold){
        this(maxDistance);
        this.isOverThreshold = isOverThreshold;
    }

    public float getRelativeX(){
        return relativeX;
    }

    public float getRelativeY(){
        return relativeY;
    }

    public long getElapsedTimeSeconds(){
        return elapsedTimeSeconds;
    }

    public boolean isOverThreshold(){
        return isOverThreshold;
    }

    public float getGoodTimePercent(){
        return goodTimePercent;
    }

    public float getMaxDistance(){
        return maxDistance;
    }

    public int getResultType(){
        return resultType;
    }

}
