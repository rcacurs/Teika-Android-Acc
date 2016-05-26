package lv.edi.HeadAndPosture;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import lv.edi.BluetoothLib.*;

import lv.edi.SmartWearGraphics3D.ColorMapper;
import lv.edi.SmartWearProcessing.Segment;
import lv.edi.SmartWearProcessing.Sensor;
import lv.edi.SmartWearProcessing.SensorDataProcessing;

/**
 * Created by Richards on 18/06/2015.
 */
public class HeadAndPostureApplication extends Application implements SharedPreferences.OnSharedPreferenceChangeListener, BluetoothEventListener, ProcessingEventListener, BatteryLevelEventListener {
    public static final int  BATTERY_LEVEL_UPDATE = 45;
    final int REQUEST_ENABLE_BT = 2;
    SharedPreferences sharedPrefs;
    BluetoothAdapter btAdapter;
    BluetoothDevice btDevice;
    boolean vibrateFeedback;
    boolean alertFeedback;
    float threshold, postureThreshold=3;
    float colormapMaxRange=5;
    float relativeIconRadius;

    int activeActivity=0;

    int numberOfSensors;
    int headSensorIndex;
    int nrOfCols, nrOfRows;
    int refRow, refCol;
    int batteryPacketIndex;
    float rowDist, colDist;
    float samplingFrequency;
    boolean startSensorLeft;

    private boolean isStateSaved=false;
    File applicationFolder;

    BluetoothService btService;
    Vector<Sensor> sensors;
    Vector<Vector<Sensor>> sensorGrid;
    Vector<Vector<Segment>> segmentsSaved;
    Vector<Vector<Segment>> segmentsSavedInitial;
    Vector<Vector<Segment>> segmentsCurrent;
    Vector<Vector<byte[]>> modelColors;
    Vector<Vector<Float>> distances;
    ColorMapper colorMapper;

    BatteryLevel batteryLevel;
    Handler uiHandler;
    HeadTiltProcessingService processingService;
    PostureProcessingService postureProcessingService;
    DataLogger dataLogger;
    HeadTiltView htView;
    Vibrator vibrator;
    MediaPlayer mp;

    boolean isProcessing=false;

    @Override
    public void onCreate(){
        super.onCreate();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);

        // OBTAIN SETTING VALUES
        String numberOfSensorsS = sharedPrefs.getString("pref_nr_sensors", "21");
        int numberOfSensors = Integer.parseInt(numberOfSensorsS);
        String headSensorIndexS = sharedPrefs.getString("pref_head_idx", "20");
        headSensorIndex = Integer.parseInt(headSensorIndexS);
        String nrOfColsS = sharedPrefs.getString("pref_nr_cols", "4");
        nrOfCols = Integer.parseInt(nrOfColsS);
        String nrOfRowsS = sharedPrefs.getString("pref_nr_rows", "5");
        nrOfRows = Integer.parseInt(nrOfRowsS);
        String batteryPacketIndexS = sharedPrefs.getString("pref_battery_idx", "21");
        batteryPacketIndex = Integer.parseInt(batteryPacketIndexS);
        startSensorLeft = sharedPrefs.getBoolean("pref_start_left", false);
        alertFeedback = sharedPrefs.getBoolean("pref_vibrate", false);
        vibrateFeedback = sharedPrefs.getBoolean("pref_alert", false);
        String refRowS = sharedPrefs.getString("pref_ref_row_idx", "2");
        refRow = Integer.parseInt(refRowS);
        String refColS = sharedPrefs.getString("pref_ref_col_idx", "2");
        refCol = Integer.parseInt(refColS);
        String colDistS = sharedPrefs.getString("pref_col_distance", "4.25");
        colDist = Float.parseFloat(colDistS);
        String rowDistS = sharedPrefs.getString("pref_row_distance", "6.75");
        rowDist = Float.parseFloat(rowDistS);
        String thresholdSetting = sharedPrefs.getString("pref_threshold", "0.7");
        threshold = Float.parseFloat(thresholdSetting);
        String postureThresholdS = sharedPrefs.getString("pref_threshold_posture", "3.0") ;
        postureThreshold = Float.parseFloat(postureThresholdS);
        String colormapMaxRangeS = sharedPrefs.getString("pref_max_range_colormap", "5.0");
        colormapMaxRange = Float.parseFloat(colormapMaxRangeS);
        String samplingFrequencyS = sharedPrefs.getString("pref_sample_rate", "25");
        samplingFrequency = Float.parseFloat(samplingFrequencyS);



        colorMapper = new ColorMapper(0, colormapMaxRange);

        sensors = new Vector<Sensor>(numberOfSensors);
        sensors.setSize(numberOfSensors);
        for(int i=0; i<numberOfSensors; i++){
            int columnIndex = i/nrOfRows;
            if(columnIndex%2==0) {
                sensors.set(i, new Sensor(i, true));
            }else{
                sensors.set(i, new Sensor(i, false));
            }
        }
        sensors.get(headSensorIndex).setOrientationUp(true);

        batteryLevel = new BatteryLevel();
        batteryLevel.registerListener(this);

        sensorGrid = new Vector<Vector<Sensor>>(nrOfRows);
        segmentsSaved = new Vector<Vector<Segment>>(nrOfRows);
        segmentsSavedInitial = new Vector<Vector<Segment>>(nrOfRows);
        segmentsCurrent = new Vector<Vector<Segment>>(nrOfRows);
        modelColors = new Vector<Vector<byte[]>>(nrOfRows);
        distances = new Vector<Vector<Float>>(nrOfRows);

        sensorGrid.setSize(nrOfRows);
        segmentsSaved.setSize(nrOfRows);
        segmentsSavedInitial.setSize(nrOfRows);
        segmentsCurrent.setSize(nrOfRows);
        modelColors.setSize(nrOfRows);
        distances.setSize(nrOfRows);

        applicationFolder = new File(Environment.getExternalStorageDirectory()+"/HeadAndPosture");

        if(!applicationFolder.exists()){
            applicationFolder.mkdir();
            MediaScannerConnection.scanFile(this, new String[]{applicationFolder.toString()}, null, null);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse(applicationFolder.toString())));
            Log.d("LOGGING", applicationFolder.toString());
        }


        // initialize all aarays for posture 3D model
        for(int i=0; i<nrOfRows; i++){
            Vector<Sensor> sensorRow = new Vector<Sensor>(nrOfCols);
            Vector<Segment> segmentRow = new Vector<Segment>(nrOfCols);
            Vector<Segment> segmentRowInit = new Vector<Segment>(nrOfCols);
            Vector<Segment> segmentRowCurrent = new Vector<Segment>(nrOfCols);
            Vector<byte[]> posturesRowColors = new Vector<byte[]>(nrOfCols);
            Vector <Float> distancesRow = new Vector<Float>(nrOfCols);

            sensorRow.setSize(nrOfCols);
            segmentRow.setSize(nrOfCols);
            segmentRowInit.setSize(nrOfCols);
            segmentRowCurrent.setSize(nrOfCols);
            posturesRowColors.setSize(nrOfCols);
            distancesRow.setSize(nrOfCols);
            for(int j=0; j<nrOfCols; j++){
                sensorRow.set(j, sensors.get(SensorDataProcessing.getIndex(i, j, nrOfRows, nrOfCols, startSensorLeft)));
                Segment segment = new Segment();
                segment.setInitialCross2(rowDist, colDist);
                segmentRow.set(j, segment);

                segment=new Segment();
                segment.setInitialCross2(rowDist, colDist);
                segmentRowInit.set(j, segment);

                segment=new Segment();
                segment.setInitialCross2(rowDist, colDist);
                segmentRowCurrent.set(j, segment);

                byte [] color = new byte[3];
                color[0]=(byte)255;
                color[1]=0;
                color[2]=(byte)255;
                posturesRowColors.set(j, color);

                distancesRow.set(j, new Float(0.0));

            }
            sensorGrid.set(i, sensorRow);
            segmentsSaved.set(i, segmentRow);
            segmentsSavedInitial.set(i, segmentRowInit);
            segmentsCurrent.set(i, segmentRowCurrent);
            modelColors.set(i, posturesRowColors);
            distances.set(i, distancesRow);
        }

        if(processingService == null) {
            Log.d("PREFERENCES", "HEAD SENSOR IDX "+headSensorIndex);
            processingService = new HeadTiltProcessingService(sensors.get(headSensorIndex), 10, threshold);
            processingService.setProcessingEventListener(this);
            processingService.setThreshold(threshold);
        }

        if(postureProcessingService==null){
            postureProcessingService=new PostureProcessingService(segmentsCurrent,
                    sensorGrid, refRow, refCol, postureThreshold);
            postureProcessingService.setDistancesArray(distances);
            postureProcessingService.setProcessingResultEventListener(this);
        }

        dataLogger = new DataLogger(applicationFolder, processingService, postureProcessingService, samplingFrequency);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals("pref_bluetooth_target")){
            String btDeviceAddress = sharedPreferences.getString("pref_bluetooth_target", "none");
            if(btDeviceAddress.equals("none")){
                btDevice = null;
            } else{
                btAdapter.getRemoteDevice(btDeviceAddress);
            }
        }

        if(key.equals("pref_threshold")){
            String thresholdSetting = sharedPreferences.getString("pref_threshold", "0.7");
            float thresholdSettingf = Float.parseFloat(thresholdSetting);
            htView.setThreshold(thresholdSettingf);
            if(processingService!=null){
                processingService.setThreshold(thresholdSettingf);
            }
        }

        if(key.equals("pref_vibrate")){
            vibrateFeedback = sharedPreferences.getBoolean("pref_vibrate", false);
            Log.d("PREFERENCES", "vibrate set " + vibrateFeedback);
        }

        if(key.equals("pref_alert")){
            alertFeedback = sharedPreferences.getBoolean("pref_alert", false);
            Log.d("PREFERENCES", "alert set "+alertFeedback);
        }

        if(key.equals("pref_nr_sensors")){
            String numberOfSensorsS = sharedPreferences.getString("pref_nr_sensors", "21");
            numberOfSensors = Integer.parseInt(numberOfSensorsS);
            sensors.setSize(numberOfSensors);
            for(int i=0; i<numberOfSensors; i++){
                sensors.set(i,new Sensor(i, true));
            }
            Log.d("PREFERENCES", "sensor nr changed "+numberOfSensors);

        }

        if(key.equals("pref_head_idx")){
            String headSensorIndexS = sharedPreferences.getString("pref_head_idx", "20");
            headSensorIndex = Integer.parseInt(headSensorIndexS);
            if((processingService!=null)&&(headSensorIndex<sensors.size())){
                Log.d("PREFERENCES", "SENSOR IDX CHANGED IN PROCESSING SERVICE "+headSensorIndex);
                processingService.setSensor(sensors.get(headSensorIndex));
            }
            Log.d("PREFERENCES", "head sensor idx changed "+headSensorIndex);
        }

        if(key.equals("pref_nr_cols")){
            String nrOfColsS = sharedPreferences.getString("pref_nr_cols", "4");
            nrOfCols = Integer.parseInt(nrOfColsS);

            Log.d("PREFERENCES", "number of columns changed "+nrOfCols);
        }

        if(key.equals("pref_nr_rows")){
            String nrOfRowsS = sharedPreferences.getString("pref_nr_rows", "5");
            nrOfRows = Integer.parseInt(nrOfRowsS);

            Log.d("PREFERENCES", "number of rows changed "+nrOfRows);
        }

        if(key.equals("pref_start_left")){
            startSensorLeft = sharedPreferences.getBoolean("pref_start_left", false);
            Log.d("PREFERENCES", "starting sensor from left: "+startSensorLeft);
        }

        if(key.equals("pref_battery_idx")){
            String batteryPacketIndexS = sharedPreferences.getString("pref_battery_idx", "21");
            batteryPacketIndex = Integer.parseInt(batteryPacketIndexS);
            if(btService!=null){
                btService.setBatteryPacketIndex(batteryPacketIndex);
            }
            Log.d("PREFERENCES", "battery packet index changed "+batteryPacketIndex);
        }

        if(key.equals("pref_ref_row_idx")){
            String refRowS = sharedPreferences.getString("pref_ref_row_idx", "2");
            refRow = Integer.parseInt(refRowS);

            Log.d("PREFERENCES", "reference row changed "+refRow);
        }

        if(key.equals("pref_ref_col_idx")){
            String refColS = sharedPreferences.getString("pref_ref_col_idx", "2");
            refCol = Integer.parseInt(refColS);

            Log.d("PREFERENCES", "reference col changed  "+refCol);
        }
        if(key.equals("pref_max_range_colormap")){
            String colormmapMaxRangeS = sharedPreferences.getString("pref_max_range_colormap", "5");
            colormapMaxRange = Float.parseFloat(colormmapMaxRangeS);
            colorMapper.setMaxRange(colormapMaxRange);
            Log.d("PREFERENCES", "colormapper range vluae changed "+colormapMaxRange);
        }

        if(key.equals("pref_threshold_posture")){
            String postureThresholdS = sharedPreferences.getString("pref_threshold_posture", "3.0");
            postureThreshold = Float.parseFloat(postureThresholdS);
            Log.d("PREFERENCES", "posture threshold value changed "+postureThreshold);
            if(postureProcessingService!=null){
                postureProcessingService.setThreshold(postureThreshold);
            }
        }
        if(key.equals("pref_sample_rate")){
            String samplingFrequencyS = sharedPreferences.getString("pref_sample_rate", "25");
            samplingFrequency = Float.parseFloat(samplingFrequencyS);
            Log.d("PREFERENCES", "SAMPLING FREQ SET "+samplingFrequency);
        }

    }

    public void setIsStateSaved(boolean isStateSaved){
        this.isStateSaved = isStateSaved;
    }

    public boolean isStateSaved(){
        return isStateSaved;
    }

    // battery level listeners
    public void onBatteryLevelChange(BatteryLevel bLevel){
        uiHandler.obtainMessage(BATTERY_LEVEL_UPDATE, (int)bLevel.getBatteryPercentage(), 1).sendToTarget();
    }
    // Bluetooth event listeners
    @Override
    public void onBluetoothDeviceConnecting(){
        uiHandler.obtainMessage(BluetoothService.BT_CONNECTING).sendToTarget();
    }
    @Override
    public void onBluetoothDeviceConnected(){
        uiHandler.obtainMessage(BluetoothService.BT_CONNECTED).sendToTarget();
    }
    @Override
    public void onBluetoothDeviceDisconnected(){
        uiHandler.obtainMessage(BluetoothService.BT_DISCONNECTED).sendToTarget();
    }

    @Override
    public void onProcessingResult(ProcessingResult result){
        if((result.getResultType())==ProcessingResult.RESULT_HEAD) {
            htView.onProcessingResult(result);
            Log.d("PROCESSING", "ON HEAD PROCESSING"+result.isOverThreshold());
            if (result.isOverThreshold()&&(activeActivity==0)) {
                if (vibrateFeedback) {
                    vibrator.vibrate(100);
                }
                if (alertFeedback && !mp.isPlaying()) {
                    mp.start();
                }
            }
        }

        if(result.getResultType()==ProcessingResult.RESULT_POSTURE){
            if (result.isOverThreshold()&&(activeActivity==1)) {
                if (vibrateFeedback) {
                    vibrator.vibrate(100);
                }
                if (alertFeedback && !mp.isPlaying()) {
                    mp.start();
                }
            }

            for(int i=0; i<distances.size(); i++){
                for(int j=0; j<distances.get(0).size(); j++){
                    byte[] color = colorMapper.getColor(distances.get(i).get(j));
                    modelColors.get(i).get(j)[0]=color[0];
                    modelColors.get(i).get(j)[1]=color[1];
                    modelColors.get(i).get(j)[2]=color[2];
                }
            }
        }
    }

    /** sets if both processing services are running
     *
     */
    public void setIsProcessing(boolean isProcessing){
        this.isProcessing = isProcessing;
    }

    /**
     * returns true if both processes are processing
     * @return
     */
    public boolean isProcessing(){
        return isProcessing;
    }

    public void setActiveActivity(int activeActivity){
        this.activeActivity=activeActivity;
        dataLogger.setActiveActivity(activeActivity);
    }
}
