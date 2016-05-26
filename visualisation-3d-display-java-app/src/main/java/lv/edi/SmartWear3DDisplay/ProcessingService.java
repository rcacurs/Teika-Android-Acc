package lv.edi.SmartWear3DDisplay;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import lv.edi.SmartWearProcessing.Segment;
import lv.edi.SmartWearProcessing.Sensor;
import lv.edi.SmartWearProcessing.SensorDataProcessing;

public class ProcessingService{
	private int ROWS, COLS;
	private float ROW_DIST, COL_DIST;
	private Vector<Vector<Sensor>> sensorGrid;
	private Vector<Vector<Segment>> segmentGrid;
	private Vector<Vector<Segment>> initialReference;
	private Vector<Vector<Segment>> currentReference;
	private float[][] currentStateRot;
	private float[][] savedStateRot;
	private Vector<Vector<Float>> distances;
	
	private boolean isProcessing = false;
	private int timerInterval = 100;
	private Timer timer;
	private WavefrontModelCreator wfCreator;
	
	public ProcessingService(Vector<Vector<Sensor>> sensorGrid, Vector<Vector<Segment>> segmentGrid, float ROW_DIST, float COL_DIST){
		this.sensorGrid = sensorGrid;
		this.segmentGrid = segmentGrid;
		wfCreator = new WavefrontModelCreator(segmentGrid);
		ROWS = segmentGrid.size();
		COLS = segmentGrid.get(0).size();
		
		distances = new Vector<Vector<Float>>(segmentGrid.size());
		for(int i=0 ;i<segmentGrid.size(); i++){
			Vector<Float> row = new Vector<Float>(segmentGrid.get(0).size());
			for(int j=0; j<segmentGrid.get(0).size(); j++){
				row.add(new Float(0));
			}
			distances.add(row);
		}
		this.ROW_DIST = ROW_DIST;
		this.COL_DIST = COL_DIST;
	}
	
	public void saveReference(){
		 initialReference = new Vector<Vector<Segment>>(ROWS);
		 currentReference = new Vector<Vector<Segment>>(ROWS); 
	     for(int i =0; i<ROWS; i++){
	    	  Vector<Segment> initialReferenceRow = new Vector<Segment>();
	    	  Vector<Segment> currentReferenceRow = new Vector<Segment>();
	    	  for(int j=0; j<COLS; j++){
	    		  
	    		  Segment savedSegment = new Segment();
	    		  savedSegment.setInitialCross2(ROW_DIST, COL_DIST);
	    		  initialReferenceRow.add(savedSegment);
	    		  
	    		  Segment currentReferenceSegment = new Segment();
	    		  currentReferenceSegment.setInitialCross2(ROW_DIST, COL_DIST);
	    		  currentReferenceRow.add(currentReferenceSegment);
	    		    
	    	  }
	    	  initialReference.add(initialReferenceRow);
	    	  currentReference.add(currentReferenceRow);
	    }
		Segment.setAllSegmentOrientationsTRIAD(initialReference, sensorGrid);
		Segment.setSegmentCenters(initialReference, (short) (initialReference.size()/2), (short) (initialReference.get(0).size()/2));
		savedStateRot = SensorDataProcessing.getRotationTRIAD(sensorGrid.get(sensorGrid.size()/2).get(sensorGrid.get(0).size()/2).getAccNorm(),
				  sensorGrid.get(sensorGrid.size()/2).get(sensorGrid.get(0).size()/2).getMagNorm());
	}
	
	public void clearReference(){
		initialReference = null;
		currentReference = null;
		savedStateRot = null;
		
		distances = new Vector<Vector<Float>>(segmentGrid.size());
		for(int i=0 ;i<segmentGrid.size(); i++){
			Vector<Float> row = new Vector<Float>(segmentGrid.get(0).size());
			for(int j=0; j<segmentGrid.get(0).size(); j++){
				row.add(new Float(0));
			}
			distances.add(row);
		}
	}

	
	
	// starts processing service!
	public void startProcessing(float samplingFrequency){
	    timerInterval = (int)(1000/samplingFrequency);
		isProcessing = true;
		timer = new Timer();
	    timer.scheduleAtFixedRate(new TimerTask(){
	    @Override
	    	public void run(){
	    		Segment.setAllSegmentOrientationsTRIAD(segmentGrid, sensorGrid);
	    		Segment.setSegmentCenters(segmentGrid, (short) (segmentGrid.size()/2), (short) (segmentGrid.get(0).size()/2));
	    		currentStateRot = SensorDataProcessing.getRotationTRIAD(sensorGrid.get(sensorGrid.size()/2).get(sensorGrid.get(0).size()/2).getAccNorm(),
	    											  sensorGrid.get(sensorGrid.size()/2).get(sensorGrid.get(0).size()/2).getMagNorm());
	    		
	    		if(initialReference!=null){
					Segment.compansateCentersForTilt(initialReference, currentReference, savedStateRot, currentStateRot);
	    			Segment.compareByDistances(segmentGrid, currentReference, distances);
	    			wfCreator.updateVertexData(currentReference);
	    			
	    		}
	    		wfCreator.updateVertexData(segmentGrid);
	    		wfCreator.updateColorData(distances);
	    		
	    		try {
	    			
	    			File tempFile = new File("modelTemp.obj");
	    			
	    			wfCreator.writeToDisk(tempFile);

	    			File file = new File("model.obj");
	    			if(!file.exists()){
	    				tempFile.renameTo(file);
	    			}
	    		} catch (IOException e) {
	    			// TODO Auto-generated catch block
	    			System.out.println("Problem on flushing to disk");
	    			e.printStackTrace();
	    		}

	    	}
	    }, 0, timerInterval);
		
	}
	
	public void stopProcessing(){
		if(timer!=null){
			isProcessing = false;
			timer.cancel();
			timer =null;
		}
	}
	
	
	
	

}
