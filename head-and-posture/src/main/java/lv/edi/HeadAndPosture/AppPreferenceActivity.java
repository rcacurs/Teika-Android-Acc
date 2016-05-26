package lv.edi.HeadAndPosture;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceClickListener;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import lv.edi.BluetoothLib.*;


/**
 * Created by Richards on 17/06/2015.
 */
public class AppPreferenceActivity extends Activity {
    private static final int REQUEST_SELECT_TARGET_DEVICE = 2;
    private static HeadAndPostureApplication application;

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private Preference bluetoothTargetPreference;
        private Preference openSensorSettingsPreference;
        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            bluetoothTargetPreference = findPreference("pref_bluetooth_target");
            bluetoothTargetPreference.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            bluetoothTargetPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), DeviceListActivity.class);
                    startActivityForResult(intent, REQUEST_SELECT_TARGET_DEVICE);
                    return true;
                }
            });

            openSensorSettingsPreference = findPreference("open_sensor_preference_activity");
            openSensorSettingsPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), SensorHardwarePreferenceActivity.class);
                    startActivity(intent);
                    return true;
                }
            });

            String setting = bluetoothTargetPreference.getSharedPreferences().getString("pref_bluetooth_target", "none");
            Resources res = getResources();
            if(setting.equals("none")) {
                bluetoothTargetPreference.setSummary(res.getString(R.string.pref_summary_bluetooth_target)+": none");
            } else{
                BluetoothDevice btDevice = application.btAdapter.getRemoteDevice(setting);
                bluetoothTargetPreference.setSummary(res.getString(R.string.pref_summary_bluetooth_target)+": "+btDevice.getName());
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data){
            switch(requestCode){
                case REQUEST_SELECT_TARGET_DEVICE: // if
                    // When DeviceListActivity returns with a device to connect
                    if (resultCode == Activity.RESULT_OK) {
                        // Get the device MAC address
                        String address = data.getExtras()
                                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                        // Get the BLuetoothDevice object
                        application.btDevice = application.btAdapter.getRemoteDevice(address); // instantate bluetooth target device
                        SharedPreferences.Editor editor = bluetoothTargetPreference.getSharedPreferences().edit();
                        editor.putString("pref_bluetooth_target", application.btDevice.getAddress());
                        editor.commit();
                        Log.d("ON_ACTIVITY_RESULT", bluetoothTargetPreference.getSharedPreferences().getString("pref_bluetooth_target", "none"));
                    }
                    break;
            }
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key){
            if(key.equals("pref_bluetooth_target")) {
                Preference pref = findPreference(key);
                String targetDeviceAddress = sharedPreferences.getString("pref_bluetooth_target", "none");
                Resources res = getResources();

                if (targetDeviceAddress.equals("none")) {
                    pref.setSummary(res.getString(R.string.pref_summary_bluetooth_target) + ": none");
                } else {
                    BluetoothDevice btDevice = application.btAdapter.getRemoteDevice(targetDeviceAddress);
                    pref.setSummary(res.getString(R.string.pref_summary_bluetooth_target) + ": " + btDevice.getName());
                }

            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        application = (HeadAndPostureApplication)getApplication();
    }
}
