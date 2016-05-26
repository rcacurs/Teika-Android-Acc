package lv.edi.SmartWearGraphics3D;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Toast;

import lv.edi.SmartWearProcessing.SensorDataProcessing;

/**
 * Created by Richards on 15.08.2015..
 * Class that extends gl sufrace view to draw posture models
 */

public class PostureView extends GLSurfaceView {
    private PostureRenderer renderer;
    private float rotationAngleZ = 0; // camera rotation angle
    private float rotationAngleY = 0; // camera rotaiton angle y
    private float ROTATION_CONSTANT = 0.01f; // rotation sensitivity
    private final float[] INITIAL_VECT = {0, -30, 0};
    private final float[] INITIAL_UP_VECT = {0, 0, 1}; // CAMERA UP VECTOR
    private GestureDetector gestDetector;
    Context context;
    /**
     * sonctructor of view
     * @param context
     */

    public PostureView(Context context){
        this(context, null);
    }

    public PostureView(Context context, AttributeSet attrs){
        this(context, attrs, 0);
    }

    public PostureView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs);
        this.context = context;
        setEGLContextClientVersion(1);
        renderer = new PostureRenderer(context);
        setRenderer(renderer);
        gestDetector = new GestureDetector(context, new GestureListener());
    }

    /**
     * Sets posture model to be drawed in this view
     * @param model model to be drawed
     */
    public void addPostureModel(PostureSurfaceModel model){
        renderer.addPostureModel(model);
    }

    /**
     * removes all models from renderer
     */
    public void removeAllPostureModels(){
        renderer.removeAllPostureModels();
    }

    //private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;
    float temp[] = new float[3];// temporary vector for rotations
    float temp2[] = new float[3];
    float tempUp[] = new float[3];
    float tempUp2[] = new float[3];
    float quaternionY[] = new float[4];
    float quaternionZ[] = new float[4];

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        float x = e.getX();
        float y = e.getY();
        // Log.d("Touch input"," x: "+x);
        // Log.d("Touch input"," y:"+y);
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:

                float dx = x - mPreviousX;
                float dy = y - mPreviousY;

                rotationAngleZ+= ((dx) * ROTATION_CONSTANT);
                rotationAngleY+= ((dy) * ROTATION_CONSTANT);//
                // quaternion for x axis rotation
                quaternionY[0] = (float)Math.cos(rotationAngleY/2);
                quaternionY[1] = (float)Math.sin(rotationAngleY/2);
                quaternionY[2] = 0;
                quaternionY[3] = 0;
                //quaternion for z axis rotation
                quaternionZ[0]=(float)Math.cos(rotationAngleZ/2);
                quaternionZ[1]=0;
                quaternionZ[2]=0;
                quaternionZ[3]=(float)Math.sin(rotationAngleZ/2);


                //rotate around x axis from initial vector position
                // temporary vector for  camera up vector
                float[] offset = {0, 0, 0};
                float[] initialVectOffset = new float[3];
                for(int i=0; i<3;i++){
                    initialVectOffset[i]=INITIAL_VECT[i]-offset[i];
                }
                SensorDataProcessing.quatRotate(quaternionY, initialVectOffset, temp);
                SensorDataProcessing.quatRotate(quaternionY, INITIAL_UP_VECT, tempUp);

                //rotate around Z axis
                SensorDataProcessing.quatRotate(quaternionZ, temp, temp2);
                SensorDataProcessing.quatRotate(quaternionZ, tempUp, tempUp2);
                // update rotated vector coordinates
                for(int i = 0; i<renderer.viewPointVector.length;i++){
                    renderer.viewPointVector[i]=temp2[i]+offset[i];
                    renderer.cameraUpVector[i]=tempUp2[i];
                }
        }

        mPreviousX = x;
        mPreviousY = y;

        gestDetector.onTouchEvent(e);
        return true;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
        // event when double tap occurs
        @Override
        public boolean onDoubleTap(MotionEvent e) {
//	        	Intent intent = new Intent(thisContext, SavePostureIMGActivity.class);
//	        	//Bitmap screenShot = takeScreenshot(currentActivity);
//	            View v = container.getRootView();
//	            v.setDrawingCacheEnabled(true);
//	            //Bitmap screenShot = v.getDrawingCache();
//	        	v.setDrawingCacheEnabled(true);
//	        	Bitmap fullScreenBitmap = Bitmap.createBitmap(v.getDrawingCache());
//	        	container.setDrawingCacheEnabled(false);
//	        	intent.putExtra("screenBitmap",fullScreenBitmap);
//	        	startActivity(intent);
//	            //Log.d("Double Tap", "Tapped at: (" + x + "," + y + ")");
            for(int i = 0; i<renderer.viewPointVector.length;i++){
                renderer.viewPointVector[i]=INITIAL_VECT[i];
                renderer.cameraUpVector[i]=INITIAL_UP_VECT[i];
                rotationAngleZ=0;
                rotationAngleY=0;
            }
            //Toast.makeText(getBaseContext(), "Camera View Reset", Toast.LENGTH_SHORT).show();
            return true;
        }
    }

}
