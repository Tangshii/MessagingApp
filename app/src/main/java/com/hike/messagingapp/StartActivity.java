package com.hike.messagingapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
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

import de.hdodenhof.circleimageview.CircleImageView;

public class StartActivity extends AppCompatActivity {

    Button login, register;
    FirebaseAuth auth;
    SignInButton btSignIn;
    GoogleSignInClient googleSignInClient;
    FirebaseUser firebaseUser;
    FirebaseDatabase firebaseDatabase;


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
                Intent intent = googleSignInClient.getSignInIntent();
                startActivityForResult(intent,100);
            }
        });



        login  = findViewById(R.id.login);
        register = findViewById(R.id.register);

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==100){
            Task<GoogleSignInAccount> signInAccountTask = GoogleSignIn
                    .getSignedInAccountFromIntent(data);

            if(signInAccountTask.isSuccessful()){
                String s = "Google sign in successful";
                displayToast(s);
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
                                            //hashMap.put("isGoogleSignUp",isGoogleSignUp);

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
                                    }
                                });


                    }


                } catch (ApiException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private void displayToast(String s) {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_SHORT).show();
    }
}
