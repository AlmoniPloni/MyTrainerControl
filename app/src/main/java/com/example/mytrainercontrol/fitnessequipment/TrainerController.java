package com.example.mytrainercontrol.fitnessequipment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.dsi.ant.plugins.antplus.common.FitFileCommon;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestStatus;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusCommonPcc;
import com.dsi.ant.plugins.antplus.pccbase.MultiDeviceSearch;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;
import com.dsi.ant.plugins.antplus.pcc.AntPlusFitnessEquipmentPcc.Settings;
import com.example.mytrainercontrol.Fragment_ManualPowerControl;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.EnumSet;

public class TrainerController {

    private static final String TAG = "PowerControl TC";


    Context mContext;
    Activity mActivity;
    Fragment_ManualPowerControl mFragment;
    AntPlusFitnessEquipmentPcc fePcc = null;
    PccReleaseHandle<AntPlusFitnessEquipmentPcc> releaseHandle = null;
    boolean subscriptionsDone = false;
    FitFileCommon.FitFile[] files;
    Settings settings;
    BigDecimal speedConvert;

    private boolean autoReconnectEnabled = true;
    private int reconnectAttempt = 0;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());


    public TrainerController(Activity activity, Context context, Fragment_ManualPowerControl fragment){
        mContext = context;
        mActivity = activity;
        mFragment = fragment;
        speedConvert = new BigDecimal(3.601);
    }

    public PccReleaseHandle<AntPlusFitnessEquipmentPcc> getReleaseHandle() {
        return releaseHandle;
    }

    public AntPlusFitnessEquipmentPcc getFePcc() {
        return fePcc;
    }


    final AntPlusCommonPcc.IRequestFinishedReceiver requestFinishedReceiver = new AntPlusCommonPcc.IRequestFinishedReceiver()
    {
        @Override
        public void onNewRequestFinished(final RequestStatus requestStatus)
        {
            mActivity.runOnUiThread(
                    new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            switch(requestStatus)
                            {
                                case SUCCESS:
                                    Log.d(TAG, "Request Successfully Sent");
                                    //Toast.makeText(mContext, "Request Successfully Sent", Toast.LENGTH_SHORT).show();
                                    break;
                                case FAIL_PLUGINS_SERVICE_VERSION:
                                    Log.d(TAG, "Plugin Service Upgrade Required?");
                                    Toast.makeText(mContext, "Plugin Service Upgrade Required?", Toast.LENGTH_SHORT).show();
                                    break;
                                default:
                                    Log.d(TAG, "Request Failed to be Sent");
                                    //Toast.makeText(mContext, "Request Failed to be Sent", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }
                    });
        }
    };


    public boolean setTargetPower(java.math.BigDecimal targetPower) {
        //TODO The capabilities should be requested before attempting to send new control settings to determine which modes are supported.
        boolean submitted = false;
        if (fePcc != null) {
            submitted = fePcc.getTrainerMethods().requestSetTargetPower(targetPower, requestFinishedReceiver);
            if (!submitted) {
                Log.d(TAG, "Too Fast, Request Could not be Made");
                //showToast("Too Fast, Request Could not be Made");
                //Toast.makeText(mContext, "Too Fast, Request Could not be Made", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "No Trainer found, Target Power can not be set");
            showToast("No Trainer found, Target Power can not be set");
            //Toast.makeText(mContext, "No Trainer found, Target Power can not be set", Toast.LENGTH_SHORT).show();
        }
        return submitted;
    }

    /**
     * Resets the PCC connection to request access again and clears any existing display data.
     */
    public void resetPcc(boolean include_workout)
    {
        //Release the old access if it exists
        if(releaseHandle != null)
            releaseHandle.close();

        //Reset event subscriptions
        subscriptionsDone = false;
        
        final AntPluginPcc.IPluginAccessResultReceiver<AntPlusFitnessEquipmentPcc> mPluginAccessResultReceiver =
                new AntPluginPcc.IPluginAccessResultReceiver<AntPlusFitnessEquipmentPcc>()
                {
                    //Handle the result, connecting to events on success or reporting failure to user.
                    @Override
                    public void onResultReceived(AntPlusFitnessEquipmentPcc result,
                                                 RequestAccessResult resultCode, DeviceState initialDeviceState)
                    {
                        switch(resultCode)
                        {
                            case SUCCESS:
                                fePcc = result;
                                Toast.makeText(mContext, "Access Successful to Device ID: " + String.valueOf(fePcc.getAntDeviceNumber()), Toast.LENGTH_SHORT).show();

                                clearReconnectState();  // ⬅️ reset retry state on success

                                //if (initialDeviceState == DeviceState.TRACKING) {
                                mFragment.setActualPowerColor(Color.WHITE);
                                mFragment.setActualPowerTitleColor(Color.GREEN);
                                mFragment.setSpeedTitleColor(Color.GREEN);
                                mFragment.setSpeedTextColor(Color.WHITE);
                                mFragment.setTotalDistanceColor(Color.WHITE);
                                mFragment.setTotalDistanceTitleColor(Color.GREEN);
                                mFragment.setWorkoutTimerColor(Color.WHITE);
                                mFragment.setWorkoutTimerTitleColor(Color.GREEN);
                                mFragment.setCadenceColor(Color.WHITE);
                                mFragment.setCadenceTitleColor(Color.GREEN);
                                //}
                                /*else {
                                    mFragment.setActualPowerColor(Color.GRAY);
                                    mFragment.setActualPowerTitleColor(Color.RED);
                                    mFragment.setSpeedTextColor(Color.GRAY);
                                    mFragment.setSpeedTitleColor(Color.RED);
                                    mFragment.setSpeedTextColor(Color.GRAY);
                                    mFragment.setTotalDistanceColor(Color.GRAY);
                                    mFragment.setWorkoutTimerColor(Color.GRAY);
                                    mFragment.setWorkoutTimerTitleColor(Color.RED);
                                    mFragment.setTotalDistanceTitleColor(Color.RED);
                                    mFragment.setCadenceColor(Color.GRAY);
                                    mFragment.setCadenceTitleColor(Color.RED);
                                }*/
                                //if(initialDeviceState == DeviceState.CLOSED)
                                    //Toast.makeText(mContext, fePcc.getDeviceName() + ": " + "Waiting for FE Session Request", Toast.LENGTH_SHORT).show();
                                //else
                                    //Toast.makeText(mContext, result.getDeviceName() + ": " + initialDeviceState, Toast.LENGTH_SHORT).show();



                                subscribeToEvents();
                                break;
                            case CHANNEL_NOT_AVAILABLE:
                                Toast.makeText(mContext, "Channel Not Available", Toast.LENGTH_SHORT).show();
                                scheduleAutoReconnect(false);   // ⬅️ retry without resetting workout
                                break;
                            case ADAPTER_NOT_DETECTED:
                                Toast.makeText(mContext, "ANT Adapter Not Available. Built-in ANT hardware or external adapter required.", Toast.LENGTH_SHORT).show();
                                // usually no auto-reconnect here; user must fix hardware
                                break;
                            case BAD_PARAMS:
                                //Note: Since we compose all the params ourself, we should never see this result
                                Toast.makeText(mContext, "Bad request parameters.", Toast.LENGTH_SHORT).show();
                                break;
                            case OTHER_FAILURE:
                                Toast.makeText(mContext, "RequestAccess failed. See logcat for details.", Toast.LENGTH_SHORT).show();
                                scheduleAutoReconnect(false);   // ⬅️ try again
                                break;
                            case DEPENDENCY_NOT_INSTALLED:
                                Toast.makeText(mContext, "Dependency not installed", Toast.LENGTH_SHORT).show();
                                AlertDialog.Builder adlgBldr = new AlertDialog.Builder(mActivity);
                                adlgBldr.setTitle("Missing Dependency");
                                adlgBldr.setMessage("The required service\n\"" + AntPlusFitnessEquipmentPcc.getMissingDependencyName() + "\"\n was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?");
                                adlgBldr.setCancelable(true);
                                adlgBldr.setPositiveButton("Go to Store", new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        Intent startStore = null;
                                        startStore = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=" + AntPlusFitnessEquipmentPcc.getMissingDependencyPackageName()));
                                        startStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                        mActivity.startActivity(startStore);
                                    }
                                });
                                adlgBldr.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        dialog.dismiss();
                                    }
                                });

                                final AlertDialog waitDialog = adlgBldr.create();
                                waitDialog.show();
                                break;
                            case USER_CANCELLED:
                                Toast.makeText(mContext, "Cancelled", Toast.LENGTH_SHORT).show();

                                scheduleAutoReconnect(false);   // ⬅️ try again, maybe cancelled by mistake ?

                                break;
                            case UNRECOGNIZED:
                                Toast.makeText(mContext, "Failed: UNRECOGNIZED. PluginLib Upgrade Required?", Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                Toast.makeText(mContext, "Unrecognized result: " + resultCode, Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }

                    /**
                     * Subscribe to all the data events, connecting them to display their data.
                     */
                    private void subscribeToEvents()
                    {
                        fePcc.subscribeGeneralFitnessEquipmentDataEvent(new AntPlusFitnessEquipmentPcc.IGeneralFitnessEquipmentDataReceiver()
                        {
                            @Override
                            public void onNewGeneralFitnessEquipmentData(final long estTimestamp,
                                                                         EnumSet<EventFlag> eventFlags, final BigDecimal elapsedTime,
                                                                         final long cumulativeDistance, final BigDecimal instantaneousSpeed,
                                                                         final boolean virtualInstantaneousSpeed, final int instantaneousHeartRate,
                                                                         final AntPlusFitnessEquipmentPcc.HeartRateDataSource heartRateDataSource)
                            {
                                mActivity.runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        //Toast.makeText(mContext, "Timestamp: " + String.valueOf(estTimestamp), Toast.LENGTH_SHORT).show();

                                        /*
                                        if(elapsedTime.intValue() == -1)
                                            Toast.makeText(mContext, "Invalid elapsed time = -1", Toast.LENGTH_SHORT).show();
                                        else
                                            Toast.makeText(mContext, "Elapsed time = " + String.valueOf(elapsedTime) + "s", Toast.LENGTH_SHORT).show();
                                        */

                                        /*if(cumulativeDistance == -1) {
                                            Log.d(TAG, "Invalid cumulative distance time = -1");
                                            Toast.makeText(mContext, "Invalid cumulative distance time = -1", Toast.LENGTH_SHORT).show();
                                        }
                                        else {
                                            mFragment.setTotalDistance(String.valueOf(cumulativeDistance));
                                            //Toast.makeText(mContext, "Cumulative distance = " + String.valueOf(cumulativeDistance) + "m", Toast.LENGTH_SHORT).show();
                                        }*/

                                        mFragment.setWorkoutTimer(elapsedTime.floatValue());
                                        if(instantaneousSpeed.intValue() == -1) {
                                            Log.d(TAG, "Invalid instantaneous speed = -1");
                                            Toast.makeText(mContext, "Invalid instantaneous speed = -1", Toast.LENGTH_SHORT).show();
                                        }
                                        else {
                                            mFragment.setSpeed(instantaneousSpeed.multiply(speedConvert).toPlainString());
                                            mFragment.setDistance(elapsedTime.floatValue(), instantaneousSpeed.floatValue());
                                            //Toast.makeText(mContext, "Instantaneous speed = " + String.valueOf(instantaneousSpeed) + "m/s", Toast.LENGTH_SHORT).show();
                                        }
                                        /*
                                        if(virtualInstantaneousSpeed)
                                            Toast.makeText(mContext, "(Virtual) Instantaneous speed = " + String.valueOf(instantaneousSpeed) + "m/s", Toast.LENGTH_SHORT).show();
                                        */

                                        /*
                                        if(instantaneousHeartRate == -1)
                                            Toast.makeText(mContext, "Invalid Heart Rate = -1", Toast.LENGTH_SHORT).show();
                                        else
                                            Toast.makeText(mContext, "Heart rate = " + String.valueOf(instantaneousHeartRate) + "bpm", Toast.LENGTH_SHORT).show();
                                        */

                                        /*switch(heartRateDataSource)
                                        {
                                            case ANTPLUS_HRM:
                                            case EM_5KHz:
                                            case HAND_CONTACT_SENSOR:
                                            case UNKNOWN:
                                                //Toast.makeText(mContext, "Heart rate Unknown = " + heartRateDataSource.toString(), Toast.LENGTH_SHORT).show();

                                                break;
                                            case UNRECOGNIZED:
                                                Toast.makeText(mContext,
                                                        "Failed: UNRECOGNIZED. PluginLib Upgrade Required?",
                                                        Toast.LENGTH_SHORT).show();
                                                break;
                                        }*/

                                    }
                                });
                            }
                        });
                    }
                };

        AntPluginPcc.IDeviceStateChangeReceiver mDeviceStateChangeReceiver =
                //Receives state changes and shows it on the status display line
                new AntPluginPcc.IDeviceStateChangeReceiver()
                {
                    @Override
                    public void onDeviceStateChange(final DeviceState newDeviceState)
                    {
                        mActivity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run() {
                                if (newDeviceState == DeviceState.DEAD) {
                                    mFragment.autoPauseFromTrainerDisconnect();
                                    resetPcc(false);
                                }
                                if (newDeviceState == DeviceState.SEARCHING) {

                                    mFragment.autoPauseFromTrainerDisconnect();

                                    // ⚠️ Warning state (yellow)
                                    int warn = Color.YELLOW;
                                    int normal = Color.GRAY;

                                    mFragment.setActualPowerColor(normal);
                                    mFragment.setActualPowerTitleColor(warn);
                                    mFragment.setSpeedTitleColor(warn);
                                    mFragment.setSpeedTextColor(normal);
                                    mFragment.setSpeed("999");
                                    mFragment.setTotalDistanceColor(normal);
                                    mFragment.setTotalDistanceTitleColor(warn);
                                    mFragment.setWorkoutTimerColor(normal);
                                    mFragment.setWorkoutTimerTitleColor(warn);
                                    mFragment.setCadenceColor(normal);
                                    mFragment.setCadenceTitleColor(warn);

                                } else if (newDeviceState == DeviceState.DEAD ||
                                        newDeviceState == DeviceState.CLOSED ||
                                        newDeviceState == DeviceState.UNRECOGNIZED) {

                                    mFragment.autoPauseFromTrainerDisconnect();

                                    // ❌ Error state (red)
                                    int error = Color.RED;
                                    int normal = Color.GRAY;

                                    mFragment.setActualPowerColor(normal);
                                    mFragment.setActualPowerTitleColor(error);
                                    mFragment.setSpeedTitleColor(error);
                                    mFragment.setSpeedTextColor(normal);
                                    mFragment.setSpeed("999");
                                    mFragment.setTotalDistanceColor(normal);
                                    mFragment.setTotalDistanceTitleColor(error);
                                    mFragment.setWorkoutTimerColor(normal);
                                    mFragment.setWorkoutTimerTitleColor(error);
                                    mFragment.setCadenceColor(normal);
                                    mFragment.setCadenceTitleColor(error);
                                } else if (newDeviceState == DeviceState.TRACKING) {

                                    // ✅ Good state (green)
                                    int ok = Color.GREEN;
                                    int normal = Color.WHITE;

                                    mFragment.setActualPowerColor(normal);
                                    mFragment.setActualPowerTitleColor(ok);
                                    mFragment.setSpeedTitleColor(ok);
                                    mFragment.setSpeedTextColor(normal);
                                    //mFragment.setSpeed("000");
                                    mFragment.setTotalDistanceColor(normal);
                                    mFragment.setTotalDistanceTitleColor(ok);
                                    mFragment.setWorkoutTimerColor(normal);
                                    mFragment.setWorkoutTimerTitleColor(ok);
                                    mFragment.setCadenceColor(normal);
                                    mFragment.setCadenceTitleColor(ok);

                                    mFragment.autoResumeFromTrainerReconnect();

                                }


                                //Note: The state here is the state of our data receiver channel which is closed until the ANTFS session is established
                                //if(newDeviceState == DeviceState.CLOSED)
                                //{
                                //    Toast.makeText(mContext, fePcc.getDeviceName() + ": " + "Waiting for FE Session Request", Toast.LENGTH_SHORT).show();
                                //}
                                //else
                                //{
                                    //Toast.makeText(mContext, fePcc.getDeviceName() + ": " + newDeviceState, Toast.LENGTH_SHORT).show();

                                //}
                            }
                        });
                    }
                };

        AntPlusFitnessEquipmentPcc.IFitnessEquipmentStateReceiver mFitnessEquipmentStateReceiver =
                new AntPlusFitnessEquipmentPcc.IFitnessEquipmentStateReceiver()
                {
                    @Override
                    public void onNewFitnessEquipmentState(final long estTimestamp,
                                                           EnumSet<EventFlag> eventFlags, final AntPlusFitnessEquipmentPcc.EquipmentType equipmentType,
                                                           final AntPlusFitnessEquipmentPcc.EquipmentState equipmentState)
                    {
                        mActivity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                //Toast.makeText(mContext, "Timestamp = " + String.valueOf(estTimestamp), Toast.LENGTH_SHORT).show();


                                switch(equipmentType)
                                {
                                    case GENERAL:
                                        Toast.makeText(mContext, "GENERAL", Toast.LENGTH_SHORT).show();
                                        break;
                                    case TREADMILL:
                                        Toast.makeText(mContext, "TREADMILL", Toast.LENGTH_SHORT).show();
                                        break;
                                    case ELLIPTICAL:
                                        Toast.makeText(mContext, "ELLIPTICAL", Toast.LENGTH_SHORT).show();
                                        break;
                                    case BIKE:
                                        Toast.makeText(mContext, "BIKE", Toast.LENGTH_SHORT).show();
                                        break;
                                    case ROWER:
                                        Toast.makeText(mContext, "ROWER", Toast.LENGTH_SHORT).show();
                                        break;
                                    case CLIMBER:
                                        Toast.makeText(mContext, "CLIMBER", Toast.LENGTH_SHORT).show();
                                        break;
                                    case NORDICSKIER:
                                        Toast.makeText(mContext, "NORDIC SKIER", Toast.LENGTH_SHORT).show();
                                        break;
                                    case TRAINER:
                                        //Toast.makeText(mContext, "TRAINER", Toast.LENGTH_SHORT).show();
                                        
                                        if(subscriptionsDone)
                                            break;

                                        fePcc.getTrainerMethods().subscribeCalculatedTrainerPowerEvent(new AntPlusFitnessEquipmentPcc.ICalculatedTrainerPowerReceiver()
                                        {
                                            @Override
                                            public void onNewCalculatedTrainerPower(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                                                                    final AntPlusFitnessEquipmentPcc.TrainerDataSource dataSource, final BigDecimal calculatedPower)
                                            {
                                                mActivity.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        
                                                        //Toast.makeText(mContext, "Timestamp = "+ String.valueOf(estTimestamp) + "Power = " + String.valueOf(calculatedPower) + "W", Toast.LENGTH_SHORT).show();

                                                        String source;

                                                        //NOTE: The calculated power event will send an initial value code if it needed to calculate a NEW average.
                                                        //This is important if using the calculated power event to record user data, as an initial value indicates an average could not be guaranteed.
                                                        //The event prioritizes calculating with torque data over power only data.
                                                        switch(dataSource)
                                                        {
                                                            case COAST_OR_STOP_DETECTED:
                                                                //A coast or stop condition detected by the ANT+ Plugin.
                                                                //This is automatically sent by the plugin after 3 seconds of unchanging events.
                                                                //NOTE: This value should be ignored by apps which are archiving the data for accuracy.
                                                            case INITIAL_VALUE_TRAINER_DATA:
                                                                //New data calculated from initial value data source
                                                            case TRAINER_DATA:
                                                            case INITIAL_VALUE_TRAINER_TORQUE_DATA:
                                                                //New data calculated from initial value data source
                                                            case TRAINER_TORQUE_DATA:
                                                                source = dataSource.toString();
                                                                break;
                                                            case UNRECOGNIZED:
                                                                Toast.makeText(
                                                                        mContext,
                                                                        "Failed: UNRECOGNIZED. PluginLib Upgrade Required?",
                                                                        Toast.LENGTH_SHORT).show();
                                                            default:
                                                                source = "N/A";
                                                                break;
                                                        }

                                                        //Toast.makeText(mContext, "Source = " + source , Toast.LENGTH_SHORT).show();

                                                    }
                                                });
                                            }
                                        });
                                        fePcc.getTrainerMethods().subscribeCalculatedTrainerSpeedEvent(new AntPlusFitnessEquipmentPcc.CalculatedTrainerSpeedReceiver(new BigDecimal("0.70"))   //0.70m wheel diameter
                                        {
                                            @Override
                                            public void onNewCalculatedTrainerSpeed(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                                                                    final AntPlusFitnessEquipmentPcc.TrainerDataSource dataSource, final BigDecimal calculatedSpeed)
                                            {
                                                mActivity.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        //Toast.makeText(mContext, "Timestamp = " + String.valueOf(estTimestamp) + " Speed =  " + String.valueOf(calculatedSpeed) + " km/h", Toast.LENGTH_SHORT).show();

                                                        String source;

                                                        //NOTE: The calculated speed event will send an initial value code if it needed to calculate a NEW average.
                                                        //This is important if using the calculated speed event to record user data, as an initial value indicates an average could not be guaranteed.
                                                        switch(dataSource)
                                                        {
                                                            case COAST_OR_STOP_DETECTED:
                                                                //A coast or stop condition detected by the ANT+ Plugin.
                                                                //This is automatically sent by the plugin after 3 seconds of unchanging events.
                                                                //NOTE: This value should be ignored by apps which are archiving the data for accuracy.
                                                            case INITIAL_VALUE_TRAINER_TORQUE_DATA:
                                                                //New data calculated from initial value data source
                                                            case TRAINER_TORQUE_DATA:
                                                                source = dataSource.toString();
                                                                break;
                                                            case UNRECOGNIZED:
                                                                Toast.makeText(
                                                                        mContext,
                                                                        "Failed: UNRECOGNIZED. PluginLib Upgrade Required?",
                                                                        Toast.LENGTH_SHORT).show();
                                                            default:
                                                                source = "N/A";
                                                                break;
                                                        }

                                                        //Toast.makeText(mContext, "Source = " + source , Toast.LENGTH_SHORT).show();

                                                    }
                                                });
                                            }
                                        });
                                        fePcc.getTrainerMethods().subscribeCalculatedTrainerDistanceEvent(new AntPlusFitnessEquipmentPcc.CalculatedTrainerDistanceReceiver(new BigDecimal("0.70")) //0.70m wheel diameter
                                        {

                                            @Override
                                            public void onNewCalculatedTrainerDistance(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                                                                       final AntPlusFitnessEquipmentPcc.TrainerDataSource dataSource, final BigDecimal calculatedDistance)
                                            {
                                                mActivity.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        //Toast.makeText(mContext, "Timestamp = " + String.valueOf(estTimestamp) + " Calculated distance =  " + String.valueOf(calculatedDistance) + " m", Toast.LENGTH_SHORT).show();

                                                        String source;

                                                        //NOTE: The calculated speed event will send an initial value code if it needed to calculate a NEW average.
                                                        //This is important if using the calculated speed event to record user data, as an initial value indicates an average could not be guaranteed.
                                                        switch(dataSource)
                                                        {
                                                            case COAST_OR_STOP_DETECTED:
                                                                //A coast or stop condition detected by the ANT+ Plugin.
                                                                //This is automatically sent by the plugin after 3 seconds of unchanging events.
                                                                //NOTE: This value should be ignored by apps which are archiving the data for accuracy.
                                                            case INITIAL_VALUE_TRAINER_TORQUE_DATA:
                                                                //New data calculated from initial value data source
                                                            case TRAINER_TORQUE_DATA:
                                                                source = dataSource.toString();
                                                                break;
                                                            case UNRECOGNIZED:
                                                                Toast.makeText(
                                                                       mContext,
                                                                        "Failed: UNRECOGNIZED. PluginLib Upgrade Required?",
                                                                        Toast.LENGTH_SHORT).show();
                                                            default:
                                                                source = "N/A";
                                                                break;
                                                        }

                                                        //Toast.makeText(mContext, "Source = " + source , Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        });
                                        fePcc.getTrainerMethods().subscribeRawTrainerDataEvent(new AntPlusFitnessEquipmentPcc.IRawTrainerDataReceiver()
                                        {
                                            @Override
                                            public void onNewRawTrainerData(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                                                            final long updateEventCount, final int instantaneousCadence, final int instantaneousPower,
                                                                            final long accumulatedPower)
                                            {
                                                mActivity.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        //Toast.makeText(mContext, "Raw Trainer Data Event @ Timestamp = " + String.valueOf(estTimestamp), Toast.LENGTH_SHORT).show();


                                                        //NOTE: If the update event count has not incremented then the data on this page has not changed.
                                                        //Please refer to the ANT+ Fitness Equipment Device Profile for more information.
                                                        /*textView_TrainerUpdateEventCount.setText(String.valueOf(updateEventCount));
                                                        // LEONID
                                                        */
                                                        if(instantaneousCadence != -1) {
                                                            //textView_TrainerInstantaneousCadence.setText(String.valueOf(instantaneousCadence) + "RPM");
                                                            mFragment.setCadence(String.valueOf(instantaneousCadence));
                                                        } else {
                                                            Log.d(TAG, "instantaneousCadence = -1");
                                                        }

                                                        if(instantaneousPower != -1) {
                                                            //textView_TrainerInstantaneousPower.setText(String.valueOf(instantaneousPower) + "W");
                                                            mFragment.setActualPower(String.valueOf(instantaneousPower));

                                                        } else {
                                                            Log.d(TAG, "instantaneousPower = -1");

                                                        }
                                                        /*
                                                        if(accumulatedPower != -1)
                                                            textView_TrainerAccumulatedPower.setText(String.valueOf(accumulatedPower) + "W");
                                                        else
                                                            textView_TrainerAccumulatedPower.setText("N/A");
                                                            */
                                                    }
                                                });
                                            }
                                        });
                                        fePcc.getTrainerMethods().subscribeRawTrainerTorqueDataEvent(new AntPlusFitnessEquipmentPcc.IRawTrainerTorqueDataReceiver()
                                        {
                                            @Override
                                            public void onNewRawTrainerTorqueData(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                                                                  final long updateEventCount, final long accumulatedWheelTicks, final BigDecimal accumulatedWheelPeriod,
                                                                                  final BigDecimal accumulatedTorque)
                                            {
                                                mActivity.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        //Toast.makeText(mContext, "Raw Trainer Torque Data Event @ Timestamp = " + String.valueOf(estTimestamp), Toast.LENGTH_SHORT).show();
                                                        /*
                                                        tv_estTimestamp.setText(String.valueOf(estTimestamp));

                                                        textView_TrainerTorqueUpdateEventCount.setText(String.valueOf(updateEventCount));
                                                        textView_AccumulatedWheelTicks.setText(String.valueOf(accumulatedWheelTicks) + "rotations");
                                                        textView_AccumulatedWheelPeriod.setText(String.valueOf(accumulatedWheelPeriod) + "s");
                                                        textView_TrainerAccumulatedTorque.setText(String.valueOf(accumulatedTorque) + "Nm");
                                                        */
                                                    }
                                                });
                                            }
                                        });
                                        fePcc.subscribeCapabilitiesEvent(new AntPlusFitnessEquipmentPcc.ICapabilitiesReceiver()
                                        {

                                            @Override
                                            public void onNewCapabilities(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                                                          final AntPlusFitnessEquipmentPcc.Capabilities capabilities)
                                            {
                                                mActivity.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        //tv_estTimestamp.setText(String.valueOf(estTimestamp));
                                                        Toast.makeText(mContext, "New Capabilities Event @ Timestamp = " + String.valueOf(estTimestamp), Toast.LENGTH_SHORT).show();

                                                        /*
                                                        if(capabilities.maximumResistance != null)
                                                            textView_MaximumResistance.setText(String.valueOf(capabilities.maximumResistance) + "N");
                                                        else
                                                            textView_MaximumResistance.setText("N/A");

                                                        textView_BasicResistanceSupport.setText(capabilities.basicResistanceModeSupport ? "True" : "False");
                                                        textView_TargetPowerSupport.setText(capabilities.targetPowerModeSupport ? "True" : "False");
                                                        textView_SimulationModeSupport.setText(capabilities.simulationModeSupport ? "True" : "False");
                                                         */
                                                    }
                                                });
                                            }
                                        });
                                        fePcc.getTrainerMethods().subscribeTrainerStatusEvent(new AntPlusFitnessEquipmentPcc.ITrainerStatusReceiver()
                                        {
                                            @Override
                                            public void onNewTrainerStatus(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                                                           final EnumSet<AntPlusFitnessEquipmentPcc.TrainerStatusFlag> trainerStatusFlags)
                                            {
                                                mActivity.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        //Toast.makeText(mContext, "New Trainer Status @ Timestamp = " + String.valueOf(estTimestamp), Toast.LENGTH_SHORT).show();


                                                        for(AntPlusFitnessEquipmentPcc.TrainerStatusFlag flag : trainerStatusFlags)
                                                        {
                                                            switch(flag)
                                                            {
                                                                case BICYCLE_POWER_CALIBRATION_REQUIRED:
                                                                    break;
                                                                case MAXIMUM_POWER_LIMIT_REACHED:
                                                                    break;
                                                                case MINIMUM_POWER_LIMIT_REACHED:
                                                                    break;
                                                                case RESISTANCE_CALIBRATION_REQUIRED:
                                                                    break;
                                                                case UNRECOGNIZED_FLAG_PRESENT:
                                                                    break;
                                                                case USER_CONFIGURATION_REQUIRED:
                                                                    break;
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                        });
                                        fePcc.subscribeCalibrationInProgressEvent(new AntPlusFitnessEquipmentPcc.ICalibrationInProgressReceiver()
                                        {
                                            @Override
                                            public void onNewCalibrationInProgress(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                                                                   final AntPlusFitnessEquipmentPcc.CalibrationInProgress calibrationInProgress)
                                            {
                                                mActivity.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        Toast.makeText(mContext, "New Calibration event at Timestamp =  " + String.valueOf(estTimestamp) , Toast.LENGTH_SHORT).show();
                                                        /*
                                                        tv_estTimestamp.setText(String.valueOf(estTimestamp));
                                                        textView_ZeroOffsetCalPending.setText(calibrationInProgress.zeroOffsetCalibrationPending ? "Pending" : "Not Requested");
                                                        textView_SpinDownCalPending.setText(calibrationInProgress.spinDownCalibrationPending ? "Pending" : "Not Requested");
                                                         */
                                                        switch(calibrationInProgress.temperatureCondition)
                                                        {
                                                            case CURRENT_TEMPERATURE_OK:
                                                            case CURRENT_TEMPERATURE_TOO_HIGH:
                                                            case CURRENT_TEMPERATURE_TOO_LOW:
                                                            case NOT_APPLICABLE:
                                                            case UNRECOGNIZED:
                                                            default:
                                                                Toast.makeText(mContext, "Calibration Temperature Conditions  = " + calibrationInProgress.temperatureCondition.toString() , Toast.LENGTH_SHORT).show();

                                                                break;

                                                        }

                                                        switch(calibrationInProgress.speedCondition)
                                                        {
                                                            case CURRENT_SPEED_OK:
                                                            case CURRENT_SPEED_TOO_LOW:
                                                            case NOT_APPLICABLE:
                                                            case UNRECOGNIZED:
                                                            default:
                                                                Toast.makeText(mContext, "Calibration Speed Conditions  = " + calibrationInProgress.speedCondition.toString() , Toast.LENGTH_SHORT).show();
                                                                break;

                                                        }
                                                        /*
                                                        if(calibrationInProgress.currentTemperature != null)
                                                            textView_CurrentTemperature.setText(calibrationInProgress.currentTemperature.toString() + "C");
                                                        else
                                                            textView_CurrentTemperature.setText("N/A");

                                                        if(calibrationInProgress.targetSpeed != null)
                                                            textView_TargetSpeed.setText(calibrationInProgress.currentTemperature.toString() + "m/s");
                                                        else
                                                            textView_TargetSpeed.setText("N/A");

                                                        if(calibrationInProgress.targetSpinDownTime != null)
                                                            textView_TargetSpinDownTime.setText(calibrationInProgress.targetSpinDownTime.toString() + "ms");
                                                        else
                                                            textView_TargetSpinDownTime.setText("N/A");
                                                        */
                                                    }
                                                });
                                            }
                                        });
                                        fePcc.subscribeCalibrationResponseEvent(new AntPlusFitnessEquipmentPcc.ICalibrationResponseReceiver()
                                        {

                                            @Override
                                            public void onNewCalibrationResponse(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                                                                 final AntPlusFitnessEquipmentPcc.CalibrationResponse calibrationResponse)
                                            {
                                                mActivity.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {

                                                        Toast.makeText(mContext, "New Calibration response at Timestamp =  " + String.valueOf(estTimestamp) , Toast.LENGTH_SHORT).show();

                                                        /*
                                                        tv_estTimestamp.setText(String.valueOf(estTimestamp));
                                                        textView_ZeroOffsetCalSuccess.setText(calibrationResponse.zeroOffsetCalibrationSuccess ? "Success" : "N/A");
                                                        textView_SpinDownCalSuccess.setText(calibrationResponse.spinDownCalibrationSuccess ? "Success" : "N/A");

                                                        if(calibrationResponse.temperature != null)
                                                            textView_Temperature.setText(calibrationResponse.temperature.toString() + "C");
                                                        else
                                                            textView_Temperature.setText("N/A");

                                                        if(calibrationResponse.zeroOffset != null)
                                                            textView_ZeroOffset.setText(calibrationResponse.zeroOffset.toString());
                                                        else
                                                            textView_ZeroOffset.setText("N/A");

                                                        if(calibrationResponse.spinDownTime != null)
                                                            textView_SpinDownTime.setText(calibrationResponse.spinDownTime.toString() + "ms");
                                                        else
                                                            textView_SpinDownTime.setText("N/A");
                                                     */
                                                    }
                                                });
                                            }
                                        });
                                        fePcc.getTrainerMethods().subscribeCommandStatusEvent(new AntPlusFitnessEquipmentPcc.ICommandStatusReceiver()
                                        {

                                            @Override
                                            public void onNewCommandStatus(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                                                           final AntPlusFitnessEquipmentPcc.CommandStatus commandStatus)
                                            {
                                                mActivity.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        Toast.makeText(mContext, "New Command at Timestamp =  " + String.valueOf(estTimestamp) , Toast.LENGTH_SHORT).show();

                                                        /*
                                                        if(commandStatus.lastReceivedSequenceNumber != -1)
                                                            textView_SequenceNumber.setText(String.valueOf(commandStatus.lastReceivedSequenceNumber));
                                                        else
                                                            textView_SequenceNumber.setText("No control page Rx'd");
                                                         */
                                                        switch(commandStatus.status)
                                                        {
                                                            case FAIL:
                                                            case NOT_SUPPORTED:
                                                            case PASS:
                                                            case PENDING:
                                                            case REJECTED:
                                                            case UNINITIALIZED:
                                                            case UNRECOGNIZED:
                                                            default:
                                                                Toast.makeText(mContext, "Command status =  " + commandStatus.status.toString() , Toast.LENGTH_SHORT).show();
                                                                break;
                                                        }

                                                        String rawData = "";
                                                        for(byte b : commandStatus.rawResponseData)
                                                            rawData += "[" + b + "]";
                                                        Toast.makeText(mContext, "Raw Data =  " + rawData , Toast.LENGTH_SHORT).show();

                                                        //textView_LastRxCmdId.setText(commandStatus.lastReceivedCommandId.toString());
                                                        switch(commandStatus.lastReceivedCommandId)
                                                        {
                                                            case BASIC_RESISTANCE:
                                                                //textView_TotalResistanceStatus.setText(commandStatus.totalResistance.toString()+ "%");
                                                                break;
                                                            case TARGET_POWER:
                                                                //textView_TargetPowerStatus.setText(commandStatus.targetPower.toString() + "W");
                                                                break;
                                                            case WIND_RESISTANCE:
                                                                //textView_WindResistanceCoefficientStatus.setText(commandStatus.windResistanceCoefficient.toString() + "kg/m");
                                                                //textView_WindSpeedStatus.setText(commandStatus.windSpeed.toString() + "km/h");
                                                                //textView_DraftingFactorStatus.setText(commandStatus.draftingFactor.toString());
                                                                break;
                                                            case TRACK_RESISTANCE:
                                                                //textView_GradeStatus.setText(commandStatus.grade.toString() + "%");
                                                                //textView_RollingResistanceCoefficientStatus.setText(commandStatus.rollingResistanceCoefficient.toString());
                                                                break;
                                                            case NO_CONTROL_PAGE_RECEIVED:
                                                                break;
                                                            case UNRECOGNIZED:
                                                                break;
                                                            default:
                                                                break;
                                                        }

                                                    }
                                                });
                                            }
                                        });
                                        fePcc.subscribeUserConfigurationEvent(new AntPlusFitnessEquipmentPcc.IUserConfigurationReceiver()
                                        {
                                            @Override
                                            public void onNewUserConfiguration(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                                                               final AntPlusFitnessEquipmentPcc.UserConfiguration userConfiguration)
                                            {
                                                mActivity.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        Toast.makeText(mContext, "New USer Configuration at Timsestamp =  " + String.valueOf(estTimestamp) , Toast.LENGTH_SHORT).show();
                                                        /*
                                                        tv_estTimestamp.setText(String.valueOf(estTimestamp));
                                                        textView_UserWeight.setText(userConfiguration.userWeight != null ? userConfiguration.userWeight.toString() + "kg" : "N/A");
                                                        textView_BicycleWeight.setText(userConfiguration.bicycleWeight != null ? userConfiguration.bicycleWeight.toString() + "kg" : "N/A");
                                                        textView_BicycleWheelDiameter.setText(userConfiguration.bicycleWheelDiameter != null ? userConfiguration.bicycleWheelDiameter.toString() + "m" : "N/A");
                                                        textView_GearRatio.setText(userConfiguration.gearRatio != null ? userConfiguration.gearRatio.toString() : "N/A");
                                                        */
                                                    }
                                                });
                                            }
                                        });
                                        fePcc.getTrainerMethods().subscribeBasicResistanceEvent(new AntPlusFitnessEquipmentPcc.IBasicResistanceReceiver()
                                        {

                                            @Override
                                            public void onNewBasicResistance(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                                                             final BigDecimal totalResistance)
                                            {
                                                mActivity.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        //tv_estTimestamp.setText(String.valueOf(estTimestamp));
                                                        Toast.makeText(mContext, "New Basic Resistance at Timsestamp =  " + String.valueOf(estTimestamp) , Toast.LENGTH_SHORT).show();

                                                        //textView_TotalResistance.setText(totalResistance.toString() + "%");
                                                    }
                                                });
                                            }
                                        });
                                        fePcc.getTrainerMethods().subscribeTargetPowerEvent(new AntPlusFitnessEquipmentPcc.ITargetPowerReceiver()
                                        {

                                            @Override
                                            public void onNewTargetPower(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                                                         final BigDecimal targetPower)
                                            {
                                                mActivity.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        Toast.makeText(mContext, "New Target Power at Timsestamp =  " + String.valueOf(estTimestamp) + " Target Power = " + targetPower.toString() + " W", Toast.LENGTH_SHORT).show();

                                                        //tv_estTimestamp.setText(String.valueOf(estTimestamp));
                                                        //textView_TargetPower.setText(targetPower.toString() + "W");
                                                    }
                                                });
                                            }
                                        });
                                        fePcc.getTrainerMethods().subscribeTrackResistanceEvent(new AntPlusFitnessEquipmentPcc.ITrackResistanceReceiver()
                                        {

                                            @Override
                                            public void onNewTrackResistance(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                                                             final BigDecimal grade, final BigDecimal rollingResistanceCoefficient)
                                            {
                                                mActivity.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        Toast.makeText(mContext, "New Track Resistance at Timestamp =  " + String.valueOf(estTimestamp) + " Target Power = " + grade.toString() + " %", Toast.LENGTH_SHORT).show();
                                                        /*
                                                        tv_estTimestamp.setText(String.valueOf(estTimestamp));
                                                        textView_Grade.setText(grade.toString() + "%");
                                                        textView_RollingResistanceCoefficient.setText(rollingResistanceCoefficient.toString());
                                                        */
                                                    }
                                                });
                                            }
                                        });
                                        fePcc.getTrainerMethods().subscribeWindResistanceEvent(new AntPlusFitnessEquipmentPcc.IWindResistanceReceiver()
                                        {

                                            @Override
                                            public void onNewWindResistance(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                                                            final BigDecimal windResistanceCoefficient, final int windSpeed, final BigDecimal draftingFactor)
                                            {
                                                mActivity.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        Toast.makeText(mContext, "New Wind Resistance at Timestamp =  " + String.valueOf(estTimestamp), Toast.LENGTH_SHORT).show();
                                                        /*
                                                        tv_estTimestamp.setText(String.valueOf(estTimestamp));
                                                        textView_WindResistanceCoefficient.setText(windResistanceCoefficient.toString() + "kg/m");
                                                        textView_WindSpeed.setText(String.valueOf(windSpeed) + "km/h");
                                                        textView_DraftingFactor.setText(draftingFactor.toString());
                                                        */
                                                    }
                                                });
                                            }
                                        });
                                        subscriptionsDone = true;
                                        break;
                                    case UNKNOWN:
                                        //Toast.makeText(mContext, "UNKNOWN", Toast.LENGTH_SHORT).show();
                                        break;
                                    case UNRECOGNIZED:
                                        Toast.makeText(mContext, "UNRECOGNIZED", Toast.LENGTH_SHORT).show();
                                        break;
                                    default:
                                        Toast.makeText(mContext, "INVALID", Toast.LENGTH_SHORT).show();

                                        break;
                                }

                                switch(equipmentState)
                                {
                                    case ASLEEP_OFF:
                                        Toast.makeText(mContext, "OFF", Toast.LENGTH_SHORT).show();
                                        break;
                                    case READY:
                                        //Toast.makeText(mContext, "READY", Toast.LENGTH_SHORT).show();
                                        break;
                                    case IN_USE:
                                        //Toast.makeText(mContext, "IN USE", Toast.LENGTH_SHORT).show();
                                        break;
                                    case FINISHED_PAUSED:
                                        Toast.makeText(mContext, "FINISHE/PAUSE", Toast.LENGTH_SHORT).show();
                                        break;
                                    case UNRECOGNIZED:
                                        Toast.makeText(mContext,
                                                "Failed: UNRECOGNIZED. PluginLib Upgrade Required?",
                                                Toast.LENGTH_SHORT).show();
                                    default:
                                        Toast.makeText(mContext, "INVALID", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }

                };


        if (include_workout == false) {
            //Make the access request
            //If the Activity was passed the bundle it indicates this Activity was started intended for traditional Fitness Equipment, otherwise connect to broadcast based FE Controls
            //TODO Both PCC request types may be done concurrently on separate release handles in order to simultaneously support both types of FE without requiring prior knowledge of types
            releaseHandle = AntPlusFitnessEquipmentPcc.requestNewOpenAccess(mActivity,
                    mContext, mPluginAccessResultReceiver, mDeviceStateChangeReceiver,
                    mFitnessEquipmentStateReceiver);
        }
        else {
            try {
                // Make available a FIT workout file to the fitness equipment
                // The sample file included with this project was obtained from the FIT SDK, v7.10
                InputStream is = mActivity.getAssets().open("example.fit");
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int next;
                while ((next = is.read()) != -1)
                    bos.write(next);
                bos.flush();
                is.close();
                FitFileCommon.FitFile workoutFile = new FitFileCommon.FitFile(bos.toByteArray());
                workoutFile.setFileType((short) 5);  // Make sure to set the File Type, so this information is also available to the fitness equipment
                // Refer to the FIT SDK for more details on FIT file types
                files = new FitFileCommon.FitFile[]{workoutFile};
            } catch (IOException e) {
                files = null;
            }

            settings = new AntPlusFitnessEquipmentPcc.Settings("Leonid", Settings.Gender.MALE, (short)42 , 175, 65);

            //releaseHandle = AntPlusFitnessEquipmentPcc.requestNewOpenAccess(mActivity,
            //        mContext, mPluginAccessResultReceiver, mDeviceStateChangeReceiver,
            //        mFitnessEquipmentStateReceiver);

            //result.getAntDeviceNumber()
            releaseHandle = AntPlusFitnessEquipmentPcc.requestNewPersonalSessionAccess(mContext,
                    mPluginAccessResultReceiver, mDeviceStateChangeReceiver,
                    mFitnessEquipmentStateReceiver, 0, settings, files);
        }

            /*if (b == null)
        {
            releaseHandle = AntPlusFitnessEquipmentPcc.requestNewOpenAccess(mActivity,
                    mContext, mPluginAccessResultReceiver, mDeviceStateChangeReceiver,
                    mFitnessEquipmentStateReceiver);
        }
        else if (b.containsKey(
                Activity_MultiDeviceSearchSampler.EXTRA_KEY_MULTIDEVICE_SEARCH_RESULT))
        {
            // device has already been selected through the multi-device search
            MultiDeviceSearch.MultiDeviceSearchResult result = b.getParcelable(
                    Activity_MultiDeviceSearchSampler.EXTRA_KEY_MULTIDEVICE_SEARCH_RESULT);

            releaseHandle = AntPlusFitnessEquipmentPcc.requestNewOpenAccess(this,
                    result.getAntDeviceNumber(), 0, mPluginAccessResultReceiver,
                    mDeviceStateChangeReceiver, mFitnessEquipmentStateReceiver);
        }
        else
        {
            releaseHandle = AntPlusFitnessEquipmentPcc.requestNewPersonalSessionAccess(this,
                    mPluginAccessResultReceiver, mDeviceStateChangeReceiver,
                    mFitnessEquipmentStateReceiver, 0, settings, files);
        }*/
    }

    private void showToast(final String message){
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onDestroy()
    {
        if(releaseHandle != null)
        {
            releaseHandle.close();
            releaseHandle = null;
        }
    }

    private void scheduleAutoReconnect(final boolean includeWorkout) {
        if (!autoReconnectEnabled)
            return;

        reconnectAttempt++;

        long delayMs = Math.min(30000L, 2000L * reconnectAttempt);
        long delaySec = delayMs / 1000;

        // BEFORE waiting
        Log.d("TrainerController",
                "Auto-reconnect scheduled: attempt " + reconnectAttempt +
                        " in " + delaySec + " seconds");

        Toast.makeText(mContext,
                "Trainer disconnected — retrying (" + reconnectAttempt + ") in " +
                        delaySec + "s…",
                Toast.LENGTH_SHORT).show();

        reconnectHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                // WHEN retries actually start
                Log.d("TrainerController",
                        "Auto-reconnect attempt " + reconnectAttempt + " — retrying now");

                Toast.makeText(mContext,
                        "Reconnecting now (attempt " + reconnectAttempt + ")…",
                        Toast.LENGTH_SHORT).show();

                resetPcc(includeWorkout);
            }
        }, delayMs);
    }

    private void clearReconnectState() {
        Log.d("TrainerController", "Auto-reconnect state cleared");
        reconnectAttempt = 0;
        reconnectHandler.removeCallbacksAndMessages(null);
    }


}
