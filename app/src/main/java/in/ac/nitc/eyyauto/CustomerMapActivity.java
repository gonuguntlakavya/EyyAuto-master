package in.ac.nitc.eyyauto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

import in.ac.nitc.eyyauto.models.User;

import static in.ac.nitc.eyyauto.Constants.CUSTOMER_REQUESTS;
import static in.ac.nitc.eyyauto.Constants.DRIVER_AVAILABLE_LOCATION;
import static in.ac.nitc.eyyauto.Constants.DRIVER_INFO_ROOT_PATH;
import static in.ac.nitc.eyyauto.Constants.DRIVER_WORKING_LOCATION;
import static in.ac.nitc.eyyauto.Constants.INTENT_USER;

public class CustomerMapActivity extends AppCompatActivity implements OnMapReadyCallback {


    private static final String TAG = "CustomerMapActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static final float DEFAULT_ZOOM = 15f;
    private User user;
    private Button mRequest;
    private EditText mDestinationLocation;
    private Location currentLocation;
    private LatLng mPickUpLocation;
    private Boolean requestBoolean = false;
    private Marker pickupMarker;

    @Override
    public void onMapReady(GoogleMap googleMap) {

        if(user!=null)
            Toast.makeText(this, "Signed in  as "+user.getName()+"\n"+user.getContact(), Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "User details are NULL", Toast.LENGTH_SHORT).show();


        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,"Permissions denied for pointing the location",Toast.LENGTH_SHORT).show();
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);

            //mDestinationLocation = findViewById(R.id.destinationLocation);
            //String location = mDestinationLocation.getText().toString();

        }
    }

    private Button mSignOut;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.customer_settings_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id==R.id.customer_signout_settings){
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(CustomerMapActivity.this,MainActivity.class);
            startActivity(intent);
            finish();
        }
        if(id==R.id.customer_profile_settings)
        {
            Intent intent =new Intent(CustomerMapActivity.this,CustomerSettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        user = (User) getIntent().getExtras().get(INTENT_USER);

        setContentView(R.layout.activity_customer_map);

        getLocationPermission();
        mRequest = findViewById(R.id.requestRide);
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestBoolean){
                    requestBoolean = false;
                    geoQuery.removeAllListeners();
                    if(driverLocationRefListener!=null){
                        driverLocationRef.removeEventListener(driverLocationRefListener);
                    }

                    if(driverFoundId != null){
                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference(DRIVER_INFO_ROOT_PATH)
                                .child(driverFoundId);
                       //driverRef.setValue(true);
                        driverRef.removeValue();
                        driverFoundId = null;
                    }
                    driverFound = false;
                    radius = 1;

                    String mUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference(CUSTOMER_REQUESTS);
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.removeLocation(mUserId);
                    if(pickupMarker !=null){
                        pickupMarker.remove();
                    }
                    Toast.makeText(CustomerMapActivity.this,"ur ride is cancelled",Toast.LENGTH_SHORT).show();
                    mRequest.setText("Find a Ride");

                }else{
                    requestBoolean = true;
                    String mUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference(CUSTOMER_REQUESTS);
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(mUserId,new GeoLocation(currentLocation.getLatitude(),currentLocation.getLongitude()));

                    mPickUpLocation = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(mPickUpLocation));

                    mRequest.setText("Finding your Driver...");

                    getClosestDrivers();
                }
            }
        });
    }

    private void initMap(){
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(CustomerMapActivity.this);
    }

    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the devices current location");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try{
            if(mLocationPermissionsGranted){

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: found location!");
                            currentLocation = (Location) task.getResult();

                            if(currentLocation!=null){
                                moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                        DEFAULT_ZOOM);
                            }
                            else{
                                Log.d(TAG, "onComplete: current location is null gps not enabled");
                                Toast.makeText(CustomerMapActivity.this, "gps not enabled:unable to get current location", Toast.LENGTH_SHORT).show();

                            }

                        }else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(CustomerMapActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }

    private void moveCamera(LatLng latLng, float zoom){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initMap();
            }else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }

    private int radius = 1;
    private boolean driverFound = false;
    private String driverFoundId;
    GeoQuery geoQuery;

    private void getClosestDrivers(){
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child(DRIVER_AVAILABLE_LOCATION);

        GeoFire geofire = new GeoFire(driverLocation);
        geoQuery = geofire.queryAtLocation(new GeoLocation(currentLocation.getLatitude(),currentLocation.getLongitude()),radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestBoolean){
                    driverFound = true;
                    driverFoundId = key;
                    Log.d("TAG",driverFoundId);
                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference(DRIVER_INFO_ROOT_PATH)
                            .child(driverFoundId);
                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("customerRideId",customerId);
                    driverRef.updateChildren(map);

                    getDriverLocation();
                    mRequest.setText("Looking for Driver Location.....");
                }
            }

            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }
            @Override
            public void onGeoQueryReady() {
                if(!driverFound && requestBoolean){
                    radius = radius++;
                    Log.d(TAG, "radius incremented");
                    getClosestDrivers();
                }
            }
            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        });
    }

        private Marker mDriverMarker;
        DatabaseReference driverLocationRef;
        ValueEventListener driverLocationRefListener;

        private void getDriverLocation(){
            driverLocationRef = FirebaseDatabase.getInstance().getReference(DRIVER_WORKING_LOCATION)
                    .child(driverFoundId)
                    .child("l");
            driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists() && requestBoolean){
                        List<Object> map = (List<Object>) dataSnapshot.getValue();
                        double locationLat=0;
                        double locationLng=0;
                        mRequest.setText("Driver Found");
                        //Toast.makeText(CustomerMapActivity.this,"found a driver",Toast.LENGTH_SHORT).show();
                        if(map.get(0)!=null) {
                            locationLat = Double.parseDouble(map.get(0).toString());
                        }
                        if(map.get(1)!=null){
                            locationLng=Double.parseDouble(map.get(1).toString());
                        }
                        LatLng driverLatLng=new LatLng(locationLat,locationLng);
                        if (mDriverMarker!=null)
                        {
                            mDriverMarker.remove();
                        }
                        mDriverMarker= mMap.addMarker(new MarkerOptions().position(driverLatLng).title("your driver"));

                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    @Override
    protected void onStop() {
        super.onStop();

    }
}
