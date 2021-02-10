package com.urrecliner.photomemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;

import com.google.android.gms.common.api.GoogleApiClient;

public class Vars {

    static String currActivity = null;
    static Context mContext = null;
    static String strPlace = null;
    static String strAddress = null;
    static String strPosition = null;
    static String strVoice = " ";

    static Camera mCamera;
    static Bitmap bitMapCamera;
    static double latitude = 0;
    static double longitude = 0;
    static int zoomValue = 17;

    static Utils utils = new Utils();
    static Activity mActivity;
    static String phoneModel = null;
    static String phoneMake = null;
    static String phonePrefix = "";
    static int xPixel, yPixel;
    static int cameraOrientation;
    static Bitmap signatureMap;
    static String[] PlaceNames, PlaceVicinity;
    static String PlaceName = null, PlaceAddress = null;
    static boolean isPlacesReady = false;
    static GoogleApiClient mGoogleApiClient;
}
