package lv.edi.SmartWearCalibJavaApp;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;

import lv.edi.SmartWear3DDisplay.BluetoothEventListener;
import lv.edi.SmartWear3DDisplay.BluetoothService;
import lv.edi.SmartWearProcessing.Calibration;
import lv.edi.SmartWearProcessing.Sensor;

public class Main {
    static int ROWS = 9;
    static int COLS = 7;
    static int numberOfSensors = 63;
    static int numberOfSamples = 500;
    static boolean RUN_CALIBRATION = false;
    static boolean ACQUIRE_DATA = false;
    static Vector<Sensor> sensorBuffer;
    static Vector<DenseMatrix64F> magnMeas = new Vector<DenseMatrix64F>(numberOfSensors);

    static String currentDirectory;

    static BluetoothService btService;
    static String s;
    static int selectedDeviceIndex=0;
    static private File calibFile;
    static Scanner cons;
    static MyBluetoothListener btListener = new MyBluetoothListener();

    public Main(){
        btListener = new MyBluetoothListener();
    }
    public static void main(String args[]) throws InterruptedException {
        Calibration.init(); // initalize calibration
            if(args.length>0){
                numberOfSamples = Integer.parseInt(args[0]);
                ///System.out.println("Number of samples: "+numberOfSamples);
            }

        cons = new Scanner(System.in);
        currentDirectory = System.getProperty("user.dir");
        sensorBuffer = new Vector<Sensor>(numberOfSensors);
        sensorBuffer.setSize(numberOfSensors);

        // allocate memory for sensor data
        for(int i=0; i<numberOfSensors; i++){
            int columnIndex = i/ROWS;
            Sensor sen = new Sensor(i, true);
            sensorBuffer.set(i, sen);

            magnMeas.add(new DenseMatrix64F(numberOfSamples, 3));
        }



        // prepare file for calibration data
        calibFile = new File("calibration_data.csv");
        // bluetooth listener


        btService = new BluetoothService(sensorBuffer);
        btService.registerSensorDataPacketReceivedListener(btListener);
        try {
            btService.initBluetoothService();
        } catch (BluetoothStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("Error: bluetooth error");
        }

        System.out.println("Enter index of device to connect!");
        s=cons.nextLine();
        if(s.equals("exit")){
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


        for(int i = 3; i>=0; i--){

            System.out.printf("Calibation will start in %d seconds. Be ready! ", i);
            if(i==0){
                System.out.print("\n");
            } else{
                System.out.print("\r");
            }

            Thread.sleep(1000);
        }
        ACQUIRE_DATA=true;

        while(!RUN_CALIBRATION){
            Thread.sleep(100);
        }



        Vector<DenseMatrix64F> offsets = new Vector<DenseMatrix64F>(numberOfSensors);
        Vector<DenseMatrix64F> scaling = new Vector<DenseMatrix64F>(numberOfSensors);
        System.out.println("Magnetometer data size: " + magnMeas.size());

        for(int i=0; i<magnMeas.size(); i++){
            DenseMatrix64F offs = new DenseMatrix64F(3,1);
            DenseMatrix64F Winv = new DenseMatrix64F(3,3);
            Calibration.ellipsoidFitCalibration(magnMeas.get(i), offs, Winv);

            offsets.add(offs);
            scaling.add(Winv);
        }




        try {
            Calibration.writeCalibDataToFile(offsets, scaling, calibFile);
            System.out.println("Calibration finished! Calibration data saved to " + calibFile.toString());
        } catch (IOException e) {
            System.out.println("Problem saving calib data");
        }

        System.out.println("Program closing");
    }

    /**
     * print progress bar on console
     * @param progress float 0-1.0 showing current progress
     * @param carrReturn if next text will be drawen on top of this
     */
    public static void printProgressBar(float progress, boolean carrReturn){
        int size = 10;
        int progressI = (int ) (progress*size);
        String endline = "";
        if(carrReturn){
           endline="\r";
        } else{
            endline="\n";
        }
        System.out.printf("Progress: ");

        for(int i=0; i<progressI; i++){
            System.out.printf("\u2588");
        }
        for(int i=progressI; i<size; i++){
            System.out.printf("\u2591");
        }
        System.out.printf("\t %d %%" + endline, (int)(progress * 100));
    }

    static class MyBluetoothListener implements BluetoothEventListener{
        int sampleCount = 0;
        int packetsReceived=0;

        public void onSensorDataPacketReceived(int index, Vector<Float> sensorData){
            if(ACQUIRE_DATA) {
                if (packetsReceived == index) {
                    magnMeas.get(index).set(sampleCount, 0, sensorData.get(3));
                    magnMeas.get(index).set(sampleCount, 1, sensorData.get(4));
                    magnMeas.get(index).set(sampleCount, 2, sensorData.get(5));
                    packetsReceived++;

                    if (packetsReceived >= numberOfSensors) {
                        packetsReceived = 0;
                        sampleCount++;
                        printProgressBar((float) sampleCount / numberOfSamples, true);
                        if (sampleCount == numberOfSamples) {
                            printProgressBar(1.0f, false);
                            btService.stopReceiveData();
                            ACQUIRE_DATA = false;
                            RUN_CALIBRATION = true;


                        }
                    }
                    ;
                }
            }
        }
    }
}


