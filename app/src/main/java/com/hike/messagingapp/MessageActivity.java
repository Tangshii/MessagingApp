package com.hike.messagingapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.hike.messagingapp.Adapter.MessageAdapter;
import com.hike.messagingapp.Fragments.APIService;
import com.hike.messagingapp.Model.Chat;
import com.hike.messagingapp.Model.User;
import com.hike.messagingapp.Notifications.Client;
import com.hike.messagingapp.Notifications.Data;
import com.hike.messagingapp.Notifications.MyResponse;
import com.hike.messagingapp.Notifications.Sender;
import com.hike.messagingapp.Notifications.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageActivity extends AppCompatActivity {


    // TODO comment more
    CircleImageView profile_image;
    TextView username;
    ImageButton btn_send;
    EditText text_send;
    RecyclerView recyclerView;

    FirebaseUser fuser;
    DatabaseReference reference;

    ProgressDialog spinner;

    MessageAdapter messageAdapter;
    List < Chat > mchat; //holds the user's chats with others

    Intent intent;
    ValueEventListener seenListener;
    String receiverId;

    APIService apiService;
    boolean notify = false;

    private ImageButton SendFilesButton;
    private String checker ="", myUrl ="";
    private StorageTask uploadTask;
    private Uri fileUri;
    private DatabaseReference RootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        // set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() { // set back button
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        // recycler view
        recyclerView = findViewById(R.id.recycler_view); //find it
        //recyclerView.setHasFixedSize(true);
        // create manager
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true); // start from bottom
        recyclerView.setLayoutManager(linearLayoutManager); // set manager to recycler view

        // initialize widgets
        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        btn_send = findViewById(R.id.btn_send);
        text_send = findViewById(R.id.text_send);
        spinner = new ProgressDialog(this);
        SendFilesButton = (ImageButton) findViewById(R.id.send_files_btn);

        // get the tapped on receiver intent extra from user adapter
        intent = getIntent();
        receiverId = intent.getStringExtra("userid");

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

        SendFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                 CharSequence options[] = new CharSequence[]
                         {
                                 "Images",
                                 "PDF Files",
                                 "MS Word Files"
                         };
                AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
                builder.setTitle("Select the File");

                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(i ==0)
                        {
                            checker = "image";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent.createChooser(intent, "Select Image"), 438);
                        }
                        if(i ==1)
                        {
                            checker = "pdf";

                        }
                        if(i ==2)
                        {
                            checker = "docx";
                        }
                    }
                });
                builder.show();
            }
        });


        setProfilePic();

        seenMessage(receiverId);


        // close keyboard on tap outside
        if(findViewById(R.id.recycler_view) != null) {
            findViewById(R.id.recycler_view).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (getCurrentFocus() != null) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }
                    return true;
                }
            });
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==438 && resultCode == RESULT_OK && data!= null && data.getData()!= null)
        {
            fileUri = data.getData();

            if(!checker.equals("image"))
            {

            }
            else if(checker.equals("image"))
            {
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");

                String messageSenderRef = "ChatList/" + fuser.getUid() + "/" + receiverId;
                String messageReceiverRef = "ChatList/" + receiverId + "/" + fuser.getUid();

                final DatabaseReference userMessageKeyRef = FirebaseDatabase.getInstance().getReference("Chatlist")
                        .child(fuser.getUid()).child(receiverId);

                String messagePushID = userMessageKeyRef.getKey();

                final StorageReference filePath = storageReference.child(messagePushID + "." + "jpg");

                uploadTask = filePath.putFile(fileUri);

                uploadTask.continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {
                        if(!task.isSuccessful()){
                            throw task.getException();
                        }
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if(task.isSuccessful()){
                            Uri downloadUrl = task.getResult();
                            myUrl = downloadUrl.toString();

                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

                            // put message info into hash map
                            HashMap < String, Object > hashMapImage = new HashMap < > ();
                            hashMapImage.put("sender", fuser.getUid());
                            hashMapImage.put("receiver", receiverId);
                            hashMapImage.put("message", myUrl);
                            hashMapImage.put("name", fileUri.getLastPathSegment());
                            hashMapImage.put("type", checker);
                            hashMapImage.put("isseen", false);

                            // push to Chats table
                            reference.child("Chats").push().setValue(hashMapImage);

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
                                public void onCancelled(@NonNull DatabaseError databaseError) {}
                            });

                            //TODO ???????????????????????????????????????????????????
                            final DatabaseReference chatRefReceiver = FirebaseDatabase.getInstance().getReference("Chatlist")
                                    .child(receiverId).child(fuser.getUid());

                            chatRefReceiver.child("id").setValue(fuser.getUid());



                        }
                    }
                });
            }
            else
            {
                Toast.makeText(this, "Nothing selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // send message
    private void sendMessage(String sender, final String receiver, String message) {
        spinner.show(); // show spinner

        // create reference to db
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        // put message info into hash map
        HashMap < String, Object > hashMap = new HashMap < > ();
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
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        //TODO ???????????????????????????????????????????????????
        final DatabaseReference chatRefReceiver = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(receiverId).child(fuser.getUid());

        chatRefReceiver.child("id").setValue(fuser.getUid());


        final String msg = message;
        // search Users table
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (notify) { // make sure to send notification only once
                    sendNotification(receiver, user.getUsername(), msg); // send notification
                }
                notify = false;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        spinner.dismiss();
    }


    private void sendNotification(String receiver, final String username, final String message) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    Token token = snapshot.getValue(Token.class);

                    // create data that goes into a notification
                    Data data = new Data(fuser.getUid(), R.mipmap.ic_launcher, "",
                            username + ": " + message, receiverId);
                    // put data with a sender
                    Sender sender = new Sender(data, token.getToken());

                    // user api service to send notification
                    apiService.sendNotification(sender).enqueue(new Callback <MyResponse> () {
                        @Override
                        public void onResponse(Call <MyResponse> call, Response <MyResponse> response) {
                            if (response.code() == 200) {
                                if (response.body().success != 1) {
                                    Toast.makeText(MessageActivity.this, "Failed to send", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call <MyResponse> call, Throwable t) { }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    void setProfilePic() {
        // get Users table
        reference = FirebaseDatabase.getInstance().getReference("Users").child(receiverId);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class); // get a snapshot
                username.setText(user.getUsername()); // set receiver name at toolbar
                if (user.getImageURL().equals("default")) { // set default profile
                    profile_image.setImageResource(R.mipmap.ic_launcher);
                } else { // else  load their save profile img
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(profile_image);
                }
                // read messages
                readMessages(fuser.getUid(), receiverId, user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void seenMessage(final String userid) {
        reference = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(fuser.getUid()) && chat.getSender().equals(userid)) {
                        HashMap < String, Object > hashMap = new HashMap < > ();
                        hashMap.put("isseen", true);
                        snapshot.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    private void readMessages(final String myid, final String userid, final String imageurl) {
        mchat = new ArrayList < > ();

        // read messages between sender and receiver
        reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mchat.clear();
                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(myid) && chat.getSender().equals(userid) ||
                            chat.getReceiver().equals(userid) && chat.getSender().equals(myid)) {
                        mchat.add(chat);
                    }
                }

                // search for user/sender's profile pic for messages
                reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
                reference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class); // get a snapshot
                        if (user.getImageURL().equals("default")) {

                            //** create message adapter and set it to recycler view using default img
                            messageAdapter = new MessageAdapter(MessageActivity.this, mchat, imageurl,"default");
                            recyclerView.setAdapter(messageAdapter);
                        } else { // else  load their save profile img

                            //** create message adapter and set it to recycler view using their profile pic
                            messageAdapter = new MessageAdapter(MessageActivity.this, mchat, imageurl, user.getImageURL());
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        menu.findItem(R.id.logout).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){

            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MessageActivity.this, StartActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;

        }

        return false;
    }


    private void currentUser(String userid) {
        SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
        editor.putString("currentuser", userid);
        editor.apply();
    }

    private void status(String status) {
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());

        HashMap < String, Object > hashMap = new HashMap < > ();
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