package com.shivang.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceManager;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by SHIVVVV on 5/2/2018.
 */

public class CameraModule {

    public void setTextureView(TextureView textureView) {
        this.textureView = textureView;
    }

    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraPreviewSession;
    protected CameraCaptureSession cameraCapSession;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private CameraManager manager;
    private Size captureSize = null;
    private ImageReader jpegImageReader;
    public static ImageView previewImage;
    private int currentVal = 0;
    private int hBelow = 100;
    private CameraCharacteristics characteristics;

    //Zooming
    protected float fingerSpacing = 0;
    protected float zoomLevel = 1f;
    protected float maximumZoomLevel;
    protected Rect zoom;

    public void setActivity(Activity activity) {
        mActivity = activity;
    }

    private Activity mActivity;
    TextureView.SurfaceTextureListener textureListener = null;

    public void setmContext(Context mContext) {
        this.mContext = mContext;
    }

    private Context mContext;

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e("TAG", "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    /**
     * Create Camera Preview
     */
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraPreviewSession = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Open Camera
     * @param val
     */
    public void openCamera(int val) {
        manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        Log.e("TAG", "is camera open");
        try {
            cameraId = manager.getCameraIdList()[val];
            currentVal = val;
            String[] str = manager.getCameraIdList();

            for (String tp : str) {
                Log.v("CAMERA ID", tp);
            }

            characteristics = manager.getCameraCharacteristics(cameraId);
            maximumZoomLevel = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);


            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;

            int w = textureView.getWidth();
            int h = textureView.getHeight();
            Log.v("TEXT", w + " " + h);

            Size[] sz = map.getOutputSizes(SurfaceTexture.class);
            captureSize = sz[0];

            for (Size s : sz) {

                Log.v("SIZE", s.getWidth() + " - " + s.getHeight());
                if ( s.getWidth() <= h && s.getHeight() <= w) {

                    //imageDimension = s;
                   // break;
                }
            }

            if (val == 0) {
                imageDimension = map.getOutputSizes(SurfaceTexture.class)[7];
            } else {
                imageDimension = map.getOutputSizes(SurfaceTexture.class)[3];
            }


            Log.v("CAMERA", imageDimension.getWidth() + " : " + imageDimension.getHeight());
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e("TAG", "openCamera X");
    }

    /**
     * Update Preview
     */
    public void updatePreview() {
        if(null == cameraDevice) {
            Log.e("TAG", "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);

        try {
            cameraPreviewSession.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            image = reader.acquireLatestImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            //Arrays.rever
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            Matrix matrix = new Matrix();

            if (currentVal == 0) {
                matrix.postRotate(90);
            } else {
                matrix.postRotate(270);
            }


            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,captureSize.getWidth(),captureSize.getHeight(),true);
            final Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap , 0, 0, scaledBitmap .getWidth(), scaledBitmap .getHeight(), matrix, true);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (currentVal == 1) {
                        previewImage.setRotationY(180.0f);
                    }
                    previewImage.setImageBitmap(rotatedBitmap);
                }
            });

            // Assume block needs to be inside a Try/Catch block.
            String path = mContext.getExternalFilesDir(null).getPath();
            OutputStream fOut;

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            int filePos = preferences.getInt("fileLastPos", 0);
            filePos++;

            File file = new File(path, "camImage_" + filePos + ".jpg"); // the File to save , append increasing numeric counter to prevent files from getting overwritten.
            Log.v("PATH", file.getAbsolutePath());
            try {
                fOut = new FileOutputStream(file);
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
                fOut.flush(); // Not really required
                fOut.close(); // do not forget to close the stream
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("fileLastPos",filePos);
            editor.apply();

            Log.v("IMAGE CAPTURE", bytes.toString());
        }
    };

    /**
     * Capture Camera
     */
    public void captureCamera() {
        try {
            cameraPreviewSession.close();

            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);
            jpegImageReader = ImageReader.newInstance(captureSize.getWidth(), captureSize.getHeight(), ImageFormat.JPEG, 2);
            jpegImageReader.setOnImageAvailableListener(readerListener, mBackgroundHandler);

            captureRequestBuilder.addTarget(jpegImageReader.getSurface());
            cameraDevice.createCaptureSession(Arrays.asList(surface, jpegImageReader.getSurface()), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                    // When the session is ready, we start displaying the preview.
                    cameraCapSession = cameraCaptureSession;
                    try {
                        cameraCapSession.capture(captureRequestBuilder.build(), null,mBackgroundHandler );
                        createCameraPreview();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close Camera
     */
    public void closeCamera() {

        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }

        if (null != cameraDevice) {
            startBackgroundThread();
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    /**
     * Start backgroung thread
     */
    public void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stop background
     */
    public void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * On Touch Event
     * @param event
     * @return
     */
    public boolean onTouch(MotionEvent event) {
        try {
            Rect rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            if (rect == null) return false;
            float currentFingerSpacing;

            if (event.getPointerCount() == 2) { //Multi touch.
                currentFingerSpacing = getFingerSpacing(event);
                Log.v("TOUCH", currentFingerSpacing + " FF");
                float delta = 0.05f; //Control this value to control the zooming sensibility
                if (fingerSpacing != 0) {
                    if (currentFingerSpacing > fingerSpacing) { //Don't over zoom-in
                        if ((maximumZoomLevel - zoomLevel) <= delta) {
                            delta = maximumZoomLevel - zoomLevel;
                        }
                        zoomLevel = zoomLevel + delta;
                    } else if (currentFingerSpacing < fingerSpacing){ //Don't over zoom-out
                        if ((zoomLevel - delta) < 1f) {
                            delta = zoomLevel - 1f;
                        }
                        zoomLevel = zoomLevel - delta;
                    }
                    float ratio = (float) 1 / zoomLevel; //This ratio is the ratio of cropped Rect to Camera's original(Maximum) Rect
                    //croppedWidth and croppedHeight are the pixels cropped away, not pixels after cropped
                    int croppedWidth = rect.width() - Math.round((float)rect.width() * ratio);
                    int croppedHeight = rect.height() - Math.round((float)rect.height() * ratio);
                    //Finally, zoom represents the zoomed visible area
                    zoom = new Rect(croppedWidth/2, croppedHeight/2,
                            rect.width() - croppedWidth/2, rect.height() - croppedHeight/2);
                    Log.v("TOUCH ZOOM", zoom.toString() + " FF");
                    captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                }
                fingerSpacing = currentFingerSpacing;
            } else { //Single touch point, needs to return true in order to detect one more touch point
                return true;
            }
            cameraCapSession.setRepeatingRequest(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    if (zoom != null) {
                        captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                    }
                }
            }, null);
            return true;
        } catch (final Exception e) {
            //Error handling up to you
            return true;
        }
    }

    /**
     * Finger Spacing
     * @param event
     * @return
     */
    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

}
