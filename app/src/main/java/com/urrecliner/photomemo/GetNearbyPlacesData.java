package com.urrecliner.photomemo;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static com.urrecliner.photomemo.Vars.PlaceVicinity;
import static com.urrecliner.photomemo.Vars.PlaceName;
import static com.urrecliner.photomemo.Vars.PlaceNames;
import static com.urrecliner.photomemo.Vars.isPlacesReady;
import static com.urrecliner.photomemo.Vars.utils;

/**
 * @author Priyanka
 */

class GetNearbyPlacesData extends AsyncTask<String, String, String> {

    private String googlePlacesData;
    private GoogleMap mMap;
    String url;

    @Override
    protected String doInBackground(String... strings){
        url = (String)strings[0];

        DownloadURL downloadURL = new DownloadURL();
        try {
            googlePlacesData = downloadURL.readUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return googlePlacesData;
    }

    @Override
    protected void onPostExecute(String s){

        List<HashMap<String, String>> nearbyPlaceList;
        DataParser parser = new DataParser();
        nearbyPlaceList = parser.parse(s);
        Log.d("nearbyplacesdata","called parse method");
        showNearbyPlaces(nearbyPlaceList);
    }

    private void showNearbyPlaces(List<HashMap<String, String>> nearbyPlaceList)
    {

        PlaceNames = new String[nearbyPlaceList.size()];
        PlaceVicinity = new String[nearbyPlaceList.size()];

        for(int i = 0; i < nearbyPlaceList.size(); i++)
        {
            MarkerOptions markerOptions = new MarkerOptions();
            HashMap<String, String> googlePlace = nearbyPlaceList.get(i);

            String placeName = googlePlace.get("place_name");
            String vicinity = googlePlace.get("vicinity");
            PlaceNames[i] = placeName;
            PlaceVicinity[i] = vicinity;
            utils.log("i "+i,PlaceName+";"+placeName+";"+vicinity);
//            double lat = Double.parseDouble( googlePlace.get("lat"));
//            double lng = Double.parseDouble( googlePlace.get("lng"));

//            LatLng latLng = new LatLng( lat, lng);
//            markerOptions.position(latLng);
//            markerOptions.title(placeName + " : "+ vicinity);
//            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
//
//            mMap.addMarker(markerOptions);
//            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
//            mMap.animateCamera(CameraUpdateFactory.zoomTo(10));
        }

        isPlacesReady = true;
    }
}
