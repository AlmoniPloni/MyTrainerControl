package com.example.mytrainercontrol;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Workout {

    List<Integer> mDurations;
    List<Integer> mPowers;
    List<String> mDescriptions;
    int mTotalDuraction;

    public Workout() {
        mDurations = new ArrayList<>();
        mPowers = new ArrayList<>();
        mDescriptions = new ArrayList<>();
        mTotalDuraction = 0;
    }

    public void addSegment(int duration, int power, String description) {
        mDurations.add(duration);
        mPowers.add(power);
        mDescriptions.add(description);
        mTotalDuraction += duration;
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

    public int getTotalDuraction(){
        return mTotalDuraction;
    }

    public String getDescription(int i) {return mDescriptions.get(i); }

    public int getSize(){
        return mPowers.size();
    }

}
