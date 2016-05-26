package lv.edi.HeadAndPosture;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileNotFoundException;

import lv.edi.BluetoothLib.*;
import lv.edi.SmartWearProcessing.Segment;


public class MainActivity extends Activity {
    final int REQUEST_ENABLE_BT = 1;
    private HeadAndPostureApplication application;
    private Menu optionsMenu;
    private HeadTiltView htView;
    private ToggleButton runButton;
    private Resources res;
    double r=0.5;
    double phi=0;
    private int[] batteryIcons = {R.drawable.battery_discharging_000,
                                  R.drawable.battery_discharging_040,
                                  R.drawable.battery_discharging_060,
                                  R.drawable.battery_discharging_080,
                                  R.drawable.battery_discharging_100};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        res = getResources();
        setContentView(R.layout.activity_main);

        application = (HeadAndPostureApplication)getApplication();
        htView = (HeadTiltView) findViewById(R.id.headtiltview);
        runButton = (ToggleButton) findViewById(R.id.buttonRun);
        application.btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(application.btAdapter == null){
            Toast.makeText(this, res.getString(R.string.toast_bt_not_supported), Toast.LENGTH_SHORT).show();
            finish();
        }
        // check if bluetooth is turned on
        if(!application.btAdapter.isEnabled()){
            // intnet to open activity, to turn on bluetooth if bluetooth no turned on
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT); //start activity for result
        }

        // fetch preferences

        String btAddress = application.sharedPrefs.getString("pref_bluetooth_target", "none");
        if(btAddress.equals("none")){
            application.btDevice = null;
        } else{
            application.btDevice = application.btAdapter.getRemoteDevice(btAddress);
        }
        application.htView = htView;

    }
    @Override
    public void onResume(){
        super.onResume();

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
                        int batteryLevelIndex = inputMessage.arg1 / 20;
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

        application.vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        application.mp = MediaPlayer.create(this, R.raw.beep);

        String thresholdSetting = application.sharedPrefs.getString("pref_threshold", "0.7");
        float thresholdSettingf = Float.parseFloat(thresholdSetting);
        application.threshold = thresholdSettingf;
        htView.setThreshold(thresholdSettingf);

        application.vibrateFeedback=application.sharedPrefs.getBoolean("pref_vibrate", false);
        application.alertFeedback=application.sharedPrefs.getBoolean("pref_alert", false);

        //create bluetooth service object and register event listener
        if(application.btService==null) {
            application.btService = new BluetoothService(application.sensors); // create service instance
            application.btService.setBatteryLevelAlocator(application.batteryLevel);
            application.btService.registerBluetoothEventListener(application);
            application.btService.registerBateryLevelEventListener(application);
        }


        // create processing service


        runButton.setChecked(application.processingService.isProcessing());

        htView.post(new Runnable() {
            @Override
            public void run() {
                Log.d("HEADTILTVIEW", "ICON RADIUS" + htView.getIconRelativeRadius());
                Log.d("HEADTILTVIEW", "WIDTH" + htView.getMeasuredWidth());

                application.processingService.setIconRadius(htView.getIconRelativeRadius());


            }
        });
        application.relativeIconRadius=htView.getIconRelativeRadius();
        htView.setGoodTimePercentLable(res.getString(R.string.good_time_percent_lable));
        htView.setSessionTimeLable(res.getString(R.string.session_time_lable));
        htView.setGoodTimePercent(application.processingService.getGoodPercentage());
        htView.setSessionTime(DateUtils.formatElapsedTime(application.processingService.getSessionDuration()));
        application.setActiveActivity(0);
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

        if(id==R.id.action_posture){
            Intent intent = new Intent(this, PostureActivity.class);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        switch(requestCode){
            case REQUEST_ENABLE_BT:
                if(resultCode!=Activity.RESULT_OK){
                    Toast.makeText(this, res.getString(R.string.toast_bt_must_be_on), Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
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
        if(button.isChecked()){
            if(application.btService.isConnected())
                if(application.processingService.isStateSaved()) {
                    application.processingService.setIconRadius(htView.getIconRelativeRadius());
                    application.processingService.startProcessing(application.samplingFrequency);

                    application.postureProcessingService.startProcessing(application.samplingFrequency);
                    try {
                        application.dataLogger.startLogSession(application.samplingFrequency);
                    } catch (FileNotFoundException ex){
                        Log.d("LOGGING", "FILE NOT FOUND EXCEPTION");
                    }
                    application.setIsProcessing(true);
                } else{
                    button.setChecked(false);
                    Toast.makeText(this, res.getString(R.string.toast_save_state), Toast.LENGTH_SHORT).show();
                    }
            else{
                button.setChecked(false);
                Toast.makeText(this, res.getString(R.string.toast_must_connect_bt), Toast.LENGTH_SHORT).show();
            }
        } else{
            File logFile = application.dataLogger.stopLogSession();
            MediaScannerConnection.scanFile(this, new String[]{logFile.toString()}, null, null); // solves problem with mtp
            application.processingService.stopProcessing();
            application.postureProcessingService.stopProcessing();
            application.setIsProcessing(false);

            }
    }

}
