package lv.edi.SmartWearGraphics3D;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.util.Log;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Richards on 15.08.2015..
 */
public class PostureRenderer implements GLSurfaceView.Renderer{
    private Context mContext;
    volatile public double viewPointVector[] = {0, -30, 0};
    volatile public double cameraUpVector[] = {0, 0, 1};
    private Vector<PostureSurfaceModel> surfaceModels;

    public PostureRenderer(Context context)
    {   surfaceModels = new Vector<PostureSurfaceModel>();
        mContext = context;
    }

    @Override
    public void onDrawFrame(GL10 gl)
    {
        //DO STUFF

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity(); //7
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        gl.glDisable(GL10.GL_CULL_FACE);

        GLU.gluLookAt(gl, (float) viewPointVector[0], (float) viewPointVector[1], (float) viewPointVector[2], 0, 0, 0, (float) cameraUpVector[0], (float) cameraUpVector[1], (float) cameraUpVector[2]);
        for(int i=0; i<surfaceModels.size(); i++) {
            if (surfaceModels.get(i) != null) {
                surfaceModels.get(i).draw(gl);
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        gl.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumf(-ratio, ratio, -1, 1, 1, 140);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        //DO STUFF
        gl.glDisable(GL10.GL_DITHER);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                GL10.GL_FASTEST);

        gl.glClearColor(1, 1, 1, 1);
        //gl.glEnable(GL10.GL_CULL_FACE);
        gl.glEnable(GL10.GL_DEPTH_TEST);
    }

    /**
     * pushes posutre model to render
     * @param model PostureSurfaceModel object
     */
    public void addPostureModel(PostureSurfaceModel model){

        this.surfaceModels.add(model);
    }

    /**
     * epties posture surface model buffer
     */
    public void removeAllPostureModels(){
        if(surfaceModels!=null) {
            this.surfaceModels.clear();
        }
    }
}
