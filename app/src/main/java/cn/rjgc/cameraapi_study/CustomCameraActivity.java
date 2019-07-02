package cn.rjgc.cameraapi_study;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.rjgc.cameraapi_study.customview.CameraPreview;
import cn.rjgc.cameraapi_study.customview.CustomDialog;
import cn.rjgc.cameraapi_study.utils.CheckScreenOritation;


/**
 * 自定义相机类
 * TODO 缩放，水印功能,有问题
 *
 * */
public class CustomCameraActivity extends AppCompatActivity implements View.OnTouchListener {

    private final String TAG = "CustomCameraActivity";
    private final int MEDIA_TYPE_IMAGE = 1;

    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private int mCameraId = 0;//0：后置；1:前置
    private String mPicCurrentPath;
    private CheckScreenOritation checkScreenOritation;
    private FrameLayout previewFL;
    private View focusView;

    private CustomDialog customDialog;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏标题栏(ActionBar)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        //隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_custom_camera);

        checkScreenOritation = new CheckScreenOritation(CustomCameraActivity.this);
        checkScreenOritation.enable();

        previewFL = (FrameLayout) findViewById(R.id.camera_preview);
        previewFL.setOnTouchListener(this);
        focusView = findViewById(R.id.focus_rect);

        customDialog = new CustomDialog(this,R.style.CustomDialog);

        setWaterAlpha();
    }

    private void setWaterAlpha() {
        View v = findViewById(R.id.watermark);
        v.getBackground().setAlpha(100);
    }

    private void init() {
        if (mCamera == null) {
            //Create an instance of camera
            mCamera = getCameraInstance();
            Log.e(TAG, "camera=null");
        }

        if (mCameraPreview == null) {
            //Create our preview view and set it as the content of our activity.
            mCameraPreview = new CameraPreview(this, mCamera,mCameraId);
            Log.e(TAG, "CameraPreview=null");
        }
        previewFL.removeAllViews();
        previewFL.addView(mCameraPreview);
    }

    public void capture(View view) {
        showDialog();
        mCamera.takePicture(null,null,mPicture);
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            Bitmap bitmap;
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Bitmap bitmapRotate;
            if (checkScreenOritation.rotation != 0) {// 解决保存的图片被旋转的问题
                Matrix matrix = new Matrix();
                matrix.reset();
                if (mCameraId == 1) {
                    checkScreenOritation.rotation = 360 - checkScreenOritation.rotation;
                }
                matrix.postRotate(checkScreenOritation.rotation);
                bitmapRotate = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                        bitmap.getHeight(), matrix, true);
                bitmap = bitmapRotate;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                bos.flush();
                bos.close();
                /*fos.write(data);
                fos.close();*/
                galleryAddPicture();
                finish();
                dismissDialog();
                Intent intent = new Intent(CustomCameraActivity.this, PreviewPictureActivity.class);
                intent.putExtra("path", mPicCurrentPath);
                startActivity(intent);


//                Toast.makeText(CustomCameraActivity.this, "success", Toast.LENGTH_SHORT).show();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    private File getOutputMediaFile(int type) {
        File mediaFile = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File mediaDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "customDir");

            if (!mediaDir.exists()) {
                if (!mediaDir.mkdirs()) {
                    Log.e(TAG, "存储文件夹创建失败，请检查权限或其他问题");
                }
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            if (type == MEDIA_TYPE_IMAGE) {
                mediaFile = new File(mediaDir.getPath() + File.separator +
                        "IMG_" + timeStamp + ".jpg");
            } else {
                //创建其他类型的文件
            }
        }else {
            Toast.makeText(CustomCameraActivity.this, "没有SDCard", Toast.LENGTH_SHORT).show();
        }

        mPicCurrentPath = mediaFile.getAbsolutePath();
        return mediaFile;
    }

    //check if this device has a camera
    private boolean checkCameraHardware() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            //this device has a camera
            return true;
        } else {
            //no camera on this device
            return false;
        }
    }

    //A safe way to get an instance of the camera object.
    public Camera getCameraInstance() {
        Camera camera = null;
        try {
            if (checkCameraHardware()) {
                camera = Camera.open(mCameraId);//attempt to get a Camera instance
            }else {
                Toast.makeText(this, "相机不存在", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, "Camera is not available（in use or no permission）", Toast.LENGTH_SHORT).show();
        }
        return camera;
    }


    //将图片添加到相册
    private void galleryAddPicture() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mPicCurrentPath);
        Uri uri = Uri.fromFile(f);
        mediaScanIntent.setData(uri);
        this.sendBroadcast(mediaScanIntent);
    }

    /**
     * 显示进度条对话框
     */
    private void showDialog() {
        customDialog.setCancelable(false);
        customDialog.setCanceledOnTouchOutside(false);
        customDialog.setMsg("处理中...");
        customDialog.show();
    }

    /**
     * 隐藏进度条对话框
     */
    private void dismissDialog() {
        if (customDialog != null) {
            customDialog.dismiss();
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();//release the camera for other applications
            mCamera = null;
        }
        if (mCameraPreview != null) {
            mCameraPreview = null;
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
        checkScreenOritation.disable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkScreenOritation.enable();
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
        checkScreenOritation.disable();
    }


    @Override
    public boolean onTouch(View view, MotionEvent event) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mCameraPreview.pointFocus(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(
                focusView.getLayoutParams());
        layout.setMargins((int) event.getX() - 60, (int) event.getY() - 60, 0, 0);

        focusView.setLayoutParams(layout);
        focusView.setVisibility(View.VISIBLE);

        /*
                参数解释：
                    第一个参数：X轴水平缩放起始位置的大小（fromX）。1代表正常大小
                    第二个参数：X轴水平缩放完了之后（toX）的大小，0代表完全消失了
                    第三个参数：Y轴垂直缩放起始时的大小（fromY）
                    第四个参数：Y轴垂直缩放结束后的大小（toY）
                    第五个参数：pivotXType为动画在X轴相对于物件位置类型
                    第六个参数：pivotXValue为动画相对于物件的X坐标的开始位置
                    第七个参数：pivotXType为动画在Y轴相对于物件位置类型
                    第八个参数：pivotYValue为动画相对于物件的Y坐标的开始位置

                   （第五个参数，第六个参数），（第七个参数,第八个参数）是用来指定缩放的中心点
                    0.5f代表从中心缩放
             */
        ScaleAnimation sa = new ScaleAnimation(3f, 1f, 3f, 1f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        sa.setDuration(800);
        focusView.startAnimation(sa);
        handler.postAtTime(new Runnable() {
            @Override
            public void run() {
                focusView.setVisibility(View.INVISIBLE);
            }
        }, 800);
        return false;
    }

    //闪光灯
    public void turnFlash(View view) {
        mCameraPreview.turnLight(view);
    }

    //切换摄像头
    public void switchCamera(View view) {
        mCameraId = (mCameraId + 1) % Camera.getNumberOfCameras();
        releaseCamera();
        init();
    }
}
