/*
This software is subject to the license described in the License.txt file
included with this software distribution. You may not use this file except in compliance
with this license.

Copyright (c) Dynastream Innovations Inc. 2014
All rights reserved.
 */

package com.example.mytrainercontrol.multidevicesearch;

import java.util.ArrayList;
import java.util.EnumSet;

//import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/*
import com.dsi.ant.antplus.pluginsampler.Activity_BikeCadenceSampler;
import com.dsi.ant.antplus.pluginsampler.Activity_BikeSpeedDistanceSampler;
import com.dsi.ant.antplus.pluginsampler.Activity_EnvironmentSampler;
import com.dsi.ant.antplus.pluginsampler.Activity_StrideSdmSampler;

import com.dsi.ant.antplus.pluginsampler.bloodpressure.Activity_BloodPressureSampler;
import com.dsi.ant.antplus.pluginsampler.fitnessequipment.Fragment_FitnessEquipmentSampler;
import com.dsi.ant.antplus.pluginsampler.heartrate.Activity_SearchUiHeartRateSampler;
import com.dsi.ant.antplus.pluginsampler.weightscale.Activity_WeightScaleSampler;
*/
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.mytrainercontrol.Fragment_ManualPowerControl;
import com.example.mytrainercontrol.R;
//import com.example.mytrainercontrol.Activity_BikePowerSampler;
import com.dsi.ant.plugins.antplus.pcc.MultiDeviceSearch;
import com.dsi.ant.plugins.antplus.pcc.MultiDeviceSearch.RssiSupport;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.MultiDeviceSearch.MultiDeviceSearchResult;

/**
 * Searches for multiple devices on the same channel using the multi-device
 * search interface
 */
public class Fragment_MultiDeviceSearchSampler extends Fragment
{
    /**
     * Relates a MultiDeviceSearchResult with an RSSI value
     */
    public class MultiDeviceSearchResultWithRSSI
    {
        public MultiDeviceSearchResult mDevice;
        public int mRSSI = Integer.MIN_VALUE;
    }
    public static final String EXTRA_KEY_MULTIDEVICE_SEARCH_RESULT = "com.dsi.ant.antplus.pluginsampler.multidevicesearch.result";
    public static final String BUNDLE_KEY = "com.dsi.ant.antplus.pluginsampler.multidevicesearch.bundle";
    public static final String FILTER_KEY = "com.dsi.ant.antplus.pluginsampler.multidevicesearch.filter";
    public static final int RESULT_SEARCH_STOPPED = 1;

    View rootView;
    Context mContext;
    TextView mStatus;

    Fragment_ManualPowerControl fragmentManualPowerControl;

    ListView mFoundDevicesList;
    ArrayList<MultiDeviceSearchResultWithRSSI> mFoundDevices = new ArrayList<MultiDeviceSearchResultWithRSSI>();
    ArrayAdapter_MultiDeviceSearchResult mFoundAdapter;

    ListView mConnectedDevicesList;
    ArrayList<MultiDeviceSearchResultWithRSSI> mConnectedDevices = new ArrayList<MultiDeviceSearchResultWithRSSI>();
    ArrayAdapter_MultiDeviceSearchResult mConnectedAdapter;

    MultiDeviceSearch mSearch;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.fragment_multidevice_scan);
        rootView =  inflater.inflate(R.layout.fragment_multidevice_scan, container, false);


        mContext = getActivity().getApplicationContext();
        mStatus = rootView.findViewById(R.id.textView_Status);

        mFoundDevicesList = rootView.findViewById(R.id.listView_FoundDevices);

        mFoundAdapter = new ArrayAdapter_MultiDeviceSearchResult(getActivity(), mFoundDevices);
        mFoundDevicesList.setAdapter(mFoundAdapter);

        mFoundDevicesList.setOnItemClickListener(new OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                launchConnection(mFoundAdapter.getItem(position).mDevice);
            }
        });

        mConnectedDevicesList = rootView.findViewById(R.id.listView_AlreadyConnectedDevices);

        mConnectedAdapter = new ArrayAdapter_MultiDeviceSearchResult(getActivity(), mConnectedDevices);
        mConnectedDevicesList.setAdapter(mConnectedAdapter);

        mConnectedDevicesList.setOnItemClickListener(new OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                launchConnection(mConnectedAdapter.getItem(position).mDevice);
            }
        });
        /*
        Intent i = getActivity().getIntent();
        Bundle args = i.getBundleExtra(BUNDLE_KEY);
        @SuppressWarnings("unchecked")
        EnumSet<DeviceType> devices = (EnumSet<DeviceType>) args.getSerializable(FILTER_KEY);
        */
        // Manually force to scan only for Power Meter (LD TODO)
        EnumSet<DeviceType> devices = EnumSet.noneOf(DeviceType.class);
        devices.add(DeviceType.BIKE_POWER);

        // start the multi-device search
        mSearch = new MultiDeviceSearch(getActivity(), devices, mCallback, mRssiCallback);

        return rootView;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        // close and clean-up the multi-device search
        mSearch.close();
    }

    public void launchConnection(MultiDeviceSearchResult result)
    {
        switch (result.getAntDeviceType())
        {
            /*case BIKE_CADENCE:
                activity = Activity_BikeCadenceSampler.class;
                break;*/
            case BIKE_POWER:
                Toast.makeText(getContext(), "Manual Power Control Found and can be used.", Toast.LENGTH_SHORT).show();
                fragmentManualPowerControl = new Fragment_ManualPowerControl();
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.main_activity, fragmentManualPowerControl);
                transaction.addToBackStack("ManualPowerControl");
                transaction.commitAllowingStateLoss();
                break;
            /*case BIKE_SPD:
                activity = Activity_BikeSpeedDistanceSampler.class;
                break;*/
            /*case BIKE_SPDCAD:
                activity = Activity_BikeSpeedDistanceSampler.class;
                // could also start with Activity_BikeCadenceSampler
                // need to request access through both, but best practice is to request one after another
                break;
            case BLOOD_PRESSURE:
                activity = Activity_BloodPressureSampler.class;
                break;
            case ENVIRONMENT:
                activity = Activity_EnvironmentSampler.class;
                break;
            case WEIGHT_SCALE:
                activity = Activity_WeightScaleSampler.class;
                break;
            case HEARTRATE:
                activity = Activity_SearchUiHeartRateSampler.class;
                break;
            case STRIDE_SDM:
                activity = Activity_StrideSdmSampler.class;
                break;
            case FITNESS_EQUIPMENT:
                activity = Fragment_FitnessEquipmentSampler.class;
                break;
            case GEOCACHE:
            case CONTROLLABLE_DEVICE:
                Toast.makeText(this, "Not currently supported", Toast.LENGTH_SHORT).show();
                break;
             */
            case UNKNOWN:
                break;
            default:
                break;
        }
    }

    /**
     * Callbacks from the multi-device search interface
     */
    private MultiDeviceSearch.SearchCallbacks mCallback = new MultiDeviceSearch.SearchCallbacks()
    {
        /**
         * Called when a device is found. Display found devices in connected and
         * found lists
         */
        public void onDeviceFound(final MultiDeviceSearchResult deviceFound)
        {
            final MultiDeviceSearchResultWithRSSI result = new MultiDeviceSearchResultWithRSSI();
            result.mDevice = deviceFound;

            // We split up devices already connected to the plugin from
            // un-connected devices to make this information more visible to the
            // user, since the user most likely wants to be aware of which
            // device they are already using in another app
            if (deviceFound.isAlreadyConnected())
            {
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        // connected device category is invisible unless there
                        // are some present
                        if (mConnectedAdapter.isEmpty())
                        {
                            rootView.findViewById(R.id.textView_AlreadyConnectedTitle).setVisibility(
                                    View.VISIBLE);
                            mConnectedDevicesList.setVisibility(View.VISIBLE);
                        }

                        mConnectedAdapter.add(result);
                        mConnectedAdapter.notifyDataSetChanged();
                    }
                });
            }
            else
            {
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mFoundAdapter.add(result);
                        mFoundAdapter.notifyDataSetChanged();
                    }
                });
            }
        }

        /**
         * The search has been stopped unexpectedly
         */
        public void onSearchStopped(RequestAccessResult reason)
        {
            /*Intent result = new Intent();
            result.putExtra(EXTRA_KEY_MULTIDEVICE_SEARCH_RESULT, reason.getIntValue());
            setResult(RESULT_SEARCH_STOPPED, result);
            finish();*/
            Toast.makeText(mContext, "Search was stopped.", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onSearchStarted(RssiSupport supportsRssi) {
            if(supportsRssi == RssiSupport.UNAVAILABLE)
            {
                Toast.makeText(mContext, "Rssi information not available.", Toast.LENGTH_SHORT).show();
            } else if(supportsRssi == RssiSupport.UNKNOWN_OLDSERVICE)
            {
                Toast.makeText(mContext, "Rssi might be supported. Please upgrade the plugin service.", Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * Callback for RSSI data of previously found devices
     */
    private MultiDeviceSearch.RssiCallback mRssiCallback = new MultiDeviceSearch.RssiCallback()
    {
        /**
         * Receive an RSSI data update from a specific found device
         */
        @Override
        public void onRssiUpdate(final int resultId, final int rssi)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    for (MultiDeviceSearchResultWithRSSI result : mFoundDevices)
                    {
                        if (result.mDevice.resultID == resultId)
                        {
                            result.mRSSI = rssi;
                            mFoundAdapter.notifyDataSetChanged();

                            break;
                        }
                    }
                }
            });
        }
    };
}
