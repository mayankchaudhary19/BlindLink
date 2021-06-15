package com.example.blindlink;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.blindlink.utils.SharedPref;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

public class AccountActivity extends AppCompatActivity {

    private Pattern VALID_CONTACT_REGEX = Pattern.compile("^((?!(0))[0-9]{10})$");
    private Pattern VALID_EMAIL_REGEX = Pattern.compile("^[a-z0-9](\\.?[a-z0-9]){5,}@g(oogle)?mail\\.com$\n");
    private CircleImageView profileImg;
    private TextInputEditText careGiverEmail, contact_no;
    private TextView saveChanges, setHomeAddr;

    float Latitude, Longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        profileImg = findViewById(R.id.profile_image);
        careGiverEmail = findViewById(R.id.caregiverEmail);
        contact_no = findViewById(R.id.contactno);
        saveChanges = findViewById(R.id.saveChanges_btn);
        setHomeAddr = findViewById(R.id.set_home_addr);

        SharedPref.init(AccountActivity.this);


        setHomeAddr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPref.write(SharedPref.HOME_LATITUDE, "XXXX");//save string in shared preference.
                SharedPref.write(SharedPref.HOME_LONGITUDE, "25");//save int in shared preference.

            }
        });

        saveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                careGiverEmail.setError(null);
                contact_no.setError(null);
                if (careGiverEmail.getText().toString().isEmpty()) {
                    careGiverEmail.setError("Required!");
                    return;
                }
                if (contact_no.getText().toString().isEmpty()) {
                    contact_no.setError("Required!");
                    return;
                }
                if (!VALID_CONTACT_REGEX.matcher(contact_no.getText().toString()).find()) {
                    contact_no.setError("Invalid Phone Number");
                    return;
                }
                if (!VALID_EMAIL_REGEX.matcher(careGiverEmail.getText().toString()).find()) {
                    careGiverEmail.setError("Invalid G-mail ID");
                    return;
                }

                SharedPref.write(SharedPref.CAREGIVER_CONTACT, contact_no.getText().toString());//save string in shared preference.
                SharedPref.write(SharedPref.CAREGIVER_EMAIL, careGiverEmail.getText().toString());//save int in shared preference.


            }
        });

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