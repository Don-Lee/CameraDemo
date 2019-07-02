package cn.rjgc.cameraapi_study.customview;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.zip.Inflater;

import cn.rjgc.cameraapi_study.R;

/**
 * Created by Don on 2017/5/26.
 */

public class CustomDialog extends ProgressDialog {
    private TextView mProgressBarText;
    private String msg;

    public CustomDialog(Context context) {
        super(context);
    }

    public CustomDialog(Context context, int theme) {
        super(context,theme);
    }

    /***
     * 当此页面被加载到前端时才会执行此方法
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_dialog);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(params);

        initView();
    }

    private void initView() {
        mProgressBarText = (TextView) findViewById(R.id.progress_bar_text_cd);
        mProgressBarText.setText(msg);
        mProgressBarText.setVisibility(TextUtils.isEmpty(msg) ? View.GONE : View.VISIBLE);
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

}
