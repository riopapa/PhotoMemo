package com.urrecliner.photomemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static com.urrecliner.photomemo.Vars.bitMapCamera;
import static com.urrecliner.photomemo.Vars.cameraOrientation;
import static com.urrecliner.photomemo.Vars.latitude;
import static com.urrecliner.photomemo.Vars.longitude;
import static com.urrecliner.photomemo.Vars.mActivity;
import static com.urrecliner.photomemo.Vars.mContext;
import static com.urrecliner.photomemo.Vars.phoneMake;
import static com.urrecliner.photomemo.Vars.phoneModel;
import static com.urrecliner.photomemo.Vars.phonePrefix;
import static com.urrecliner.photomemo.Vars.signatureMap;
import static com.urrecliner.photomemo.Vars.strAddress;
import static com.urrecliner.photomemo.Vars.strPlace;
import static com.urrecliner.photomemo.Vars.strPosition;
import static com.urrecliner.photomemo.Vars.strVoice;
import static com.urrecliner.photomemo.Vars.utils;


class BuildBitMap {

    private String logID = "buildCameraImage";
    private long nowTime;
    private static final SimpleDateFormat sdfExif = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.KOREA);
    private final SimpleDateFormat sdfFileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA);

    void makeOutMap() {

        nowTime = System.currentTimeMillis();
        int width = bitMapCamera.getWidth();
        int height = bitMapCamera.getHeight();
        if (cameraOrientation == 6 && width > height)
            bitMapCamera = utils.rotateBitMap(bitMapCamera, 90);
        if (cameraOrientation == 1 && width < height)
            bitMapCamera = utils.rotateBitMap(bitMapCamera, 90);
        if (cameraOrientation == 3)
            bitMapCamera = utils.rotateBitMap(bitMapCamera, 180);

        String outFileName = sdfFileName.format(nowTime);
        File newFile = new File(utils.getPublicCameraDirectory(), phonePrefix + outFileName + ".jpg");
        writeCameraFile(bitMapCamera, newFile);
        setNewFileExif(newFile);
        mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(newFile)));

        Bitmap mergedMap = markDateLocSignature(bitMapCamera, nowTime);
        nowTime += 150;
        String outText = (strVoice.length() > 10) ? strVoice.substring(0, 10) : strVoice;
        String outFileName2 = sdfFileName.format(nowTime) + "_" + strPlace + " (" + outText.trim() +")";
        File newFile2 = new File(utils.getPublicCameraDirectory(), phonePrefix + outFileName2 + " _ha.jpg");
        writeCameraFile(mergedMap, newFile2);
        setNewFileExif(newFile2);
        mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(newFile2)));
        strVoice = "";
    }

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
            exifHa.setAttribute(ExifInterface.TAG_DATETIME, sdfExif.format(nowTime));
            exifHa.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "photomemo by riopapa");
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


    Bitmap markDateLocSignature(Bitmap photoMap, long timeStamp) {
        final SimpleDateFormat sdfHourMin = new SimpleDateFormat("`yy/MM/dd(EEE) HH:mm", Locale.KOREA);
        int fontSize;
        int width = photoMap.getWidth();
        int height = photoMap.getHeight();
        Bitmap newMap = Bitmap.createBitmap(width, height, photoMap.getConfig());
        Canvas canvas = new Canvas(newMap);
        canvas.drawBitmap(photoMap, 0f, 0f, null);
        fontSize = (width>height) ? (width+height)/56 : (width+height)/64;  // date time
        String dateTime = sdfHourMin.format(timeStamp);
        int sigSize = (width + height) / 24;
        Bitmap sigMap = Bitmap.createScaledBitmap(signatureMap, sigSize, sigSize, false);
        int xPos = (width>height) ? width / 5: width / 4;
        int yPos = (width>height) ? height / 8: height / 9;
        drawTextOnCanvas(canvas, dateTime, fontSize, xPos, yPos);
        xPos = width - sigSize - sigSize / 2;
        yPos = sigSize/ 2;
        canvas.drawBitmap(sigMap, xPos, yPos, null);
        if (strPosition.length() == 0) strPosition = "_";
        if (strPlace.length() == 0) strPlace = "_";
        if (strVoice.length() == 0) strVoice = "_";
        xPos = width / 2;
        fontSize = (height + width) / 72;  // gps
        yPos = height - fontSize - fontSize / 5;
        yPos = drawTextOnCanvas(canvas, strPosition, fontSize, xPos, yPos);
        fontSize = fontSize * 13 / 10;  // address
        yPos -= fontSize + fontSize / 5;
        yPos = drawTextOnCanvas(canvas, strAddress, fontSize, xPos, yPos);
        fontSize = fontSize * 14 / 10;  // Place
        yPos -= fontSize + fontSize / 5;
        yPos = drawTextOnCanvas(canvas, strPlace, fontSize, xPos, yPos);
        yPos -= fontSize + fontSize / 5;
        drawTextOnCanvas(canvas, strVoice, fontSize, xPos, yPos);
        return newMap;
    }

    private int drawTextOnCanvas(Canvas canvas, String text, int fontSize, int xPos, int yPos) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(fontSize);
        int cWidth = canvas.getWidth() * 3 / 4;
        float tWidth = paint.measureText(text);
        int pos;
        if (tWidth > cWidth) {
//            utils.log("size","cWidth:"+cWidth+" tWidth:"+tWidth);
            int length = text.length() / 2;
            for (pos = length; pos < text.length(); pos++)
                if (text.substring(pos,pos+1).equals(" "))
                    break;
            String text1 = text.substring(pos);
            drawOutLinedText(canvas, text1, xPos, yPos, fontSize);
            yPos -= fontSize + fontSize / 4;
            text1 = text.substring(0, pos);
            drawOutLinedText(canvas, text1, xPos, yPos, fontSize);
            return yPos;
        }
        else
            drawOutLinedText(canvas, text, xPos, yPos, fontSize);
        return yPos;
    }

    private void drawOutLinedText(Canvas canvas, String text, int xPos, int yPos, int textSize) {

        int color = ContextCompat.getColor(mContext, R.color.infoColor);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        paint.setTextSize(textSize);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth((int)(textSize/8));
        paint.setTypeface(mContext.getResources().getFont(R.font.nanumbarungothic));
        canvas.drawText(text, xPos, yPos, paint);

        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
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

    static Bitmap buildSignatureMap() {
        Bitmap sigMap;
        File sigFile = new File (Environment.getExternalStorageDirectory(),"signature.png");
        if (sigFile.exists()) {
            sigMap = BitmapFactory.decodeFile(sigFile.toString(), null);
        }
        else
            sigMap = BitmapFactory.decodeResource(mContext.getResources(), R.raw.signature);
        Bitmap newBitmap = Bitmap.createBitmap(sigMap.getWidth(), sigMap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newBitmap);
        Paint alphaPaint = new Paint();
        alphaPaint.setAlpha(120);
        canvas.drawBitmap(sigMap, 0, 0, alphaPaint);
        return newBitmap;
    }
}
