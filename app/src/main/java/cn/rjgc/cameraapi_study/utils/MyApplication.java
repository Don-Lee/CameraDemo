package cn.rjgc.cameraapi_study.utils;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import cn.rjgc.cameraapi_study.MainActivity;

/**
 * Created by Don on 2017/4/12.
 */

public class MyApplication extends Application {
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    public static void verifyStoragePermissions(Activity activity) {
        //Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            //we don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity,PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }


    protected static MyApplication myApplication;
    private final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        myApplication = this;
        Thread.setDefaultUncaughtExceptionHandler(restartHandler);// 程序崩溃时触发线程  以下用来捕获程序崩溃异常
    }


    // 创建服务用于捕获崩溃异常
    private Thread.UncaughtExceptionHandler restartHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            writeFile(throwable.getMessage()+":"+throwable.toString()+":"+thread.toString()+"--"+thread.getName());

            Log.e(TAG, throwable.getMessage());
            restartApp();//发生崩溃异常时,重启应用
        }
    };
    public void restartApp() {
        Intent intent = new Intent(myApplication, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        myApplication.startActivity(intent);
        android.os.Process.killProcess(android.os.Process.myPid());//结束进程之前可以把你程序的注销或者退出代码放在这段代码之前
    }

    private void writeFile(String s){
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            String filePath = getExternalFilesDir(Environment.DIRECTORY_DCIM).getPath();//照片保存路径
            File file = new File(filePath,"66.txt");

            try {

                FileOutputStream fos = new FileOutputStream(file);

                ObjectOutputStream oos = new ObjectOutputStream(fos);

                oos.writeObject(s);// 写入

                fos.close(); // 关闭输出流

            } catch (FileNotFoundException e) {

                // TODO Auto-generated catch block

                e.printStackTrace();

            } catch (IOException e) {

                // TODO Auto-generated catch block

                e.printStackTrace();

            }
        }
    }
}
