package com.hike.messagingapp.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.hike.messagingapp.Adapter.UserAdapter;
import com.hike.messagingapp.Model.ChatList;
import com.hike.messagingapp.Model.User;
import com.hike.messagingapp.Notifications.Token;
import com.hike.messagingapp.R;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.ArrayList;
import java.util.List;


public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    TextView textView;

    private UserAdapter userAdapter;
    private List<User> mUsers;

    FirebaseUser fuser;
    DatabaseReference reference;

    private List<ChatList> usersList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext())); // set manager to recycler view

        textView = view.findViewById(R.id.chat_tap);

        //register context menu on long tap a text view under the conservation list
        registerForContextMenu(textView);

        // get color shared preferences and set view to the colors
        SharedPreferences prefs = this.getActivity().getSharedPreferences("colorPref", Context.MODE_PRIVATE);
        int secondary = prefs.getInt("secondary", -1);
        if(secondary != -1){
            recyclerView.setBackgroundColor(secondary);
            textView.setBackgroundColor(secondary);
        }

        fuser = FirebaseAuth.getInstance().getCurrentUser();

        // search for chat lists that has the user as a receiver and adds to user’s chatlist arraylist
        usersList = new ArrayList<>();
        // get all the chat list the user has
        reference = FirebaseDatabase.getInstance().getReference("Chatlist").child(fuser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersList.clear();
                // add all the chat list the user has to array list
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    ChatList chatlist = snapshot.getValue(ChatList.class);
                    usersList.add(chatlist);
                }
                // get the receiver info in the list
                getUserInChat();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        updateToken(FirebaseInstanceId.getInstance().getToken());

        return view;
    }

    // get the receiver info in the list
    private void  getUserInChat() {
        mUsers = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mUsers.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){ //loop thru all users
                    User user = snapshot.getValue(User.class);
                    for (ChatList chatlist : usersList){ // loop thru chats user has
                        if (user.getId() != null) {
                            if (user.getId().equals(chatlist.getId())) {//add that user if it's also in the chats
                                mUsers.add(user);
                            }
                        }
                    }
                }

                // create use adapter with the arraylist of receivers, and set to recycler view
                userAdapter = new UserAdapter(getContext(), mUsers, true);
                recyclerView.setAdapter(userAdapter);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, final View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        SharedPreferences prefs = this.getActivity().getSharedPreferences("colorPref", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();

        if(v.getId() == R.id.chat_tap) {
            new ColorPickerDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                    .setPreferenceName("MyColorPickerDialog")
                    .setPositiveButton("confirm",
                            new ColorEnvelopeListener() {
                                @Override
                                public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {

                                    int color = envelope.getColor();
                                    recyclerView.setBackgroundColor(color);
                                    textView.setBackgroundColor(color);
                                    editor.putInt("secondary", color);
                                    editor.apply();
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

    private void updateToken(String token){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token1 = new Token(token);
        reference.child(fuser.getUid()).setValue(token1);
    }

}
