package cn.rjgc.cameraapi_study.utils;

import android.content.Context;
import android.view.OrientationEventListener;

/**
 * Created by Don on 2017/4/19.
 * 监测屏幕放置角度
 */

public class CheckScreenOritation extends OrientationEventListener {

    public int rotation = 0;
    public CheckScreenOritation(Context context) {
        super(context);
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if(orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return;  //手机平放时，检测不到有效的角度
        }
        // orientation的范围是0～359
        // 屏幕左边在顶部的时候 orientation = 90;
        // 屏幕顶部在底部的时候 orientation = 180;
        // 屏幕右边在底部的时候 orientation = 270;
        // 正常情况默认i = 0;
        if (45 <= orientation && orientation < 135) {
            rotation = 180;
        } else if (135 <= orientation && orientation < 225) {
            rotation = 270;
        } else if (225 <= orientation && orientation < 315) {
            rotation = 0;
        } else {
            rotation = 90;
        }
    }
}
