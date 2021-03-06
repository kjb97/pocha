package halla.icsw.pocha;


import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class BuyerMain extends AppCompatActivity
        implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;
    private static final int GPS_ENABLE_REQUEST_CODE=2001;
    private static final int UPDATE_INTERVAL_MS=1000; //1???
    private static final int FASTEST_UPDATE_INTERVAL_MS=500;
    private static final int PERMISSIONS_REQUEST_CODE=100;
    boolean needRequest = false;
    String[] REQUIRED_PERMISSIONS =
            {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}; //?????? ?????????

    JSONArray jsonArray;
    JSONObject jsonObject;
    Marker marker;
    LatLng loc;
    ArrayList<String> item=new ArrayList<>();
    ArrayList<Double> lat=new ArrayList<>();
    ArrayList<Double> lng=new ArrayList<>();

    AutoCompleteTextView edit;
    ArrayList ja=new ArrayList<>();
    Location currentlocation;
    LatLng currentposition;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private  Location location;
    private View Layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.buyer);
        NetworkUtil.setNetworkPolicy();

        locationRequest= new LocationRequest().
                setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)// ???????????? ??????
                .setInterval(UPDATE_INTERVAL_MS)//
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest); //?????? ?????? ??????

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);//?????? ?????? ????????????

        SupportMapFragment supportMapFragment =(SupportMapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(this);//?????? ????????????
    }

    public void marker(){

        try {
            PHPRequest request = new PHPRequest("http://101.101.210.207/getLocation.php");
            String result = request.getLocation();
            Log.i("?????? ??????",result);
            try {
                jsonArray = new JSONArray(result);
                for(int i = 0 ; i<jsonArray.length(); i++){
                    jsonObject = jsonArray.getJSONObject(i);
                    if(jsonObject.getString("lat").equals(null)){ continue;}
                    item.add(jsonObject.getString("shopname"));
                    lat.add(jsonObject.getDouble("lat"));
                    lng.add(jsonObject.getDouble("lng"));
                    ja.add(new LatLng(jsonObject.getDouble("lat"),jsonObject.getDouble("lng")));
                    loc=new LatLng(jsonObject.getDouble("lat"),jsonObject.getDouble("lng"));
                    marker = mMap.addMarker(new MarkerOptions().position(loc).title(jsonObject.getString("shopname")));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 12));
                    mMap.setOnInfoWindowClickListener(this::onInfoWindowClick);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void select() {
        Button tx = (Button)findViewById(R.id.textbn);
        edit=(AutoCompleteTextView) findViewById(R.id.edit);
        edit.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,item));
        tx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(int i=0;item.size()>i;i++){
                    if(edit.getText().toString().equals(item.get(i))){
                        mMap.moveCamera(CameraUpdateFactory.newLatLng((LatLng) ja.get(i)));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
                        loc = new LatLng(lat.get(i),lng.get(i));
                        marker = mMap.addMarker(new MarkerOptions().position(loc).title(item.get(i)));
                        marker.showInfoWindow();
                    }else continue;
                }



            }
        });

            }



    public void onInfoWindowClick(Marker marker){
        SharedPreferences pref = getSharedPreferences("shopID",MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();

        edit.commit();
        double la,ln;
        la=marker.getPosition().latitude;
        ln=marker.getPosition().longitude;
        Log.i("?????? ??????",la+" "+ln);
        try {
            PHPRequest request = new PHPRequest("http://101.101.210.207/getShopID.php");
            String result = request.GetShopID(la,ln);
            Log.i("?????? id",result);
            edit.putString("shopid",result);
            edit.commit();
        }catch (MalformedURLException e){
            e.printStackTrace();
        }
        Intent i = new Intent(getApplicationContext(),Shopinfor.class);
        startActivity(i);

    }



    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;


        //????????? ????????? ??????
        int FineLocationPermission = ContextCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        // ??????????????? ???????????? ??????
        int CoarseLocationPermission = ContextCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if (FineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                CoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdate(); //?????? ????????????
        } else {
            //??????
            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (this, REQUIRED_PERMISSIONS[0])) {
                Snackbar.make(Layout, "?????? ????????? ??????",
                        Snackbar.LENGTH_INDEFINITE).setAction("??????", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityCompat.requestPermissions(BuyerMain.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
                    }
                }).show();

            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }
        LatLng SEOUL = new LatLng(37.302453, 127.908115);//????????? ??????
        mMap.getUiSettings().setMyLocationButtonEnabled(true); //?????? ?????? ??????
        MarkerOptions markerOptions = new MarkerOptions(); // ?????? ??????
        markerOptions.position(SEOUL);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));  // ?????? ??????
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
        UiSettings settings = mMap.getUiSettings();
        settings.setZoomControlsEnabled(true); //??? ??????
        marker();
        select();

    }

    LocationCallback locationCallback = new LocationCallback(){

        public void onLocationResult(LocationResult locationResult){
            super.onLocationResult(locationResult);

            List<Location> locationList = locationResult.getLocations();


            if(locationList.size()>0){
                currentlocation= locationList.get(locationList.size()-1);
                location= locationList.get(0);

                //????????????!!!!! ???????????? setcurrentposition
                currentposition= new LatLng(location.getLatitude(),location.getLongitude());

                String markerTitle = getGeocoder(currentposition);// ???????????? ??????



            }
        }
    };




    public String getGeocoder(LatLng latLng){

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(
                    latLng.latitude, latLng.longitude, 1);
        }catch (IOException e){
            return "???????????? ????????? ??????";
        }catch (IllegalArgumentException e){
            return"????????? gps";
        }

        if (addresses==null ||addresses.size()==0){
            return "?????? ?????????";
        }else {
            Address address = addresses.get(0);
            return address.getAddressLine(0).toString();
        }
    }

    private void startLocationUpdate(){//???????????? ?????????
        if (!checkLocationServicesStatus()){
            GpsActivation();
        }
        else {
            int FinePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            int CoarsePermission = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION);

            if(FinePermission!= PackageManager.PERMISSION_GRANTED ||
                    CoarsePermission != PackageManager.PERMISSION_GRANTED){
                return;
            }
            fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);//???????????? ??????
            mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

        }

    }


    public boolean checkLocationServicesStatus() {//???????????? and gps????????? ??????
        LocationManager locationManager =(LocationManager)getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)||
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


    private  void GpsActivation(){//gps??????
        AlertDialog.Builder builder = new AlertDialog.Builder(BuyerMain.this);
        builder.setTitle("?????? ????????????");
        builder.setMessage("?????? ???????????? ????????????");
        builder.setCancelable(true);
        builder.setPositiveButton("??????", (dialog, which) -> {
            Intent callGps= new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(callGps,GPS_ENABLE_REQUEST_CODE);
        });

        //gps ????????????
        builder.setNegativeButton("??????", (dialog, which) -> dialog.cancel());
        builder.create().show();
    }

    protected void onActivityResult(int requestCode,int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){//gps????????? ??????

            case GPS_ENABLE_REQUEST_CODE:
                if(checkLocationServicesStatus()){
                    if(checkLocationServicesStatus()){
                        needRequest=true;
                        return;
                    }
                }
                break;
        }
    }


    private void setDefaultKeyMode() {
    }

}
