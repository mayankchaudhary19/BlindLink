package com.example.blindlink;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.blindlink.utils.Dialogs;
import com.example.blindlink.utils.SharedPref;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";

    private ImageView pause, logoutBtn;
    private CircleImageView profileImage;
    private GoogleApiClient googleApiClient;
    private GoogleSignInOptions gso;
    private CardView emergency, googleMap, shareLocation, connectArduino, call;
    private TextView checkLocationOrEmergency;
    private String contact = "";
    private String home_lat, home_long, user_type, caregiver_contact, visually_impaired_contact;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");
                //TODO: AUDIO
//                tvAppend(textView, data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            setUiEnabled(true);
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            //TODO: AUDIO
//                            tvAppend(textView, "Serial Connection Opened!\n");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(pause);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(pause);

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        SharedPref.init(MainActivity.this);

        home_lat = SharedPref.read(SharedPref.HOME_LATITUDE, "");//read string in shared preference.
        home_long = SharedPref.read(SharedPref.HOME_LONGITUDE, "");//read int in shared preference.
        user_type = SharedPref.read(SharedPref.USER_TYPE, "");//read int in shared preference.
        caregiver_contact = SharedPref.read(SharedPref.CAREGIVER_CONTACT, "");//read int in shared preference.
        visually_impaired_contact = SharedPref.read(SharedPref.VISUALLY_IMPAIRED_CONTACT, "");//read int in shared preference.

        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);

        call = findViewById(R.id.call);
        emergency = findViewById(R.id.emergency);
        googleMap = findViewById(R.id.google_map);
        shareLocation = findViewById(R.id.share_location);
        connectArduino = findViewById(R.id.connect_arduino);
        pause = findViewById(R.id.pause);
        logoutBtn = findViewById(R.id.logout_btn);
        profileImage = findViewById(R.id.profileImage);

        checkLocationOrEmergency = findViewById(R.id.check_location_or_emergency);

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent5 = new Intent(MainActivity.this, AccountActivity.class);
                startActivity(intent5);
            }
        });

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        if (user_type.equals("")) {
            final Dialog userDialog = Dialogs.setUserDialog(MainActivity.this);
            userDialog.show();
            TextView caregiverBtn = userDialog.findViewById(R.id.caregiver_user_btn);
            TextView visuallyImpairedBtn = userDialog.findViewById(R.id.visually_impaired_user_btn);

            caregiverBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPref.write(SharedPref.USER_TYPE, "caregiver");//read int in shared preference.
                    userDialog.dismiss();
                    setOption(false);
                }
            });

            visuallyImpairedBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPref.write(SharedPref.USER_TYPE, "visually-impaired");//read int in shared preference.
                    userDialog.dismiss();
                    setOption(true);
                }
            });
        } else if (user_type.equals("caregiver")) {
            setOption(false);
        } else {
            setOption(true);
        }

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                if (status.isSuccess()) {
                                    SharedPref.clearPref();
                                    Intent intent = new Intent(MainActivity.this, AuthenticationActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(getApplicationContext(), "Session not close", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });

        emergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                    Intent intent2 = new Intent(MainActivity.this, ScanActivity.class);
//                    startActivity(intent2);
            }
        });

        googleMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                INTENT ON MAPS
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + home_lat + "," + home_long + "&mode=w");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
//                    Intent intent4 = new Intent(MainActivity.this, FirebaseUploadActivity.class);
//                    startActivity(intent4);
            }
        });
        shareLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                    Intent intent5 = new Intent(MainActivity.this, EditAccountActivity.class);
//                    startActivity(intent5);
            }
        });
        connectArduino.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUiEnabled(false);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_USB_PERMISSION);
                filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
                filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
                registerReceiver(broadcastReceiver, filter);
            }
        });

        contact = user_type.equals("caregiver") ? caregiver_contact : visually_impaired_contact;

        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!contact.equals("")) {
                    try {
                        if (Build.VERSION.SDK_INT > 22) {
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CALL_PHONE}, 101);
                                try {
                                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                                    callIntent.setData(Uri.parse("tel:" + contact));
                                    startActivity(callIntent);
                                } catch (Error ignored) {
                                }
                                return;
                            }
                            Intent callIntent = new Intent(Intent.ACTION_CALL);
                            callIntent.setData(Uri.parse("tel:" + contact));
                            startActivity(callIntent);
                        } else {
                            Intent callIntent = new Intent(Intent.ACTION_CALL);
                            callIntent.setData(Uri.parse("tel:" + contact));
                            startActivity(callIntent);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please add contact in account section!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void setOption(boolean b) {
        if (b) {
            checkLocationOrEmergency.setText("EMERGENCY");
            googleMap.setVisibility(View.VISIBLE);
            shareLocation.setVisibility(View.VISIBLE);
            connectArduino.setVisibility(View.VISIBLE);
            pause.setVisibility(View.VISIBLE);
        } else {
            checkLocationOrEmergency.setText("CHECK LOCATION");
        }
        emergency.setVisibility(View.VISIBLE);
    }

    public void setUiEnabled(boolean bool) {
        if (bool) {
            pause.setImageResource(R.drawable.ic_baseline_pause_circle_filled_24);
        } else {
            pause.setImageResource(R.drawable.ic_baseline_play_circle_filled_24);
        }
    }

    public void onClickStart(ImageView view) {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                //TODO: CHANGE VENDOR ID
                if (deviceVID == Integer.parseInt("1A86", 16))//Arduino Vendor ID
                {
                    setUiEnabled(false);
//                    view.setImageResource(R.drawable.ic_baseline_play_circle_filled_24);
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    setUiEnabled(true);
//                    view.setImageResource(R.drawable.ic_baseline_pause_circle_filled_24);
                    connection = null;
                    device = null;
                }
                if (!keep)
                    break;
            }
        }
    }

    public void onClickStop(ImageView view) {
        setUiEnabled(false);
//        view.setImageResource(R.drawable.ic_baseline_play_circle_filled_24);
        serialPort.close();
//        tvAppend(textView, "\nSerial Connection Closed! \n");
    }

//    public void onClickClear(View view) {
//        textView.setText(" ");
//    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.setText(ftext);
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(googleApiClient);
        if (opr.isDone()) {
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                    handleSignInResult(googleSignInResult);
                }
            });
        }

        home_lat = SharedPref.read(SharedPref.HOME_LATITUDE, "");//read string in shared preference.
        home_long = SharedPref.read(SharedPref.HOME_LONGITUDE, "");//read int in shared preference.
        user_type = SharedPref.read(SharedPref.USER_TYPE, "");//read int in shared preference.
        caregiver_contact = SharedPref.read(SharedPref.CAREGIVER_CONTACT, "");//read int in shared preference.
        visually_impaired_contact = SharedPref.read(SharedPref.VISUALLY_IMPAIRED_CONTACT, "");//read int in shared preference.

    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            GoogleSignInAccount account = result.getSignInAccount();
//            userName.setText(account.getDisplayName());
            try {
                Glide.with(this).load(account.getPhotoUrl()).into(profileImage);
            } catch (NullPointerException e) {
                Toast.makeText(getApplicationContext(), "image not found", Toast.LENGTH_LONG).show();
            }
        } else {
            Intent intent = new Intent(this, AuthenticationActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}