package com.shivang.camera;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import com.jsibbold.zoomage.ZoomageView;

import java.io.File;

/**
 * Created by SHIVVVV on 5/6/2018.
 */

public class DisplayActivity extends AppCompatActivity implements View.OnClickListener{

    private ZoomageView myZoomageView;
    private int filePos;
    private int currentPos;
    private ImageView leftImage;
    private ImageView rightImage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_image);

        myZoomageView = (ZoomageView) findViewById(R.id.myZoomageView);
        leftImage = (ImageView) findViewById(R.id.leftImage);
        rightImage = (ImageView) findViewById(R.id.rightImage);

        leftImage.setOnClickListener(this);
        rightImage.setOnClickListener(this);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        filePos = preferences.getInt("fileLastPos", 1);

        Log.v("File Path", filePos + "");
        displayImage(filePos);
        currentPos = filePos;
    }

    @Override
    public void onClick(View v) {

        int ID = v.getId();

        if (ID == R.id.leftImage) {
            if (currentPos+1 <= filePos) {
                currentPos++;
                slideOutRight(myZoomageView);
                displayImage(currentPos);
                slideInLeft(myZoomageView);
            }
        } else if(ID == R.id.rightImage) {
            if (currentPos-1 > 0) {
                currentPos--;
                slideOutLeft(myZoomageView);
                displayImage(currentPos);
                slideInRight(myZoomageView);
            }
        }

    }

    /**
     * Read and set image from Storage
     * @param pos
     */
    public void displayImage(int pos) {
        String path = getExternalFilesDir(null).getPath();
        File file = new File(path, "camImage_" + pos +".jpg"); // the File to save , append increasing numeric counter to prevent files from getting overwritten.
        Bitmap bMap = BitmapFactory.decodeFile(file.getAbsolutePath());
        myZoomageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        myZoomageView.setImageBitmap(bMap);
    }

    // slide the view from below itself to the current position
    public void slideOutLeft(View view){
        view.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(
                0,                 // fromXDelta
                -view.getWidth(),                 // toXDelta
                0,  // fromYDelta
                0);                // toYDelta
        animate.setDuration(200);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    // slide the view from its current position to below itself
    public void slideInRight(View view){
        TranslateAnimation animate = new TranslateAnimation(
                view.getWidth(),                 // fromXDelta
                0,                 // toXDelta
                0,                 // fromYDelta
                0); // toYDelta
        animate.setDuration(200);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    // slide the view from below itself to the current position
    public void slideInLeft(View view){
        view.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(
                -view.getWidth(),                 // fromXDelta
                0,                 // toXDelta
                0,  // fromYDelta
                0);                // toYDelta
        animate.setDuration(200);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    // slide the view from its current position to below itself
    public void slideOutRight(View view){
        TranslateAnimation animate = new TranslateAnimation(
                0,                 // fromXDelta
                view.getWidth(),                 // toXDelta
                0,                 // fromYDelta
                0); // toYDelta
        animate.setDuration(200);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }
}
