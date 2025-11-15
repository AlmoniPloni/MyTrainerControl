package com.example.mytrainercontrol;

import android.app.Activity;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mytrainercontrol.fitnessequipment.TrainerController;

import java.math.BigDecimal;

import static java.lang.Math.min;

public class WorkoutExecThread extends Thread {

    private static final String TAG = "PowerControl WET";

    Workout mWorkout;
    Fragment_ManualPowerControl mFragment;
    Context mContext;
    Activity mActivity;
    TrainerController mTrainerController;
    //TextView mTargetPower;
    //TextView mSegmentDescription;
    CountDownTimer cTimer;
    //TextView mSegmentTimer;
    //TextView mNextSegmentTimer;
    int segmentTimeLeft;
    boolean segmentFinished;
    int lastTargetPower;
    boolean shouldPause;
    boolean segmentCompleted;
    boolean skip_segment;
    boolean go_back;
    boolean move_line;
    int startSegment;
    String segmentDescription;
    String nextSegmentDescription;
    int nextSegmentStart;
    //boolean isForward;

    // to avoid quick two taps
    private long lastNavAtMs = 0;
    private boolean navDebounce() {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastNavAtMs < 300) return false; // 300ms guard
        lastNavAtMs = now;
        return true;
    }



    public WorkoutExecThread(Fragment_ManualPowerControl fragmentFEC){

        mActivity = fragmentFEC.getActivity();
        mFragment = fragmentFEC;
        mContext = fragmentFEC.getContext();
        mWorkout = fragmentFEC.workout;
        mTrainerController = fragmentFEC.trainerController;
        //mTargetPower = fragmentFEC.targetPower;
        //mSegmentDescription = fragmentFEC.segmentDescription;

        //mSegmentTimer = fragmentFEC.segmentTimer;
        //mNextSegmentTimer = fragmentFEC.nextSegmentTimer;
        shouldPause = false;
        startSegment = fragmentFEC.lastSegment;
        segmentTimeLeft = fragmentFEC.segmentTimeLeft;
        nextSegmentStart = 0;
        go_back = false;
        //isForward = true;

    }

    public void run() {
        int i = startSegment;
        workout_loop: while (i < mWorkout.getSize()) {
            mFragment.setLastSegment(i);
            lastTargetPower = mWorkout.getPower(i);
            if (segmentFinished)
                segmentTimeLeft = mWorkout.getDuration(i)+1;
            else
                // this branch when we resume previously unfinished workout workout (don't really happen often)
                segmentTimeLeft =  min(segmentTimeLeft, mWorkout.getDuration(i)+1);

            if (i == mWorkout.getSize()-1)
                showToast("Workout (well) DONE !");


            // Set moving linе to start from the beginning of segment
            final int ind = i;
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (ind == 0) {
                        mFragment.setLine(0);
                    }
                    else {
                        //int direction = isForward ? +1 : -1;
                        //nextSegmentStart += direction * mWorkout.getDuration(ind);
                        int nextSegmentStart = 0;
                        for (int j = 0; j < ind; j++) {
                            nextSegmentStart += mWorkout.getDuration(j);
                        }
                        mFragment.setLine(nextSegmentStart);
                    }
                }
            });

            int segmentDuration = segmentTimeLeft;
            segmentDescription = mWorkout.getDescription(i);
            segmentCompleted = false;
            skip_segment = false;
            Log.d(TAG, "Segment " + i + " setting Target Power: " + lastTargetPower + " W for " + segmentTimeLeft + " seconds");
            setTargetPower(i, lastTargetPower, true, segmentDescription);
            setNextSegment(i+1);
            while (segmentTimeLeft > 0) {
                move_line= (i != mWorkout.getSize() - 1);
                startTimer(segmentTimeLeft, move_line);
                try {
                    Log.d(TAG, "Going to sleep for " + segmentTimeLeft + " seconds");
                    segmentFinished = false;
                    sleep(segmentTimeLeft * 1000);
                    //segmentTimeLeft = Integer.MAX_VALUE;
                    segmentFinished = true;
                } catch (InterruptedException e) {
                    Log.d(TAG, "Workout Interrupted");
                    if (shouldPause == true) {
                        try {
                            Log.d(TAG, "Pausing workout at Segment #" + i + " current power = " + lastTargetPower + " W left time " + segmentTimeLeft + " sec. to the end of current segment");
                            setTargetPower(i, 100, false, "Easy Spin");
                            synchronized (this) {
                                wait();
                            }
                            Log.d(TAG, "Continue workout");
                            if (segmentTimeLeft > 0)
                                setTargetPower(i, lastTargetPower, true, mWorkout.getDescription(i));

                        } catch (InterruptedException e1) {
                            Log.d(TAG, "Wait Interrupted");
                            Log.d(TAG, "Stopping workout");
                            break workout_loop;
                        }
                    } else {
                        if (skip_segment == true) {
                            Log.d(TAG, "Skipping segment");
                            skip_segment = false;
                            if (i == mWorkout.getSize()-1) {
                                // do nothing
                                Log.d(TAG, "Will park on the last segment and do nothing");
                                showToast("Do nothing !");
                                i = i-1; // should prevent from existing the outer loop !
                                // segmentTimeLeft += 1;
                                // don't update segmentTimeLeft, because we want to continue from the sama as before
                            }
                            else {
                                segmentTimeLeft = Integer.MAX_VALUE;
                            }
                            //showToast("Skipping current segment!");
                            //isForward = true;
                            break;
                        }
                        else if (go_back == true){
                            Log.d(TAG, "Going back one segment: i = " + i);
                            go_back = false;
                            if (i >= 1) {
                                //segmentTimeLeft = mWorkout.getDuration(i - 1); // Reset segment time
                                i = i - 2;  // No need for Math.max, since i >= 1
                            } else {
                                Log.d(TAG, "Already at first segment, can't go back");
                                //segmentTimeLeft = mWorkout.getDuration(0);
                                i = -1; // So i++ makes it 0 again
                            }
                            //isForward = false;
                            segmentTimeLeft = Integer.MAX_VALUE;
                            break;
                            //continue;

                        }
                        else {
                            Log.d(TAG, "Stopping workout");
                            showToast("Workout Stopped !");
                            setTargetPower(i, 100, true, "Easy Spin");
                            updateTimer("00:00");
                            return;
                        }
                    }
                }
            }
            //segmentTimeLeft = Integer.MAX_VALUE;
            Log.d(TAG, "Segment finished");
            segmentFinished = true;
            i++;
        }

        showToast("Workout DONE!");
        setTargetPower(0, 100, true, "Easy Spin");
        updateTimer("00:00");
        mFragment.setWorkoutInProgress(false);
        Thread.currentThread().interrupt();
    }

    void startTimer(final int countDownSeconds, final boolean move_line) {
        Log.d(TAG, "Starting Timer for " + countDownSeconds + " sec." );
        final int countDownInterval = 1000;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                cTimer = new CountDownTimer((countDownSeconds) * 1000, countDownInterval) {
                    //int currentTime = (countDownSeconds)*1000;

                    public void onTick(final long millisUntilFinished) {
                        segmentTimeLeft = (int) millisUntilFinished/1000;
                        //Log.d(TAG, "onTick: " + millisUntilFinished + " millis until finished " + segmentTimeLeft + " sec. until finished" );
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
                                mFragment.setSegmentTimer(strToDisplay);
                                mFragment.setSegmentTimeLeft(segmentTimeLeft);
                                if (move_line)
                                    mFragment.setLine(-1);
                            }
                        });
                    }
                    public void onFinish() {
                        segmentTimeLeft = 0;        // <- add this
                        updateTimer("00:00");
                    }
                };
                cTimer.start();

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

        if (!navDebounce()) return;

        skip_segment = true;
        cTimer.cancel();
        this.interrupt();
    }

    public void previousSegment() {

        if (!navDebounce()) return;

        go_back = true;
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

    private void updateTimer(final String strTime){
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFragment.setSegmentTimer(strTime);
            }
        });

    }

    private void setTargetPower(final int segment, final int power, boolean setLastTargetPower, final String description){
        int attempts = 1;
        boolean res = false;
        while (res == false && attempts <= 5) {
            Log.d(TAG, "Setting Target Power " + power + " attempt #" + attempts);

            res = mTrainerController.setTargetPower(new BigDecimal(power));
            if (res == false) {
                attempts++;
                continue;
            }

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFragment.setTargetPower("" + power);
                    mFragment.setSegmentDescription(description);
                }
            });
            mFragment.setLastSegment(segment);
            if (setLastTargetPower == true)
                lastTargetPower = power;
            break;
        }
        final int num_attempts = attempts;
        if (res == false){
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                   Toast.makeText(mContext, "After " + num_attempts + " attempts Target Power " + power + " was not set. ", Toast.LENGTH_SHORT);
                }
            });

        }
    }

    private void setNextSegment(final int i){

        if (i < mWorkout.getSize()){
            final String strToDisplay;
            int timeSeconds = mWorkout.getDuration(i);
            int seconds = (int)((timeSeconds) % 60) ;
            int minutes = (int)((timeSeconds/60) % 60);
            int hours   = (int)((timeSeconds/(60*60)) % 24);
            if (hours == 0)
                strToDisplay = String.format("%02d:%02d", minutes, seconds);
            else
                strToDisplay = String.format("%02d:%02d:%02d", hours, minutes, seconds);

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFragment.setNextSegmentTimer(strToDisplay);
                    mFragment.setNextSegmentDescription(mWorkout.getDescription(i));
                    mFragment.setNextTargetPower(""+mWorkout.getPower(i));
                }
            });

        }
        else {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFragment.setNextSegmentTimer("00:00");
                    mFragment.setNextSegmentDescription("Easy Spin");
                    mFragment.setNextTargetPower("100");
                }
            });

        }
    }

}
