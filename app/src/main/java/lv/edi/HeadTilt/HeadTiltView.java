package lv.edi.HeadTilt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.graphics.Paint;
import android.util.AttributeSet;

/**
 * Created by Richards on 16/06/2015.
 * Class that represents view for representing view feedback
 */
public class HeadTiltView extends View implements ProcessingEventListener{
    private double coordX=0;
    private double coordY=0;
    private int canvwidth;
    private int canvheight;
    private Bitmap smiley;
    private Bitmap sadface;
    private float threshold = 0.4f;
    private boolean isOverThreshold = false;
    private String sessionTimeLable = "Time";
    private String goodTimePercentLable = "Good";
    private String sessionTime="00-00-00";
    private float goodTimePercent=100;
    Paint paint = new Paint();
    Paint textPaint = new Paint();

    public HeadTiltView(Context context) {
        super(context);
        setWillNotDraw(false);
        smiley = BitmapFactory.decodeResource(getResources(), R.drawable.happyface500);
        sadface = BitmapFactory.decodeResource(getResources(), R.drawable.sadface500);
        smiley = Bitmap.createScaledBitmap(smiley, smiley.getWidth() / 4, smiley.getHeight() / 4, false);
        sadface = Bitmap.createScaledBitmap(sadface, sadface.getWidth() / 4, sadface.getHeight() / 4, false);

        textPaint.setColor(Color.YELLOW);
        textPaint.setStrokeWidth(10);
        textPaint.setTextSize(30);

    }
    public HeadTiltView(Context context, AttributeSet attrs){
        super(context, attrs);
        setWillNotDraw(false);
        smiley = BitmapFactory.decodeResource(getResources(), R.drawable.happyface500);
        sadface = BitmapFactory.decodeResource(getResources(), R.drawable.sadface500);
        smiley = Bitmap.createScaledBitmap(smiley, smiley.getWidth() / 4, smiley.getHeight() / 4, false);
        sadface = Bitmap.createScaledBitmap(sadface, sadface.getWidth() / 4, sadface.getHeight() / 4, false);

        textPaint.setColor(Color.YELLOW);
        textPaint.setStrokeWidth(10);
        textPaint.setTextSize(30);
    }

    /**
     * Method sets postion for HeadTilt feadback view element definec in polar coordinates.
     * method enforces redraw of view.
     * @param R - radius vector in range 0-1;
     * @param phi - angle defined in radians;
     */
    public void setPolarLocation(double R, double phi){
        ;
        coordX=R*Math.cos(phi);
        coordY=R*Math.sin(phi);
        postInvalidate();
    }

    /**
     * Sets location of the smiley
     * @param x - coordinate
     * @param y - coordinate
     */
    public void setLocation(float x, float y){

        coordX=x;
        coordY=y;
        postInvalidate();
    }

    /**
     * Method sets if head tilt is over threshold
     * @param overThreshold
     */
    public void setOverThreshold(boolean overThreshold){
        this.isOverThreshold = overThreshold;
        postInvalidate();
    }

    /**
     * Sets cuurent threshold value for head tilt view
     * @param threshold curren threshold setting. designed to be for value 0-1.0. Forces to redraw view
     */
    public void setThreshold(float threshold){
        this.threshold = threshold;
        postInvalidate();
    }
    @Override
    protected void onDraw(Canvas canvas){
        Log.d("HEAD_TILT_VIEW", "ON_DRAW_START");
        super.onDraw(canvas);
        int bcx = smiley.getWidth()/2;
        int bcy = smiley.getHeight()/2;
        int cx = canvas.getWidth()/2;
        int cy = canvas.getHeight()/2;

        canvwidth = cx;
        canvheight = cy;
        int range = Math.min(cx,cy);

//        float rad = (float)Math.sqrt(Math.pow(coordX,2)+Math.pow(coordY,2));
//        if((rad+((float)bcx)/range)>threshold){
//            isOverThreshold=true;
//        } else{
//            isOverThreshold=false;
//        }
        if(!isOverThreshold) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.GREEN);
            paint.setAlpha(60);
            canvas.drawPaint(paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6);
            paint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
            paint.setColor(Color.YELLOW);
            canvas.drawCircle(cx, cy, range*(threshold), paint);
            canvas.drawBitmap(smiley, (int) (coordX * range + cx - bcx), (int) (coordY * range + cy - bcy), null);
        } else{
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.RED);
            paint.setAlpha(75);
            canvas.drawPaint(paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6);
            paint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
            paint.setColor(Color.YELLOW);
            canvas.drawCircle(cx, cy, range*(threshold), paint);
            canvas.drawBitmap(sadface, (int) (coordX * range + cx - bcx), (int) (coordY * range + cy - bcy), null);
        }

        canvas.drawText(sessionTimeLable+": "+sessionTime, 10, canvas.getHeight()-10, textPaint);
        canvas.drawText(goodTimePercentLable+": "+String.format("%3.1f", goodTimePercent)+"%", 10, canvas.getHeight()-40, textPaint);
        Log.d("HEAD_TILT_VIEW", "ON_DRAW_END");
    }

    @Override
    public void onProcessingResult(ProcessingResult result){
        this.isOverThreshold = result.isOverThreshold();
        sessionTime=DateUtils.formatElapsedTime(result.getElapsedTimeSeconds());
        goodTimePercent=result.getGoodTimePercent();
        setLocation(result.getRelativeX(), result.getRelativeY());

        Log.d("PROCESSING_SERVICE", "RESULT");
    }

    /**
     * Returns relative radius for the icon in head tilt view
     * @return relative radius 0-1;
     */
    public float getIconRelativeRadius(){
        Log.d("HEAD_TILT_VIEW", "width "+canvwidth+" height "+canvheight+"smiley width: "+smiley.getWidth());
        return (((float)smiley.getWidth())/2)/Math.min(getMeasuredWidth()/2, getMeasuredHeight()/2);

    }

    /**
     * Sets session time lable
     * @param sessionTimeLable
     */
    public void setSessionTimeLable(String sessionTimeLable){
        this.sessionTimeLable = sessionTimeLable;
    }

    /**
     * Sets lable for good time percent
     * @param goodTimePercentLable
     */
    public void setGoodTimePercentLable(String goodTimePercentLable){
        this.goodTimePercentLable = goodTimePercentLable;
    }

    /**
     * sets session time
     * @param sessionTime
     */
    public void setSessionTime(String sessionTime){
        this.sessionTime = sessionTime;
    }

    /**
     * sets good time percentage
     * @param goodTimePercent
     */
    public void setGoodTimePercent(float goodTimePercent){
        this.goodTimePercent=goodTimePercent;
    }
}
