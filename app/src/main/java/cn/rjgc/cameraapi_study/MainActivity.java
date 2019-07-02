package cn.rjgc.cameraapi_study;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.rjgc.cameraapi_study.utils.MyApplication;

/**
 * 调用系统相机拍照
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private Uri fileUri;
    private String mCurrentImagePath;

    private ImageView mImageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Check storage permissions       6.0以上版本必须如此申请权限
        MyApplication.verifyStoragePermissions(MainActivity.this);

        initView();

    }

    public void customCapture(View view) {
        Intent cameraIntent = new Intent(MainActivity.this, CustomCameraActivity.class);
        startActivity(cameraIntent);
    }

    public void systemCapture(View view) {
        //create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //resolveActivity返回能处理该intent的第一个actiivty，只要不为null，intent就是安全的
        if (intent.resolveActivity(getPackageManager()) != null) {

            fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);//create a file to save the image
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);//set the image file name

            //start the image capture Intent
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }
    }

    public void QR(View view) {
        Intent qrIntent = new Intent(MainActivity.this, QRActivity.class);
        startActivity(qrIntent);
    }

    public void generateQR(View view) {
        Intent qrIntent = new Intent(MainActivity.this, GenerateQRActivity.class);
        startActivity(qrIntent);
    }

    private void initView() {
        mImageView = (ImageView) findViewById(R.id.id_img);
    }

    /**
     *  Receiving camera intent result
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    galleryAddPic();
                    setPic();
                    //Image captured and saved to fileUri specified in the intent
                    Toast.makeText(this, "拍照成功" , Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage() );
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "User cancelled the image capture", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Image capture failed", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**
     * Create a file uri for saving an image
     * @param type
     * @return
     */
    private Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    private File getOutputMediaFile(int type) {
        //To be safe,you should check that the SDCard is mounted
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //This location works best if you want the created images to be shared
            //between applications and persist after your app has been uninstalled.
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "MyCameraApp");

            //Create the storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
//                Log.e(TAG,mediaStorageDir.canWrite() + ";");
                if (!mediaStorageDir.mkdirs()) {
                    Log.d(TAG, "failed to create directory");
                    return null;
                }

            }

            //Create a media file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File mediaFile=null;
            if (type == MEDIA_TYPE_IMAGE) {
                //方法一
                /*mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "IMG_" + timeStamp + ".jpg");*/
                //方法二
                String imageFileName = "JPEG_" + timeStamp;
                try {
                    mediaFile = File.createTempFile(
                            imageFileName,
                            ".jpg",
                            mediaStorageDir
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                return null;
            }

            mCurrentImagePath = mediaFile.getAbsolutePath();
            return mediaFile;
        } else {
            Toast.makeText(MainActivity.this, "没有SDCard", Toast.LENGTH_SHORT).show();
            return null;
        }

    }

    //把图片添加到相册
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentImagePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private int targetW;
    private int targetH;
    private void setPic() {

        // Get the dimensions of the View
        ViewTreeObserver viewTreeObserver = mImageView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < 16) {
                    mImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                targetW = mImageView.getWidth();
                targetH = mImageView.getHeight();
            }
        });


        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentImagePath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int scaleFactor = 0;

        try {
            // Determine how much to scale down the image
            scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        } catch (Exception e) {
            if(BuildConfig.DEBUG){
                Log.e(TAG, "targetW 为0");
            }
        }

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentImagePath, bmOptions);
        mImageView.setImageBitmap(bitmap);
    }
}
