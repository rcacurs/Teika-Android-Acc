package lv.edi.SmartWear3DDisplay;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;

import lv.edi.SmartWearProcessing.Segment;
import lv.edi.SmartWearProcessing.Sensor;
import lv.edi.SmartWearProcessing.SensorDataProcessing;

public class Main {
	
	static int ROWS = 9;
	static int COLS = 7;
	static float ROW_DIST = 4.7f;
	static float COL_DIST = 3.5f;
	static int numberOfSensors = 63;
	static Scanner cons;
	static Vector<Sensor> sensorBuffer;
	static Vector<Vector<Sensor>> sensorGrid;
	static Vector<Vector<Segment>> segmentGrid;
	static Vector<Vector<Segment>> savedStateGrid;
	static ProcessingService processingService;
	
	static String currentDirectory;
	
	static BluetoothService btService;
	static String s;
	static int selectedDeviceIndex=0;
	public static void main(String[] args) {
		
		currentDirectory = System.getProperty("user.dir");
		
		System.out.println(currentDirectory);
		
		cons = new Scanner(System.in);
		
		// testing 
		// INITIALIZE SENSOR ARARY
        sensorBuffer = new Vector<Sensor>(numberOfSensors);
        sensorBuffer.setSize(numberOfSensors);
	    
        for(int i=0; i<numberOfSensors; i++){
	         int columnIndex = i/ROWS;
	         if(columnIndex%2==0) {
	        	 Sensor sen = new Sensor(i, true);
	        	 sen.setMountTransformMatrix(1, 3, -2, -1, 3, 2);
	        	 sensorBuffer.set(i, sen);
	             
	         }else{
	        	 Sensor sen = new Sensor(i, false);
	        	 sen.setMountTransformMatrix(1, 3, -2, -1, 3, 2);
	             sensorBuffer.set(i, sen);
	     		 
	         }
	    }
        
//      // INITIALIZE SENSOR GRID
//      
      sensorGrid = new Vector<Vector<Sensor>>(ROWS);
      segmentGrid = new Vector<Vector<Segment>>(ROWS);

      for(int i =0; i<ROWS; i++){
    	  Vector<Sensor> row = new Vector<Sensor>();
    	  Vector<Segment> segmentRow = new Vector<Segment>();

    	  for(int j=0; j<COLS; j++){
    		  row.add(sensorBuffer.get(SensorDataProcessing.getIndex(i, j, ROWS, COLS, false)));
    		  
    		  Segment segment = new Segment();
    		  segment.setInitialCross2(ROW_DIST, COL_DIST);
    		  segmentRow.add(segment);

    	  }
    	  segmentGrid.add(segmentRow);
    	  sensorGrid.add(row);
      }

     
      try {
    	File calibFile = new File("calibration_data.csv");
		Sensor.setGridMagnetometerCalibData(calibFile, sensorGrid);
		System.out.println("Calibration data loaded");
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		System.out.println("Calibration File not specified");
	}
		// SETUP BLUETOOTH SERVICE
        
        btService = new BluetoothService(sensorBuffer);
        
        try {
			btService.initBluetoothService();
		} catch (BluetoothStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Error: bluetooth error");
		}
        
        System.out.println("Enter index of device to connect!");
        s=cons.nextLine();
        if(s.equals("quit")){
        	System.out.println("Prgogram will now close!");
        	return;
        }
        
        selectedDeviceIndex=Integer.parseInt(s);
        
        try {
			btService.startReceiveData(selectedDeviceIndex);
		} catch (IOException e) {
			System.out.println("Problem with specified index! Program will close");
			return;
		}
        
        System.out.println("Connected to device!");
        
        // INITALIZE PROCESSING SERVICE
        processingService = new ProcessingService(sensorGrid, segmentGrid, ROW_DIST, COL_DIST);
        processingService.startProcessing(10);
//
//		if(args.length < 1){
//			System.out.println("Please specify com port");
//			return; 
//		}
//		
//		System.out.println("Creating comport object...");
//		try{
//			comPortService = new SerialPortService(args[0], sensorBuffer);
//		} catch(PortInUseException ex){
//			System.out.println("Error specified port in use! Program will finish");
//			return;
//		}
//		
//		comPortService.startListening();

		do{
			s = cons.nextLine();
			if(s.equals("save")){
				processingService.saveReference();
				System.out.println("Saved!");
			}
			if(s.equals("clear")){
				processingService.clearReference();
				System.out.println("Cleared!");
			}
			
		} while(!(s.equals("quit")));
		
		btService.stopReceiveData();
		processingService.stopProcessing();
		System.out.println("Program closed");


	}
}
