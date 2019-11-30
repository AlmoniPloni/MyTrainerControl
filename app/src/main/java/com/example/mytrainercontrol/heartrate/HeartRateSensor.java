/*
This software is subject to the license described in the License.txt file
included with this software distribution. You may not use this file except in compliance
with this license.

Copyright (c) Dynastream Innovations Inc. 2013
All rights reserved.
 */

package com.example.mytrainercontrol.heartrate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

//import com.dsi.ant.antplus.pluginsampler.R;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.DataState;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.ICalculatedRrIntervalReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IPage4AddtDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.RrFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusCommonPcc.IRssiReceiver;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.ICumulativeOperatingTimeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.IManufacturerAndSerialReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.IVersionAndModelReceiver;

import java.math.BigDecimal;
import java.util.EnumSet;

/**
 * Base class to connects to Heart Rate Plugin and display all the event data.
 */
public class HeartRateSensor
{
    private static final String TAG = "PowerControl HRS";

    Context mContext;
    Activity mActivity;
    TextView mHeartRate;
    TextView mHeartRateTitle;

    AntPlusHeartRatePcc hrPcc = null;
    protected PccReleaseHandle<AntPlusHeartRatePcc> releaseHandle = null;

    public HeartRateSensor(Activity activity, Context context, TextView heartRate, TextView heartRateTitle){
        mContext = context;
        mActivity = activity;
        mHeartRate = heartRate;
        mHeartRateTitle = heartRateTitle;
    }
    public void requestAccessToPcc()
    {
        // starts the plugins UI search
        releaseHandle = AntPlusHeartRatePcc.requestAccess(mActivity, mContext,
                base_IPluginAccessResultReceiver, base_IDeviceStateChangeReceiver);
    }

    /**
     * Resets the PCC connection to request access again and clears any existing display data.
     */
    protected void handleReset()
    {
        //Release the old access if it exists
        if(releaseHandle != null)
        {
            releaseHandle.close();
        }

        requestAccessToPcc();
    }


    /**
     * Switches the active view to the data display and subscribes to all the data events
     */
    public void subscribeToHrEvents()
    {
        hrPcc.subscribeHeartRateDataEvent(new IHeartRateDataReceiver()
        {
            @Override
            public void onNewHeartRateData(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                final int computedHeartRate, final long heartBeatCount,
                final BigDecimal heartBeatEventTime, final DataState dataState)
            {
                // Mark heart rate with asterisk if zero detected
                final String textHeartRate = String.valueOf(computedHeartRate)
                    + ((DataState.ZERO_DETECTED.equals(dataState)) ? "*" : "");

                // Mark heart beat count and heart beat event time with asterisk if initial value
                final String textHeartBeatCount = String.valueOf(heartBeatCount)
                    + ((DataState.INITIAL_VALUE.equals(dataState)) ? "*" : "");
                final String textHeartBeatEventTime = String.valueOf(heartBeatEventTime)
                    + ((DataState.INITIAL_VALUE.equals(dataState)) ? "*" : "");

                mActivity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mHeartRate.setText(textHeartRate);
                    }
                });
            }
        });

        hrPcc.subscribePage4AddtDataEvent(new IPage4AddtDataReceiver()
        {
            @Override
            public void onNewPage4AddtData(final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                final int manufacturerSpecificByte,
                final BigDecimal previousHeartBeatEventTime)
            {
                mActivity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.d(TAG, "In onNewPage4AddtData");
                    }
                });
            }
        });

        hrPcc.subscribeCumulativeOperatingTimeEvent(new ICumulativeOperatingTimeReceiver()
        {
            @Override
            public void onNewCumulativeOperatingTime(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final long cumulativeOperatingTime)
            {
                mActivity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.d(TAG, "In onNewCumulativeOperatingTime");
                    }
                });
            }
        });

        /*hrPcc.subscribeManufacturerAndSerialEvent(new IManufacturerAndSerialReceiver()
        {
            @Override
            public void onNewManufacturerAndSerial(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final int manufacturerID,
                final int serialNumber)
            {
                mActivity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.d(TAG, "In onNewManufacturerAndSerial");

                    }
                });
            }
        });*/

        /*hrPcc.subscribeVersionAndModelEvent(new IVersionAndModelReceiver()
        {
            @Override
            public void onNewVersionAndModel(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final int hardwareVersion,
                final int softwareVersion, final int modelNumber)
            {
                mActivity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.d(TAG, "In onNewVersionAndModel");
                    }
                });
            }
        });*/

        /*hrPcc.subscribeCalculatedRrIntervalEvent(new ICalculatedRrIntervalReceiver()
        {
            @Override
            public void onNewCalculatedRrInterval(final long estTimestamp,
                EnumSet<EventFlag> eventFlags, final BigDecimal rrInterval, final RrFlag flag)
            {
                mActivity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.d(TAG, "In onNewCalculatedRrInterval");

                    }
                });
            }
        });*/

        /*hrPcc.subscribeRssiEvent(new IRssiReceiver() {
            @Override
            public void onRssiData(final long estTimestamp, final EnumSet<EventFlag> evtFlags, final int rssi) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "In onRssiData");

                    }
                });
            }
        });*/
    }

    protected IPluginAccessResultReceiver<AntPlusHeartRatePcc> base_IPluginAccessResultReceiver =
        new IPluginAccessResultReceiver<AntPlusHeartRatePcc>()
        {
        //Handle the result, connecting to events on success or reporting failure to user.
        @Override
        public void onResultReceived(AntPlusHeartRatePcc result, RequestAccessResult resultCode,
            DeviceState initialDeviceState)
        {
            //showDataDisplay("Connecting...");
            switch(resultCode)
            {
                case SUCCESS:
                    hrPcc = result;
                    subscribeToHrEvents();
                    Toast.makeText(mContext, "Heart Rate Successfully Connected", Toast.LENGTH_SHORT).show();
                    if (initialDeviceState == DeviceState.TRACKING) {
                        mHeartRateTitle.setTextColor(Color.GREEN);
                        mHeartRate.setTextColor(Color.WHITE);
                    }
                    else {
                        mHeartRateTitle.setTextColor(Color.RED);
                        mHeartRate.setTextColor(Color.GRAY);
                        mHeartRate.setText("999");
                    }
                    break;
                case CHANNEL_NOT_AVAILABLE:
                    Toast.makeText(mContext, "Channel Not Available", Toast.LENGTH_SHORT).show();
                    break;
                case ADAPTER_NOT_DETECTED:
                    Toast.makeText(mContext, "ANT Adapter Not Available. Built-in ANT hardware or external adapter required.", Toast.LENGTH_SHORT).show();
                    break;
                case BAD_PARAMS:
                    //Note: Since we compose all the params ourself, we should never see this result
                    Toast.makeText(mContext, "Bad request parameters.", Toast.LENGTH_SHORT).show();
                    break;
                case OTHER_FAILURE:
                    Toast.makeText(mContext, "RequestAccess failed. See logcat for details.", Toast.LENGTH_SHORT).show();
                    break;
                case DEPENDENCY_NOT_INSTALLED:
                    Log.d(TAG, "DEPENDENCY_NOT_INSTALLED");
                    AlertDialog.Builder adlgBldr = new AlertDialog.Builder(mContext);
                    adlgBldr.setTitle("Missing Dependency");
                    adlgBldr.setMessage("The required service\n\"" + AntPlusHeartRatePcc.getMissingDependencyName() + "\"\n was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?");
                    adlgBldr.setCancelable(true);
                    adlgBldr.setPositiveButton("Go to Store", new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            Intent startStore = null;
                            startStore = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=" + AntPlusHeartRatePcc.getMissingDependencyPackageName()));
                            startStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            mActivity.startActivity(startStore);
                        }
                    });
                    adlgBldr.setNegativeButton("Cancel", new OnClickListener()
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
                    Toast.makeText(mContext, "User Cancelled", Toast.LENGTH_SHORT).show();
                    break;
                case UNRECOGNIZED:
                    Toast.makeText(mContext,
                        "Failed: UNRECOGNIZED. PluginLib Upgrade Required?",
                        Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(mContext, "Unrecognized result: " + resultCode, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
        };

        //Receives state changes and shows it on the status display line
        protected  IDeviceStateChangeReceiver base_IDeviceStateChangeReceiver =
            new IDeviceStateChangeReceiver()
        {
            @Override
            public void onDeviceStateChange(final DeviceState newDeviceState)
            {
                mActivity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (newDeviceState == DeviceState.TRACKING) {
                            mHeartRateTitle.setTextColor(Color.GREEN);
                            mHeartRate.setTextColor(Color.WHITE);
                            Toast.makeText(mContext, "Heart Rate: Tracking", Toast.LENGTH_SHORT).show();
                        }
                        else if (newDeviceState == DeviceState.DEAD){
                            mHeartRateTitle.setTextColor(Color.RED);
                            mHeartRate.setTextColor(Color.GRAY);
                            mHeartRate.setText("999");
                            handleReset();
                        }
                        else {
                            mHeartRateTitle.setTextColor(Color.RED);
                            mHeartRate.setTextColor(Color.GRAY);
                            mHeartRate.setText("999");
                            Toast.makeText(mContext, "Heart Rate State: " + newDeviceState, Toast.LENGTH_SHORT).show();
                        }
                    }
                });


            }
        };

        protected void onDestroy()
        {
            if(releaseHandle != null)
            {
                releaseHandle.close();
            }
        }



}
