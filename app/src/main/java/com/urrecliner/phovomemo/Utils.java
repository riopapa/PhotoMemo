package com.urrecliner.phovomemo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.urrecliner.phovomemo.Vars.latitude;
import static com.urrecliner.phovomemo.Vars.longitude;
import static com.urrecliner.phovomemo.Vars.mContext;
import static com.urrecliner.phovomemo.Vars.nowTime;
import static com.urrecliner.phovomemo.Vars.outFileName;
import static com.urrecliner.phovomemo.Vars.phoneMake;
import static com.urrecliner.phovomemo.Vars.phoneModel;
import static com.urrecliner.phovomemo.Vars.phonePrefix;
import static com.urrecliner.phovomemo.Vars.utils;
import static com.urrecliner.phovomemo.Vars.xPixel;
import static com.urrecliner.phovomemo.Vars.yPixel;

class Utils {

    final private String PREFIX = "log_";
    final private String logID = "utils";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd", Locale.US);
    private final SimpleDateFormat dateTimeLogFormat = new SimpleDateFormat("MM-dd HH.mm.ss sss", Locale.US);

    void log(String tag, String text) {
        StackTraceElement[] traces;
        traces = Thread.currentThread().getStackTrace();
        String log = traceName(traces[5].getMethodName()) + traceName(traces[4].getMethodName()) + traceClassName(traces[3].getClassName())+"> "+traces[3].getMethodName() + "#" + traces[3].getLineNumber() + " {"+ tag + "} " + text;
        Log.w(tag , log);
        append2file(dateTimeLogFormat.format(new Date())+" " +log);
    }

    private String traceName (String s) {
        if (s.equals("performResume") || s.equals("performCreate") || s.equals("callActivityOnResume") || s.equals("access$1200")
                || s.equals("access$000") || s.equals("handleReceiver"))
            return "";
        else
            return s + "> ";
    }
    private String traceClassName(String s) {
        return s.substring(s.lastIndexOf(".")+1);
    }

    void logE(String tag, String text) {
        StackTraceElement[] traces;
        traces = Thread.currentThread().getStackTrace();
        String log = traceName(traces[5].getMethodName()) + traceName(traces[4].getMethodName()) + traceClassName(traces[3].getClassName())+"> "+traces[3].getMethodName() + "#" + traces[3].getLineNumber() + " {"+ tag + "} " + text;
        Log.e("<" + tag + ">" , log);
        append2file(dateTimeLogFormat.format(new Date())+" : " +log);
    }

    private void append2file(String textLine) {

        File directory = getPackageDirectory();
        BufferedWriter bw = null;
        FileWriter fw = null;
        String fullName = directory.toString() + "/" + PREFIX + dateFormat.format(new Date())+".txt";
        try {
            File file = new File(fullName);
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    logE("createFile", " Error");
                }
            }
            String outText = "\n"+textLine+"\n";
            fw = new FileWriter(file.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);
            bw.write(outText);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) bw.close();
                if (fw != null) fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private File getPackageDirectory() {
        File directory = new File(Environment.getExternalStorageDirectory(), utils.getAppLabel(mContext));
        try {
            if (!directory.exists()) {
                if(directory.mkdirs()) {
                    Log.e("mkdirs","Failed "+directory);
                }
            }
        } catch (Exception e) {
            Log.e("creating Directory error", directory.toString() + "_" + e.toString());
        }
        return directory;
    }

    File getPublicCameraDirectory() {
        return new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM),"/Camera");
    }

    Bitmap rotateBitMap(Bitmap bitmap, int degree) {
            Matrix matrix = new Matrix();
            matrix.postRotate(degree);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }

    File captureMapScreen(View view) {

        utils.log(logID, "capture screen ///");

        if (xPixel < yPixel) {
            int t = xPixel; xPixel = yPixel; yPixel = t;
        }
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap screenBitmap = view.getDrawingCache();
        assert screenBitmap != null;
        int width = screenBitmap.getWidth();
        int height = screenBitmap.getHeight();
        if (xPixel < width && yPixel <= height)
            screenBitmap = Bitmap.createBitmap(screenBitmap, 0, 0, xPixel, yPixel);  // remove right actiobar white area

        String fileName = phonePrefix + outFileName + "__ha.jpg";

        return bitmap2File (fileName, screenBitmap);
    }

    File bitmap2File (String fileName, Bitmap outMap) {
        File directory = getPublicCameraDirectory();
        File file = new File(directory, fileName);
        FileOutputStream os;
        try {
            os = new FileOutputStream(file);
            outMap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.close();
        } catch (IOException e) {
            utils.logE(logID,"Create ioException\n"+e);
            return null;
        }
        return file;
    }
    void setPhotoTag(File file) {
        SimpleDateFormat jpegTimeFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.ENGLISH);

        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, "1");
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,convertGpsToDMS(latitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF,(latitude > 0) ? "N":"S");
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertGpsToDMS(longitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF,(longitude > 0) ? "E":"W");
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "Created by riopapa");
            exif.setAttribute(ExifInterface.TAG_MAKE, phoneMake);
            exif.setAttribute(ExifInterface.TAG_MODEL, phoneModel);
            exif.setAttribute(ExifInterface.TAG_DATETIME, jpegTimeFormat.format(nowTime));
            exif.saveAttributes();
        } catch (IOException e) {
            utils.log(logID,"EXIF ioException\n"+e.toString());
        }
    }

    synchronized private static String convertGpsToDMS(double latitude) {
        StringBuilder sb = new StringBuilder(30);
        latitude=Math.abs(latitude);
        int degree = (int) latitude;
        latitude *= 60;
        latitude -= (degree * 60.0d);
        int minute = (int) latitude;
        latitude *= 60;
        latitude -= (minute * 60.0d);
        float second = (float) latitude * 1000;

        return degree+"/1,"+minute+"/1,"+second+"/1000";
    }

    void deleteOldLogFiles() {

        String oldDate = PREFIX + dateFormat.format(System.currentTimeMillis() - 3*24*60*60*1000L);
        File packageDirectory = getPackageDirectory();
        File[] files = getFilesList(packageDirectory);
        Collator myCollator = Collator.getInstance();
        for (File file : files) {
            String shortFileName = file.getName();
            if (myCollator.compare(shortFileName, oldDate) < 0) {
                if (file.delete())
                    Log.e("file","Delete Error "+file);
            }
        }
    }

    private  String getAppLabel(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = packageManager.getApplicationInfo(context.getApplicationInfo().packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : "Unknown");
    }

    private File[] getFilesList(File fullPath) {
        return fullPath.listFiles();
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
//        bm.recycle();
        return resizedBitmap;
    }

}
