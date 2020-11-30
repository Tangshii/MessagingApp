package com.hike.messagingapp;

import android.app.ProgressDialog;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hike.messagingapp.Model.User;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class PhoneActivity extends AppCompatActivity {
    private Button SendVerificationCodeButton, VerifyButton;
    private EditText InputPhoneNumber, InputVerificationCode;

    MaterialEditText username;

    TextView error;


    DatabaseReference reference;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private FirebaseAuth mAuth;

    private ProgressDialog loadingBar;

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        username = findViewById(R.id.username);
        error = findViewById(R.id.error);



        mAuth = FirebaseAuth.getInstance();


        SendVerificationCodeButton = (Button) findViewById(R.id.send_ver_code_button);
        VerifyButton = (Button) findViewById(R.id.verify_button);
        InputPhoneNumber = (EditText) findViewById(R.id.phone_number_input);
        InputVerificationCode = (EditText) findViewById(R.id.verification_code_input);
        loadingBar = new ProgressDialog(this);

        // tap send Verificationn code button
        SendVerificationCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String phoneNumber = InputPhoneNumber.getText().toString();
                String name = username.getText().toString();
                final String fname = username.getText().toString();
                // check if empty fields
                if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(name)) {
                    Toast.makeText(PhoneActivity.this, "enter username and phone-number ", Toast.LENGTH_SHORT).show();
                }
                else {
                    loadingBar.setTitle("Verifying...");
                    loadingBar.setMessage("Verifying...");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();
                    // check is username is taken, and create phone request
                    checkUsername(fname, phoneNumber);
                }

            }

        });

        // tap verify code button
        VerifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                String verificationCode = InputVerificationCode.getText().toString();
                String name = username.getText().toString();

                if (TextUtils.isEmpty(verificationCode)) {
                    Toast.makeText(PhoneActivity.this, "Please enter verification code", Toast.LENGTH_SHORT).show();
                }
                else {
                    loadingBar.setTitle("Verifying  code...");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verificationCode);
                    signInWithPhoneAuthCredential(credential, name);
                }
            }
        });


        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential)
            {
                // this auto login on text receive
                String name = username.getText().toString();
                signInWithPhoneAuthCredential(phoneAuthCredential, name);
            }

            @Override
            public void onVerificationFailed(FirebaseException e)
            {
                loadingBar.dismiss();
                Toast.makeText(PhoneActivity.this, e +"", Toast.LENGTH_SHORT).show();
                Log.e("####################", e +"");
            }

            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token)
            {
                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                loadingBar.dismiss();
                Toast.makeText(PhoneActivity.this, "Code has been sent", Toast.LENGTH_SHORT).show();

            }
        };


        // close keyboard on tap outside
        if(findViewById(R.id.container) != null) {
            findViewById(R.id.container).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (getCurrentFocus().getWindowToken() != null) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }
                    return true;
                }
            });
        }

    }

    void checkUsername(final String fname, final String phoneNumber){
        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean alreadyExist = false;
                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (user.getUsername() != null) {

                        if (user.getUsername().equals(fname)) {
                            alreadyExist = true;
                            Log.e("AAAAAAAAAAAAAAA", user.getUsername());

                        }
                    }
                }

                if(alreadyExist){
                    error.setText("username already exist");
                    error.setVisibility(View.VISIBLE);
                    loadingBar.dismiss();
                }
                else{
                    error.setVisibility(View.INVISIBLE);

                    InputVerificationCode.setVisibility(View.VISIBLE);
                    VerifyButton.setVisibility(View.VISIBLE);

                    PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNumber, 10,
                            TimeUnit.SECONDS, PhoneActivity.this, callbacks);
                }
                return;

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }


    private void signInWithPhoneAuthCredential(final PhoneAuthCredential credential, final String username) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful())
                        {
                            loadingBar.dismiss();
                            Toast.makeText(PhoneActivity.this, "Login success", Toast.LENGTH_SHORT).show();
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();

                            insertUser(firebaseUser.getUid(), username);

                            Intent intent = new Intent(PhoneActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }
                        else
                        {
                            String message = task.getException().toString();
                            Toast.makeText(PhoneActivity.this, "Error : "  +  message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }



    private void insertUser(String userid, String username){
        // get ref to User table
        reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);

        // put user info into hash map
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("id", userid);
        hashMap.put("username", username);
        hashMap.put("imageURL", "default");
        hashMap.put("status", "offline");
        hashMap.put("search", username.toLowerCase());
        hashMap.put("isPhoneSignUp","true");

        reference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // start main activity
                if (task.isSuccessful()){
                    Intent intent = new Intent(PhoneActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        });
        loadingBar.dismiss();
    }




}

