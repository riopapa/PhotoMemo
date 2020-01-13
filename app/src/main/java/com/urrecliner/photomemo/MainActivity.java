package com.urrecliner.photomemo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.media.AudioManager;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.photomemo.BuildBitMap.buildSignatureMap;
import static com.urrecliner.photomemo.Vars.bitMapCamera;
import static com.urrecliner.photomemo.Vars.cameraOrientation;
import static com.urrecliner.photomemo.Vars.currActivity;
import static com.urrecliner.photomemo.Vars.latitude;
import static com.urrecliner.photomemo.Vars.longitude;
import static com.urrecliner.photomemo.Vars.mActivity;
import static com.urrecliner.photomemo.Vars.mCamera;
import static com.urrecliner.photomemo.Vars.mContext;
import static com.urrecliner.photomemo.Vars.nexus6P;
import static com.urrecliner.photomemo.Vars.phoneMake;
import static com.urrecliner.photomemo.Vars.phoneModel;
import static com.urrecliner.photomemo.Vars.phonePrefix;
import static com.urrecliner.photomemo.Vars.signatureMap;
import static com.urrecliner.photomemo.Vars.strAddress;
import static com.urrecliner.photomemo.Vars.strMapAddress;
import static com.urrecliner.photomemo.Vars.strMapPlace;
import static com.urrecliner.photomemo.Vars.strPlace;
import static com.urrecliner.photomemo.Vars.strPosition;
import static com.urrecliner.photomemo.Vars.strVoice;
import static com.urrecliner.photomemo.Vars.utils;
import static com.urrecliner.photomemo.Vars.xPixel;
import static com.urrecliner.photomemo.Vars.yPixel;
import static com.urrecliner.photomemo.Vars.zoomValue;

public class MainActivity extends AppCompatActivity {

    private GoogleApiClient mGoogleApiClient;
    private final static int PLACE_PICKER_REQUEST = 1;
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
    private boolean sttMode;

    private TextView tVAddress;
    private int addressBackColor;
    AudioManager audioManager = null;
    private TextView tvVoice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        currActivity =  this.getClass().getSimpleName();
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
        if (phoneModel.equals(nexus6P))
            phonePrefix = "IMG_";

        xPixel = Resources.getSystem().getDisplayMetrics().widthPixels;     // 2094, 2960
        yPixel = Resources.getSystem().getDisplayMetrics().heightPixels;    // 1080, 1440

        tvVoice = findViewById(R.id.textVoice);
        tVAddress = findViewById(R.id.addressText);
        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        zoomValue = mSettings.getInt("Zoom", 16);

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
                }
                else
                    Toast.makeText(mContext,"No Voice Text", Toast.LENGTH_LONG).show();
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
                }
                else
                    Toast.makeText(mContext,"No Voice Text", Toast.LENGTH_LONG).show();
            }
        });
        ColorDrawable buttonColor = (ColorDrawable) btnShot.getBackground();
        this.buttonBackColor = buttonColor.getColor();

        startCamera();

        ImageView mSpeak = findViewById(R.id.btnSpeak);
        mSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sttMode = !sttMode;
                ImageView iv = findViewById(R.id.btnSpeak);
                iv.setImageResource(sttMode ? R.mipmap.micro_phone_off: R.mipmap.micro_phone_on);
                if (sttMode)
                    startGetVoice();
            }
        });

        ready_GoogleAPIClient();
        if (isNetworkAvailable()) {
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            Intent intent = null;
            try {
                intent = builder.build(MainActivity.this);
            } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                utils.log(logID,"#PP" + e.toString());
                e.printStackTrace();
            }
            startActivityForResult(intent, PLACE_PICKER_REQUEST);
        }
        else {
            Toast.makeText(mContext,"No Network", Toast.LENGTH_LONG).show();;
            showCurrentLocation();
        }
        tvVoice.setText("");
        final View v = findViewById(R.id.frame);
        v.post(new Runnable() {
            @Override
            public void run() {
                ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) v.getLayoutParams();
                lp.width = lp.height * 10 / 17;
                v.setLayoutParams(lp);
                utils.deleteOldLogFiles();
                signatureMap = buildSignatureMap();
            }
        });
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
        utils.log(logID," new Config "+newConfig.orientation);
        Toast.makeText(mContext,"curr orentation is "+newConfig.orientation,Toast.LENGTH_SHORT).show();
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
                    audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_PLAY_SOUND);
                    mActivity.finishAffinity();
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }
            }, 3000);
        }
        else {
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

    private class SaveImageTask extends AsyncTask<String, String , String> {

        @Override
        protected String doInBackground(String ... data) {
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
        public void onConnected(Bundle bundle) {}

        public void onConnectionSuspended(int i) {}
    }

    private class MyOnConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            utils.log(logID,"#oF");
        }
    }

    protected void onStart() {
        super.onStart();
        ready_GoogleAPIClient();
        mGoogleApiClient.connect();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
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
            utils.log(logID,"CAMERA not found " + ex.getMessage());
        }
        Camera.Parameters params = mCamera.getParameters();
        params.setRotation(90);
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        for (Camera.Size size : params.getSupportedPictureSizes()) {
            float ratio= (float) size.width / (float) size.height;
            if (ratio > 1.7) {
                params.setPictureSize(size.width, size.height);
                break;
            }
        }
        mCamera.setParameters(params);
        mCamera.startPreview();
        mCameraPreview.setCamera(mCamera);
    }

    public void showCurrentLocation() {

        Location location = getGPSCord();
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            double altitude = location.getAltitude();
            strPosition = String.format(Locale.ENGLISH, "%.5f ; %.5f ; %.2f", latitude, longitude, altitude);
        }

        if (isNetworkAvailable()) {
            Geocoder geocoder = new Geocoder(this, Locale.KOREA);
            strAddress = GPS2Address.get(geocoder, latitude, longitude);
        }
        else {
            strAddress = "_";
        }
        String text = ((strMapPlace == null) ? " ":strMapPlace) + "\n" + ((strMapAddress == null) ? strAddress:strMapAddress);
        EditText et = findViewById(R.id.addressText);
        et.setText(text);
        et.setSelection(text.indexOf("\n"));
        tvVoice.setText(strVoice);
        new Timer().schedule(new TimerTask() {
            public void run() {
                sttMode = true;
                startGetVoice();
            }
        }, 1000);
    }

    public Location getGPSCord() {

        utils.log("gpscord called", "here");
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

        switch (requestCode) {
            case PLACE_PICKER_REQUEST:
                if (resultCode == RESULT_OK) {  // user picked up place within the google map list
                    Place place = PlacePicker.getPlace(this, data);
                    strMapPlace = place.getName().toString();
                    String text = place.getAddress().toString();
                    if (text.length() > 5)
                        strMapAddress = text.replace("대한민국 ", "");
                } else if (resultCode == RESULT_CANCELED) {
                    strMapPlace = null;
                    strMapAddress = null;
                }
                mCamera.enableShutterSound(true);
                showCurrentLocation();
                break;
            case VOICE_RECOGNISE:
                if (resultCode == RESULT_OK) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    strVoice = strVoice + result.get(0);
                    tvVoice.setText(strVoice);
                    new Timer().schedule(new TimerTask() {
                        public void run() {
                            if (sttMode)
                                startGetVoice();
                        }
                    }, 1000);
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

//     ↓ ↓ ↓ P E R M I S S I O N    RELATED /////// ↓ ↓ ↓ ↓
    ArrayList<String> permissions = new ArrayList<>();
    private final static int ALL_PERMISSIONS_RESULT = 101;
    ArrayList<String> permissionsToRequest;
    ArrayList<String> permissionsRejected = new ArrayList<>();

    private void askPermission() {
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.INTERNET);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);
        permissionsToRequest = findUnAskedPermissions(permissions);
        if (permissionsToRequest.size() != 0) {
            requestPermissions(permissionsToRequest.toArray(new String[0]),
//            requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                    ALL_PERMISSIONS_RESULT);
        }
    }

    private ArrayList findUnAskedPermissions(@NonNull ArrayList<String> wanted) {
        ArrayList <String> result = new ArrayList<String>();
        for (String perm : wanted) if (hasPermission(perm)) result.add(perm);
        return result;
    }
    private boolean hasPermission(@NonNull String permission) {
        return (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ALL_PERMISSIONS_RESULT) {
            for (String perms : permissionsToRequest) {
                if (hasPermission(perms)) {
                    permissionsRejected.add(perms);
                }
            }
            if (permissionsRejected.size() > 0) {
                if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                    String msg = "These permissions are mandatory for the application. Please allow access.";
                    showDialog(msg);
                }
            }
//            else
//                Toast.makeText(mContext, "Permissions not granted.", Toast.LENGTH_LONG).show();
        }
    }
    private void showDialog(String msg) {
        showMessageOKCancel(msg,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(permissionsRejected.toArray(
                                new String[0]), ALL_PERMISSIONS_RESULT);
                    }
                });
    }
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(mActivity)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
// ↑ ↑ ↑ ↑ P E R M I S S I O N    RELATED /////// ↑ ↑ ↑

}
