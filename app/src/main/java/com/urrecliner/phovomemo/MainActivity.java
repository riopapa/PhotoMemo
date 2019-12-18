package com.urrecliner.phovomemo;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
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
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.urrecliner.phovomemo.Vars.bitMapCamera;
import static com.urrecliner.phovomemo.Vars.cameraOrientation;
import static com.urrecliner.phovomemo.Vars.currActivity;
import static com.urrecliner.phovomemo.Vars.latitude;
import static com.urrecliner.phovomemo.Vars.longitude;
import static com.urrecliner.phovomemo.Vars.mActivity;
import static com.urrecliner.phovomemo.Vars.mCamera;
import static com.urrecliner.phovomemo.Vars.mContext;
import static com.urrecliner.phovomemo.Vars.nexus6P;
import static com.urrecliner.phovomemo.Vars.nowTime;
import static com.urrecliner.phovomemo.Vars.outFileName;
import static com.urrecliner.phovomemo.Vars.phoneMake;
import static com.urrecliner.phovomemo.Vars.phoneModel;
import static com.urrecliner.phovomemo.Vars.phonePrefix;
import static com.urrecliner.phovomemo.Vars.strAddress;
import static com.urrecliner.phovomemo.Vars.strDateTime;
import static com.urrecliner.phovomemo.Vars.strMapAddress;
import static com.urrecliner.phovomemo.Vars.strMapPlace;
import static com.urrecliner.phovomemo.Vars.strPlace;
import static com.urrecliner.phovomemo.Vars.strPosition;
import static com.urrecliner.phovomemo.Vars.strVoice;
import static com.urrecliner.phovomemo.Vars.utils;
import static com.urrecliner.phovomemo.Vars.xPixel;
import static com.urrecliner.phovomemo.Vars.yPixel;
import static com.urrecliner.phovomemo.Vars.zoomValue;

public class MainActivity extends AppCompatActivity {

    private GoogleApiClient mGoogleApiClient;
    private final static int PLACE_PICKER_REQUEST = 1;
    private CameraPreview mCameraPreview;
    private String logID = "main";

    SharedPreferences.Editor editor = null;

    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private SensorManager mSensorManager;
    private DeviceOrientation deviceOrientation;

    private Button btnCamera;
    private ImageButton btnClear;
    private int buttonBackColor;

    private TextView tVAddress;
    private int addressBackColor;

    private TextView tvVoice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        currActivity =  this.getClass().getSimpleName();
        mContext = getApplicationContext();
        if (!AccessPermission.isPermissionOK(getApplicationContext(), this)) {
            finish();
            return;
        }
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

        tVAddress = findViewById(R.id.addressText);
        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        editor = mSettings.edit();
        zoomValue = mSettings.getInt("Zoom", 16);

//        String hardware = Build.HARDWARE;   // samsungexynos9810    angler
//        utils.log(logID,"this phone model is " + phoneModel + " manu " + manufacturer + " hardware " + hardware);

        btnClear = findViewById(R.id.btnClear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvVoice = findViewById(R.id.textVoice);
                tvVoice.setText("");
            }
        });

        btnCamera = findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnCamera.setBackgroundColor(Color.MAGENTA);
                tVAddress.setBackgroundColor(Color.MAGENTA);
                reactClick();
                take_Picture();
            }
        });
        ColorDrawable buttonColor = (ColorDrawable) btnCamera.getBackground();
        this.buttonBackColor = buttonColor.getColor();

        startCamera();

        if (isNetworkAvailable()) {
            ImageView mAudio = findViewById(R.id.btnSpeak);
            mAudio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                    try {
                        startActivityForResult(intent, 1234);
                    } catch (ActivityNotFoundException a) {
                        //
                    }
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "Plese Connect to Internet", Toast.LENGTH_LONG).show();
        }

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
        utils.deleteOldLogFiles();
        TextView tv = findViewById(R.id.textVoice);
        tv.setText("");
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
            final SimpleDateFormat imgDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA);
            outFileName  = imgDateFormat.format(nowTime) + "_" + strPlace;
        } catch (Exception e) {
            strPlace = strAddress;
            strAddress = "?";
        }
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
            return "";
        }

        @Override
        protected void onPostExecute(String none) {
//            Log.w("post", "Executed");
            mCamera.stopPreview();
            mCamera.release();
            BuildImage buildImage = new BuildImage();
            buildImage.makeOutMap();
            startCamera();
            strVoice = "";
            tVAddress.setBackgroundColor(addressBackColor);
            btnCamera.setBackgroundColor(buttonBackColor);
        }
    }

    private class MyConnectionCallBack implements GoogleApiClient.ConnectionCallbacks {
        public void onConnected(Bundle bundle) {}

        public void onConnectionSuspended(int i) {}
    }

    private class MyOnConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
//            utils.log(logID,"#oF");
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
            mCameraPreview = new com.urrecliner.phovomemo.CameraPreview(this, (SurfaceView) findViewById(R.id.camera_surface));
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
            if (ratio > 1.7) {  // force ratio to wider screen
//                params.setPreviewSize(size.width, size.height);
                params.setPictureSize(size.width, size.height);
                break;
            }
        }

        mCamera.setParameters(params);
        mCamera.startPreview();
        mCameraPreview.setCamera(mCamera);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        utils.log(logID,"#oP");
    }

    public void showCurrentLocation() {

        double altitude;

        Location location = getGPSCord();
        if (location == null) {
            strPosition = " ";
        }
        else {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            altitude = location.getAltitude();
            strPosition = String.format(Locale.ENGLISH,"%.5f ; %.5f ; %.2f", latitude, longitude, altitude);
        }

        if (isNetworkAvailable()) {
            Geocoder geocoder = new Geocoder(this, Locale.KOREA);
            strAddress = getAddressByGPSValue(geocoder, latitude, longitude);
        }
        else {
            strAddress = " ";
        }
        String text = ((strMapPlace == null) ? " ":strMapPlace) + "\n" + ((strMapAddress == null) ? strAddress:strMapAddress);
        EditText et = findViewById(R.id.addressText);
        et.setText(text);
        et.setSelection(text.indexOf("\n"));
    }

    public Location getGPSCord() {

//        Log.w("gpscord called", "here");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "ACCESS FINE LOCATION not allowed", Toast.LENGTH_LONG).show();
            return null;
        }
        mGoogleApiClient.connect();
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        return lastLocation;
    }

    final String noInfo = "No_Info";
    public String getAddressByGPSValue(Geocoder geocoder, double latitude, double longitude) {

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                String Feature = address.getFeatureName();
                String Thorough = address.getThoroughfare();
                String Locality = address.getLocality();
                String SubLocality = address.getSubLocality();
                String Country = address.getCountryName();  // or getCountryName()
                String CountryCode = address.getCountryCode();
                String SState = address.getSubAdminArea();
                String State = address.getAdminArea();
                Feature = (Feature == null) ? noInfo : Feature;
                Thorough = (Thorough == null) ? noInfo : Thorough;  // Kakakaua Avernue
                SubLocality = (SubLocality == null) ? noInfo : SubLocality; // 분당구
                Locality = (Locality == null) ? noInfo : Locality;  // Honolulu, 성남시
                SState = (SState == null) ? noInfo : SState;
                State = (State == null) ? noInfo : State;   // Hawaii, 경기도
                if (Country == null && CountryCode == "KR")
                    Country = noInfo; // United States, 대한민국

                return MergedAddress(Feature, Thorough, SubLocality, Locality, State, SState, Country, CountryCode);
            } else {
                return "\nnull address text";
            }
        } catch (IOException e) {
            Toast.makeText(this, "No Address List", Toast.LENGTH_LONG).show();
            utils.log(logID,"#IOE " + e.toString());
            return "\n" + strPosition;
        }
    }

    public String MergedAddress(String Feature, String Thorough, String SubLocality, String Locality, String SState, String State, String Country, String CountryCode) {

        if (Thorough.equals(Feature)) Feature = noInfo;
        if (SubLocality.equals(Feature)) Feature = noInfo;
        if (SubLocality.equals(Thorough)) Thorough = noInfo;
        if (Locality.equals(Thorough)) Thorough = noInfo;
        if (Locality.equals(SubLocality)) SubLocality = noInfo;
        if (SState.equals(Locality)) Locality = noInfo;
        if (State.equals(SState)) SState = noInfo;

        String addressMerged = "";
        if (CountryCode.equals("KR")) {
            if (!State.equals(noInfo)) addressMerged += " " + State;
            if (!SState.equals(noInfo)) addressMerged += " " + SState;
            if (!Locality.equals(noInfo)) addressMerged += " " + Locality;
            if (!SubLocality.equals(noInfo)) addressMerged += " " + SubLocality;
            if (!Thorough.equals(noInfo)) addressMerged += " " + Thorough;
            if (!Feature.equals(noInfo)) addressMerged += " " + Feature;
        }
        else {
            if (!Feature.equals(noInfo)) addressMerged += " " + Feature;
            if (!Thorough.equals(noInfo)) addressMerged += " " + Thorough;
            if (!SubLocality.equals(noInfo)) addressMerged += " " + SubLocality;
            if (!Locality.equals(noInfo)) addressMerged += " " + Locality;
            if (!SState.equals(noInfo)) addressMerged += " " + SState;
            if (!State.equals(noInfo)) addressMerged += " " + State;
            addressMerged += " " + Country;
        }
        return addressMerged;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {  // user picked up place within the google map list
                Place place = PlacePicker.getPlace(this, data);
                strMapPlace = place.getName().toString();
                String text = place.getAddress().toString();
                strMapAddress = (text.length() > 10) ? text : null;
            } else if (resultCode == RESULT_CANCELED) {
                strMapPlace = null;
                strMapAddress = null;
            }
            mCamera.enableShutterSound(true);
            showCurrentLocation();
            nowTime = System.currentTimeMillis();
            final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("`yy/MM/dd HH:mm", Locale.ENGLISH);
            strDateTime = dateTimeFormat.format(nowTime);
        }
        else if (requestCode == 1234 && resultCode == RESULT_OK) {
            ArrayList<String> result = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            TextView mVoice = findViewById(R.id.textVoice);
            strVoice = mVoice.getText().toString()+ " " + result.get(0);
            mVoice.setText(strVoice);
        }
        else
            Toast.makeText(mContext, "Request Code:"+requestCode+", Result Code:"+resultCode+" not as expected", Toast.LENGTH_LONG).show();

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
}
