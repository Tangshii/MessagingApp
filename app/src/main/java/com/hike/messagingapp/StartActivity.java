package com.hike.messagingapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;


public class StartActivity extends AppCompatActivity {

    Button login, register, phone;
    FirebaseAuth auth;
    SignInButton btSignIn;
    GoogleSignInClient googleSignInClient;
    FirebaseUser firebaseUser;

    private ProgressDialog loadingBar;



    protected void onStart(){
        super.onStart();

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if(firebaseUser != null){
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(new Intent(StartActivity.this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        btSignIn = findViewById(R.id.btn_signin);
        login  = findViewById(R.id.login);
        register = findViewById(R.id.register);
        phone = findViewById(R.id.phone_button);

        loadingBar = new ProgressDialog(this);

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("818774222746-q188m2m6al4k65vkev9c5k3hud1i8lkb.apps.googleusercontent.com")
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(StartActivity.this
                ,googleSignInOptions);


        auth = FirebaseAuth.getInstance();

        FirebaseUser firebaseUser = auth.getCurrentUser();

        if(firebaseUser != null){
            startActivity(new Intent(StartActivity.this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }


        btSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingBar.setTitle("Loading Account...");
                loadingBar.setCanceledOnTouchOutside(true);
                loadingBar.show();
                Intent intent = googleSignInClient.getSignInIntent();
                startActivityForResult(intent,100);
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(StartActivity.this, LoginActivity.class));
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(StartActivity.this, RegisterActivity.class));
            }
        });

        phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent phoneIntent = new Intent(StartActivity.this, PhoneActivity.class);
                startActivity(phoneIntent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==100){
            Task<GoogleSignInAccount> signInAccountTask = GoogleSignIn
                    .getSignedInAccountFromIntent(data);

            if(signInAccountTask.isSuccessful()){
                String s = "Google sign in successful";
                Toast.makeText(getApplicationContext(),s,Toast.LENGTH_SHORT).show();
                try{
                    GoogleSignInAccount googleSignInAccount = signInAccountTask
                            .getResult(ApiException.class);
                    if(googleSignInAccount != null){
                        AuthCredential authCredential = GoogleAuthProvider
                                .getCredential(googleSignInAccount.getIdToken()
                                        ,null);

                        auth.signInWithCredential(authCredential)
                                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if(task.isSuccessful()){
                                            startActivity(new Intent(StartActivity.this
                                                    ,StartActivity.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                            auth = FirebaseAuth.getInstance();

                                            FirebaseUser firebaseUser = auth.getCurrentUser();
                                            String userid = firebaseUser.getUid();

                                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);
                                            //String isGoogleSignUp = "true";
                                            String username = firebaseUser.getDisplayName();
                                            String profile_image = firebaseUser.getPhotoUrl().toString();
                                            HashMap<String, String > hashMap = new HashMap<>();
                                            hashMap.put("id",userid);
                                            hashMap.put("username",username);
                                            hashMap.put("imageURL",profile_image);
                                            hashMap.put("isGoogleSignUp","true");
                                            hashMap.put("status","offline");
                                            hashMap.put("search",username.toLowerCase());

                                            reference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()){
                                                        Intent intent = new Intent(StartActivity.this, MainActivity.class);
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                        startActivity(intent);
                                                        finish();
                                                    }

                                                } });


                                            finish();
                                        }
                                        else{
                                            Toast.makeText(StartActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                                        }

                                        loadingBar.dismiss();
                                    }
                                });


                    }


                } catch (ApiException e) {
                    e.printStackTrace();
                }

            }
        }


    }

}
