package com.example.blindlink;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.blindlink.utils.SharedPref;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

public class AccountActivity extends AppCompatActivity {

    private Pattern VALID_CONTACT_REGEX = Pattern.compile("^((?!(0))[0-9]{10})$");
    private TextInputEditText visuallyImpairedContact, caregiverContact, visuallyImpairedAddress;
    private TextView saveChanges, setHomeAddr, profileusername;
    private ImageView back;
    int PERMISSION_ID = 44;
    float Latitude, Longitude;
    FusedLocationProviderClient mFusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        getSupportActionBar().hide();
        visuallyImpairedContact = findViewById(R.id.visually_impaired_contact);
        caregiverContact = findViewById(R.id.caregiver_contact);
        saveChanges = findViewById(R.id.saveChanges_btn);
        setHomeAddr = findViewById(R.id.set_home_addr);
        visuallyImpairedAddress = findViewById(R.id.visually_impaired_addr);
        profileusername = findViewById(R.id.profileusername);
        back = findViewById(R.id.img_back);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        SharedPref.init(AccountActivity.this);
        String userType = SharedPref.read(SharedPref.USER_TYPE, "");
        String caregiver_contact = SharedPref.read(SharedPref.CAREGIVER_CONTACT, "");//read int in shared preference.
        String visually_impaired_contact = SharedPref.read(SharedPref.VISUALLY_IMPAIRED_CONTACT, "");//read int in shared preference.
        String visually_impaired_addr = SharedPref.read(SharedPref.VISUALLY_IMPAIRED_ADDRESS, "");//read int in shared preference.

        if(userType.equals("caregiver")){
            profileusername.setText("Hello Caregiver,");
        }else{
            profileusername.setText("Hello Friend,");
        }
        visuallyImpairedAddress.setText(visually_impaired_addr);
        visuallyImpairedContact.setText(visually_impaired_contact);
        caregiverContact.setText(caregiver_contact);

//        Toast.makeText(this, ""+getLocationFromAddress(AccountActivity.this, "634, R. K. Puram, New Delhi-110022"), Toast.LENGTH_SHORT).show();
        setHomeAddr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // method to get the location
                getLastLocation();
                //TODO: LOCATION
//                SharedPref.write(SharedPref.HOME_LATITUDE, "XXXX");//save string in shared preference.
//                SharedPref.write(SharedPref.HOME_LONGITUDE, "25");//save int in shared preference.
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        saveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (caregiverContact.getText().toString().isEmpty()) {
                    caregiverContact.setError("Required!");
                    return;
                }
                if (!VALID_CONTACT_REGEX.matcher(caregiverContact.getText().toString()).find()) {
                    caregiverContact.setError("Invalid Phone Number");
                    return;
                }
                if (visuallyImpairedContact.getText().toString().isEmpty()) {
                    visuallyImpairedContact.setError("Required!");
                    return;
                }
                if (!VALID_CONTACT_REGEX.matcher(visuallyImpairedContact.getText().toString()).find()) {
                    visuallyImpairedContact.setError("Invalid Phone Number");
                    return;
                }
                SharedPref.write(SharedPref.CAREGIVER_CONTACT, caregiverContact.getText().toString());//save string in shared preference.
                SharedPref.write(SharedPref.VISUALLY_IMPAIRED_ADDRESS, visuallyImpairedAddress.getText().toString());//save string in shared preference.
                SharedPref.write(SharedPref.VISUALLY_IMPAIRED_CONTACT, visuallyImpairedContact.getText().toString());//save int in shared preference.
                Toast.makeText(AccountActivity.this, "" + getLocationFromAddress(AccountActivity.this, visuallyImpairedAddress.getText().toString()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public LatLng getLocationFromAddress(Context context, String strAddress) {
        Geocoder coder = new Geocoder(context);
        List<Address> address;
        LatLng p1 = null;
        try {
            // May throw an IOException
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            p1 = new LatLng(location.getLatitude(), location.getLongitude());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return p1;
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        // check if permissions are given
        if (checkPermissions()) {
            // check if location is enabled
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location == null) {
                            requestNewLocationData();
                        } else {
                            Toast.makeText(AccountActivity.this, "" + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show();
//                            latitudeTextView.setText(location.getLatitude() + "");
//                            longitTextView.setText(location.getLongitude() + "");
                        }
                    }
                });
            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            // if permissions aren't available,
            // request for permissions
            requestPermissions();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        // Initializing LocationRequest
        // object with appropriate methods
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);
        // setting LocationRequest
        // on FusedLocationClient
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            Toast.makeText(AccountActivity.this, "" + mLastLocation.getLatitude() + ", " + mLastLocation.getLongitude(), Toast.LENGTH_SHORT).show();
//            latitudeTextView.setText("Latitude: " + mLastLocation.getLatitude() + "");
//            longitTextView.setText("Longitude: " + mLastLocation.getLongitude() + "");
        }
    };

    // method to check for permissions
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        // If we want background location
        // on Android 10.0 and higher,
        // use:
        // ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // method to request for permissions
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    // method to check
    // if location is enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // If everything is alright then
    @Override
    public void
    onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }
    }

    private Float convertToDegree(String stringDMS) {
        Float result;
        String[] DMS = stringDMS.split(",", 3);

        String[] stringD = DMS[0].split("/", 2);
        Double D0 = new Double(stringD[0]);
        Double D1 = new Double(stringD[1]);
        Double FloatD = D0 / D1;

        String[] stringM = DMS[1].split("/", 2);
        Double M0 = new Double(stringM[0]);
        Double M1 = new Double(stringM[1]);
        Double FloatM = M0 / M1;

        String[] stringS = DMS[2].split("/", 2);
        Double S0 = new Double(stringS[0]);
        Double S1 = new Double(stringS[1]);
        Double FloatS = S0 / S1;

        result = new Float(FloatD + (FloatM / 60) + (FloatS / 3600));

        return result;


    }
}