package com.urrecliner.photomemo;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.photomemo.BuildBitMap.buildSignatureMap;
import static com.urrecliner.photomemo.Vars.PlaceNames;
import static com.urrecliner.photomemo.Vars.bitMapCamera;
import static com.urrecliner.photomemo.Vars.cameraOrientation;
import static com.urrecliner.photomemo.Vars.currActivity;
import static com.urrecliner.photomemo.Vars.latitude;
import static com.urrecliner.photomemo.Vars.longitude;
import static com.urrecliner.photomemo.Vars.mActivity;
import static com.urrecliner.photomemo.Vars.mCamera;
import static com.urrecliner.photomemo.Vars.mContext;
import static com.urrecliner.photomemo.Vars.mGoogleApiClient;
import static com.urrecliner.photomemo.Vars.phoneMake;
import static com.urrecliner.photomemo.Vars.phoneModel;
import static com.urrecliner.photomemo.Vars.signatureMap;
import static com.urrecliner.photomemo.Vars.strAddress;
import static com.urrecliner.photomemo.Vars.strPlace;
import static com.urrecliner.photomemo.Vars.strPosition;
import static com.urrecliner.photomemo.Vars.strVoice;
import static com.urrecliner.photomemo.Vars.utils;
import static com.urrecliner.photomemo.Vars.xPixel;
import static com.urrecliner.photomemo.Vars.yPixel;
import static com.urrecliner.photomemo.Vars.zoomValue;

public class MainActivity extends AppCompatActivity {

    final int REQUEST_PLACE_PICKER = 1111;
    private final static int VOICE_RECOGNISE = 1234;
    private CameraPreview mCameraPreview;
    private String logID = "main";

    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private SensorManager mSensorManager;
    private DeviceOrientation deviceOrientation;

    private Button btnShot;
    private int buttonBackColor;
    private boolean exitApp;

    private TextView tVAddress;
    private int addressBackColor;
    AudioManager audioManager = null;
    private TextView tvVoice;

    public static final String THEME_RES_ID_EXTRA = "widget_theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        currActivity = this.getClass().getSimpleName();
        mContext = getApplicationContext();
        askPermission();
        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_PLAY_SOUND);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        deviceOrientation = new DeviceOrientation();

        mActivity = this;
        phoneModel = Build.MODEL;           // SM-G965N             Nexus 6P
        phoneMake = Build.MANUFACTURER;     // samsung              Huawei
//        if (phoneModel.equals(nexus6P))
//            phonePrefix = "IMG_";
        xPixel = Resources.getSystem().getDisplayMetrics().widthPixels;     // 2094, 2960
        yPixel = Resources.getSystem().getDisplayMetrics().heightPixels;    // 1080, 1440

        tvVoice = findViewById(R.id.textVoice);
        tVAddress = findViewById(R.id.addressText);
        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        zoomValue = mSettings.getInt("zoom", 16);
        utils = new Utils();
//        String hardware = Build.HARDWARE;   // samsungexynos9810    angler
//        utils.log(logID,"this phone model is " + phoneModel);

        ImageButton btnClear = findViewById(R.id.btnClear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvVoice.setText("");
            }
        });

        btnShot = findViewById(R.id.btnShot);
        btnShot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tvVoice.getText().toString().length() > 1) {
                    btnShot.setBackgroundColor(Color.MAGENTA);
                    tVAddress.setBackgroundColor(Color.MAGENTA);
                    exitApp = false;
                    reactClick();
                    take_Picture();
                } else
                    Toast.makeText(mContext, "No Voice Text", Toast.LENGTH_LONG).show();
            }
        });

        final Button btnShotExit = findViewById(R.id.btnShotExit);
        btnShotExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tvVoice.getText().toString().length() > 1) {
                    btnShotExit.setBackgroundColor(Color.MAGENTA);
                    tVAddress.setBackgroundColor(Color.MAGENTA);
                    exitApp = true;
                    reactClick();
                    take_Picture();
                } else
                    Toast.makeText(mContext, "No Voice Text", Toast.LENGTH_LONG).show();
            }
        });
        ColorDrawable buttonColor = (ColorDrawable) btnShot.getBackground();
        this.buttonBackColor = buttonColor.getColor();

        startCamera();

        ImageView mSpeak = findViewById(R.id.btnSpeak);
        mSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGetVoice();
            }
        });

//        ready_GoogleAPIClient();
        int waitCount = 0;
        getMostLastLocation();
        while (latitude == 0 && waitCount++ < 10) {
            SystemClock.sleep(1000);
            Log.w(""+waitCount,"latitude="+latitude);
        }
        if (isNetworkAvailable()) {
            showPlacePicker();
//
//            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
//            Intent intent = null;
//            try {
//                intent = builder.build(MainActivity.this);
//            } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
//                utils.log(logID,"#PP" + e.toString());
//                e.printStackTrace();
//            }
//            startActivityForResult(intent, PLACE_PICKER_REQUEST);
        } else {
            Toast.makeText(mContext, "No Network", Toast.LENGTH_LONG).show();
            ;
            showCurrentLocation();
        }
        tvVoice.setText("");
        final View v = findViewById(R.id.frame);
        v.post(new Runnable() {
            @Override
            public void run() {
//                ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) v.getLayoutParams();
//                lp.width = lp.height * 10 / 17;
//                v.setLayoutParams(lp);
                utils.deleteOldLogFiles();
                signatureMap = buildSignatureMap();
            }
        });
    }

    private void showPlacePicker() {

        GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData();
        String url = getUrl(latitude, longitude);
        getNearbyPlacesData.execute(url);
    }


    int PROXIMITY_RADIUS = 10000;

    private String getUrl(double latitude , double longitude)
    {

        StringBuilder googlePlaceUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlaceUrl.append("location="+latitude+","+longitude);
        googlePlaceUrl.append("&radius="+PROXIMITY_RADIUS);
        googlePlaceUrl.append("&sensor=true");
        googlePlaceUrl.append("&language=ko");
        googlePlaceUrl.append("&fields=formatted_address,name,geometry,vicinity");
        googlePlaceUrl.append("&key="+getString(R.string.maps_api_key));
        utils.log("getUrl", "url = "+googlePlaceUrl.toString());

        return googlePlaceUrl.toString();
    }

    private void startGetVoice() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());    //데이터 설정
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);   //검색을 말한 결과를 보여주는 갯수

        try {
            startActivityForResult(intent, VOICE_RECOGNISE);
        } catch (ActivityNotFoundException a) {
            //
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        utils.log(logID, " new Config " + newConfig.orientation);
        Toast.makeText(mContext, "curr orentation is " + newConfig.orientation, Toast.LENGTH_SHORT).show();
    }

    private void ready_GoogleAPIClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new MyConnectionCallBack())
                    .addOnConnectionFailedListener(new MyOnConnectionFailedListener())
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    private void take_Picture() {
        mCamera.takePicture(null, null, rawCallback, jpegCallback); // null is for silent shot
        if (exitApp) {
            new Timer().schedule(new TimerTask() {
                public void run() {
                    finish();
                    audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_PLAY_SOUND);
                    mActivity.finishAffinity();
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }
            }, 3000);
        } else {
            startGetVoice();
        }
    }

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(ExifInterface.ORIENTATION_NORMAL, 0);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_90, 90);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_180, 180);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_270, 270);
    }

    private void reactClick() {

        int mDeviceRotation = ORIENTATIONS.get(deviceOrientation.getOrientation());
        if (mDeviceRotation == 0)
            cameraOrientation = 1;
        else if (mDeviceRotation == 180)
            cameraOrientation = 3;
        else if (mDeviceRotation == 90)
            cameraOrientation = 6;
        else
            cameraOrientation = 8;

        strAddress = tVAddress.getText().toString();
        try {
            strPlace = strAddress.substring(0, strAddress.indexOf("\n"));
            if (strPlace.equals("")) {
                strPlace = " ";
            }
            strAddress = strAddress.substring(strAddress.indexOf("\n") + 1);
        } catch (Exception e) {
            strPlace = strAddress;
            strAddress = "?";
        }
        strVoice = tvVoice.getText().toString();
        tvVoice.setText("");
    }

    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            //			 Log.d(TAG, "onShutter'd");
        }
    };

    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            //			 Log.d(TAG, "onPictureTaken - raw");
        }
    };

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitMapCamera = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            new SaveImageTask().execute("");
        }
    };

    private class SaveImageTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... data) {
            mCamera.stopPreview();
            mCamera.release();
            BuildBitMap buildBitMap = new BuildBitMap();
            buildBitMap.makeOutMap();
            return "";
        }

        @Override
        protected void onPostExecute(String none) {
            startCamera();
            utils.log("post", "Executed");
            strVoice = "";
            tVAddress.setBackgroundColor(addressBackColor);
            btnShot.setBackgroundColor(buttonBackColor);
        }
    }

    private class MyConnectionCallBack implements GoogleApiClient.ConnectionCallbacks {
        public void onConnected(Bundle bundle) {
        }

        public void onConnectionSuspended(int i) {
        }
    }

    private class MyOnConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            utils.log(logID, "#oF");
        }
    }

    protected void onStart() {
        super.onStart();
//        ready_GoogleAPIClient();
//        mGoogleApiClient.connect();
    }

    protected void onStop() {
//        mGoogleApiClient.disconnect();
        super.onStop();
    }

    public void startCamera() {

        if (mCameraPreview == null) {
            mCameraPreview = new com.urrecliner.photomemo.CameraPreview(this, (SurfaceView) findViewById(R.id.camera_surface));
            mCameraPreview.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            ((FrameLayout) findViewById(R.id.frame)).addView(mCameraPreview);
            mCameraPreview.setKeepScreenOn(true);
        }

        mCameraPreview.setCamera(null);
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        mCamera = Camera.open(0);
        try {
            // camera cameraOrientation
            mCamera.setDisplayOrientation(90);

        } catch (RuntimeException ex) {
            Toast.makeText(getApplicationContext(), "camera cameraOrientation " + ex.getMessage(),
                    Toast.LENGTH_LONG).show();
            utils.log(logID, "CAMERA not found " + ex.getMessage());
        }
        Camera.Parameters params = mCamera.getParameters();
        params.setRotation(90);
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        for (Camera.Size size : params.getSupportedPictureSizes()) {
            float ratio = (float) size.width / (float) size.height;
            if (ratio > 1.7) {
                params.setPictureSize(size.width, size.height);
                break;
            }
        }
        mCamera.setParameters(params);
        mCamera.startPreview();
        mCameraPreview.setCamera(mCamera);
    }

    void getCurrentLatLng() {

        Location location = getGPSCord();
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            double altitude = location.getAltitude();
            strPosition = String.format(Locale.ENGLISH, "%.5f ; %.5f ; %.2f", latitude, longitude, altitude);
        }
    }

    void showCurrentLocation() {

        strAddress = null;
        if (isNetworkAvailable()) {
            Geocoder geocoder = new Geocoder(this, Locale.KOREA);
            strAddress = GPS2Address.get(geocoder, latitude, longitude);
        }
        String text = "\n" + strAddress;
        EditText et = findViewById(R.id.addressText);
        et.setText(text);
        et.setSelection(text.indexOf("\n"));
        tvVoice.setText(strVoice);
    }

    public Location getGPSCord() {

//        utils.log("called", "here");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "ACCESS FINE LOCATION not allowed", Toast.LENGTH_LONG).show();
            return null;
        }
        mGoogleApiClient.connect();
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        return lastLocation;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        utils.log("activity","result="+requestCode+", "+requestCode);
        switch (requestCode) {
            case REQUEST_PLACE_PICKER:
                if (resultCode == RESULT_OK) {
                    if (PlaceNames.length > 0) {
//                        if (place != null) {
//                            strPlace = place.getName();
//                            latitude = place.getLatLng().latitude;
//                            longitude = place.getLatLng().longitude;
                            Geocoder geocoder = new Geocoder(this, Locale.KOREA);
                            strAddress = GPS2Address.get(geocoder, latitude, longitude);
                            EditText et = findViewById(R.id.addressText);
                            String text = strPlace + "\n" + strAddress;
                            et.setText(text);
                            et.setSelection(text.indexOf("\n") + 1);
//                        mCamera.enableShutterSound(true);
                            new Timer().schedule(new TimerTask() {
                                public void run() {
                                    startGetVoice();
                                }
                            }, 1000);
                    }
                }
                break;
            case VOICE_RECOGNISE:
                if (resultCode == RESULT_OK) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    strVoice = (strVoice + " " + result.get(0)).trim();
                    tvVoice.setText(strVoice);
                }
                break;
            default:
                Toast.makeText(mContext, "Request Code:" + requestCode + ", Result Code:" + resultCode + " not as expected", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cM = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo aNI = cM.getActiveNetworkInfo();
        return aNI != null && aNI.isConnected();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(deviceOrientation.getEventListener(), mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(deviceOrientation.getEventListener(), mMagnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    void getMostLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location;
        assert locationManager != null;
        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null)
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            double altitude = location.getAltitude();
            strPosition = String.format(Locale.ENGLISH, "%.5f ; %.5f ; %.2f", latitude, longitude, altitude);
        }
    }


    // ↓ ↓ ↓ P E R M I S S I O N   RELATED /////// ↓ ↓ ↓ ↓  BEST CASE 20/09/27 with no lambda
    private final static int ALL_PERMISSIONS_RESULT = 101;
    ArrayList permissionsToRequest;
    ArrayList<String> permissionsRejected = new ArrayList<>();
    String [] permissions;

    private void askPermission() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS);
            permissions = info.requestedPermissions;//This array contain
        } catch (Exception e) {
            Log.e("Permission", "Not done", e);
        }

        permissionsToRequest = findUnAskedPermissions();
        if (permissionsToRequest.size() != 0) {
            requestPermissions((String[]) permissionsToRequest.toArray(new String[0]),
//            requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                    ALL_PERMISSIONS_RESULT);
        }
    }

    private ArrayList findUnAskedPermissions() {
        ArrayList <String> result = new ArrayList<String>();
        for (String perm : permissions) if (hasPermission(perm)) result.add(perm);
        return result;
    }
    private boolean hasPermission(String permission) {
        return (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == ALL_PERMISSIONS_RESULT) {
            for (Object perms : permissionsToRequest) {
                if (hasPermission((String) perms)) {
                    permissionsRejected.add((String) perms);
                }
            }
            if (permissionsRejected.size() > 0) {
                if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                    String msg = "These permissions are mandatory for the application. Please allow access.";
                    showDialog(msg);
                }
                if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                    String msg = "These permissions are mandatory for the application. Please allow access.";
                    showDialog(msg);
                }
            }
        }
    }
    private void showDialog(String msg) {
        showMessageOKCancel(msg,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.requestPermissions(permissionsRejected.toArray(
                                new String[0]), ALL_PERMISSIONS_RESULT);
                    }
                });
    }
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new android.app.AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

// ↑ ↑ ↑ ↑ P E R M I S S I O N    RELATED /////// ↑ ↑ ↑

}
