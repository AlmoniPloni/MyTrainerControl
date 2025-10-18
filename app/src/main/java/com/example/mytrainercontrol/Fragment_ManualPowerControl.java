package com.example.mytrainercontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
//import android.support.v4.app.Fragment;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mytrainercontrol.fitnessequipment.TrainerController;
import com.example.mytrainercontrol.heartrate.HeartRateSensor;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.gson.Gson;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static java.lang.Thread.sleep;

public class Fragment_ManualPowerControl extends Fragment {

    private static final String TAG = "PowerControl FMPC";

    final String WORKOUT_IN_PROGRESS = "WORKOUT_IN_PROGRESS";
    final String WORKOUT_PAUSED = "WORKOUT_PAUSED";
    final String WORKOUT = "WORKOUT";
    final String LAST_TARGET_POWER = "LAST_TARGET_POWER";
    final String SEGMENT_TIME_LEFT = "SEGMENT_TIME_LEFT";
    final String LAST_SEGMENT = "LAST_SEGMENT";



    View rootView;
    Button btnPowerIncrease;
    Button btnPowerDecrease;
    Button btnStartStopWorkout;
    Button btnPauseWorkout;
    //Button btnStopWorkout;
    Button btnNextSegment;
    Button btnPrevSegment;

    TextView targetPower;
    TextView nextTargetPower;
    int targetPowerVal;

    TextView segmentDescription;
    TextView nextSegmentDescription;

    TextView mActualPowerTitle;
    TextView mActualPower;

    TextView segmentTimer;
    TextView nextSegmentTimer;

    TextView mCadenceTitle;
    TextView mCadence;

    TextView mSpeedTitle;
    TextView mSpeed;

    TextView mHeartRateTitle;
    TextView mHearRate;

    TextView openWokrout;
    LineChart powerChart;

    TextView mDistanceTitle;
    TextView mTotalDistance;
    float mTotalDist;
    float prevDist;
    float mTime;

    TextView mWorkoutTimeTitle;
    TextView mWorkoutTime;

    public TrainerController trainerController;
    PowerSensor powerSensor;
    HeartRateSensor heartRateSensor;
    WorkoutReader workoutReader;
    public Workout workout;
    WorkoutExecThread workoutExecutionThread;
    boolean workoutPaused;
    boolean workoutInProgress;
    //int lastTargetPower;
    int segmentTimeLeft;
    int lastSegment;

    DialogInterface.OnClickListener stopDialogClickListener = new DialogInterface.OnClickListener() { // from class: com.example.mytrainercontrol.Fragment_ManualPowerControl.7
        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialog, int which) {
            if (which == -2) {
                Log.d(Fragment_ManualPowerControl.TAG, "No clicked, Don't Stop Workout");
                return;
            }
            if (which == -1) {
                Toast.makeText(Fragment_ManualPowerControl.this.getContext(), "Stopping workout", Toast.LENGTH_SHORT).show();
                Log.d(Fragment_ManualPowerControl.TAG, "Interrupting Workout Execution Thread Now !");
                Fragment_ManualPowerControl.this.workoutExecutionThread.stopWorkout();
                Fragment_ManualPowerControl.this.reset();
                Fragment_ManualPowerControl.this.openWokrout.setVisibility(View.VISIBLE);
                Fragment_ManualPowerControl.this.powerChart.setVisibility(View.INVISIBLE);
                Fragment_ManualPowerControl.this.btnStartStopWorkout.setText("START");
            }
        }
    };
    DialogInterface.OnClickListener resumeDialogClickListener = new DialogInterface.OnClickListener() { // from class: com.example.mytrainercontrol.Fragment_ManualPowerControl.8
        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialog, int which) {
            if (which == -2) {
                Log.d(Fragment_ManualPowerControl.TAG, "No clicked, Don't resume anything");
                Fragment_ManualPowerControl.this.reset();
            } else if (which == -1) {
                Toast.makeText(Fragment_ManualPowerControl.this.getContext(), "Resuming workout, where it stopped last time.", Toast.LENGTH_SHORT).show();
                Log.d(Fragment_ManualPowerControl.TAG, "Resuming Workout where it stopped last time, now !");
                Fragment_ManualPowerControl fragment_ManualPowerControl = Fragment_ManualPowerControl.this;
                fragment_ManualPowerControl.workoutExecutionThread = new WorkoutExecThread(fragment_ManualPowerControl);
                Fragment_ManualPowerControl.this.workoutExecutionThread.start();
                Fragment_ManualPowerControl.this.workoutInProgress = true;
            }
        }
    };
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "On Create");
        // Inflate the layout for this fragment
        rootView =  inflater.inflate(R.layout.fragment_manual_power_control, container, false);
        targetPower = rootView.findViewById(R.id.target_power);
        targetPowerVal = Integer.parseInt(targetPower.getText().toString());
        nextTargetPower = rootView.findViewById(R.id.next_segment_power);

        mActualPowerTitle = rootView.findViewById(R.id.power_title);
        mActualPower = rootView.findViewById(R.id.actual_power);

        segmentTimer = rootView.findViewById(R.id.segment_timer);
        segmentDescription = rootView.findViewById(R.id.segment_description);

        nextSegmentTimer = rootView.findViewById(R.id.next_segment_time);
        nextSegmentDescription = rootView.findViewById(R.id.next_segment_description);

        // Cadence views
        mCadenceTitle = rootView.findViewById(R.id.cadence_title);
        mCadence =  rootView.findViewById(R.id.cadence);

        // Speed views
        mSpeedTitle = rootView.findViewById(R.id.speed_title);
        mSpeed =  rootView.findViewById(R.id.speed);

        // Heart Rate views
        mHeartRateTitle = rootView.findViewById(R.id.hr_title);
        mHearRate =  rootView.findViewById(R.id.hear_rate);

        // Total distance
        mDistanceTitle = rootView.findViewById(R.id.distance_title);
        mTotalDistance = rootView.findViewById(R.id.total_distance);
        mTotalDist = 0;
        mTime = 0;
        prevDist = 0;

        mWorkoutTimeTitle = rootView.findViewById(R.id.time_title);
        mWorkoutTime = rootView.findViewById(R.id.workout_timer);

        btnPowerIncrease = rootView.findViewById(R.id.power_increase);
        btnPowerIncrease.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                increasePower(v);
            }
        });

        btnPowerDecrease = rootView.findViewById(R.id.power_decrease);
        btnPowerDecrease.setOnClickListener(new View .OnClickListener() {
            public void onClick(View v) {
                decreasePower(v);
            }
        });

        openWokrout = rootView.findViewById(R.id.open_workout);
        openWokrout.setOnClickListener(new View .OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getContext(), "Loading workout !", Toast.LENGTH_SHORT).show();
                loadWorkout();
            }
        });

        btnStartStopWorkout = rootView.findViewById(R.id.start_stop_workout);
        btnStartStopWorkout.setOnClickListener(new View .OnClickListener() {
            public void onClick(View v) {
                if (!Fragment_ManualPowerControl.this.workoutInProgress) {
                    Fragment_ManualPowerControl.this.startWorkout();
                } else {
                    Fragment_ManualPowerControl.this.stopWorkout();
                }
            }
        });

        btnPauseWorkout = rootView.findViewById(R.id.pause_workout);
        btnPauseWorkout.setOnClickListener(new View .OnClickListener() {
            public void onClick(View v) {
                if (workoutPaused == true)
                    resumeWorkout();
                else
                    pauseWorkout();
            }
        });

//
//        btnStopWorkout = rootView.findViewById(R.id.stop_workout);
//        btnStopWorkout.setOnClickListener(new View .OnClickListener() {
//            public void onClick(View v) {
//                //Toast.makeText(getContext(), "Stop Workout not implemented yet", Toast.LENGTH_SHORT).show();
//                stopWorkout();
//            }
//        });

        btnNextSegment = rootView.findViewById(R.id.next_segment);
        btnNextSegment.setOnClickListener(new View .OnClickListener() {
            public void onClick(View v) {
                //sToast.makeText(getContext(), "Next Segment not implemented yet", Toast.LENGTH_SHORT).show();
                nextSegment();
            }
        });

        btnPrevSegment = rootView.findViewById(R.id.prev_segment);
        btnPrevSegment.setOnClickListener(new View .OnClickListener() {
            public void onClick(View v) {
                //sToast.makeText(getContext(), "Next Segment not implemented yet", Toast.LENGTH_SHORT).show();
                prevSegment();
            }
        });

        powerChart = rootView.findViewById(R.id.power_chart);

        trainerController = new TrainerController(getActivity(), getContext(), this);
        trainerController.resetPcc(false);

        getActivity().startService(new Intent(getActivity(), KeepAliveService.class));

        //powerSensor = new PowerSensor(getActivity(), getContext(), actualPower, mActualPowerTitle);
        //powerSensor.resetPcc();

        //heartRateSensor = new HeartRateSensor(getActivity(), getContext(), mHearRate, mHeartRateTitle);
        //heartRateSensor.requestAccessToPcc();

        workoutPaused = false;
        workoutInProgress = false;
        lastSegment = 0;
        segmentTimeLeft = Integer.MAX_VALUE;

        return rootView;
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "On Destroy");

        trainerController.onDestroy();

        getActivity().stopService(new Intent(getActivity(), KeepAliveService.class));

        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch(requestCode){
            case 333:
                if(resultCode==RESULT_OK){
                    Uri csvFileURI = data.getData();
                    Log.d(TAG, csvFileURI + " was selected");
                    workout = workoutReader.setWorkout(csvFileURI);
                    setPowerCharData(workout);
                    openWokrout.setVisibility(View.INVISIBLE);
                    powerChart.setVisibility(View.VISIBLE);
                    setLine(0);
                    powerChart.invalidate();
                    //Toast.makeText(getActivity(), csvFileURI.toString() , Toast.LENGTH_SHORT).show();
                }
                break;

        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "In onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);

        Log.d(TAG, "Saving state to shared preferences");

        SharedPreferences sp = getActivity().getSharedPreferences("my_prefs", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(WORKOUT_IN_PROGRESS, workoutInProgress);
        Log.d(TAG, "Saving workoutInProgress " + workoutInProgress);

        editor.putBoolean(WORKOUT_PAUSED, workoutPaused);

        Gson gson = new Gson();
        String jsonWorkout = gson.toJson(workout);
        editor.putString(WORKOUT, jsonWorkout);

        //editor.putInt(LAST_TARGET_POWER, lastTargetPower);
        editor.putInt(SEGMENT_TIME_LEFT, segmentTimeLeft);
        editor.putInt(LAST_SEGMENT, lastSegment);
        editor.commit();

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "In onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        Log.d(TAG, "Restoring previous state from shared preferences");
        SharedPreferences sp = getActivity().getSharedPreferences("my_prefs", Activity.MODE_PRIVATE);

        workoutInProgress = sp.getBoolean(WORKOUT_IN_PROGRESS, false);
        Log.d(TAG, "Restored workoutInProgress: " + workoutInProgress);

        if (workoutInProgress == true) {

            workoutPaused = sp.getBoolean(WORKOUT_PAUSED, false);
            Log.d(TAG, "Restored workoutPaused: " + workoutPaused);

            String jsonWorkout= sp.getString("WORKOUT", "");
            if(jsonWorkout.isEmpty() == false) {
                Gson gson = new Gson();
                workout = gson.fromJson(jsonWorkout, Workout.class);
            }
            Log.d(TAG, "Restored last Workout ");

            segmentTimeLeft = sp.getInt(SEGMENT_TIME_LEFT, Integer.MAX_VALUE);
            Log.d(TAG, "Restored segmentTimeLeft: " + segmentTimeLeft);

            lastSegment = sp.getInt(LAST_SEGMENT, 0);
            Log.d(TAG, "Restored lastSegment: " + lastSegment);

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage("Would you like to resume last unfinished workout ?").setPositiveButton("Yes", resumeDialogClickListener)
                    .setNegativeButton("No", resumeDialogClickListener)
                    .show();
        }

        if (savedInstanceState != null) {
            Log.d(TAG, "Restoring previous state");
              /*
            workoutInProgress = savedInstanceState.getBoolean(WORKOUT_IN_PROGRESS, false);
            workoutPaused = savedInstanceState.getBoolean(WORKOUT_PAUSED, false);

            String jsonWorkout= savedInstanceState.getString("WORKOUT");
            if(jsonWorkout.isEmpty() == false) {
                Gson gson = new Gson();
                workout = gson.fromJson(jsonWorkout, Workout.class);
            }

            lastTargetPower =  savedInstanceState.getInt(LAST_TARGET_POWER, 100);
            segmentTimeLeft = savedInstanceState.getInt(SEGMENT_TIME_LEFT, Integer.MAX_VALUE);
            lastSegment = savedInstanceState.getInt(LAST_SEGMENT, 10000);
            */
        }
    }

//    DialogInterface.OnClickListener stopDialogClickListener = new DialogInterface.OnClickListener() {
//        @Override
//        public void onClick(DialogInterface dialog, int which) {
//            switch (which){
//                case DialogInterface.BUTTON_POSITIVE:
//                    //Yes button clicked
//                    Toast.makeText(getContext(), "Stopping workout", Toast.LENGTH_SHORT).show();
//                    Log.d(TAG, "Interrupting Workout Execution Thread Now !");
//                    workoutExecutionThread.stopWorkout();
//                    /*btnPauseWorkout.setText("Pause");
//                    segmentTimer.setText("00:00");
//                    workoutPaused = false;
//                    workoutInProgress = false;
//                    segmentTimeLeft = Integer.MAX_VALUE;
//                    lastSegment = 0;*/
//                    reset();
//                    openWokrout.setVisibility(View.VISIBLE);
//                    powerChart.setVisibility(View.INVISIBLE);
//                    break;
//
//                case DialogInterface.BUTTON_NEGATIVE:
//                    //No button clicked
//                    Log.d(TAG, "No clicked, Don't Stop Workout");
//                    break;
//            }
//        }
//    };
//
//    DialogInterface.OnClickListener resumeDialogClickListener = new DialogInterface.OnClickListener() {
//        @Override
//        public void onClick(DialogInterface dialog, int which) {
//            switch (which){
//                case DialogInterface.BUTTON_POSITIVE:
//                    //Yes button clicked
//                    Toast.makeText(getContext(), "Resuming workout, where it stopped last time.", Toast.LENGTH_SHORT).show();
//                    Log.d(TAG, "Resuming Workout where it stopped last time, now !");
//                    workoutExecutionThread = new WorkoutExecThread(Fragment_ManualPowerControl.this);
//                    workoutExecutionThread.start();
//                    workoutInProgress = true;
//                    break;
//
//                case DialogInterface.BUTTON_NEGATIVE:
//                    //No button clicked
//                    Log.d(TAG, "No clicked, Don't resume anything");
//                    /*workoutPaused = false;
//                    workoutInProgress = false;
//                    lastSegment = 0;
//                    segmentTimeLeft = Integer.MAX_VALUE;
//                    workout = null;*/
//                    reset();
//                    break;
//            }
//        }
//    };


    private void increasePower(View view) {
        targetPowerVal = Integer.parseInt(targetPower.getText().toString()) + 10;
        boolean success = trainerController.setTargetPower(new BigDecimal(targetPowerVal));
        if (success == true)
            targetPower.setText("" + targetPowerVal);
        else
            Toast.makeText(getContext(), "Could not increase power !", Toast.LENGTH_LONG).show();

    }

    private void decreasePower(View view) {
        targetPowerVal = Integer.parseInt(targetPower.getText().toString()) - 10;
        if (targetPowerVal < 0)
            targetPowerVal = 0;
        boolean success = trainerController.setTargetPower(new BigDecimal(targetPowerVal));
        if (success == true)
            targetPower.setText("" + targetPowerVal);
        else
            Toast.makeText(getContext(), "Could not decrease power !", Toast.LENGTH_LONG).show();
    }

    private void loadWorkout(){

        if (workoutInProgress == false) {
            reset();
            workoutReader = new WorkoutReader(getContext(), getActivity(), this);
            workoutReader.chooseWorkout();

        }
        else {
            Toast.makeText(getContext(), "Workout in progress ! Stop to load new workout", Toast.LENGTH_SHORT).show();

        }

    }

    private void startWorkout(){
        if (workout == null) {
            Toast.makeText(getContext(), "Load Workout First", Toast.LENGTH_SHORT).show();
        }

        else if (trainerController.getFePcc() == null || trainerController.getReleaseHandle() == null) {
            Toast.makeText(getContext(), "No connected trainer, do search again", Toast.LENGTH_SHORT).show();
        }

        else if (workoutExecutionThread != null && workoutExecutionThread.isAlive() == true) {
            Toast.makeText(getContext(), "Workout is executing, stop it to start a new one", Toast.LENGTH_SHORT).show();
        }

        else {
            Toast.makeText(getContext(), "Starting workout execution thread", Toast.LENGTH_SHORT).show();
            workoutExecutionThread = new WorkoutExecThread(this);
            workoutExecutionThread.start();
            workoutInProgress = true;
            btnStartStopWorkout.setText("STOP");

        }
    }

    private void stopWorkout(){
        if (workoutExecutionThread != null && workoutExecutionThread.isAlive() == true) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage("Are you sure ?").setPositiveButton("Yes", stopDialogClickListener)
                    .setNegativeButton("No", stopDialogClickListener)
                    .show();



            /*Toast.makeText(getContext(), "Stopping workout", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Interrupting Workout Execution Thread Now !");
            workoutExecutionThread.stopWorkout();
            btnPauseWorkout.setText("Pause");
            segmentTimer.setText("00:00");
            workoutPaused = false;
            workoutInProgress = false;
            */
        }
        else {
            Toast.makeText(getContext(), "No active workout to stop", Toast.LENGTH_SHORT).show();
            openWokrout.setVisibility(View.VISIBLE);
            powerChart.setVisibility(View.INVISIBLE);
        }
    }

    private void pauseWorkout() {
        if (workoutExecutionThread != null && workoutExecutionThread.isAlive() == true) {
            Toast.makeText(getContext(), "Pausing workout", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Pausing Workout Execution Thread Now !");
            btnPauseWorkout.setText("Resume");
            workoutExecutionThread.pauseWorkout();
            workoutPaused = true;
        }
        else {
            Toast.makeText(getContext(), "No active workout to pause", Toast.LENGTH_SHORT).show();
        }
    }

    private void resumeWorkout() {
        if (workoutExecutionThread != null && workoutExecutionThread.isAlive() == true) {
            Toast.makeText(getContext(), "Resuming workout", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Resuming Workout Execution Thread Now !");
            btnPauseWorkout.setText("Pause");
            workoutExecutionThread.resumeWorkout();
            workoutPaused = false;
        }
        else {
            Toast.makeText(getContext(), "No active workout to pause", Toast.LENGTH_SHORT).show();
        }
    }

    private void nextSegment() {
        if (workoutPaused == true) {
            Toast.makeText(getContext(), "Resume workout to Skip to next Segment", Toast.LENGTH_SHORT).show();
            return;
        }

        if (workoutExecutionThread != null && workoutExecutionThread.isAlive() == true) {
            //Toast.makeText(getContext(), "Skipping to Next Segment", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Next Segment !");
            workoutExecutionThread.nextSegment();
        }
        else {
            Toast.makeText(getContext(), "No active workout ! ", Toast.LENGTH_SHORT).show();
        }
    }

    private void prevSegment() {
        if (workoutPaused == true) {
            Toast.makeText(getContext(), "Resume workout to go back to prev Segment", Toast.LENGTH_SHORT).show();
            return;
        }

        if (workoutExecutionThread != null && workoutExecutionThread.isAlive() == true) {
            //Toast.makeText(getContext(), "Skipping to Next Segment", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Prev Segment !");
            workoutExecutionThread.previousSegment();
        }
        else {
            Toast.makeText(getContext(), "No active workout ! ", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isWorkoutInProgress(){
        return workoutInProgress;
    }

    /*public void setLastTargetPower(int power) {
        lastTargetPower = power;
    }*/

    public void setSegmentTimeLeft(int timeLeft) {
        segmentTimeLeft = timeLeft;
    }

    public void setLastSegment(int segment) {
        lastSegment = segment;
    }

    public void setWorkoutInProgress(boolean workoutInProgress) {
        this.workoutInProgress = workoutInProgress;
    }

    public void reset(){
        btnPauseWorkout.setText("Pause");
        segmentTimer.setText("00:00");
        workoutPaused = false;
        workoutInProgress = false;
        segmentTimeLeft = Integer.MAX_VALUE;
        lastSegment = 0;
        workout = null;
    }

    public void setNextSegmentDescription(String description){
        nextSegmentDescription.setText(description);
    }

    public void setSegmentDescription(String description){
        segmentDescription.setText(description);
    }

    public void setTargetPower(String power){
        targetPower.setText(power);
    }

    public void setNextTargetPower(String power){
        nextTargetPower.setText(power);
    }

    public void setSegmentTimer(String time) {
        segmentTimer.setText(time);
    }

    public void setNextSegmentTimer(String time) {
        nextSegmentTimer.setText(time);
    }


    public void setActualPower(String power) {
        mActualPower.setText(power);
    }

    public void setActualPowerColor(int color) {
        mActualPower.setTextColor(color);
    }

    public void setActualPowerTitleColor(int color) {
        mActualPowerTitle.setTextColor(color);
    }


    public void setCadence(String cadence) {
        mCadence.setText(cadence);
    }

    public void setCadenceColor(int color) {
        mCadence.setTextColor(color);
    }

    public void setCadenceTitleColor(int color) {
        mCadenceTitle.setTextColor(color);
    }

    public void setSpeed(String speed) {
        mSpeed.setText(speed);
    }

    public void setSpeedTitleColor(int color){
        mSpeedTitle.setTextColor(color);
    }

    public void setSpeedTextColor(int color) {
        mSpeed.setTextColor(color);
    }

    public void setTotalDistance(String distance){
        mTotalDistance.setText(distance);
    }

    public void setTotalDistanceColor(int color) {
        mTotalDistance.setTextColor(color);
    }

    public void setTotalDistanceTitleColor(int color) {
        mDistanceTitle.setTextColor(color);
    }


    public void setWorkoutTimer(float timeSeconds){
        int seconds = (int)((timeSeconds) % 60) ;
        int minutes = (int)((timeSeconds/60) % 60);
        int hours   = (int)((timeSeconds/(60*60)) % 24);
        mWorkoutTime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    public void setWorkoutTimerColor(int color){
        mWorkoutTime.setTextColor(color);
    }

    public void setWorkoutTimerTitleColor(int color) {
        mWorkoutTimeTitle.setTextColor(color);
    }

    public void setDistance(float time, float speed){
        //float prevDist = mTotalDist;
        mTotalDist += (time-mTime)*speed;
        mTime = time;
        if (mTotalDist - prevDist >= 10) {
            setTotalDistance(String.format ("%,.2f", mTotalDist/1000));
            prevDist = mTotalDist;
        }

    }

    private void setPowerCharData(Workout workout) {

        ArrayList<BarEntry> values = new ArrayList<>();

        for (int i = 0; i < workout.getSize()-1; i++) {
            values.add(new BarEntry(i, workout.getPower(i)));
        }
        BarDataSet barPowerDataSet = new BarDataSet(values, "My Workout");
        barPowerDataSet.setDrawValues(false);
        /*
        int startColor = ContextCompat.getColor(getContext(), android.R.color.holo_green_light);
        int endColor = ContextCompat.getColor(getContext(), android.R.color.holo_red_dark);
        barPowerDataSet.setGradientColor(startColor, endColor);
        */

        BarData powerData = new BarData(barPowerDataSet);
        powerData.setBarWidth(1f);
        //powerData.wid
        //powerChart.setData(powerData);

        List<ILineDataSet> dataSets = generateLineData(workout);
        LineData data = new LineData(dataSets);
        powerChart.setData(data);


        //powerChart.setFitBars(true);
        //powerChart.setDrawValueAboveBar(true);

        YAxis leftAxis = powerChart.getAxisLeft();
        //leftAxis.setGranularity(50f);
        leftAxis.setAxisMinimum(0);
        leftAxis.setAxisMaximum(350f);
        //leftAxis.setDrawLabels(true);
        leftAxis.setLabelCount(8, true);
        leftAxis.setDrawLabels(true);
        leftAxis.setTextColor(Color.rgb(211,211,211));

        YAxis rightAxis = powerChart.getAxisRight();
        rightAxis.setDrawGridLines(false);

        //rightAxis.setGranularity(10f);


        XAxis xAxis = powerChart.getXAxis();
        //xAxis.setAxisMaximum(workout.getTotalDuraction()/60);
        //xAxis.setAxisMinimum(0);
        //xAxis.setLabelCount((int)Math.ceil(workout.getTotalDuraction()/600)+2, true);

        //YAxis.YAxisLabelPosition

        //xAxis.setGranularityEnabled(true);
        //xAxis.setGranularity(600f);

        xAxis.setDrawLabels(true);
        xAxis.setTextColor(Color.rgb(211,211,211));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        powerChart.getDescription().setEnabled(false);
        powerChart.getLegend().setEnabled(false);
        powerChart.setTouchEnabled(false);

        //powerChart.set
    }

    public void setLine(int pos){
        if (pos ==- 1)
            if (powerChart.getXAxis().getLimitLines() != null)
                pos = (int) powerChart.getXAxis().getLimitLines().get(0).getLimit() + 1;
            
        LimitLine ll = new LimitLine(pos);
        ll.setLineColor(Color.RED);
        ll.setLineWidth(2f);

        powerChart.getXAxis().removeAllLimitLines();
        powerChart.getXAxis().addLimitLine(ll);
        powerChart.invalidate();

    }

    private List<ILineDataSet> generateLineData(Workout workout) {
        List<ILineDataSet> dataSets = new ArrayList<>();
        int currentTime = 0;
        for (int i = 0; i < workout.getSize()-1; i++) {
            List<Entry> barPoints = new ArrayList<>();
            Entry barPoint1;
            Entry barPoint2;
            LineDataSet dataSet;
            barPoint1 = new Entry(currentTime, workout.getPower(i));
            barPoint2 = new Entry(currentTime + workout.getDuration(i), workout.getPower(i));
            currentTime += workout.getDuration(i);
            barPoints.add(barPoint1);
            barPoints.add(barPoint2);
            dataSet = new LineDataSet(barPoints, "");
            dataSet.setColor(Color.BLACK);
            Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.power_fill);
            dataSet.setFillDrawable(drawable);

            dataSet.setDrawCircles(false);
            dataSet.setDrawFilled(true);
            dataSet.setDrawValues(false);
            dataSets.add(dataSet);
    }
        return dataSets;
    }

}
