package com.example.mysmarttrainercontrol;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class WorkoutReader {

    private static final String TAG = "PowerControl WR";

    String csvFile;
    String line = "";
    String cvsSplitBy = ",";
    BufferedReader br = null;
    //List<Pair<String, String>> workout = new ArrayList<>();
    Context mContext;
    Activity mActivity;
    Workout workout;

    public WorkoutReader(Context context, Activity activity) {
        mContext = context;
        mActivity = activity;
        //Toast.makeText(mContext, "Creating WorkoutReader", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Creating Workout Reader");
        workout = new Workout();
        //csvFile = "C/Users/Almoni/Documents/workout_example.csv";
    }

    public Workout readWorkout() {
        File csvFile = new File(Environment.getExternalStorageDirectory() + "/Workouts/workout_example.csv");
        try {
            //br = new BufferedReader(new FileReader(csvFile));

            br = new BufferedReader(new InputStreamReader(mActivity.getAssets().open("workout_example.csv")));
            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] row = line.split(cvsSplitBy);

                Pair <String, String> time_power = new Pair<String, String>(row[0], row[1]);
                //Toast.makeText(mContext, "Reading line" + time_power.first + " : " + time_power.second, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Reading line: " + time_power.first + " - " + time_power.second);

                //workout.add(time_power);
                int duration = (int) (60* Double.parseDouble(row[0]));
                int power = Integer.parseInt(row[1]);
                workout.addDurationPower(duration, power);
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
}


