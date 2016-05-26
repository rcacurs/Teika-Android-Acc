package lv.edi.YesNo;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Vector;

import lv.edi.BluetoothLib.BatteryLevel;
import lv.edi.BluetoothLib.BatteryLevelEventListener;
import lv.edi.BluetoothLib.BluetoothEventListener;
import lv.edi.BluetoothLib.BluetoothService;
import lv.edi.SmartWearProcessing.Sensor;

/**
 * Created by Richards on 18.11.2015..
 */
public class YesNoApplication extends Application implements SharedPreferences.OnSharedPreferenceChangeListener, BluetoothEventListener, BatteryLevelEventListener {
    final int NUMBER_OF_SENSORS = 1;
    static final int BATTERY_LEVEL_UPDATE=45;
    static final int BATTERY_PACKET_INDEX=1;
    private static Context context;
    BluetoothService btService;
    BluetoothAdapter btAdapter;
    BluetoothDevice btDevice;
    Handler uiHandler;
    SharedPreferences sharedPrefs;
    BatteryLevel batteryLevel;
    boolean vibrateFeedback=false;
    boolean alertFeedback=false;
    Vector<Sensor> sensors;
    float radiusSetting;

    YesNoProcessingService processingService;

    public void onCreate(){
        super.onCreate();
        context = getApplicationContext();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);

        sensors = new Vector<Sensor>(NUMBER_OF_SENSORS, BATTERY_PACKET_INDEX);
        sensors.setSize(NUMBER_OF_SENSORS);
        for(int i=0; i<NUMBER_OF_SENSORS; i++){
            sensors.set(i,new Sensor(i, true));
        }
        processingService = new YesNoProcessingService(sensors.get(0));

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals("pref_bluetooth_target")){
            String btDeviceAddress = sharedPreferences.getString("pref_bluetooth_target", "none");
            if(btDeviceAddress.equals("none")){
                btDevice = null;
            } else{
                btDevice = btAdapter.getRemoteDevice(btDeviceAddress);
            }
        }

        if(key.equals("pref_radius")){
            String thresholdSetting = sharedPreferences.getString("pref_radius", "0.7");
            float radiusSettingf = Float.parseFloat(thresholdSetting);
            processingService.setRadius(radiusSettingf);
            Log.d("PREFERENCES", "radius set");
        }

       if(key.equals("pref_accept_time")){
           String acceptTimeS = sharedPreferences.getString("pref_accept_time", "2.0");
           float acceptTime = Float.parseFloat(acceptTimeS);
           processingService.setAcceptTimeThreshold((long) (acceptTime * 1000));
           Log.d("PREFERENCES", "acceptTimeset: "+acceptTime+" [s]");
       }
    }

    public static Context getAppContext(){
        return context;
    }
    // battery level listeners
    public void onBatteryLevelChange(BatteryLevel bLevel){
        int lvl = (int) bLevel.getBatteryPercentage();
        uiHandler.obtainMessage(BATTERY_LEVEL_UPDATE, lvl, 0).sendToTarget();
        Log.d("BATTERY_LEVEL_UPD", "BATTERY_LEVEL_CHANGE_APPCALLBACK "+lvl);
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

}
