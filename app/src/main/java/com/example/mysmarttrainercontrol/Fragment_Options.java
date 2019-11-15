package com.example.mysmarttrainercontrol;

import android.os.Bundle;
//import android.support.v4.app.Fragment;
//import android.support.v4.app.FragmentTransaction;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mysmarttrainercontrol.fitnessequipment.Fragment_FitnessEquipmentSampler;
import com.example.mysmarttrainercontrol.fitnessequipment.TrainerController;
import com.example.mysmarttrainercontrol.multidevicesearch.Fragment_MultiDeviceSearchSampler;

public class Fragment_Options extends Fragment implements View.OnClickListener {
    View rootView;
    TextView linkManualPowerControl;
    TextView linkScanForDevices;
    TextView linkScanTrainer;

    Fragment_ManualPowerControl fragmentManualPowerControl;
    Fragment_MultiDeviceSearchSampler fragmentScanDevices;
    Fragment_FitnessEquipmentSampler fragmentScanTrainer;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView =  inflater.inflate(R.layout.fragment_options, container, false);

        linkManualPowerControl = rootView.findViewById(R.id.manual_power_control);
        if (linkManualPowerControl!= null)
            linkManualPowerControl.setOnClickListener(this);
        else
            Toast.makeText(getContext(), "linkManualPowerControl is NULL", Toast.LENGTH_SHORT).show();

        linkScanForDevices = rootView.findViewById(R.id.scan_devices);
        if (linkScanForDevices!= null)
            linkScanForDevices.setOnClickListener(this);
        else
            Toast.makeText(getContext(), "linkScanForDevices is NULL", Toast.LENGTH_SHORT).show();

        linkScanTrainer = rootView.findViewById(R.id.scan_trainer);
        if (linkScanTrainer!= null)
            linkScanTrainer.setOnClickListener(this);
        else
            Toast.makeText(getContext(), "linkScanTrainer is NULL", Toast.LENGTH_SHORT).show();


        return rootView;
    }

    @Override
    public void onClick(View v) {
        FragmentTransaction transaction;
        switch (v.getId()) {
            case R.id.manual_power_control: {
                Toast.makeText(getContext(), "Manual Power Control Selected.", Toast.LENGTH_SHORT).show();
                fragmentManualPowerControl = new Fragment_ManualPowerControl();
                transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.main_activity, fragmentManualPowerControl, "ManualPowerControlFragment");
                transaction.addToBackStack("ManualPowerControl");
                transaction.commitAllowingStateLoss();
                break;
            }
            case R.id.scan_devices:{
                Toast.makeText(getContext(), "Scan for Devices Selected.", Toast.LENGTH_SHORT).show();
                fragmentScanDevices = new Fragment_MultiDeviceSearchSampler();
                transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.main_activity, fragmentScanDevices);
                transaction.addToBackStack("ScandDevices");
                transaction.commitAllowingStateLoss();
                break;
            }
            case R.id.scan_trainer:{
                Toast.makeText(getContext(), "Scan for Trainer Selected.", Toast.LENGTH_SHORT).show();

                fragmentScanTrainer = new Fragment_FitnessEquipmentSampler();
                transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.main_activity, fragmentScanTrainer);
                transaction.addToBackStack("ScanTrainer");
                transaction.commitAllowingStateLoss();
                break;
            }
        }

    }
}
