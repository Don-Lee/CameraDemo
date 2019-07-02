package cn.rjgc.cameraapi_study;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.qrcode.QRCodeReader;

import java.lang.ref.WeakReference;

import cn.bingoogolapple.qrcode.core.BGAQRCodeUtil;
import cn.bingoogolapple.qrcode.zxing.QRCodeDecoder;
import cn.bingoogolapple.qrcode.zxing.QRCodeEncoder;

public class GenerateQRActivity extends AppCompatActivity {
    private static final String TAG = "GenerateQRActivity";
    private static final int TYPE_CN = 1;
    private static final int TYPE_EN = 2;
    private static final int TYPE_CN_LOGO = 3;
    private static final int TYPE_EN_LOGO = 4;

    private static ImageView mChineseIv;
    private static ImageView mEnglishIv;
    private static ImageView mChineseLogoIv;
    private static ImageView mEnglishLogoIv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        initView();
        createQRCode();
    }
    private void initView() {
        mChineseIv = (ImageView) findViewById(R.id.iv_chinese);
        mChineseLogoIv = (ImageView) findViewById(R.id.iv_chinese_logo);
        mEnglishIv = (ImageView) findViewById(R.id.iv_english);
        mEnglishLogoIv = (ImageView) findViewById(R.id.iv_english_logo);
    }
    private void createQRCode() {
        createChineseQRCode();
        createEnglishQRCode();
        createChineseQRCodeWithLogo();
        createEnglishQRCodeWithLogo();
    }
    private void createChineseQRCode() {
        GenerateQRTask task = new GenerateQRTask(getApplicationContext(), "唐老大", TYPE_CN);
        task.execute();
    }
    private void createEnglishQRCode() {
        GenerateQRTask task = new GenerateQRTask(getApplicationContext(), "Don", TYPE_EN);
        task.execute();
    }
    private void createChineseQRCodeWithLogo() {
        GenerateQRTask task = new GenerateQRTask(getApplicationContext(), "我不做大哥好多年", TYPE_CN_LOGO);
        task.execute();
    }
    private void createEnglishQRCodeWithLogo() {
        GenerateQRTask task = new GenerateQRTask(getApplicationContext(), "Hello World", TYPE_EN_LOGO);
        task.execute();
    }

    public void decodeChinese(View view) {
        mChineseIv.setDrawingCacheEnabled(true);
        Bitmap bitmap = mChineseIv.getDrawingCache();
        decode(bitmap, "解析中文二维码失败");
    }

    public void decodeEnglish(View view) {
        mEnglishIv.setDrawingCacheEnabled(true);
        Bitmap bitmap = mEnglishIv.getDrawingCache();
        decode(bitmap, "解析英文二维码失败");
    }

    public void decodeChineseLogo(View v) {
        mChineseLogoIv.setDrawingCacheEnabled(true);
        Bitmap bitmap = mChineseLogoIv.getDrawingCache();
        decode(bitmap, "解析带logo的中文二维码失败");
    }

    public void decodeEnglishLogo(View v) {
        mEnglishLogoIv.setDrawingCacheEnabled(true);
        Bitmap bitmap = mEnglishLogoIv.getDrawingCache();
        decode(bitmap, "解析带logo的英文二维码失败");
    }

    public void decodeISBN(View v) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.test_isbn);
        decode(bitmap, "解析ISBN失败");
    }

    private void decode(final Bitmap bitmap, final String errorTip) {
        /*
        这里为了偷懒，就没有处理匿名 AsyncTask 内部类导致 Activity 泄漏的问题
        请开发在使用时自行处理匿名内部类导致Activity内存泄漏的问题，处理方式可参考 https://github.com/GeniusVJR/LearningNotes/blob/master/Part1/Android/Android%E5%86%85%E5%AD%98%E6%B3%84%E6%BC%8F%E6%80%BB%E7%BB%93.md
         */
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                return QRCodeDecoder.syncDecodeQRCode(bitmap);
            }

            @Override
            protected void onPostExecute(String result) {
                if (TextUtils.isEmpty(result)) {
                    Toast.makeText(GenerateQRActivity.this, errorTip, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(GenerateQRActivity.this, result, Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }


    private static class GenerateQRTask extends AsyncTask<String ,String ,Bitmap> {
        private Context mContext;
        private String qrContent;
        private int qrType;
        public GenerateQRTask(Context context,String content,int type) {
            mContext = context;
            qrContent = content;
            qrType = type;
        }
        @Override
        protected Bitmap doInBackground(String... strings) {
            Log.e(TAG, "doInBackground: "+strings.toString()+"--"+strings.length );
            Bitmap bitmap = null;
            if (qrType == TYPE_CN || qrType == TYPE_EN) {
                bitmap = QRCodeEncoder.syncEncodeQRCode(qrContent, BGAQRCodeUtil.dp2px(mContext, 150));
            } else {
                Bitmap logoBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.logo);
                bitmap = QRCodeEncoder.syncEncodeQRCode(qrContent,
                        BGAQRCodeUtil.dp2px(mContext, 150), Color.BLACK, Color.WHITE, logoBitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                switch (qrType) {
                    case TYPE_CN:
                        mChineseIv.setImageBitmap(bitmap);
                        break;
                    case TYPE_EN:
                        mEnglishIv.setImageBitmap(bitmap);
                        break;
                    case TYPE_CN_LOGO:
                        mChineseLogoIv.setImageBitmap(bitmap);
                    case TYPE_EN_LOGO:
                        mEnglishLogoIv.setImageBitmap(bitmap);
                    default:
                        break;
                }

            } else {
                Toast.makeText(mContext, "生成二维码失败"+qrType, Toast.LENGTH_SHORT).show();
            }
        }
    }

}
