package cn.rjgc.cameraapi_study;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.QRCodeDecoder;

public class QRActivity extends AppCompatActivity implements QRCodeView.Delegate {
    private static final int REQUEST_CODE_CHOOSE_QRCODE_FROM_GALLERY = 666;
    private QRCodeView qrCodeView;

    private String TAG = "QRActivity";
    private ImgSpotTask spotTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);
        qrCodeView = (QRCodeView) findViewById(R.id.zxingview);
        qrCodeView.setDelegate(this);
    }

    public void startSpot(View view) {
        qrCodeView.startSpot();
    }

    public void stopSpot(View view) {
        qrCodeView.stopSpot();
    }

    public void showBox(View view) {
        qrCodeView.showScanRect();
    }
    public void hideBox(View view) {
        qrCodeView.hiddenScanRect();
    }
    public void spotBarcode(View view) {
        qrCodeView.changeToScanBarcodeStyle();
    }
    public void openFlash(View view) {
        qrCodeView.openFlashlight();
    }
    public void closeFlash(View view) {
        qrCodeView.closeFlashlight();
    }

    public void spotQR(View view) {
        qrCodeView.changeToScanQRCodeStyle();
    }

    //自动识别图片
    public void spotImg(View view) {
        Intent i = new Intent(Intent.ACTION_PICK, android.
                provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_CODE_CHOOSE_QRCODE_FROM_GALLERY);
    }

    //振动
    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }

    @Override
    protected void onStart() {
        super.onStart();
        qrCodeView.startCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();
        qrCodeView.stopCamera();
        if (spotTask != null) {
            spotTask.cancel(true);
            spotTask = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        qrCodeView.onDestroy();
        Log.e(TAG, "onDestroy: ");
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
        vibrate();
        qrCodeView.startSpot();
    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        Toast.makeText(this, "扫描失败", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        qrCodeView.showScanRect();
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_CHOOSE_QRCODE_FROM_GALLERY) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            final String picturePath = cursor.getString(columnIndex);
            cursor.close();

            spotTask = new ImgSpotTask(getApplicationContext(), picturePath);
            spotTask.execute((Void) null);
        }
    }

    //  static内部类+Application 的 Context+弱引用(此处没用到)  可以解决
    // 内部类AsyncTask 导致 Activity 内存 泄漏的问题
    //处理方式可参考https://github.com/Don-Lee/LearningNotes/blob/master/Part1/Android/Android%E5%86%85%E5%AD%98%E6%B3%84%E6%BC%8F%E6%80%BB%E7%BB%93.md
    private static class ImgSpotTask extends AsyncTask<Void, Void, String> {
        private Context mContext;
        private String imgPath;

        public ImgSpotTask(Context context, String imgPath) {
            this.mContext = context;
            this.imgPath = imgPath;
        }
        @Override
        protected String doInBackground(Void... voids) {
            return QRCodeDecoder.syncDecodeQRCode(imgPath);
        }

        @Override
        protected void onPostExecute(String result) {
            if (TextUtils.isEmpty(result)) {
                Toast.makeText(mContext, "未发现二维码", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
            }
        }
    }


}
