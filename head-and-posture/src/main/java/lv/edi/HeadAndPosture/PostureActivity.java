package lv.edi.HeadAndPosture;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Vector;

import lv.edi.BluetoothLib.BluetoothService;
import lv.edi.SmartWearGraphics3D.PostureRenderer;
import lv.edi.SmartWearGraphics3D.PostureSurfaceModel;
import lv.edi.SmartWearGraphics3D.PostureView;
import lv.edi.SmartWearProcessing.Segment;
import lv.edi.SmartWearProcessing.Sensor;
import lv.edi.SmartWearProcessing.SensorDataProcessing;

public class PostureActivity extends Activity {
    private PostureView postureView;

    PostureSurfaceModel currentStateModel;
    PostureSurfaceModel savedStateModel;

    private Resources res;
    private HeadAndPostureApplication application;
    private Menu optionsMenu;

    private ToggleButton runButton;

    private int[] batteryIcons = {R.drawable.battery_discharging_000,
            R.drawable.battery_discharging_040,
            R.drawable.battery_discharging_060,
            R.drawable.battery_discharging_080,
            R.drawable.battery_discharging_100};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posture);
        runButton = (ToggleButton) findViewById(R.id.buttonRunPosture);

        postureView = (PostureView)findViewById(R.id.posture_view);
        this.application = (HeadAndPostureApplication)getApplication();
        res=getResources();

    }

    @Override
    protected void onResume(){
        super.onResume();

        currentStateModel = new PostureSurfaceModel(application.segmentsCurrent, application.modelColors, true);
        savedStateModel = new PostureSurfaceModel(application.segmentsSaved);
        postureView.removeAllPostureModels();

        postureView.addPostureModel(currentStateModel);
        postureView.addPostureModel(savedStateModel);

        application.uiHandler = new Handler(Looper.getMainLooper()){

            public void handleMessage(Message inputMessage){
                switch(inputMessage.what){
                    case BluetoothService.BT_CONNECTING:
                        Toast.makeText(getApplicationContext(), res.getString(R.string.toast_connecting_bt), Toast.LENGTH_SHORT).show();
                        optionsMenu.findItem(R.id.action_bluetooth_connection_status).setIcon(R.drawable.loading);
                        break;
                    case BluetoothService.BT_CONNECTED:
                        Toast.makeText(getApplicationContext(), res.getString(R.string.toast_connected_bt), Toast.LENGTH_SHORT).show();
                        optionsMenu.findItem(R.id.action_bluetooth_connection_status).setIcon(R.drawable.check);
                        break;
                    case BluetoothService.BT_DISCONNECTED:
                        Toast.makeText(getApplicationContext(), res.getString(R.string.toast_disconnected_bt), Toast.LENGTH_SHORT).show();
                        optionsMenu.findItem(R.id.action_bluetooth_connection_status).setIcon(R.drawable.not);
                        application.processingService.stopProcessing();
                        application.postureProcessingService.stopProcessing();
                        runButton.setChecked(false);
                        break;
                    case HeadAndPostureApplication.BATTERY_LEVEL_UPDATE:
                        int batteryLevelIndex = inputMessage.arg1 / 20;;
                        if(batteryLevelIndex < 0){
                            batteryLevelIndex = 0;
                        }
                        if(batteryLevelIndex>=batteryIcons.length){
                            batteryLevelIndex = batteryIcons.length - 1;
                        }
                        optionsMenu.findItem(R.id.action_battery_level_icon).setIcon(batteryIcons[batteryLevelIndex]);
                    default:
                        break;
                }
            }
        };

        runButton.setChecked(application.processingService.isProcessing());
        application.setActiveActivity(1);

    }

    public void onClickSave(View view){
        if(application.btService.isConnected()) {

            application.processingService.setReference(application.sensors.get(application.headSensorIndex).getAccNorm());
            application.segmentsSaved.get(application.refRow).get(application.refCol).center[0]=0;
            application.segmentsSaved.get(application.refRow).get(application.refCol).center[1]=0;
            application.segmentsSaved.get(application.refRow).get(application.refCol).center[2]=0;

            application.segmentsSavedInitial.get(application.refRow).get(application.refCol).center[0]=0;
            application.segmentsSavedInitial.get(application.refRow).get(application.refCol).center[1]=0;
            application.segmentsSavedInitial.get(application.refRow).get(application.refCol).center[2]=0;

            Segment.setAllSegmentOrientations(application.segmentsSaved, application.sensorGrid);
            Segment.setAllSegmentOrientations(application.segmentsSavedInitial, application.sensorGrid);

            Segment.setSegmentCenters(application.segmentsSaved, (short) application.refRow, (short) application.refCol);
            Segment.setSegmentCenters(application.segmentsSavedInitial, (short) application.refRow, (short)application.refCol);

            application.postureProcessingService.setReferenceState(application.segmentsSaved, application.segmentsSavedInitial);


            application.setIsStateSaved(true);

            Toast.makeText(this, res.getString(R.string.toast_saved), Toast.LENGTH_SHORT).show();
        } else{
            Toast.makeText(this, res.getString(R.string.toast_must_connect_bt), Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickStart(View view){
        ToggleButton button = (ToggleButton)view;
        if(button.isChecked()) {
            if (application.btService.isConnected()){
                if (application.postureProcessingService.isStateSaved()) {
                    application.postureProcessingService.startProcessing(application.samplingFrequency);


                    application.processingService.setIconRadius(application.htView.getIconRelativeRadius());
                    application.processingService.startProcessing(application.samplingFrequency);

                    application.setIsProcessing(true);
                    try {
                        application.dataLogger.startLogSession(application.samplingFrequency);
                    } catch (FileNotFoundException ex) {
                        Log.d("LOGGING", "FILE NOT FOUND EXCEPTION");
                    }
                } else {
                    button.setChecked(false);
                    Toast.makeText(this, res.getString(R.string.toast_save_state), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, res.getString(R.string.toast_must_connect_bt), Toast.LENGTH_SHORT).show();
                button.setChecked(false);
            }
        } else {
            File logFile = application.dataLogger.stopLogSession();
            MediaScannerConnection.scanFile(this, new String[]{logFile.toString()}, null, null); // solves problem with mtp
            application.postureProcessingService.stopProcessing();
            application.processingService.stopProcessing();
            application.setIsProcessing(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu);
        optionsMenu = menu;
        if(application.btService!=null){
            if(application.btService.isConnected()){
                optionsMenu.findItem(R.id.action_bluetooth_connection_status).setIcon(R.drawable.check);
            } else{
                optionsMenu.findItem(R.id.action_bluetooth_connection_status).setIcon(R.drawable.not);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, AppPreferenceActivity.class);
            startActivity(intent);
            return true;
        }

        if(id==R.id.action_head){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            return true;
        }

        if(id == R.id.action_bluetooth_connection_status){
            if(!application.btService.isConnecting()) {
                if (application.btService.isConnected()) {
                    application.btService.disconnectDevice();
                } else {
                    if (application.btDevice != null) {
                        application.btService.connectDevice(application.btDevice);
                    } else {
                        Toast.makeText(this, res.getString(R.string.toast_set_bt_target), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
