package lv.edi.SmartWear3DDisplay;

import java.util.Vector;

/**
 * Created by Richards on 02.12.2015..
 */
public interface BluetoothEventListener {
    /**
     * Returns one packet of sensor data when received
     * @param inedx
     * @param sensorData
     */
    public void  onSensorDataPacketReceived(int inedx, Vector<Float> sensorData);
}
