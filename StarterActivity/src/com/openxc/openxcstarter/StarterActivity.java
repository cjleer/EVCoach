package com.openxc.openxcstarter;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewDataInterface;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.openxc.VehicleManager;
import com.openxc.measurements.FuelLevel;
import com.openxc.measurements.Measurement;
import com.openxc.measurements.TransmissionGearPosition;
import com.openxc.measurements.UnrecognizedMeasurementTypeException;
import com.openxc.measurements.EngineSpeed;
import com.openxc.remote.VehicleServiceException;

public class StarterActivity extends Activity {
    private static final String TAG = "StarterActivity";
    private static final int G_ARRAY_LEN = 500;

    private VehicleManager mVehicleManager;
    
    // TextViews on activity
    private TextView mEngineSpeedView;
    private TextView mGearPositionView;
    private TextView mFuelLevelView;
    
    // Variables needed for RPM Graph View
    private GraphView mRPMGraphView;
    private GraphViewData[] rpmData;
    private GraphViewSeries rpmSeries;
    
    // Variables needed for Gear Graph View
    private GraphView mGearGraphView;
    private GraphViewData[] gearData;
    private GraphViewSeries gearSeries;
    
    // Various indices, make better names
    private int i = 0;
    private int j = 0;
    private int k = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starter);
        // grab a reference to the text objects in the UI, so we can
        // manipulate its value later from Java code
        mEngineSpeedView = (TextView) findViewById(R.id.vehicle_speed);
        mGearPositionView = (TextView) findViewById(R.id.gear_position);
        mFuelLevelView = (TextView) findViewById(R.id.fuel_level);
        
        // Instantiating the data arrays as length G_ARRAY_LEN constant
        rpmData = new GraphViewData[G_ARRAY_LEN];
        gearData = new GraphViewData[G_ARRAY_LEN];
        
        // Instantiating each value in array as (k,0)
        for(int k = 0; k < G_ARRAY_LEN; k++){
        	rpmData[k] = new GraphViewData(k,0);
        	gearData[k] = new GraphViewData(k,0);
        }
        
        // Setting up RPM Graph View
        mRPMGraphView = new LineGraphView(this, "Engine RPM");
        rpmSeries = new GraphViewSeries(rpmData);
        mRPMGraphView.addSeries(rpmSeries);
        mRPMGraphView.setViewPort(0, 20);
        mRPMGraphView.setScrollable(true);
        mRPMGraphView.setScalable(true);
        
        // Setting up Gear Graph View
        mGearGraphView = new LineGraphView(this, "Gear");
        gearSeries = new GraphViewSeries(gearData);
        mGearGraphView.addSeries(gearSeries);
        mGearGraphView.setViewPort(0, 20);
        mGearGraphView.setScrollable(true);
        mGearGraphView.setScalable(true);
                
        // Adding Graph Views to the layout
        LinearLayout layout1 = (LinearLayout) findViewById(R.id.graph1_layout);
        layout1.addView(mRPMGraphView);
        
        LinearLayout layout2 = (LinearLayout) findViewById(R.id.graph2_layout);
        layout2.addView(mGearGraphView);
        
    }

    @Override
    public void onPause() {
        super.onPause();
        // When the activity goes into the background or exits, we want to make
        // sure to unbind from the service to avoid leaking memory
        if(mVehicleManager != null) {
            Log.i(TAG, "Unbinding from Vehicle Manager");
            try {
                // Removing listeners
                mVehicleManager.removeListener(EngineSpeed.class, mSpeedListener);
                mVehicleManager.removeListener(FuelLevel.class, mFuelListener);
                mVehicleManager.removeListener(TransmissionGearPosition.class, mGearListener);
            } catch (VehicleServiceException e) {
                e.printStackTrace();
            }
            unbindService(mConnection);
            mVehicleManager = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // When the activity starts up or returns from the background,
        // re-connect to the VehicleManager so we can receive updates.
        if(mVehicleManager == null) {
            Intent intent = new Intent(this, VehicleManager.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /* This is an OpenXC measurement listener object - the type is recognized
     * by the VehicleManager as something that can receive measurement updates.
     * Later in the file, we'll ask the VehicleManager to call the receive()
     * function here whenever a new EngineSpeed value arrives.
     */
    EngineSpeed.Listener mSpeedListener = new EngineSpeed.Listener() {
        public void receive(Measurement measurement) {
            // When we receive a new EngineSpeed value from the car, we want to
            // update the UI to display the new value. First we cast the generic
            // Measurement back to the type we know it to be, an EngineSpeed.
            final EngineSpeed speed = (EngineSpeed) measurement;
            // In order to modify the UI, we have to make sure the code is
            // running on the "UI thread" - Google around for this, it's an
            // important concept in Android.
            StarterActivity.this.runOnUiThread(new Runnable() {
                 public void run() {
                    // Finally, we've got a new value and we're running on the
                    // UI thread - we set the text of the EngineSpeed view to
                	// the latest value
                     mEngineSpeedView.setText("Engine speed (RPM): "
                            + speed.getValue().doubleValue());
                      
                     GraphViewData newSpeed = new GraphViewData(i,speed.getValue().doubleValue());
                     ++i;
                     if(i < G_ARRAY_LEN){
                    	 //rpmSeries.appendData((GraphViewDataInterface) newSpeed, false, 200);
                    	 rpmData[i] = new GraphViewData(i,speed.getValue().doubleValue());
                     }
                     
                     // Toasting if criteria is met
                     if(speed.getValue().doubleValue() > 2000){
                    	 Toast.makeText(getApplicationContext(), "Don't go above 2000RPM to improve battery range", Toast.LENGTH_SHORT).show();
                    	 
                     }
                     
                     
                }
            });
        }
    };
    
    /* 
     * Here lie custom code
     * Delete entire block if it causes problems
     * 
     * 
     * This is an OpenXC measurement listener object - the type is recognized
     * by the VehicleManager as something that can receive measurement updates.
     * Later in the file, we'll ask the VehicleManager to call the receive()
     * function here whenever a new FuelLevel value arrives.
     */
    FuelLevel.Listener mFuelListener = new FuelLevel.Listener() {
        public void receive(Measurement measurement) {
            // When we receive a new FuelLevel value from the car, we want to
            // update the UI to display the new value. First we cast the generic
            // Measurement back to the type we know it to be, an FuelLevel.
            final FuelLevel fuel = (FuelLevel) measurement;
            // In order to modify the UI, we have to make sure the code is
            // running on the "UI thread" - Google around for this, it's an
            // important concept in Android.
            StarterActivity.this.runOnUiThread(new Runnable() {
                 public void run() {
                    // Finally, we've got a new value and we're running on the
                    // UI thread - we set the text of the EngineSpeed view to
                	// the latest value
                     mFuelLevelView.setText("Fuel Level (%?): "
                            + fuel.getValue().doubleValue());
                }
            });
        }
    };
    
    
    /* 
     * Here lie custom code
     * Delete entire block if it causes problems
     * 
     * 
     * This is an OpenXC measurement listener object - the type is recognized
     * by the VehicleManager as something that can receive measurement updates.
     * Later in the file, we'll ask the VehicleManager to call the receive()
     * function here whenever a new TransmissionGearPosition value arrives.
     */
    TransmissionGearPosition.Listener mGearListener = new TransmissionGearPosition.Listener() {
        public void receive(Measurement measurement) {
            // When we receive a new TransmissionGearPosition value from the car, we want to
            // update the UI to display the new value. First we cast the generic
            // Measurement back to the type we know it to be, an TransmissionGearPosition.
            final TransmissionGearPosition gear = (TransmissionGearPosition) measurement;
            // In order to modify the UI, we have to make sure the code is
            // running on the "UI thread" - Google around for this, it's an
            // important concept in Android.
            StarterActivity.this.runOnUiThread(new Runnable() {
                 public void run() {
                    // Finally, we've got a new value and we're running on the
                    // UI thread - we set the text of the EngineSpeed view to
                	// the latest value
                     mGearPositionView.setText("Gear Position: "
                            + gear.getValue());
                     
                     int gearInt = 0;
                     
                     // Debugging to LogCat
                     // Log.i("MyApp",gear.toString());
                     
                     // Converting String output to int
                     if(gear.getGenericName().toUpperCase().equals("REVERSE")){
                    	 gearInt = -1;
                     } else if(gear.toString().toUpperCase().equals("NEUTRAL")){
                    	 gearInt = 0;
                     } else if(gear.toString().toUpperCase().equals("FIRST")){
                    	 gearInt = 1;
                     } else if(gear.toString().toUpperCase().equals("SECOND")){
                    	 gearInt = 2;
                     } else if(gear.toString().toUpperCase().equals("THIRD")){
                    	 gearInt = 3;
                     } else if(gear.toString().toUpperCase().equals("FOURTH")){
                    	 gearInt = 4;
                     } else if(gear.toString().toUpperCase().equals("FIFTH")){
                    	 gearInt = 5;
                     } else if(gear.toString().toUpperCase().equals("SIXTH")){
                    	 gearInt = 6;
                     }
                     
                     j += 10;
                     if(j < G_ARRAY_LEN){
                    	 //rpmSeries.appendData((GraphViewDataInterface) newSpeed, false, 200);
                    	 gearData[j] = new GraphViewData(j, gearInt);
                    	 gearData[j+1] = new GraphViewData(j, gearInt);
                    	 gearData[j+2] = new GraphViewData(j, gearInt);
                    	 gearData[j+3] = new GraphViewData(j, gearInt);
                    	 gearData[j+4] = new GraphViewData(j, gearInt);
                    	 gearData[j+5] = new GraphViewData(j, gearInt);
                    	 gearData[j+6] = new GraphViewData(j, gearInt);
                    	 gearData[j+7] = new GraphViewData(j, gearInt);
                    	 gearData[j+8] = new GraphViewData(j, gearInt);
                    	 gearData[j+9] = new GraphViewData(j, gearInt);
                     }
                     

                }
            });
        }
    };
    

    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the VehicleManager service is established, i.e. bound.
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "Bound to VehicleManager");
            // When the VehicleManager starts up, we store a reference to it
            // here in "mVehicleManager" so we can call functions on it
            // elsewhere in our code.
            mVehicleManager = ((VehicleManager.VehicleBinder) service)
                    .getService();

            // We want to receive updates whenever the EngineSpeed changes. We
            // have an EngineSpeed.Listener (see above, mSpeedListener) and here
            // we request that the VehicleManager call its receive() method
            // whenever the EngineSpeed changes
            try {
                mVehicleManager.addListener(EngineSpeed.class, mSpeedListener);
            } catch (VehicleServiceException e) {
                e.printStackTrace();
            } catch (UnrecognizedMeasurementTypeException e) {
                e.printStackTrace();
            }
            
            // We want to receive updates whenever the FuelLevel changes. We
            // have an FuelLevel.Listener (see above, mFuelListener) and here
            // we request that the VehicleManager call its receive() method
            // whenever the FuelLevel changes
            try {
                mVehicleManager.addListener(FuelLevel.class, mFuelListener);
            } catch (VehicleServiceException e) {
                e.printStackTrace();
            } catch (UnrecognizedMeasurementTypeException e) {
                e.printStackTrace();
            }
            
            // We want to receive updates whenever the GearPosition changes. We
            // have an GearPosition.Listener (see above, mFuelListener) and here
            // we request that the VehicleManager call its receive() method
            // whenever the GearPosition changes
            try {
                mVehicleManager.addListener(TransmissionGearPosition.class, mGearListener);
            } catch (VehicleServiceException e) {
                e.printStackTrace();
            } catch (UnrecognizedMeasurementTypeException e) {
                e.printStackTrace();
            }
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.w(TAG, "VehicleManager Service  disconnected unexpectedly");
            mVehicleManager = null;
        }
    };
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.starter, menu);
        return true;
    }
}
