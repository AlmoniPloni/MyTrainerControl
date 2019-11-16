package com.example.mytrainercontrol;

//import android.support.v4.app.FragmentTransaction;
//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
        import android.view.WindowManager;
        import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity  {

    private static final String TAG = "PowerControl MA";
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Bundle bundle = new Bundle();
        //bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
        //bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        //bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        // Create a new Fragment to be placed in the Main activity layout
        Fragment_Options fragmentOptions = new Fragment_Options();

        // In case this activity was started with special instructions from an
        // Intent, pass the Intent's extras to the fragment as arguments
        // fragmentOptions.setArguments(getIntent().getExtras());

        // Add the fragment to the 'main_activity' FrameLayout
        getSupportFragmentManager().beginTransaction()
                .add(R.id.main_activity, fragmentOptions).commit();

    }

    @Override
    public void onBackPressed() {
        Fragment_ManualPowerControl myFragment = (Fragment_ManualPowerControl) getSupportFragmentManager().findFragmentByTag("ManualPowerControlFragment");
        Log.d(TAG, "Back Pressed");
        if (myFragment != null && myFragment.isVisible()) {
            if (myFragment.isWorkoutInProgress() == true){
                Log.d(TAG, "Workout Fragment is Visible");
                Toast.makeText(this, "Workout in progress. Finish workout first.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        super.onBackPressed();
    }
}
