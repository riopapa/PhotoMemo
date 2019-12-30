package com.urrecliner.phovomemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static com.urrecliner.phovomemo.Vars.bitMapCamera;
import static com.urrecliner.phovomemo.Vars.cameraOrientation;
import static com.urrecliner.phovomemo.Vars.latitude;
import static com.urrecliner.phovomemo.Vars.longitude;
import static com.urrecliner.phovomemo.Vars.mActivity;
import static com.urrecliner.phovomemo.Vars.mContext;
import static com.urrecliner.phovomemo.Vars.nowTime;
import static com.urrecliner.phovomemo.Vars.phoneMake;
import static com.urrecliner.phovomemo.Vars.phoneModel;
import static com.urrecliner.phovomemo.Vars.phonePrefix;
import static com.urrecliner.phovomemo.Vars.strAddress;
import static com.urrecliner.phovomemo.Vars.strPlace;
import static com.urrecliner.phovomemo.Vars.strVoice;
import static com.urrecliner.phovomemo.Vars.utils;


class BuildImage {

    private String logID = "buildCameraImage";

    void makeOutMap() {

        String timeStamp;
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("`yy/MM/dd HH:mm", Locale.ENGLISH);
        timeStamp = dateTimeFormat.format(nowTime);
        int width = bitMapCamera.getWidth();
        int height = bitMapCamera.getHeight();

        if (cameraOrientation == 6 && width > height)
            bitMapCamera = utils.rotateBitMap(bitMapCamera, 90);
        if (cameraOrientation == 1 && width < height)
            bitMapCamera = utils.rotateBitMap(bitMapCamera, 90);
        if (cameraOrientation == 3)
            bitMapCamera = utils.rotateBitMap(bitMapCamera, 180);
//
//        width = bitMapCamera.getWidth();
//        height = bitMapCamera.getHeight();

        final SimpleDateFormat imgDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA);
        String outFileName = imgDateFormat.format(nowTime);

        File newFile = new File(utils.getPublicCameraDirectory(), phonePrefix + outFileName + ".jpg");
        writeCameraFile(bitMapCamera, newFile);
        setNewFileExif(newFile);
        mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(newFile)));

        Bitmap mergedMap = addSignature2Bitmaps(bitMapCamera, timeStamp);
        nowTime += 50;
        String outText = (strVoice.length() > 10) ? strVoice.substring(0, 10) : strVoice;
        String outFileName2 = imgDateFormat.format(nowTime) + "_" + strPlace + " (" + outText +")";
        File newFile2 = new File(utils.getPublicCameraDirectory(), phonePrefix + outFileName2 + " _ha.jpg");
        writeCameraFile(mergedMap, newFile2);
        setNewFileExif(newFile);
        mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(newFile2)));
    }

    static final private SimpleDateFormat sdfHourMinSec = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.ENGLISH);

    private void setNewFileExif(File fileHa) {
        ExifInterface exifHa;
        try {
            exifHa = new ExifInterface(fileHa.getAbsolutePath());
            exifHa.setAttribute(ExifInterface.TAG_MAKE, phoneMake);
            exifHa.setAttribute(ExifInterface.TAG_MODEL, phoneModel);
            exifHa.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertGPS(latitude));
            exifHa.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitudeRefGPS(latitude));
            exifHa.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertGPS(longitude));
            exifHa.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitudeRefGPS(longitude));
            exifHa.setAttribute(ExifInterface.TAG_ORIENTATION, "1");
            exifHa.setAttribute(ExifInterface.TAG_DATETIME, sdfHourMinSec.format(nowTime));
            exifHa.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "PhoVoMemo by riopapa");
            exifHa.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String latitudeRefGPS(double latitude) {
        return latitude < 0.0d ? "S" : "N";
    }

    private String longitudeRefGPS(double longitude) {
        return longitude < 0.0d ? "W" : "E";
    }

    private static String convertGPS(double latitude) {
        latitude = Math.abs(latitude);
        int degree = (int) latitude;
        latitude *= 60;
        latitude -= (degree * 60.0d);
        int minute = (int) latitude;
        latitude *= 60;
        latitude -= (minute * 60.0d);
        int second = (int) (latitude * 10000.d);
        return degree + "/1," + minute + "/1," + second + "/10000";
    }

    private Bitmap addSignature2Bitmaps(Bitmap photoMap, String dateTime) {

        int width = photoMap.getWidth();
        int height = photoMap.getHeight();
        Bitmap newMap = Bitmap.createBitmap(width, height, photoMap.getConfig());
        Canvas canvas = new Canvas(newMap);
        canvas.drawBitmap(photoMap, 0f, 0f, null);
        int fontSize = (cameraOrientation == 1) ? height / 16 : width / 16;
        int xPos = (cameraOrientation == 1) ? width / 5 : width * 3 / 10;
        int yPos = height / 12;
        drawTextOnCanvas(canvas, dateTime, fontSize, xPos, yPos);

        int sigSize = (width > height) ? height / 6 : width / 4;
        Bitmap sigMap = BitmapFactory.decodeResource(mContext.getResources(), R.raw.signature_yellow_min);
        sigMap = Bitmap.createScaledBitmap(sigMap, sigSize, sigSize, false);
        xPos = width - sigSize - width / 20;
        yPos = height / 20;
        Paint sigPaint = new Paint();
        sigPaint.setAlpha(60);
        canvas.drawBitmap(sigMap, xPos, yPos, sigPaint);

        if (strPlace.length() == 0) strPlace = "_";
        if (strVoice.length() == 0) strVoice = "_";
        fontSize = (cameraOrientation == 1) ? width / 52 : width / 36;
        xPos = width / 2;
        yPos = height - fontSize - fontSize;
        yPos = drawTextOnCanvas(canvas, strAddress, fontSize, xPos, yPos);
        fontSize = fontSize * 15 / 10;
        yPos -= fontSize + fontSize / 4;
        yPos = drawTextOnCanvas(canvas, strPlace, fontSize, xPos, yPos);
        fontSize = fontSize * 15 / 10;
        yPos -= fontSize + fontSize / 4;
        yPos  = drawTextOnCanvas(canvas, strVoice, fontSize, xPos, yPos);
        return newMap;
    }

    private int drawTextOnCanvas(Canvas canvas, String text, int fontSize, int xPos, int yPos) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(fontSize);
//        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.XOR));
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        int cWidth = canvas.getWidth() * 3 / 4;
        float tWidth = paint.measureText(text);
        int pos;
        int d = fontSize / 16;
        if (tWidth > cWidth) {
            utils.log("size","cWidth:"+cWidth+" tWidth:"+tWidth);
            int length = text.length() / 2;
            for (pos = length; pos < text.length(); pos++)
                if (text.substring(pos,pos+1).equals(" "))
                    break;
            String text1 = text.substring(pos);
            drawTextMultiple(canvas, text1, xPos, yPos, d, paint);
            yPos -= fontSize + fontSize / 6;
            text1 = text.substring(0, pos);
            drawTextMultiple(canvas, text1, xPos, yPos, d, paint);
            return yPos;
        }
        else
            drawTextMultiple(canvas, text, xPos, yPos, d, paint);
        return yPos;
    }

    private void drawTextMultiple (Canvas canvas, String text, int xPos, int yPos, int d, Paint paint) {
        paint.setColor(Color.BLACK);
        canvas.drawText(text, xPos - d, yPos - d, paint);
        canvas.drawText(text, xPos + d, yPos - d, paint);
        canvas.drawText(text, xPos - d, yPos + d, paint);
        canvas.drawText(text, xPos + d, yPos + d, paint);
        canvas.drawText(text, xPos - d, yPos, paint);
        canvas.drawText(text, xPos + d, yPos, paint);
        canvas.drawText(text, xPos, yPos - d, paint);
        canvas.drawText(text, xPos, yPos + d, paint);
        paint.setColor(ContextCompat.getColor(mContext, R.color.foreColor));
        canvas.drawText(text, xPos, yPos, paint);
    }

    private void writeCameraFile(Bitmap bitmap, File file) {
        FileOutputStream os;
        try {
            os = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.close();
            mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        } catch (IOException e) {
            Log.e("ioException", e.toString());
            Toast.makeText(mContext, e.toString(),Toast.LENGTH_LONG).show();
        }
    }

}
