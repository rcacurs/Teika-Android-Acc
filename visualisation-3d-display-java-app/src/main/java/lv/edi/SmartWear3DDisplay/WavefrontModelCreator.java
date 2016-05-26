package lv.edi.SmartWear3DDisplay;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import lv.edi.SmartWearProcessing.Segment;

public class WavefrontModelCreator {
	static final float colorMap[][]={{0, 0.22f, 1.0f},
            {0, 0.5f, 1.0f},
            {0, 0.77f, 1.0f},
            {0.03f, 1.0f,  0.97f},
            {0.17f, 1.0f, 0.83f},
            {0.3f, 1.0f, 0.69f},
            {0.5f, 1.0f, 0.51f},
            {0.75f, 1.0f, 0.25f},
            {1.0f, 1.0f, 0.12f},
            {1.0f, 0.67f,   0},
            {1.0f, 0, 0}
    };
	Vector<Float> vertexes;
	Vector<Float> colors;
	Vector<Integer> vertexIndexes;
	Vector<Vector<Segment>> segments;
	private float minRange=0;
	private float maxRange=5;
	private boolean writeColor = true;
	
	// constructs wave front object from segment grid
	public WavefrontModelCreator(Vector<Vector<Segment>> segments){
		this.segments=segments;
		updateVertexData(segments);
		vertexIndexes = getIndexes(segments.size(), segments.get(0).size());
	}
	
	public void updateVertexData(Vector<Vector <Segment>> segments){
		this.segments=segments;
		
		vertexes = new Vector<Float>();
		
		for(int i=0; i<segments.size(); i++){
			for(int j=0; j<segments.get(0).size(); j++){
				for(int z=0; z<3; z++){
					vertexes.add(new Float(segments.get(i).get(j).center[z]));
				}
			}
		}
		
	}
	public void setMaxRange(float maxRange){
		this.maxRange = maxRange;
	}
	public void updateColorData(Vector<Vector<Float>> distances){
		colors = new Vector<Float>();
		for(int i=0; i<distances.size(); i++){
			for(int j=0; j<distances.get(0).size(); j++){
				int index = (int)((colorMap.length-1)*(distances.get(i).get(j)-minRange)/(maxRange-minRange));
				index = Math.min(index, colorMap.length-1);
				for(int z=0; z<3; z++){
					colors.add(colorMap[index][z]);
				}
			}
		}
	}
	
	public void writeToDisk(File file) throws IOException{
		if((vertexes==null) && (vertexIndexes==null)) return;
		
		FileWriter writer = new FileWriter(file);
		for(int i=0; i<vertexes.size(); i+=3){	
			writer.write("v "+(-vertexes.get(i))+" "+vertexes.get(i+1)+" "+vertexes.get(i+2)+"\n");
			if(writeColor){
				writer.write("c "+(colors.get(i))+" "+colors.get(i+1)+" "+colors.get(i+2)+"\n");
			}
		}
		
		for(int i=0; i<vertexIndexes.size(); i+=3){
			writer.write("f "+vertexIndexes.get(i)+" "+vertexIndexes.get(i+1)+" "+vertexIndexes.get(i+2)+"\n");
		}
		
		writer.close();
		
	};
	
	public Vector<Integer> getIndexes(int rows, int cols){
		Vector<Integer> gridFillIndexes = new Vector<Integer>();
		for(int r=0;r<rows-1;r++){
			for(int c=0;c<cols-1;c++){
				gridFillIndexes.add(new Integer(r*cols+c+1));
				gridFillIndexes.add(new Integer(r*cols+c+1+1));
				gridFillIndexes.add(new Integer((r+1)*cols+c+1+1));
				gridFillIndexes.add(new Integer(r*cols+c+1));
				gridFillIndexes.add(new Integer((r+1)*cols+c+1+1));
				gridFillIndexes.add(new Integer((r+1)*cols+c+1));
			}
		}
		return gridFillIndexes;
	}
	
	public void setColorMapperRanges(int minRange, int maxRange){
		this.minRange=minRange;
		this.maxRange=maxRange;
	}
		

}
