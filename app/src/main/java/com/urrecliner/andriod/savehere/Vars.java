package com.urrecliner.andriod.savehere;

import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.Camera;

import com.google.android.gms.maps.GoogleMap;

public class Vars {

    static String currActivity = null;
    static String strPlace = null;
    static String strAddress = null;
    static String strMapPlace = null;
    static String strMapAddress = null;
    static String strPosition = null;
    static String strDateTime = null;

    static Camera mCamera;
    static Bitmap bitMapScreen;
    static double latitude = 0;
    static double longitude = 0;
    static GoogleMap mMap = null;
    static int zoomValue = 17;
    static boolean CameraMapBoth = false;
    static boolean isTimerOn = false;

    static boolean isRUNNING = false;
    static Utils utils = new Utils();
    static Activity mActivity;
    static String phoneModel = null;
    static String phoneMake = null;
    static String galaxyS9 = "SM-G965N";
    static String nexus6P = "Nexus 6P";
}
