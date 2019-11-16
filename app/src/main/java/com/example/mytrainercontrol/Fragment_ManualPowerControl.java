package com.example.mytrainercontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
//import android.support.v4.app.Fragment;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mytrainercontrol.fitnessequipment.TrainerController;
import com.google.gson.Gson;

import java.math.BigDecimal;

import static java.lang.Thread.sleep;

public class Fragment_ManualPowerControl extends Fragment {

    private static final String TAG = "PowerControl MA";

    final String WORKOUT_IN_PROGRESS = "WORKOUT_IN_PROGRESS";
    final String WORKOUT_PAUSED = "WORKOUT_PAUSED";
    final String WORKOUT = "WORKOUT";
    final String LAST_TARGET_POWER = "LAST_TARGET_POWER";
    final String SEGMENT_TIME_LEFT = "SEGMENT_TIME_LEFT";
    final String LAST_SEGMENT = "LAST_SEGMENT";



    View rootView;
    Button btnPowerIncrease;
    Button btnPowerDecrease;
    Button btnLoadWorkout;
    Button btnStartWorkout;
    Button btnPauseWorkout;
    Button btnStopWorkout;
    Button btnNextSegment;
    public TextView targetPower;
    int targetPowerVal;
    TextView actualPower;
    int actualPowerVal;
    public TextView segmentTimer;
    public TrainerController trainerController;
    PowerSensor powerSensor;
    WorkoutReader workoutReader;
    public Workout workout;
    WorkoutExecThread workoutExecutionThread;
    boolean workoutPaused;
    boolean workoutInProgress;
    //int lastTargetPower;
    int segmentTimeLeft;
    int lastSegment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "On Create");
        // Inflate the layout for this fragment
        rootView =  inflater.inflate(R.layout.fragment_manual_power_control, container, false);
        targetPower = rootView.findViewById(R.id.target_power);
        targetPowerVal = Integer.parseInt(targetPower.getText().toString());

        actualPower = rootView.findViewById(R.id.actual_power);
        actualPowerVal = Integer.parseInt(actualPower.getText().toString());

        segmentTimer = rootView.findViewById(R.id.segment_timer);

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

        btnLoadWorkout = rootView.findViewById(R.id.load_workout);
        btnLoadWorkout.setOnClickListener(new View .OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getContext(), "Loading workout !", Toast.LENGTH_SHORT).show();
                loadWorkout();
            }
        });

        btnStartWorkout = rootView.findViewById(R.id.start_workout);
        btnStartWorkout.setOnClickListener(new View .OnClickListener() {
            public void onClick(View v) {
                //Toast.makeText(getContext(), "Start Workout not implemented yet", Toast.LENGTH_SHORT).show();
                startWorkout();
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


        btnStopWorkout = rootView.findViewById(R.id.stop_workout);
        btnStopWorkout.setOnClickListener(new View .OnClickListener() {
            public void onClick(View v) {
                //Toast.makeText(getContext(), "Stop Workout not implemented yet", Toast.LENGTH_SHORT).show();
                stopWorkout();
            }
        });

        btnNextSegment = rootView.findViewById(R.id.next_segment);
        btnNextSegment.setOnClickListener(new View .OnClickListener() {
            public void onClick(View v) {
                //sToast.makeText(getContext(), "Next Segment not implemented yet", Toast.LENGTH_SHORT).show();
                nextSegment();
            }
        });


        trainerController = new TrainerController(getActivity(), getContext());
        trainerController.resetPcc(false);

        powerSensor = new PowerSensor(getActivity(), getContext(), actualPower);
        powerSensor.resetPcc();

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
        super.onDestroy();
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

        workoutPaused = sp.getBoolean(WORKOUT_PAUSED, false);
        Log.d(TAG, "Restored workoutPaused: " + workoutPaused);


        String jsonWorkout= sp.getString("WORKOUT", "");
        if(jsonWorkout.isEmpty() == false) {
            Gson gson = new Gson();
            workout = gson.fromJson(jsonWorkout, Workout.class);
        }

        //lastTargetPower =  sp.getInt(LAST_TARGET_POWER, 100);
        segmentTimeLeft = sp.getInt(SEGMENT_TIME_LEFT, Integer.MAX_VALUE);
        Log.d(TAG, "Restored segmentTimeLeft: " + segmentTimeLeft);

        lastSegment = sp.getInt(LAST_SEGMENT, 0);
        Log.d(TAG, "Restored lastSegment: " + lastSegment);


        //Toast.makeText(getContext(), "Starting workout execution thread", Toast.LENGTH_SHORT).show();
        //Log.d(TAG, "Restarting Workout Execution Thread");

        if (workoutInProgress == true) {
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

    DialogInterface.OnClickListener stopDialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //Yes button clicked
                    Toast.makeText(getContext(), "Stopping workout", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Interrupting Workout Execution Thread Now !");
                    workoutExecutionThread.stopWorkout();
                    btnPauseWorkout.setText("Pause");
                    segmentTimer.setText("00:00");
                    workoutPaused = false;
                    workoutInProgress = false;
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    Log.d(TAG, "No clicked, Don't Stop Workout");
                    break;
            }
        }
    };

    DialogInterface.OnClickListener resumeDialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //Yes button clicked
                    Toast.makeText(getContext(), "Resuming workout, where it stopped last time.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Resuming Workout where it stopped last time, now !");
                    workoutExecutionThread = new WorkoutExecThread(Fragment_ManualPowerControl.this);
                    workoutExecutionThread.start();
                    workoutInProgress = true;
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    Log.d(TAG, "No clicked, Don't resume anything");
                    workoutPaused = false;
                    workoutInProgress = false;
                    lastSegment = 0;
                    segmentTimeLeft = Integer.MAX_VALUE;
                    workout = null;
                    break;
            }
        }
    };


    private void increasePower(View view) {
        targetPowerVal = Integer.parseInt(targetPower.getText().toString()) + 10;
        boolean success = trainerController.setTargetPower(new BigDecimal(targetPowerVal));
        if (success == true)
            targetPower.setText("" + targetPowerVal);
    }

    private void decreasePower(View view) {
        targetPowerVal = Integer.parseInt(targetPower.getText().toString()) - 10;
        if (targetPowerVal < 0)
            targetPowerVal = 0;
        boolean success = trainerController.setTargetPower(new BigDecimal(targetPowerVal));
        if (success == true)
            targetPower.setText("" + targetPowerVal);
    }

    private void loadWorkout(){

        if (workoutInProgress == false) {
            workoutReader = new WorkoutReader(getContext(), getActivity());
            workout = workoutReader.readWorkout();
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
            Toast.makeText(getContext(), "Skipping to Next Segment", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Next Segment !");
            workoutExecutionThread.nextSegment();
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
}
