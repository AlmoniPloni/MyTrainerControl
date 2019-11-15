package com.example.mysmarttrainercontrol;

import android.app.Activity;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mysmarttrainercontrol.fitnessequipment.TrainerController;

import java.math.BigDecimal;
import java.util.List;

import static java.lang.Math.min;

public class WorkoutExecThread extends Thread {

    private static final String TAG = "PowerControl WET";

    Workout mWorkout;
    Fragment_ManualPowerControl mFragment;
    Context mContext;
    Activity mActivity;
    TrainerController mTrainerController;
    TextView mTargetPower;
    CountDownTimer cTimer;
    TextView mSegmentTimer;
    int segmentTimeLeft;
    int lastTargetPower;
    boolean shouldPause;
    boolean segmentCompleted;
    boolean skip_segment;
    int startSegment;


    public WorkoutExecThread(Fragment_ManualPowerControl fragmentFEC){

        mActivity = fragmentFEC.getActivity();
        mFragment = fragmentFEC;
        mContext = fragmentFEC.getContext();
        mWorkout = fragmentFEC.workout;
        mTrainerController = fragmentFEC.trainerController;
        mTargetPower = fragmentFEC.targetPower;
        mSegmentTimer = fragmentFEC.segmentTimer;
        shouldPause = false;
        startSegment = fragmentFEC.lastSegment;
        segmentTimeLeft = fragmentFEC.segmentTimeLeft;

    }

    public void run() {
        workout_loop: for (int i = startSegment; i < mWorkout.getSize(); i++) {
            mFragment.setLastSegment(i);
            lastTargetPower = mWorkout.getPower(i);
            segmentTimeLeft =  min(segmentTimeLeft, mWorkout.getDuration(i));
            segmentCompleted = false;
            skip_segment = false;
            Log.d(TAG, "Segment" + i + " setting Target Power: " + lastTargetPower + " W for " + segmentTimeLeft + " seconds");
            //Toast.makeText(mContext, "T = " + mWorkout.get(i).first + "W" + mWorkout.get(i).second, Toast.LENGTH_SHORT).show();
            //setTargetPower(targetPowerVal);
            while (segmentCompleted == false) {
                startTimer(segmentTimeLeft, lastTargetPower);
                try {
                    Log.d(TAG, "Going to sleep for " + segmentTimeLeft + " seconds");
                    sleep(segmentTimeLeft * 1000);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Workout Interrupted");
                    if (shouldPause == true) {
                        try {
                            Log.d(TAG, "Pausing workout at Segment #" + i + " current power = " + lastTargetPower + " W left time " + segmentTimeLeft + " sec. to the end of current segment");
                            setTargetPower(100, false);
                            synchronized (this) {
                                wait();
                            }
                        } catch (InterruptedException e1) {
                            Log.d(TAG, "Wait Interrupted");
                            if (shouldPause == false) {
                                Log.d(TAG, "Stopping workout");
                                break workout_loop;
                            }
                        }
                        Log.d(TAG, "Woke up, continue workout");
                    } else {
                        if (skip_segment == true) {
                            Log.d(TAG, "Skipping segment");
                            showToast("Skipping current segment!");
                            break;
                        }
                        else {
                            Log.d(TAG, "Stopping workout");
                            showToast("Workout Stopped !");
                            setTargetPower(100, true);
                            return;
                        }
                    }
                }
                if (segmentTimeLeft == 0) {
                    segmentCompleted = true;
                    segmentTimeLeft = Integer.MAX_VALUE;
                    Log.d(TAG, "Segment finished");
                }

            }
        }

        showToast("Workout DONE!");
        setTargetPower(100, true);
        Thread.currentThread().interrupt();
    }

    void startTimer(final int countDownSeconds, final int targetPower) {
        final int countDownInterval = 1000;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                cTimer = new CountDownTimer((countDownSeconds) * 1000, countDownInterval) {
                    //int currentTime = (countDownSeconds)*1000;

                    public void onTick(final long millisUntilFinished) {
                        segmentTimeLeft = (int) millisUntilFinished/1000;
                        Log.d(TAG, "onTick: " + millisUntilFinished + " millis until finished " + segmentTimeLeft + " sec. until finished" );
                        mActivity.runOnUiThread(new Runnable() {
                            String strToDisplay;
                            @Override
                            public void run() {
                                //currentTime -= countDownInterval;
                                int seconds = (int)(millisUntilFinished / 1000) % 60 ;
                                int minutes = (int)((millisUntilFinished / (1000*60)) % 60);
                                int hours   = (int)((millisUntilFinished / (1000*60*60)) % 24);
                                if (hours == 0)
                                    strToDisplay = String.format("%02d:%02d", minutes, seconds);
                                else
                                    strToDisplay = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                                mSegmentTimer.setText(strToDisplay);
                                mFragment.setSegmentTimeLeft(segmentTimeLeft);
                            }
                        });
                    }

                    public void onFinish() {
                        mSegmentTimer.setText("00:00");

                    }
                };
                cTimer.start();
                setTargetPower(targetPower, true);

            }
        });
    }

    public void pauseWorkout() {
        shouldPause = true;
        cTimer.cancel();
        this.interrupt();
    }

    public void resumeWorkout() {
        shouldPause = false;
        synchronized(this) {
            notify();
        }
    }

    public void stopWorkout() {
        shouldPause = false;
        cTimer.cancel();
        this.interrupt();
    }

    public  void nextSegment() {
        skip_segment = true;
        cTimer.cancel();
        this.interrupt();
    }


    private void showToast(final String message){
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setTargetPower(final int power, boolean setLastTargetPower){
        mTrainerController.setTargetPower(new BigDecimal(power));
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTargetPower.setText("" + power);
            }
        });
        mFragment.setLastSegment(power);
        if (setLastTargetPower == true)
            lastTargetPower = power;
    }

}
