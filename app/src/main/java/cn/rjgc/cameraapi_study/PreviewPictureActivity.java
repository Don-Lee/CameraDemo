package cn.rjgc.cameraapi_study;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/*此处的view转bitmap有问题，在华为手机正常，Samsung Galaxy s7 edge 有问题*/
public class PreviewPictureActivity extends AppCompatActivity {

    String imgPath;

    private ImageView mImageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏标题栏
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        //隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_preview_picture);

        imgPath = getIntent().getStringExtra("path");

        initView();
        initDatas();
    }

    private void initDatas() {
        Bitmap bitmap = loadBitmap(imgPath);
        mImageView.setImageBitmap(bitmap);

        //长按截取屏幕内容
        mImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                View v = findViewById(R.id.test);

                Bitmap bm = convertViewToBitmap(v);
                test(bm);
                Toast.makeText(PreviewPictureActivity.this, "图片已保存到相册", Toast.LENGTH_LONG).show();

                return false;
            }
        });



    }

    private void initView() {
        mImageView = (ImageView) findViewById(R.id.preview_picture);
    }
    /**
     * 从给定路径加载图片
     */
    public static Bitmap loadBitmap(String imgpath) {
        return BitmapFactory.decodeFile(imgpath);
    }


    //view 转bitmap
    private Bitmap convertViewToBitmap(View view) {

        Bitmap bitmap = null;
        try {

            if (view != null) {
                /*
                //此段代码在samaung s7 edge 上有问题
                view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

                view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
                view.setDrawingCacheEnabled(true);
                view.buildDrawingCache();

                bitmap = view.getDrawingCache();*/

                bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888 );
                Canvas c = new Canvas(bitmap);
                c.translate(-view.getScrollX(), -view.getScrollY());
                view.draw(c);
            }

        } catch (Exception e) {
            Log.e("PreviewPictureActivity", e.getMessage());
        }
        return bitmap;
    }

    private String mPicCurrentPath;
    private void test(Bitmap bitmap) {
        File pictureFile = getOutputMediaFile(1);
        if (pictureFile == null) {
            Log.d("TAG", "Error creating media file, check storage permissions: ");
            return;
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getOutputMediaFile(int type) {
        File mediaFile = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File mediaDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "customDir");

            if (!mediaDir.exists()) {
                if (!mediaDir.mkdirs()) {
                    Log.e("TAG", "存储文件夹创建失败，请检查权限或其他问题");
                }
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            if (type == 1) {
                mediaFile = new File(mediaDir.getPath() + File.separator +
                        "IMG_" + timeStamp + ".jpg");
            } else {
                //创建其他类型的文件
            }
        }else {
            Toast.makeText(PreviewPictureActivity.this, "没有SDCard", Toast.LENGTH_SHORT).show();
        }

        mPicCurrentPath = mediaFile.getAbsolutePath();
        return mediaFile;
    }

    //将图片添加到相册
    private void galleryAddPicture() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mPicCurrentPath);
        Uri uri = Uri.fromFile(f);
        mediaScanIntent.setData(uri);
        this.sendBroadcast(mediaScanIntent);
    }
}
