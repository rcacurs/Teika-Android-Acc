package lv.edi.SmartWear3DDisplay;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import lv.edi.SmartWearProcessing.Sensor;


public class BluetoothService implements DiscoveryListener{

	
	private Object lock=new Object();
	private Vector<RemoteDevice> vecDevices = new Vector<RemoteDevice>();
	private Vector<Sensor> sensorBuffer;
	private String connectionURL=null;
	private LocalDevice localDevice;
	private static int remoteDeviceCounter=0; // counter for found devices
	private static StreamConnection streamConnection;
	private static InputStream inStream;
	private DiscoveryAgent agent;
	private RemoteDevice remoteDevice;
	private Thread dataReceiveThread;
	private boolean runReceiveThread=false;
	private int bytes_in_packet=13;
	private Vector<BluetoothEventListener> btEventListeners = new Vector<BluetoothEventListener>();

	public BluetoothService(Vector<Sensor> sensorBuffer){ // Bluetooth service constructor
		this.sensorBuffer = sensorBuffer;
	}
	
	public void initBluetoothService() throws BluetoothStateException{
		System.out.println("");
		
		localDevice = LocalDevice.getLocalDevice();
		System.out.println("Local Device Address: "+localDevice.getBluetoothAddress());
		System.out.println("Local Device Name: "+localDevice.getFriendlyName());
		
		agent = localDevice.getDiscoveryAgent();
		System.out.println("\nPaired bluetooth devices: ");
		RemoteDevice[] devices = agent.retrieveDevices(DiscoveryAgent.PREKNOWN);
		vecDevices.clear();
		for(int i=0; i<devices.length; i++){
			try {
				System.out.println("Found device: index "+i+" Name - "+devices[i].getFriendlyName(false)+" ");

				vecDevices.add(devices[i]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

    /**
     * Register bt event listener
     * @param listener
     */
    public void registerSensorDataPacketReceivedListener(BluetoothEventListener listener){
        btEventListeners.add(listener);
        System.out.println("Register listener: "+listener);
    }
	
	public void startReceiveData(int selectedDevice) throws IOException{
		
		remoteDevice = vecDevices.elementAt(selectedDevice);
		
		UUID[] uuidSet = new UUID[1];
		//uuidSet[0]=new UUID("27012f0c68af4fbf8dbe6bbaf7aa432a",false);
		uuidSet[0]=new UUID(0x1101);
		
		
		agent.searchServices(null,uuidSet,remoteDevice,this);
		
		
		try {
			synchronized(lock){
			lock.wait();
			}
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(connectionURL==null){
			System.out.println("SPP Service not found. Check if remote device is turned on.");
			throw new BluetoothStateException();
		}
		
		//=====Connection established=====
		
		
		streamConnection=(StreamConnection)Connector.open(connectionURL);
		inStream=streamConnection.openInputStream();
		
		runReceiveThread=true;
		dataReceiveThread=new Thread(new BluetoothDataReceiveTask());

		dataReceiveThread.start();

	}
	public void stopReceiveData(){
		try {
			inStream.close();
			streamConnection.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			runReceiveThread=false;
			dataReceiveThread=null;
		}
		runReceiveThread=false;
		dataReceiveThread=null;

	}
	
	@Override
	public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass arg1) {
		// TODO Auto-generated method stub
		
		try {
			System.out.println("Name: "+remoteDevice.getFriendlyName(true)+" Index: "+remoteDeviceCounter);
			
			vecDevices.addElement(remoteDevice);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		remoteDeviceCounter++;
		
	}

	@Override
	public void inquiryCompleted(int arg0) {
		
		
	}

	@Override
	public void serviceSearchCompleted(int arg0, int arg1) {
		synchronized(lock){
			lock.notify();
		}
		
	}

	@Override
	public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
		// TODO Auto-generated method stub
		if(servRecord!=null && servRecord.length>0){
			connectionURL=servRecord[0].getConnectionURL(0,false);
		}
		
	}
	
	private class BluetoothDataReceiveTask implements Runnable{
		private final int PACKET_SEPERATOR = 0xFF;
		private final int PACKET_ESCAPE = 0xFE;
		private int b;
		private boolean packet_indicator = false;
		private boolean escape_indicator = false;
		private int bytes_received = 0;
		private short packet[] = new short[bytes_in_packet];
		@Override
		public void run() {

			while(runReceiveThread){
				try {
					b=inStream.read();
					//System.out.println("Received byte"+b);
					if(packet_indicator==true){ // if we received start symbol of packet
						if(escape_indicator==true){ // if previous byte was escape symbol
							switch(b){
							case  0x00: // in case we have symbol that matched with packet seperator
								bytes_received++; // updating bytes received  counter
								packet[bytes_received-1]=PACKET_SEPERATOR; //adding received data byte to packet parray
								break;
							case  0x01:
								bytes_received++;
								packet[bytes_received-1]=PACKET_ESCAPE;// in case we have symbol that matched escape symbol 
								break;
							default:
								break;
							}
							escape_indicator=false; // unsetting the escape indicator
						} else{ // in case we don't expect escape character
							switch(b){ // checking what the received symbol is
							case  PACKET_SEPERATOR: //in case of packet seperator
								if(bytes_received>0){ //if we have at least one byte received
									if(bytes_received>=bytes_in_packet){ // if we received 7 bytes, then start to from packet 
										bytes_received = 0; // resetting received byte counter
//										if(packet[0]<sensorbuffer.size()){ // if received data packet
                                        short accx = (short)(packet[1]*256+packet[2]);
                                        short accy = (short)(packet[3]*256+packet[4]);
                                        short accz = (short)(packet[5]*256+packet[6]);
                                        short magx = (short)(packet[7]*256+packet[8]);
                                        short magy = (short)(packet[9]*256+packet[10]);
                                        short magz = (short)(packet[11]*256+packet[12]);
                                        double accMagnitude = Math.sqrt(Math.pow(accx, 2)+Math.pow(accy, 2)+Math.pow(accz, 2));
                                        double magMagnitude = Math.sqrt(Math.pow(magx, 2)+Math.pow(magy, 2)+Math.pow(magz, 2));
                                        //if(((accMagnitude<24000)&&(accMagnitude>11000)&&(magMagnitude>0)&&(magMagnitude<2000))){
                                        if(packet[0]<63){
                                            sensorBuffer.get(packet[0]).updateSensorData(accx, // forming accelerometer x data from two received data bytes
                                                    accy, // forming accelerometer y data from two received data bytes
                                                    accz, // forming accelerometer z data from two received data bytes
                                                    magx, // forming magnetometer  x data from two received data bytes
                                                    magy,// forming magnetometer  y data from two received data bytes
                                                    magz);// forming magnetometer z data from two recieved data bytes
											for(BluetoothEventListener i:btEventListeners){
												Vector<Float> vec = new Vector<Float>(6);
												vec.add(new Float(accx));
												vec.add(new Float(accy));
												vec.add(new Float(accz));
												vec.add(new Float(magx));
												vec.add(new Float(magy));
												vec.add(new Float(magz));
												i.onSensorDataPacketReceived(packet[0], vec);
											}

                                        }
                                    }
									packet_indicator=false; // reset packet indicator, bacause we have received all packet data
									bytes_received=0;// reset received byte counter
								}
								break;
							case PACKET_ESCAPE: // if received byte packet escape
								escape_indicator=true; // set escape indicator
								break;
							default: // in case of all other values 
								bytes_received++; // increase byte counter
								packet[bytes_received-1]=(short)b; // add read byte to packet array
								break;	
							}
						}							
					} else if(b==PACKET_SEPERATOR){ // condition for new packet start
						packet_indicator=true;
					}		
				} catch (IOException e) {
					System.out.println("Error. Bluetooth Connection problem");
					break;
				}
			}

		} 
		
	}

}
