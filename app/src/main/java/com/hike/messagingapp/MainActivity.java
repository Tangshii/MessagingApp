package com.hike.messagingapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.hike.messagingapp.Fragments.ChatsFragment;
import com.hike.messagingapp.Fragments.ProfileFragment;
import com.hike.messagingapp.Fragments.UsersFragment;
import com.hike.messagingapp.Model.Chat;
import com.hike.messagingapp.Model.User;
import com.hike.messagingapp.Adapter.ViewPagerAdapter;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    CircleImageView profile_image;
    TextView username;

    private ProgressDialog spinner;

    FirebaseUser firebaseUser;
    DatabaseReference reference;
    AppBarLayout appBarLayout;
    TabLayout tabLayout;
    Toolbar toolbar;
    ViewPager viewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");


        appBarLayout = findViewById(R.id.appbar);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = (ViewPager)findViewById(R.id.view_pager);

        registerForContextMenu(toolbar);

        SharedPreferences prefs = getApplication().getSharedPreferences("colorPref", Context.MODE_PRIVATE);
        int primary = prefs.getInt("primary", -1);
        if(primary != -1){
            setToolbarColor( primary );
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("login", true);
        editor.apply();

        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        spinner = new ProgressDialog(this);
        spinner.setMessage("loading...");
        spinner.setCanceledOnTouchOutside(false);
        spinner.show();

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        // query db for profile img and set it
        setProfileFromDB();






        // get Chats table
        reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int unread = 0;
                // loop thru a Chats snapshot for messages that user did not see
                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    Chat chat = snapshot.getValue(Chat.class);
                    if(chat.getReceiver()!=null) {
                        if (chat.getReceiver().equals(firebaseUser.getUid()) && !chat.isIsseen()) {
                            unread++;
                        }
                    }
                }

                // create adapter
                ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());


                // add fragments
                if (unread == 0) {
                    viewPagerAdapter.addFragment(new ChatsFragment(), "Chats");
                } else {
                    viewPagerAdapter.addFragment(new ChatsFragment(), "Chats (" + unread + ") ");
                }
                viewPagerAdapter.addFragment(new UsersFragment(), "Users");
                viewPagerAdapter.addFragment(new ProfileFragment(), "Profile");

                // set adapter and tab layout
                viewPager.setAdapter(viewPagerAdapter);
                tabLayout.setupWithViewPager(viewPager);
                spinner.dismiss();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });




    }




    // searches db for users profile img
    void setProfileFromDB() {
        if(firebaseUser.getUid()!=null) {
            reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
            reference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    username.setText(user.getUsername());
                    if (user.getImageURL() != null) {
                        if (user.getImageURL().equals("default")) {
                            profile_image.setImageResource(R.drawable.account);
                        } else {
                            Glide.with(getApplicationContext()).load(user.getImageURL()).into(profile_image);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){

            case R.id.translate:
                Intent intent = new Intent(MainActivity.this, TranslateActivity.class);
                intent.putExtra("main", true); //put the receiver uid
                MainActivity.this.startActivity(intent);
                return true;

            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainActivity.this, StartActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;

            case R.id.colors:
                showColorDialog();

        }

        return false;
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, final View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if(v.getId() == R.id.toolbar) {
            new ColorPickerDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                    .setPreferenceName("MyColorPickerDialog")
                    .setPositiveButton("confirm",
                            new ColorEnvelopeListener() {
                                @Override
                                public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                                    setToolbarColor( envelope.getColor() );

                                }
                            })
                    .setNegativeButton("cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                    .attachAlphaSlideBar(true)
                    .attachBrightnessSlideBar(true)
                    .show();
        }
    }


    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        return true;
    }



   void showColorDialog() {
       final ColorPickerDialog.Builder colorPicker = new ColorPickerDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
               .setPreferenceName("MyColorPickerDialog")
               .attachAlphaSlideBar(true)
               .attachBrightnessSlideBar(true)
               .setNegativeButton("cancel",
                       new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialogInterface, int i) {
                               dialogInterface.dismiss();
                           }
                       });

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Color Customization");
        alertDialog.setMessage("you can also long tap on home screen");

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Set Primary color", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                colorPicker.setPositiveButton("confirm",
                        new ColorEnvelopeListener() {
                            @Override
                            public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                                setToolbarColor( envelope.getColor() );
                            }
                        }).show();
            } });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Set Secondary color", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                colorPicker.setPositiveButton("confirm",
                        new ColorEnvelopeListener() {
                            @Override
                            public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                                int color = envelope.getColor();
                                SharedPreferences prefs = getApplication().getSharedPreferences("colorPref", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putInt("secondary", color);
                                editor.apply();
                                finish();
                                startActivity(getIntent());
                            }
                        }).show();
            }});

        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Restore Default color", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                SharedPreferences prefs = getApplication().getSharedPreferences("colorPref", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove("primary");
                editor.remove("secondary");
                editor.apply();
                finish();
                startActivity(getIntent());
            }});

        alertDialog.show();
    }





    private void status(String status) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
        HashMap < String, Object > hashMap = new HashMap < > ();
        hashMap.put("status", status);
        reference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        status("offline");
    }




    void setToolbarColor(int color){
        appBarLayout.setBackgroundColor(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
        }
        SharedPreferences prefs = getApplication().getSharedPreferences("colorPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("primary", color);
        editor.apply();
    }









    //TODO
    //TODO
    // close fragments keyboard on tap outside
    @Override
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