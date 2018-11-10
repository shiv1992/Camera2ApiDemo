package com.shivang.camera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;

public class StartupActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageView click;
    private ImageView cameraChange;
    private ImageView previewImage;
    private TextureView textureView;
    TextureView.SurfaceTextureListener textureListener = null;
    private CameraModule mCameraModule;
    private RotateAnimation rotate;
    private int camId = 0;
    private View gridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_startup);

        click = (ImageView) findViewById(R.id.click);
        click.setOnClickListener(this);

        cameraChange = (ImageView) findViewById(R.id.changeCamera);

        textureView = (TextureView) findViewById(R.id.textureView);
        assert textureView != null;

        textureView.setSurfaceTextureListener(textureListener);

        mCameraModule = new CameraModule();
        mCameraModule.setmContext(getApplicationContext());
        mCameraModule.setActivity(this);
        cameraChange.setOnClickListener(this);
        mCameraModule.setTextureView(textureView);

        gridView = findViewById(R.id.grid);

        previewImage = (ImageView) findViewById(R.id.previewImage);
        previewImage.setOnClickListener(this);
        CameraModule.previewImage = previewImage;

        rotate = new RotateAnimation(0, 180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(250);
        rotate.setInterpolator(new LinearInterpolator());

        textureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                //open your camera here
                mCameraModule.openCamera(camId);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                // Transform you image captured size according to the surface width and height
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        };

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int filePos = preferences.getInt("fileLastPos", 0);

        if (filePos > 0) {
            setPreview(filePos);
        }
    }

    /**
     * Set Preview Image
     * @param pos
     */
    public void setPreview(int pos) {
        String path = getExternalFilesDir(null).getPath();
        File file = new File(path, "camImage_" + pos +".jpg"); // the File to save , append increasing numeric counter to prevent files from getting overwritten.
        Bitmap bMap = BitmapFactory.decodeFile(file.getAbsolutePath());
        previewImage.setImageBitmap(bMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("TAG", "onResume");
        mCameraModule.startBackgroundThread();
        if (textureView.isAvailable()) {
           mCameraModule.openCamera(camId);
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.e("TAG", "onPause");
        mCameraModule.closeCamera();
        mCameraModule.stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    }

    @Override
    public void onClick(View v) {
        int ID = v.getId();

        if (ID == R.id.changeCamera) {
            int h = textureView.getHeight();
            int w = textureView.getWidth();
            textureView.setLayoutParams(new RelativeLayout.LayoutParams(0,0));

            cameraChange.startAnimation(rotate);
            if (camId == 1) {
                camId = 0;
               // gridView.setVisibility(View.INVISIBLE);
            } else {
                camId = 1;
               // gridView.setVisibility(View.VISIBLE);
            }
            mCameraModule.closeCamera();
            mCameraModule.openCamera(camId);
            textureView.setLayoutParams(new RelativeLayout.LayoutParams(w,h));
        } else if(ID == R.id.click) {
            mCameraModule.captureCamera();
        } else if (ID == R.id.previewImage) {
            Intent intent = new Intent(this, DisplayActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        Log.v("TOUCH", "ACTIVE");
        return mCameraModule.onTouch(event);
    }
}
