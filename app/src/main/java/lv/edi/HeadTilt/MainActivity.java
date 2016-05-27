package lv.edi.HeadTilt;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import lv.edi.BluetoothLib.*;


public class MainActivity extends AppCompatActivity implements ProcessingEventListener {
    final int REQUEST_ENABLE_BT = 1;
    private HeadTiltApplication application;
    private Menu optionsMenu;
    private HeadTiltView htView;
    private ToggleButton runButton;
    private Resources res;
    double r=0.5;
    double phi=0;
    boolean inactivityTrigger = true;
    private int[] batteryIcons = {R.drawable.battery_discharging_000,
                                  R.drawable.battery_discharging_040,
                                  R.drawable.battery_discharging_060,
                                  R.drawable.battery_discharging_080,
                                  R.drawable.battery_discharging_100};
    ImageView inactivityView;
    TextView inactivityTimeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        res = getResources();
        setContentView(R.layout.content_main);
        inactivityView = (ImageView)findViewById(R.id.icon1);
        inactivityTimeView = (TextView)findViewById(R.id.secondLine1);
        application = (HeadTiltApplication)getApplication();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#88D3DF")));
        actionBar.setDisplayShowTitleEnabled(false);
        Drawable icon = res.getDrawable(R.mipmap.header, null);
        actionBar.setLogo(icon);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

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
                        application.processingService.startProcessing();
                        application.lastActivityTime=System.currentTimeMillis();
                        inactivityView.setBackgroundColor(Color.parseColor("#33E280"));
                        break;
                    case BluetoothService.BT_DISCONNECTED:
                        application.processingService.stopProcessing();
                        Toast.makeText(getApplicationContext(), res.getString(R.string.toast_disconnected_bt), Toast.LENGTH_SHORT).show();
                        optionsMenu.findItem(R.id.action_bluetooth_connection_status).setIcon(R.drawable.not);
                        break;

                }
            }
        };

        application.vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        application.mp = MediaPlayer.create(this, R.raw.beep);


        //create bluetooth service object and register event listener
        if(application.btService==null) {
            application.btService = new BluetoothService(application.sensors); // create service instance
            application.btService.setBatteryLevelAlocator(application.batteryLevel);
            application.btService.registerBluetoothEventListener(application);
        }


        // create processing service


        if(application.processingService == null) {
            application.processingService = new HeadTiltProcessingService(application.sensors.get(application.HEAD_SENSOR_INDEX), 100, application.threshold);
            application.processingService.setProcessingEventListener(this);
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
            Intent intent = new Intent(this, HeadTiltPreferenceActivity.class);
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
            application.processingService.setReference(application.sensors.get(0).getAccRawNorm());
            Toast.makeText(this, res.getString(R.string.toast_saved), Toast.LENGTH_SHORT).show();
        } else{
            Toast.makeText(this, res.getString(R.string.toast_must_connect_bt), Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickStart(View view){
        ToggleButton button = (ToggleButton)view;
        if(button.isChecked()){
            if(application.processingService.isStateSaved()) {
                application.processingService.setIconRadius(htView.getIconRelativeRadius());
                application.processingService.startProcessing();
            } else{
                button.setChecked(false);
                Toast.makeText(this, res.getString(R.string.toast_save_state), Toast.LENGTH_SHORT).show();
            }
        } else{
            application.processingService.stopProcessing();
        }
    }

    public void onProcessingResult(ProcessingResult result){
        float movement = result.getRelativeX();
        if(movement>application.movementTriggerThreshold){
            Log.d("MOVEMENT_OVER_THRESHOLD", "TRUE");
            application.lastActivityTime = System.currentTimeMillis();
            inactivityTrigger=true;
            this.runOnUiThread(new Runnable() {
                                   public void run() {
                                       //Toast.makeText(getApplicationContext(), "Please check patient", Toast.LENGTH_SHORT).show();
                                       inactivityView.setBackgroundColor(Color.parseColor("#33E280"));
                                   }
                               }
            );
        }
        // check current time
        long currentTime = System.currentTimeMillis();
        final long deltaTime = currentTime - application.lastActivityTime;
        final float inactivitySeconds = Math.max((application.passivnesTimeThreshold-(float)deltaTime)/1000,0);
        runOnUiThread(new Runnable() {
            public void run() {
                if(inactivitySeconds >0.0) {
                    inactivityTimeView.setText(String.format("in %.1f seconds", inactivitySeconds));
                } else{
                    inactivityTimeView.setText("please attend this patient");
                }
            }
        });

        if(deltaTime>application.passivnesTimeThreshold*0.8 && deltaTime<=application.passivnesTimeThreshold){
            this.runOnUiThread(new Runnable() {
                                   public void run() {
                                       inactivityView.setBackgroundColor(Color.parseColor("#FAC012"));

                                   }
                               }
            );
        }
        if(deltaTime>application.passivnesTimeThreshold){
            Log.d("NOT_ACTIVE", "TRUE");
            //app.lastActivityTime=currentTime;
            this.runOnUiThread(new Runnable() {
                                   public void run() {

                                       inactivityView.setBackgroundColor(Color.parseColor("#E93E45"));
                                       if(inactivityTrigger) {
                                           application.vibrator.vibrate(500);
                                           inactivityTrigger=false;
                                           pushPebbleNotification();
                                           Toast.makeText(getApplicationContext(), "Please check patient", Toast.LENGTH_SHORT).show();
                                       }

                                   }
                               }
            );
        }

    }

    public void pushPebbleNotification() {
        // Push a notification
        final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");

        final Map data = new HashMap();
        data.put("title", "Attention!");
        data.put("body", "Patient number X needs to be repositioned.");
        final JSONObject jsonData = new JSONObject(data);
        final String notificationData = new JSONArray().put(jsonData).toString();

        i.putExtra("messageType", "PEBBLE_ALERT");
        i.putExtra("sender", "PebbleKit Android");
        i.putExtra("notificationData", notificationData);
        sendBroadcast(i);
    }

}
