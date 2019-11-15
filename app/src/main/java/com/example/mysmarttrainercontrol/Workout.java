package com.example.mysmarttrainercontrol;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Workout {

    List<Integer> mDurations;
    List<Integer> mPowers;

    public Workout() {
        mDurations = new ArrayList<>();
        mPowers = new ArrayList<>();
    }

    public void addDurationPower(int duration, int power) {
        mDurations.add(duration);
        mPowers.add(power);
    }

    public List<Integer> getDuration(){
        return mDurations;
    }

    public List<Integer> getPowers() {
        return mPowers;
    }

    public int getPower(int i) {
       return mPowers.get(i);
    }

    public int getDuration(int i) {
        return mDurations.get(i);
    }

    public int getSize(){
        return mPowers.size();
    }

}
