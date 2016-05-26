package lv.edi.SmartWearGraphics3D;

/**
 * Created by Richards on 18.08.2015..
 */
public class ColorMapper {

    static final byte drawingModelColormap[][]={{0, (byte)57, (byte)255},
            {0, (byte)128, (byte)255},
            {0, (byte)198, (byte)255},
            {8, (byte)255, (byte)247},
            {(byte)43, (byte)255, (byte)212},
            {(byte)78, (byte)255, (byte)177},
            {(byte)126, (byte)255, (byte)129},
            {(byte)190, (byte)255, 65},
            {(byte)224, (byte)225, 31},
            {(byte)255, (byte)170, (byte)0},
            {(byte)255, 0, 0}
    };
    private float minRange=0;
    private float maxRange=0;

    /**
     * Constructs colormap for specific range
     * @param minRange
     * @param maxRange
     */
    public ColorMapper(float minRange, float maxRange){
        this.minRange=minRange;
        this.maxRange=maxRange;
    }

    /** sets max range for colormapper
     *
     * @param maxRange for colormapper
     */
    public void setMaxRange(float maxRange){
        this.maxRange=maxRange;
    }
    /**
     * returns color for specific input value
     * @param value input value
     * @return byte array of rgb color
     */
    public byte[] getColor(float value){
        int index = (int)((drawingModelColormap.length-1)*(value-minRange)/(maxRange-minRange));
        index = Math.min(index, drawingModelColormap.length-1);
        return drawingModelColormap[index];
    }
}
