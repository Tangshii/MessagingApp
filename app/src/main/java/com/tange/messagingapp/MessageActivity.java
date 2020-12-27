package com.tange.messagingapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import com.tange.messagingapp.Adapter.MessageAdapter;
import com.tange.messagingapp.Notifications.APIService;

import com.tange.messagingapp.Model.Chat;
import com.tange.messagingapp.Model.TranslateViewModel;
import com.tange.messagingapp.Model.User;
import com.tange.messagingapp.Notifications.Client;
import com.tange.messagingapp.Notifications.Data;
import com.tange.messagingapp.Notifications.MyResponse;
import com.tange.messagingapp.Notifications.Sender;
import com.tange.messagingapp.Notifications.Token;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageActivity extends AppCompatActivity implements MessageAdapter.OnItemClickListener{

    CircleImageView profile_image;
    TextView username;
    ImageButton btn_send;
    EditText text_send;
    RecyclerView recyclerView;

    FirebaseUser fuser;
    DatabaseReference reference;

    ProgressDialog loading;

    MessageAdapter messageAdapter;
    List<Chat> mChat; //holds the user's chats with others

    Intent intent;
    ValueEventListener seenListener;
    String receiverId;
    Toolbar toolbar;
    RelativeLayout relativeLayout;

    APIService apiService;
    boolean notify = false;

    private TranslateViewModel translateModel;
    private LinearLayoutManager linearLayoutManager;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        // initialize widgets
        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        btn_send = findViewById(R.id.btn_send);
        text_send = findViewById(R.id.text_send);
        relativeLayout = findViewById(R.id.bottom);
        loading = new ProgressDialog(this);
        loading.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Translate model
        translateModel = new ViewModelProvider(this).get(TranslateViewModel.class);

        // set up the toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() { // set back button
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        // recycler view
        recyclerView = findViewById(R.id.recycler_view); //find it
        // create manager
        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true); // start from bottom
        recyclerView.setLayoutManager(linearLayoutManager); // set manager to recycler view

        // get the tapped on receiver intent extra from user adapter
        intent = getIntent();
        receiverId = intent.getStringExtra("userid");
        String translated_msg = intent.getStringExtra("translated_msg");
        if(translated_msg != "error")
            text_send.setText(translated_msg);

        // get language pref
        SharedPreferences prefs = getApplication().getSharedPreferences("languages", Context.MODE_PRIVATE);
        String source = prefs.getString("source", "error");
        String target = prefs.getString("target", "error");
        if(source!="error"&&target!="error")
            translateModel.setLanguages(source,target);

        //get color pref
        SharedPreferences colorPref = getApplication().getSharedPreferences("colorPref", Context.MODE_PRIVATE);
        int primary = colorPref.getInt("primary", -1);
        int secondary = colorPref.getInt("secondary", -1);
        if(primary != -1){
            toolbar.setBackgroundColor(primary);
            btn_send.setBackgroundTintList(ColorStateList.valueOf(primary));
            setToolbarColor(primary);
        }
        if(secondary != -1){
            recyclerView.setBackgroundColor(secondary);
            relativeLayout.setBackgroundColor(secondary);
        }

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        // get user who is sender
        fuser = FirebaseAuth.getInstance().getCurrentUser();

        // tapped send
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notify = true;
                String msg = text_send.getText().toString();
                if (!msg.equals("")) {
                    sendMessage(fuser.getUid(), receiverId, msg);
                } else {
                    Toast.makeText(MessageActivity.this, "You can't send empty message", Toast.LENGTH_SHORT).show();
                }
                text_send.setText("");
            }
        });

        setProfilePic();

        seenMessage(receiverId);

        // See reicver profile page
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProfileDialog(view);
            }
        });

    }



    // send message
    private void sendMessage(String sender, final String receiver, String message) {
        //loading.show(); // show spinner
        // create reference to db
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        // put message info into hash map
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);
        hashMap.put("isseen", false);

        // push to Chats table
        reference.child("Chats").push().setValue(hashMap);

        // get ref to Chat list table with ids of Sender and Receiver
        final DatabaseReference chatList = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(fuser.getUid()).child(receiverId);

        // adds a chat list
        chatList.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) { // if no chat list exist for the message
                    chatList.child("id").setValue(receiverId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        // set the receiver chat list
        final DatabaseReference chatRefReceiver = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(receiverId).child(fuser.getUid());

        chatRefReceiver.child("id").setValue(fuser.getUid());

        final String msg = message;
        // search Users table
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (notify) { // make sure to send notification only once
                    sendNotification(receiver, user.getUsername(), msg); // send notification
                }
                notify = false;
                //loading.dismiss();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });


    }


    private void sendNotification(String receiver, final String username, final String message) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Token token = snapshot.getValue(Token.class);

                    // create data that goes into a notification
                    Data data = new Data(fuser.getUid(), R.drawable.ic_launcher_foreground, "  "+message,
                            username + ":", receiverId);
                    // put data with a sender
                    Sender sender = new Sender(data, token.getToken());

                    // user api service to send notification
                    apiService.sendNotification(sender).enqueue(new Callback<MyResponse>() {
                        @Override
                        public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                            if (!response.isSuccessful()) {
                                //Toast.makeText(MessageActivity.this, "Failed to send", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<MyResponse> call, Throwable t) {}
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }


    void setProfilePic() {
        receiverId = intent.getStringExtra("userid");
        // get Users table
        reference = FirebaseDatabase.getInstance().getReference("Users").child(receiverId);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class); // get a snapshot
                username.setText(user.getUsername()); // set receiver name at toolbar
                if (user.getImageURL().equals("default")) { // set default profile
                    profile_image.setImageResource(R.drawable.account);
                } else { // else  load their save profile img
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(profile_image);
                }
                // read messages
                readMessages(fuser.getUid(), receiverId, user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

    }


    private void readMessages(final String myid, final String userid, final String imageurl) {
        mChat = new ArrayList<>();

        // read messages between sender and receiver
        reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mChat.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chat chat = snapshot.getValue(Chat.class);
                    if(chat.getReceiver()!=null && chat.getReceiver()!=null) {
                        if (chat.getReceiver().equals(myid) && chat.getSender().equals(userid) ||
                                chat.getReceiver().equals(userid) && chat.getSender().equals(myid)) {
                            mChat.add(chat);
                        }
                    }
                }

                // search for user/sender's profile pic for messages
                reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
                reference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class); // get a snapshot

                        if (user.getImageURL().equals("default")) {

                            //create message adapter and set it to recycler view using default img
                            messageAdapter = new MessageAdapter(MessageActivity.this, mChat, imageurl, "default", MessageActivity.this);
                            recyclerView.setAdapter(messageAdapter);
                        } else { // else  load their save profile img

                            //create message adapter and set it to recycler view using their profile pic
                            messageAdapter = new MessageAdapter(MessageActivity.this, mChat, imageurl, user.getImageURL(), MessageActivity.this);
                            recyclerView.setAdapter(messageAdapter);
                        }

                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    // set user's message to seen to true
    private void seenMessage(final String userid) {
        reference = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chat chat = snapshot.getValue(Chat.class);
                    if(chat.getReceiver()!=null && chat.getReceiver()!=null) {
                        if (chat.getReceiver().equals(fuser.getUid()) && chat.getSender().equals(userid)) {
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("isseen", true);
                            snapshot.getRef().updateChildren(hashMap);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }



    @Override
    // when user tap on a message
    public void onItemClick(final int position) {
        final Chat chat = mChat.get(position);
        final String msg = chat.getMessage();

        // see if there is a previous translation
        if(chat.getPrevTranslate()!=null){
            // flip flop the previous translation back to ordinal language
            chat.setMessage( chat.getPrevTranslate() );
            chat.setPrevTranslate( msg );
            messageAdapter.notifyItemChanged(position);
        }
        else {
            // get translation model and translate the message
            translateModel.getTranslator().translate(msg).addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    if (task.isSuccessful()) {
                        chat.setMessage(task.getResult()); // set message to new translation
                        chat.setPrevTranslate(msg); // save the original text

                        Toast.makeText(MessageActivity.this, task.getResult(), Toast.LENGTH_SHORT).show();

                        messageAdapter.notifyItemChanged(position); // change the the recycler view item
                    } else {
                        Toast.makeText(MessageActivity.this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

    }


    // show friends profile in dialog
    public void showProfileDialog(View view) {
        // create an alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // set the custom layout
        final View customLayout = getLayoutInflater().inflate(R.layout.others_profile, null);
        final CircleImageView image_profile = customLayout.findViewById(R.id.profile_image);
        final TextView username = customLayout.findViewById(R.id.username);
        final TextView user_title = customLayout.findViewById(R.id.user_title);
        final TextView bio = customLayout.findViewById(R.id.bio);

        if(receiverId!=null) {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(receiverId);
            reference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);

                    if (user.getImageURL().equals("default")) { // set default profile
                        image_profile.setImageResource(R.drawable.account);
                    } else { // else  load their save profile img
                        Glide.with(getApplicationContext()).load(user.getImageURL()).into(image_profile);
                    }
                    bio.setText(user.getBio());
                    username.setText(user.getUsername());
                    user_title.setText(user.getUsername()+"'s profile");
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }
        builder.setView(customLayout);
        // add a button
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        menu.findItem(R.id.logout).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {

            case R.id.translate:
                Intent intent = new Intent(MessageActivity.this, TranslateActivity.class);
                intent.putExtra("receiverId", receiverId); //put the receiver uid
                //intent.putExtra("msg", text_send.getText().toString() );
                MessageActivity.this.startActivity(intent);
                return true;

            case R.id.colors:
                showColorDialog();
        }
        return false;
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
                                setToolbarColor(envelope.getColor());
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

    void setToolbarColor(int color){
        toolbar.setBackgroundColor(color);
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

    private void currentUser(String userid) {
        SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
        editor.putString("currentuser", userid);
        editor.apply();
    }

    private void status(String status) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);
        reference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
        currentUser(receiverId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        reference.removeEventListener(seenListener);
        status("offline");
        currentUser("none");
    }

}