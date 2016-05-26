package lv.edi.SmartWearGraphics3D;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Vector;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import lv.edi.SmartWearProcessing.Segment;

/**
 * Created by Richards on 15.08.2015..
 */
public class PostureSurfaceModel {
    private ByteBuffer gridContourcolors;
    private Vector<Vector<Segment>> segments; // 2D structure containing segments of surface grid indexed from left bottom corner
    private Vector<Vector<byte[]>> colors;   // array containing model colors for grid points
    private ByteBuffer gridContourIndexes;
    private ByteBuffer gridFillIndexes;
    private ByteBuffer fillColors;
    private ByteBuffer vertexByteBuffer;
    private FloatBuffer vertexBuffer;
    private int rows, cols;
    private boolean fill = false;


    /** Constructs posture surface model from specific segments array
     *
     * @param segments 2 dimensional Vector<Vector<Segment>> data structure containing segments to bet
     *                 drawed
     */
    public PostureSurfaceModel(Vector<Vector<Segment>> segments){
        this.segments=segments;

        // also set up reference grid colors
        rows = segments.size();
        cols = segments.get(0).size();

        gridContourcolors=ByteBuffer.allocateDirect(rows * cols * 4);
        for(int i=0; i<rows*cols; i++){
            gridContourcolors.put((byte)0);
            gridContourcolors.put((byte)0);
            gridContourcolors.put((byte)0);
            gridContourcolors.put((byte)255);
        }
        gridContourcolors.position(0);

        // FILLING INDEXES FOR grid contours
        //VERTEXES PUT INT THE BUFFER INT THIS WAY
        // grid has R rows and C cols
        //
        //
        //(R-1)*(C)+1 ...............(R-1)*(C)+C-1
        //..........................
        //
        // (C-1)*1+1  C-1+2 ....... (C-1)*1+C
        // 0 		   1 	.......  C-1
        //
        // opengl vertex index for particular sensor can be calculated
        // 		index=r*C+c; where r particular row index (0, 1, R-1), C - number of columns, c-particular column index (0, 1, 2, 3)
        //
        // grid contours
        //---------  ----
        //|          |  |   |
        // --------  |  |   |
        //        |  |  |   |
        // --------  |  -----


        gridContourIndexes=ByteBuffer.allocateDirect(rows*cols*2);
        boolean flag=true; //direction flag currently indicates direction to left
        int index_counter=0; // counter for saved_state indexes
        byte index=0;
        for(int r=0;r<rows;r++){
            for(int c=0;c<cols;c++){
                if(flag==true){
                    gridContourIndexes.put((byte)(r*cols+c));
                }else {
                    gridContourIndexes.put((byte)(r*cols+((cols-1)-c)));
                }
                index_counter++;
            }
            flag=!flag;// flip direction flag
        }
        boolean flag2=false; // second flag to determine
        int cc;
        for(int c=0;c<cols;c++){
            if(flag){
                cc=c;
            } else{
                cc=cols-1-c;
            }
            for(int r=0;r<rows;r++){
                if(flag2==true){
                    gridContourIndexes.put((byte)((r*cols)+cc));
                } else{
                    gridContourIndexes.put((byte)((rows-1-r)*cols+cc));
                }
                index_counter++;
            }
            flag2=!flag2; // flip flag
        }
        gridContourIndexes.position(0);

        //====================================================================
        //filling indexes for fill of the trid
        // drawing via triangles
        //  2____3  Forms one polygon
        //   | /|
        //   |/_|
        //   0   1
        // order in which corners are  put 0, 1, 3,
//		byte indices_current[] =
//			{
//				0, 1, 8, 0, 8, 7, 1, 2, 9, 1, 9, 8, 2, 3, 10, 2, 10, 9, 3, 4, 11, 3, 11, 10, 4, 5, 12, 4, 12, 11, 5, 6, 13, 5, 13, 12,
//
//				7, 8, 15, 7, 15, 14, 8, 9, 16, 8, 16, 15, 9, 10, 17, 9, 17, 16, 10, 11, 18, 10, 18, 17, 11, 12, 19, 11, 19, 18, 12, 13, 20, 12, 20, 19,
//
//				14, 15, 22, 14, 22, 21, 15, 16, 23, 15, 23, 22, 16, 17, 24, 16, 24, 23, 17, 18, 25, 17, 25, 24, 18, 19, 26, 18, 26, 25, 19, 20, 27, 19, 27, 26,
//
//				21, 22, 29, 21, 29, 28, 22, 23, 30, 22, 30, 29, 23, 24, 31, 23, 31, 30, 24, 25, 32, 24, 32, 31, 25, 26, 33, 25, 33, 32, 26, 27, 34, 26, 34, 33,
//
//				28, 29, 36, 28, 36, 35, 29, 30, 37, 29, 37, 36, 30, 31, 38, 30, 38, 37, 31, 32, 39, 31, 39, 38, 32, 33, 40, 32, 40, 39, 33, 34, 41, 33, 41, 40,
//
//				35, 36, 43, 35, 43, 42, 36, 37, 44, 36, 44, 43, 37, 38, 45, 37, 45, 44, 38, 39, 46, 38, 46, 45, 39, 40, 47, 39, 47, 46, 40, 41, 48, 40, 48, 47,
//
//				42, 43, 50, 42, 50, 49, 43, 44, 51, 43, 51, 50, 44, 45, 52, 44, 52, 51, 45, 46, 53, 45, 53, 52, 46, 47, 54, 46, 54, 53, 47, 48, 55, 47, 55, 54,
//
//				49, 50, 57, 49, 57, 56, 50, 51, 58, 50, 58, 57, 51, 52, 59, 51, 59, 58, 52, 53, 60, 52, 60, 59, 53, 54, 61, 53, 61, 60, 54, 55, 62, 54, 62, 61,
//			};
        gridFillIndexes = ByteBuffer.allocateDirect((rows - 1) * (cols - 1)*6);
        for(int r=0;r<rows-1;r++){
            for(int c=0;c<cols-1;c++){
                gridFillIndexes.put((byte)(r*cols+c));
                gridFillIndexes.put((byte)(r*cols+c+1));
                gridFillIndexes.put((byte)((r+1)*cols+c+1));
                gridFillIndexes.put((byte)(r*cols+c));
                gridFillIndexes.put((byte)((r+1)*cols+c+1));
                gridFillIndexes.put((byte)((r+1)*cols+c));
            }
        }
        gridFillIndexes.position(0);

        // allocate vertex buffers
        vertexByteBuffer = ByteBuffer.allocateDirect(rows*cols*3*4);
        vertexByteBuffer.order(ByteOrder.nativeOrder());
        vertexBuffer=vertexByteBuffer.asFloatBuffer();
    }


    /**Constructs surface model from specifi segment array and specifies it model should be filled
     * @param segments 2d array of segments
     * @param  fill boolean flag enabling fill of the model
     */
    public PostureSurfaceModel(Vector<Vector<Segment>> segments, boolean fill){
        this(segments);
        this.fill=fill;
        if(fill){
            fillColors=ByteBuffer.allocateDirect((segments.size())*(segments.get(0).size()*4));
            fillColors.position(0);
        }
    }

    /**
     * constructor constructs posture 3D model also specifying color array
     * @param segments 2d array of segments
     * @param colors colors for each segment center
     * @param fill boolean floag enabling fill for the model with specified colors
     */
    public PostureSurfaceModel(Vector<Vector<Segment>> segments, Vector<Vector<byte[]>> colors, boolean fill){
        this(segments, fill);
        this.colors = colors;
        if(fill){
            fillColors=ByteBuffer.allocateDirect(colors.size()*colors.get(0).size()*4);
            fillColors.position(0);
            for(int i=0; i<colors.size(); i++){
                for(int j=0; j<colors.get(0).size(); j++){
                    for(int k=0; k<colors.get(0).get(0).length; k++){
                        fillColors.put(colors.get(i).get(j)[k]);
                    }
                    fillColors.put((byte)255);
                }
            }
        }
    }

    /**
     * draws surface object model
     * @param gl
     */
    public void draw(GL10 gl){

       vertexBuffer.position(0);
        // fill vertex buffer
        for (int i = 0; i < segments.size(); i++) {
            for(int j = 0; j < segments.get(0).size(); j++) {
                vertexBuffer.put(segments.get(i).get(j).getSegmentCenterX());
                vertexBuffer.put(segments.get(i).get(j).getSegmentCenterY());
                vertexBuffer.put(segments.get(i).get(j).getSegmentCenterZ());
            }
        }

        if(fill){
            //Log.d("RENDERING", "fill true");
            vertexBuffer.position(0);
            fillColors.position(0);
            //update color buffer
            for(int i=0; i<colors.size(); i++){
                for(int j=0; j<colors.get(0).size(); j++){
                    for(int k=0; k<colors.get(0).get(0).length; k++){
                        fillColors.put(colors.get(i).get(j)[k]);
                    }
                    fillColors.put((byte)255);
                }
            }
            vertexBuffer.position(0);
            fillColors.position(0);
            gl.glFrontFace(GL11.GL_CW);
            gl.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 0, fillColors);
            gl.glVertexPointer(3, GL11.GL_FLOAT, 0, vertexBuffer);
            gl.glDrawElements(GL11.GL_TRIANGLES, gridFillIndexes.capacity(), GL11.GL_UNSIGNED_BYTE, gridFillIndexes);
            //gl.glFrontFace(GL11.GL_CCW);
        }

        vertexBuffer.position(0);
        gridContourcolors.position(0);
        gridContourIndexes.position(0);
        gl.glFrontFace(GL11.GL_CW);
        gl.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 0, gridContourcolors);
        gl.glVertexPointer(3, GL11.GL_FLOAT, 0, vertexBuffer);
        gl.glDrawElements(GL11.GL_LINE_STRIP, gridContourIndexes.capacity(), GL11.GL_UNSIGNED_BYTE, gridContourIndexes);
        gl.glFrontFace(GL11.GL_CCW);

    }

}
