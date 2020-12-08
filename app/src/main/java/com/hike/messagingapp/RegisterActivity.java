package com.hike.messagingapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hike.messagingapp.Model.Chat;
import com.hike.messagingapp.Model.User;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    MaterialEditText username, email, password;
    TextView error;
    Button btn_register;

    FirebaseAuth auth;
    DatabaseReference reference;

    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // set tool bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // initialize loading spinner
        loadingBar = new ProgressDialog(this);

        // find widgets
        username = findViewById(R.id.username);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        btn_register = findViewById(R.id.btn_register);
        error = findViewById(R.id.error);
        error.setVisibility(View.GONE);

        auth = FirebaseAuth.getInstance();

        // when register btn tapped
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String txt_username = username.getText().toString();
                String txt_email = email.getText().toString();
                String txt_password = password.getText().toString();
                // display toast if empty field
                if (TextUtils.isEmpty(txt_username) || TextUtils.isEmpty(txt_email) || TextUtils.isEmpty(txt_password)){
                    Toast.makeText(RegisterActivity.this, R.string.fill_fields, Toast.LENGTH_SHORT).show();
                    // display toast password too short
                } else if (txt_password.length() < 6 ){
                    Toast.makeText(RegisterActivity.this, "password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                } else {
                    register(txt_username, txt_email, txt_password);
                }
            }
        });

    }


    // register using firebase auth
    private void register(final String username, final String email, final String password){
        // display loading spinner
        loadingBar.setTitle("Creating Account...");
        loadingBar.setCanceledOnTouchOutside(true);
        loadingBar.show();
        // TODO comment user aleady exist
        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            // search if username exist already
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean alreadyExist = false;
                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (user.getUsername() != null) {

                        if (user.getUsername().equals(username)) {
                            alreadyExist = true;

                        }
                    }
                }

                // show that username is taken
                if(alreadyExist){
                    error.setText("username already exist");
                    error.setVisibility(View.VISIBLE);
                    loadingBar.dismiss();
                }
                // create the user account
                else{
                    auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()){
                                        FirebaseUser firebaseUser = auth.getCurrentUser();
                                        assert firebaseUser != null;
                                        // insert new user account into db
                                        insertUser( firebaseUser.getUid(), username );
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Invalid email or password, try another", Toast.LENGTH_SHORT).show();
                                    }
                                    loadingBar.dismiss();
                                }
                            });
                }
                return;

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
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

        reference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // start main activity
                if (task.isSuccessful()){
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        });
        loadingBar.dismiss();
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

}
