package lv.edi.YesNo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import lv.edi.BluetoothLib.BluetoothService;


public class MainActivity extends Activity {
    private Resources res;
    private YesNoView ynView;
    private YesNoApplication application;
    private Menu optionsMenu;
    private final int REQUEST_ENABLE_BT=1;
    private ToggleButton runButton;
    private int[] batteryIcons = {R.drawable.battery_discharging_000,
            R.drawable.battery_discharging_040,
            R.drawable.battery_discharging_060,
            R.drawable.battery_discharging_080,
            R.drawable.battery_discharging_100};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        res = getResources();

        application = (YesNoApplication)getApplication();
        ynView = (YesNoView) findViewById(R.id.yesNoView);
        application.processingService.setYesNoView(ynView);
        runButton = (ToggleButton) findViewById(R.id.toggle_button_start);

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
        String radiusS = application.sharedPrefs.getString("pref_radius","0.7");
        String timeS = application.sharedPrefs.getString("pref_accept_time", "2.0");
        application.processingService.setRadius(Float.parseFloat(radiusS));
        application.processingService.setAcceptTimeThreshold((long)(Float.parseFloat(timeS)*1000));

        if(btAddress.equals("none")){
            application.btDevice = null;
        } else{
            application.btDevice = application.btAdapter.getRemoteDevice(btAddress);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        optionsMenu=menu;

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
    public void onResume(){
        super.onResume();
        if(application.btService==null) {
            application.btService = new BluetoothService(application.sensors, 63); // create service instance
            application.btService.registerBluetoothEventListener(application);
            application.btService.registerBateryLevelEventListener(application);

        }

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
                        application.processingService.stop();
                        runButton.setChecked(false);
                        break;
                    case YesNoApplication.BATTERY_LEVEL_UPDATE:
                        int batteryLevelIndex = inputMessage.arg1 /20;
                        if(batteryLevelIndex < 0){
                            batteryLevelIndex = 0;
                        }
                        if(batteryLevelIndex>=batteryIcons.length){
                            batteryLevelIndex = batteryIcons.length - 1;
                        }
                        optionsMenu.findItem(R.id.action_battery_level_icon).setIcon(batteryIcons[batteryLevelIndex]);
                        Log.d("BATTERY_LEVEL_UPD", "UI UPDATE BATTERY LVL INDEX "+batteryLevelIndex);
                    default:
                        break;
                }
            }
        };
        if(application.processingService.isProcessing()){
            runButton.setChecked(true);
        } else{
            runButton.setChecked(false);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, YesNoPreferenceActivity.class);
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

    public void onClickStart(View view){
        ToggleButton button = (ToggleButton)view;
        if(button.isChecked()){
            if(application.btService.isConnected()){
                application.processingService.start(20);
            } else{
                Toast.makeText(this, res.getString(R.string.toast_must_connect_bt), Toast.LENGTH_LONG).show();
                button.setChecked(false);
            }
        } else{
            //application.processingService.stopProcessing()
            application.processingService.stop();
        }
    }
}
