package lv.edi.HeadTilt;



/**
 * Created by Richards on 10/07/2015.
 */
public class ProcessingResult {

    private float relativeX=0;                // represents computed X coordinate
    private float relativeY=0;                // represents computed Y coordinate
    private long elapsedTimeSeconds;          // represents elapsed time in seconds for session
    private boolean isOverThreshold=false;    // represents flag for if is over threshold
    private float goodTimePercent=100.0f;

    /**
     * Construct result object that is computed with ProcessingService class
     * @param relativeX - relative X coordinate for object
     * @param relativeY - relative Y coordinate
     * @param elapsedTimeSeconds - elapsed time in seconds for session
     * @param isOverThreshold - flag that shows if head tilt is over threshold
     * @param goodTimePercent - shows what percentage of the session head tilt was good
     */
    public ProcessingResult(float relativeX, float relativeY, long elapsedTimeSeconds, boolean isOverThreshold, float goodTimePercent){
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        this.elapsedTimeSeconds = elapsedTimeSeconds;
        this.isOverThreshold = isOverThreshold;
        this.goodTimePercent = goodTimePercent;
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

}
