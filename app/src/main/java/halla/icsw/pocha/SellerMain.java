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
import android.view.View;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import halla.icsw.pocha.R;

public class SellerMain extends AppCompatActivity
        implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;
    private Marker currentMarker=null;
    private static final int GPS_ENABLE_REQUEST_CODE=2001;
    private static final int UPDATE_INTERVAL_MS=1000; //1초
    private static final int FASTEST_UPDATE_INTERVAL_MS=500;
    private static final int PERMISSIONS_REQUEST_CODE=100;
    boolean needRequest = false;
    String[] REQUIRED_PERMISSIONS =
            {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}; //외부 저장소
    PHPRequest request;

    Location currentlocation;
    LatLng currentposition;
    private Marker getCurrentMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private Location location;
    private View Layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.seller);
        NetworkUtil.setNetworkPolicy();


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.sellmap);
        mapFragment.getMapAsync(this);


        locationRequest= new LocationRequest().
                setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)// 위치설정 변경
                .setInterval(UPDATE_INTERVAL_MS)//
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest); //위치 설정 요청

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);//권한 요청 인스턴스

        SupportMapFragment supportMapFragment =(SupportMapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.sellmap);
        mapFragment.getMapAsync(this);//콜백 해줄려고
    }

    public void shopStart(View v) {
        SharedPreferences pref = getSharedPreferences("memberInformation",MODE_PRIVATE);
        try {
            PHPRequest request = new PHPRequest("http://101.101.210.207/insertLocation.php");
            String result = request.InsertLocation(pref.getString("id",""),location.getLatitude(),location.getLongitude());
            if(result.equals("1")){
                Toast.makeText(getApplication(), "등록되었습니다", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(getApplicationContext(),Selling.class);
                startActivity(i);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;

        //퍼미션 있는지 확인
        int FineLocationPermission = ContextCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        // 이미있으면 허용된거 ㅇㅇ
        int CoarseLocationPermission = ContextCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if (FineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                CoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdate(); //위치 업데이트
        } else {
            //요청
            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (this, REQUIRED_PERMISSIONS[0])) {
                Snackbar.make(Layout, "위치 권한이 필요",
                        Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityCompat.requestPermissions(SellerMain.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
                    }
                }).show();

            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }

        LatLng SEOUL = new LatLng(37.505, 126.924);//기본은 서울
        mMap.getUiSettings().setMyLocationButtonEnabled(true); //위치 버튼 가능
        MarkerOptions markerOptions = new MarkerOptions(); // 마커 생성
        markerOptions.position(SEOUL);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));  // 초기 위치
//        mMap.setMyLocationEnabled(true); //내위치 표시
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        UiSettings settings = mMap.getUiSettings();
        settings.setZoomControlsEnabled(true); //줌 버튼
    }

    LocationCallback locationCallback = new LocationCallback(){

        public void onLocationResult(LocationResult locationResult){
            super.onLocationResult(locationResult);

            List<Location> locationList = locationResult.getLocations();


            if(locationList.size()>0){
                currentlocation= locationList.get(locationList.size()-1);
                location= locationList.get(0);

                //현재위치!!!!! 사용할땐 setcurrentposition
                currentposition= new LatLng(location.getLatitude(),location.getLongitude());

                String markerTitle = getGeocoder(currentposition);// 지오코드 사용



            }
        }
    };


    public boolean onMarkerClick(Marker marker){ //마커 선택되면 가운대로
        CameraUpdate center = CameraUpdateFactory.newLatLng(marker.getPosition());
        mMap.animateCamera(center);

        return true;
    }



    public String getGeocoder(LatLng latLng){

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(
                    latLng.latitude, latLng.longitude, 1);
        }catch (IOException e){
            return "지오코드 서비스 불가";
        }catch (IllegalArgumentException e){
            return"잘못된 gps";
        }

        if (addresses==null ||addresses.size()==0){
            return "주소 잘못됨";
        }else {
            Address address = addresses.get(0);
            return address.getAddressLine(0).toString();
        }
    }

    private void startLocationUpdate(){//업데이트 해줄거
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
            mMap.setMyLocationEnabled(true);//현재위치 표시

        }

    }


    public boolean checkLocationServicesStatus() {//네트워크 and gps켯는지 ㅇㅇ
        LocationManager locationManager =(LocationManager)getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)||
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


    private  void GpsActivation(){//gps키자
        AlertDialog.Builder builder = new AlertDialog.Builder(SellerMain.this);
        builder.setTitle("위치 비활성화");
        builder.setMessage("위치 서비스를 켜주세요");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", (dialog, which) -> {
            Intent callGps= new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(callGps,GPS_ENABLE_REQUEST_CODE);
        });

        //gps 안킬꺼야
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.create().show();
    }

    protected void onActivityResult(int requestCode,int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){//gps켯는지 검사

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
