package com.example.mysmarttrainercontrol;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mysmarttrainercontrol.fitnessequipment.TrainerController;

import java.math.BigDecimal;
import java.util.List;

import static java.lang.Thread.sleep;

public class WorkoutExecutor implements Runnable {

    private static final String TAG = "PowerControl WE";
    List<Pair<String, String>> mWorkout;
    Context mContext;
    Activity mActivity;
    TrainerController mTrainerController;
    TextView mTargetPower;

    public WorkoutExecutor(Activity activity, Context context, List<Pair<String, String>> workout, TrainerController trainerController, TextView targetPower) {
        mActivity = activity;
        mContext = context;
        mWorkout = workout;
        mTrainerController = trainerController;
        mTargetPower = targetPower;
    }

    public void run() {
        for (int i = 0; i < mWorkout.size(); i++) {
            final int targetPowerVal = Integer.parseInt(mWorkout.get(i).second);
            int segmentTime =  Integer.parseInt(mWorkout.get(i).first);
            Log.d(TAG, "Segment" + i + " setting Target Power: " + targetPowerVal + " W for " + segmentTime + " seconds");
            //Toast.makeText(mContext, "T = " + mWorkout.get(i).first + "W" + mWorkout.get(i).second, Toast.LENGTH_SHORT).show();
            mTrainerController.setTargetPower(new BigDecimal(targetPowerVal));
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTargetPower.setText("" + targetPowerVal);
                }
            });


            try {
                Log.d(TAG, "Going to sleep for " + segmentTime + " seconds");
                sleep(segmentTime * 1000);
            } catch (InterruptedException e) {
                Log.d(TAG, "Could not sleep for " + mWorkout.get(i).first + "seconds");

            }
        }
        System.out.println("Workout DONE! ");
    }
}

    /*private void showToast(String message){
        mActivity.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(mContext, message, Toast.LENGTH.SHORT).show();
            }
        });
    }*/

