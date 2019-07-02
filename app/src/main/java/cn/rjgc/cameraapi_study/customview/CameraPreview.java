package cn.rjgc.cameraapi_study.customview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.rjgc.cameraapi_study.R;

/**
 * Created by Don on 2017/4/13.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback,Camera.AutoFocusCallback {

    private final String TAG = "CameraPreview";

    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private Context mContext;
    private boolean isSupportAutoFocus = false;
    private int mCameraId;
    private int mScreenWidth;
    private int mScreenHeight;
    private Camera.Parameters mCameraParameters;

    public CameraPreview(Context context, Camera camera, int cameraId) {
        super(context);
        mCamera = camera;
        mContext = context;
        mCameraId = cameraId;

        isSupportAutoFocus = context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_AUTOFOCUS);

        getScreenMetrix();
        initHolder();
    }

    private void initHolder() {
        //Install a SurfaceHolder.Callback so we get notified when the
        //underlying surface is created and destroyed
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);

        //deprecated setting, but required on Android version prior to 3.0
        //surfaceview不维护自己的缓冲区，等待屏幕渲染引擎将内容推送到用户面前
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.setKeepScreenOn(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //the Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) {
//            if (BuildConfig.DEBUG) {
//                Log.e(TAG, "Error setting camera preview: " + e.getMessage());
//            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.


        if (mSurfaceHolder.getSurface() == null) {
            //preview surface does not exist
            mSurfaceHolder = getHolder();
//            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        mCamera.setDisplayOrientation(getCameraDisplayOrientation(mCameraId, mCamera));
        setCameraParams();

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }


    //旋转预览图片
    public int getCameraDisplayOrientation(int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private void setCameraParams() {
        mCameraParameters = mCamera.getParameters();

        if (mCameraParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        //check that metering areas are supported
//        if (parameters.getMaxNumMeteringAreas() > 0) {
//            List<Camera.Area> meteringAreas = new ArrayList<>();
//            Rect areaRect1 = new Rect(-100, -100, 100, 100);    // specify an area in center of image
//            meteringAreas.add(new Camera.Area(areaRect1, 600)); // set weight to 60%
//            Rect areaRect2 = new Rect(800, -1000, 1000, -800);  // specify an area in upper right of image
//            meteringAreas.add(new Camera.Area(areaRect2, 400)); // set weight to 40%
//            parameters.setMeteringAreas(meteringAreas);
//        }
        List<Camera.Size> previewSize = mCameraParameters.getSupportedPreviewSizes();
        Camera.Size pz = getOptimalSize(previewSize, mScreenWidth, mScreenHeight);
        mCameraParameters.setPreviewSize(pz.width, pz.height);
        mCameraParameters.setJpegQuality(100);
        mCamera.setParameters(mCameraParameters);
    }

    /**
     * 寻找一个合适的分辨率，避免图片被拉伸
     * 需要注意的就是这么一行：double targetRatio = (double) h / w;这里的代码是h／w，
     * 但是在官方的例子里面是w／h。因为在官方的demo里相机是横向的，但是我自定义的相机是竖向的（AndroidMainfest.xml里面设置的）
     * @param sizes
     * @param w
     * @param h
     * @return
     */
    private static Camera.Size getOptimalSize(@NonNull List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    //获取手机屏幕宽高
    private void getScreenMetrix() {
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
    }

    //定点对焦
    public void pointFocus(MotionEvent event) {
        mCamera.cancelAutoFocus();
        if (mCameraParameters == null) {
            mCameraParameters = mCamera.getParameters();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            focusOnTouch(event);
        }else {
            mCamera.autoFocus(this);
        }

        mCamera.setParameters(mCameraParameters);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void focusOnTouch(MotionEvent event) {
        Rect focusRect = calculateTapArea(event.getRawX(), event.getRawY(), 1f);
        Rect meteringRect = calculateTapArea(event.getRawX(), event.getRawY(), 1.5f);

        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

        if (parameters.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
            focusAreas.add(new Camera.Area(focusRect, 1000));

            parameters.setFocusAreas(focusAreas);
        }

        if (parameters.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
            meteringAreas.add(new Camera.Area(meteringRect, 1000));

            parameters.setMeteringAreas(meteringAreas);
        }
        mCamera.setParameters(parameters);
        mCamera.autoFocus(this);
    }

    /**
     * Convert touch position x:y to {@link Camera.Area} position -1000:-1000 to 1000:1000.
     */
    private Rect calculateTapArea(float x, float y, float coefficient) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();

        int centerX = (int) (x / mCameraParameters.getPreviewSize().width * 2000 - 1000);
        int centerY = (int) (y / mCameraParameters.getPreviewSize().height * 2000 - 1000);

        int left = clamp(centerX - areaSize / 2, -1000, 1000);
        int right = clamp(left + areaSize, -1000, 1000);
        int top = clamp(centerY - areaSize / 2, -1000, 1000);
        int bottom = clamp(top + areaSize, -1000, 1000);

        return new Rect(left, top, right, bottom);
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }
    @Override
    public void onAutoFocus(boolean b, Camera camera) {

    }

    /**
     * 闪光灯开关 开->关->自动
     */
    public void turnLight(View view){
        if (mCamera == null || mCamera.getParameters()==null
                || mCamera.getParameters().getSupportedFlashModes()==null) {
            return;
        }
        ImageView img = (ImageView) view;
        String flashMode = mCameraParameters.getFlashMode();
        List<String> supportedModes = mCameraParameters.getSupportedFlashModes();
        if (Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)
                && supportedModes.contains(Camera.Parameters.FLASH_MODE_ON)) {//关闭状态
            mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            img.setImageDrawable(getResources().getDrawable(R.mipmap.camera_flash_on));
        } else if (Camera.Parameters.FLASH_MODE_ON.equals(flashMode)) {//开启状态
            if (supportedModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                img.setImageDrawable(getResources().getDrawable(R.mipmap.camera_flash_auto));
            } else if (supportedModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                img.setImageDrawable(getResources().getDrawable(R.mipmap.camera_flash_off));
            }
        } else if (Camera.Parameters.FLASH_MODE_AUTO.equals(flashMode) &&
                supportedModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
            mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            img.setImageDrawable(getResources().getDrawable(R.mipmap.camera_flash_off));
        }
        mCamera.setParameters(mCameraParameters);
    }
}
