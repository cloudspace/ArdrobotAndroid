package com.cloudspace.rosserial;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.ros.node.ConnectedNode;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import rosserial_msgs.TopicInfo;

public class ROSSerialADK {

	static final String TAG= "ROSSerialADK";
	private static final String ACTION_USB_PERMISSION = "org.ros.rosserial.action.USB_PERMISSION";

	
	private ROSSerial rosserial;
	Thread ioThread;
	
	
	private Context mContext;
	private PendingIntent mPermissionIntent;
	boolean mPermissionRequestPending =false;

	private UsbManager mUsbManager;
	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;
	FileDescriptor fd;

	private ConnectedNode node;
	
	
	public interface onConnectionListener{
		public void trigger(boolean connection);
	}
	
	private onConnectionListener connectionCB;
	public void setOnConnectonListener(onConnectionListener onConnectionListener){
		this.connectionCB = onConnectionListener;
	}
	
	

	public ROSSerialADK( Context context, ConnectedNode node){
		
		this.node = node;
		this.mContext = context;
		
		mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
		context.registerReceiver(mUsbReceiver, filter);
		
	}
	
	
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
                    UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "permission denied for accessory "
								+ accessory);
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};
			
	
	
	
	private static UsbAccessory getAccessory( UsbManager man){
		UsbAccessory[] accessories = man.getAccessoryList();		
		if(accessories == null) return null;
		
		for (UsbAccessory accessory : accessories){
			if (accessory !=null){
				if  (accessory.getModel().equals("ROSSerialADK") 
						&& accessory.getManufacturer().equals("Willow Garage")){
					return accessory;
				}
			}
		}
		
		return null;

	}
	
	//Check to see if an ROSSerialADK board is attached
	public static  boolean  isAttached(UsbManager man){
		if ( getAccessory(man) == null) return false;
		else return true;
	}
	
	private boolean openAccessory(UsbAccessory accessory) {
		Log.d(TAG, "Opening Accessory!");
		mFileDescriptor = mUsbManager.openAccessory(accessory);

		if (mFileDescriptor != null) {
			
			mAccessory = accessory;
			fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			
			rosserial =  new ROSSerial(node, mInputStream, mOutputStream);
			ioThread = new Thread(null, rosserial, "ROSSerialADK");
			ioThread.setContextClassLoader(ROSSerialADK.class.getClassLoader());
			ioThread.start();
			
			if (connectionCB != null) connectionCB.trigger(true);
			Log.v(TAG,"accessory opened");
			return true;

		} else {
			Log.v(TAG,"accessory open fail");
			return false;
		}
	}
	
	//Try to open the device by 
	public boolean open(UsbAccessory accessory) {
		
		if (accessory == null) {
			Log.v(TAG,"There is no ADK");
			return false;
		}
		
		if (mUsbManager.hasPermission(accessory)) {
			return openAccessory(accessory);
		} else {
			synchronized (mUsbReceiver) {
				if (!mPermissionRequestPending) {
					mUsbManager.requestPermission(accessory,
							mPermissionIntent);
					mPermissionRequestPending = true;
				}
			}
		}
			
		return true;
	}

	private void closeAccessory() {

		try {
			if (mFileDescriptor != null) {
				if(rosserial!=null) {
					rosserial.shutdown();
					rosserial = null;
				}
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
			if (connectionCB != null) connectionCB.trigger(false);

		}
	}
	
	public void shutdown(){
		closeAccessory();
		mContext.unregisterReceiver(mUsbReceiver);
	}
	
	public boolean isConnected(){
		return (mOutputStream != null);
	}
	
	
	public TopicInfo[] getSubscriptions(){
		return rosserial.getSubscriptions();
	}
    public TopicInfo[] getPublications(){
		return rosserial.getPublications();
	}
	
	//Set Callback function for new subscription
	void setOnSubscriptionCB(TopicRegistrationListener listener){
		if (rosserial!= null) rosserial.setOnNewSubcription(listener);
	}
	
	//Set Callback for new publication
	void setOnPublicationCB(TopicRegistrationListener listener){
		if (rosserial!= null) rosserial.setOnNewPublication(listener);
	}
	 
}