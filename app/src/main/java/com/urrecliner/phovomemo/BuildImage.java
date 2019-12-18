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
import static com.urrecliner.phovomemo.Vars.outFileName;
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
        timeStamp =  dateTimeFormat.format(nowTime);
        int width = bitMapCamera.getWidth();
        int height = bitMapCamera.getHeight();
        utils.log(logID, "bitMapCamera "+width+" x "+height+" orientation "+cameraOrientation+" ooooooo");

        if (cameraOrientation == 6 && width > height)
            bitMapCamera = utils.rotateBitMap(bitMapCamera, 90);
        if (cameraOrientation == 1 && width < height)
            bitMapCamera = utils.rotateBitMap(bitMapCamera, 90);
        if (cameraOrientation == 3)
            bitMapCamera = utils.rotateBitMap(bitMapCamera, 180);

        width = bitMapCamera.getWidth();
        height = bitMapCamera.getHeight();

        final SimpleDateFormat imgDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA);
        outFileName  = imgDateFormat.format(nowTime);

        File newFile = new File(utils.getPublicCameraDirectory(), phonePrefix + outFileName + ".jpg");
        writeCameraFile(bitMapCamera, newFile);
        setNewFileExif(newFile);

        utils.log(logID, "before Merge "+width+" x "+height+" orientation "+cameraOrientation+" ooooooo");
        Bitmap mergedMap = addSignature2Bitmaps(bitMapCamera, timeStamp);

        nowTime += 500;
        outFileName  = imgDateFormat.format(nowTime) + "_" + strPlace + " "+strVoice;
        newFile = new File(utils.getPublicCameraDirectory(), phonePrefix + outFileName + " _ha.jpg");
        writeCameraFile(mergedMap, newFile);
        setNewFileExif(newFile);
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
            exifHa.setAttribute(ExifInterface.TAG_DATETIME,sdfHourMinSec.format(nowTime));
            exifHa.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "PhoVoMemo by riopapa");
            exifHa.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
        private String latitudeRefGPS(double latitude) {
            return latitude<0.0d?"S":"N";
        }
        private String longitudeRefGPS(double longitude) {
            return longitude<0.0d?"W":"E";
        }

        private static String convertGPS(double latitude) {
            latitude = Math.abs(latitude);
            int degree = (int) latitude;
            latitude *= 60;
            latitude -= (degree * 60.0d);
            int minute = (int) latitude;
            latitude *= 60;
            latitude -= (minute * 60.0d);
            int second = (int) (latitude*10000.d);
            return degree+"/1,"+minute+"/1,"+second+"/10000";
        }

    private Bitmap addSignature2Bitmaps(Bitmap photoMap, String dateTime) {

        int width = photoMap.getWidth();
        int height = photoMap.getHeight();
        Bitmap newMap = Bitmap.createBitmap(width, height, photoMap.getConfig());
        Canvas canvas = new Canvas(newMap);
        canvas.drawBitmap(photoMap, 0f, 0f, null);
        int fontSize = (cameraOrientation == 1) ? height/16 : width/16;
        int xPos = (cameraOrientation == 1) ? width/5 : width*3/10;
        int yPos = height/12;
        drawTextOnCanvas(canvas, dateTime, fontSize, xPos, yPos);

        int sigSize = (width > height) ? height/6 : width/4;
        Bitmap sigMap = BitmapFactory.decodeResource(mContext.getResources(), R.raw.signature_yellow_min);
        sigMap = Bitmap.createScaledBitmap(sigMap, sigSize, sigSize, false);
        xPos = width - sigSize - width/20;
        yPos = height/20;
        Paint sigPaint = new Paint();
        sigPaint.setAlpha(60);
        canvas.drawBitmap(sigMap, xPos, yPos, sigPaint);

        if (strPlace.length() == 0) strPlace = "_";
        if (strVoice.length() == 0) strVoice = "_";
        fontSize = (cameraOrientation == 1) ? width/52 : width/36;
        xPos = width/2;
        yPos = height - fontSize - fontSize;
        drawTextOnCanvas(canvas, strAddress, fontSize, xPos, yPos);
        fontSize = fontSize * 15 / 10;
        yPos -= fontSize + fontSize / 4;
        drawTextOnCanvas(canvas, strPlace, fontSize, xPos, yPos);
        fontSize = fontSize * 15 / 10;
        yPos -= fontSize + fontSize / 4;
        drawTextOnCanvas(canvas, strVoice, fontSize, xPos, yPos);
        return newMap;
    }

    private void drawTextOnCanvas(Canvas canvas, String text, int fontSize, int xPos, int yPos) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(fontSize);
//        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.XOR));
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        int d = fontSize / 16;
        Log.w("delta","fontsize "+fontSize+" delta"+d);
        canvas.drawText(text, xPos-d, yPos-d, paint);
        canvas.drawText(text, xPos+d, yPos-d, paint);
        canvas.drawText(text, xPos-d, yPos+d, paint);
        canvas.drawText(text, xPos+d, yPos+d, paint);
        canvas.drawText(text, xPos-d, yPos, paint);
        canvas.drawText(text, xPos+d, yPos, paint);
        canvas.drawText(text, xPos, yPos-d, paint);
        canvas.drawText(text, xPos, yPos+d, paint);
        paint.setColor(Color.YELLOW);
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
