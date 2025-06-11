package com.example.mytrainercontrol;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class WorkoutReader {

    private static final String TAG = "PowerControl WR";

    //String csvFile;
    String line = "";
    String cvsSplitBy = ",";
    BufferedReader br = null;
    //List<Pair<String, String>> workout = new ArrayList<>();
    Context mContext;
    Fragment mFragment;
    Activity mActivity;
    Workout workout;
    File csvFile;

    public WorkoutReader(Context context, Activity activity, Fragment fragment) {
        mContext = context;
        mActivity = activity;
        mFragment = fragment;
        //Toast.makeText(mContext, "Creating WorkoutReader", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Creating Workout Reader");
        workout = new Workout();
        //csvFile = "C/Users/Almoni/Documents/workout_example.csv";
    }

    @AfterPermissionGranted(123)
    public void chooseWorkout(){
            String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE};
            if (EasyPermissions.hasPermissions(mActivity, perms)) {
                //Toast.makeText(this, "Opening camera", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "READ_EXTERNAL_STORAGE permission is granted");

                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.addCategory(Intent.CATEGORY_OPENABLE);

                Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath()
                        + "/Workouts/");
                chooseFile.setDataAndType(uri, "text/comma-separated-values");
                //chooseFile.setType("*/*");
                chooseFile = Intent.createChooser(chooseFile, "Choose a Workout");
                mFragment.startActivityForResult(chooseFile, 333);

            } else {
                EasyPermissions.requestPermissions(mActivity, "We need permissions because this and that",
                        123, perms);
            }
    }

    public Workout setWorkout_old(Uri csvFileURI){
        //csvFile = new File(getRealPathFromURI(csvFileURI));
        try {
            //br = new BufferedReader(new InputStreamReader(mActivity.getAssets().open("19112019.csv")));
            //InputStream targetStream = new FileInputStream(csvFile);
            InputStream targetStream = mActivity.getContentResolver().openInputStream(csvFileURI);

            br = new BufferedReader(new InputStreamReader(targetStream));
            //br = ContentResolver.openInputStream(csvFileURI);
            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] row = line.split(cvsSplitBy);

                //Pair <String, String> time_power = new Pair<String, String>(row[0], row[1]);
                //Toast.makeText(mContext, "Reading line" + time_power.first + " : " + time_power.second, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Reading line: " + Arrays.toString(row));

                //convert to seconds
                int duration = (int) (60* Double.parseDouble(row[0]));
                // if it was already in seconds, revert back
                if (row[1].contains("sec") == true)
                    duration /= 60;

                int power = Integer.parseInt(row[2]);
                String description = "";
                if (row.length >= 4)
                    description = row[3];
                workout.addSegment(duration, power, description);
            }
        } catch (FileNotFoundException e) {
            Toast.makeText(mContext, "File not found: " + csvFile, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return workout;
    }

    public Workout setWorkout(Uri csvFileURI) {
        BufferedReader br = null;
        String line;
        String cvsSplitBy = ",";
        Workout workout = new Workout(); // Ensure workout is initialized

        try {
            InputStream targetStream = mActivity.getContentResolver().openInputStream(csvFileURI);
            br = new BufferedReader(new InputStreamReader(targetStream));

            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                String[] row = line.split(cvsSplitBy);

                // Skip if fewer than 3 columns
                if (row.length < 3) {
                    Log.w(TAG, "Skipping line " + lineNumber + ": insufficient columns: " + Arrays.toString(row));
                    continue;
                }

                try {
                    // Trim each field
                    String durationStr = row[0].trim();
                    String powerStr = row[2].trim();
                    String description = (row.length >= 4) ? row[3].trim() : "";

                    // Convert to seconds (assumes minutes unless row[1] contains "sec")
                    int duration = (int) (60 * Double.parseDouble(durationStr));
                    if (row[1].toLowerCase().contains("sec")) {
                        duration /= 60;
                    }

                    int power = Integer.parseInt(powerStr);
                    workout.addSegment(duration, power, description);

                    Log.d(TAG, "Parsed line " + lineNumber + ": duration=" + duration + ", power=" + power + ", description=" + description);

                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    Log.w(TAG, "Skipping line " + lineNumber + ": parse error -> " + Arrays.toString(row), e);
                }
            }

        } catch (FileNotFoundException e) {
            Toast.makeText(mContext, "File not found.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "File not found", e);
        } catch (IOException e) {
            Log.e(TAG, "IO error reading file", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing reader", e);
                }
            }
        }

        return workout;
    }



    //public Workout readWorkout() {
    //    chooseWorkout();
        //csvFile = new File(Environment.getExternalStorageDirectory() + "/Workouts/workout_example.csv");

    //return workout;
    //}
}


