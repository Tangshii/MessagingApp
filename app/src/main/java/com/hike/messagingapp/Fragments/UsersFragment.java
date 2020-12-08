package com.hike.messagingapp.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.hike.messagingapp.Adapter.UserAdapter;
import com.hike.messagingapp.Model.User;
import com.hike.messagingapp.R;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.ArrayList;
import java.util.List;


public class UsersFragment extends Fragment {

    private RecyclerView recyclerView;
    TextView textView;
    RelativeLayout relativeLayout;

    private UserAdapter userAdapter;
    private List<User> mUsers;

    EditText search_users;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users, container, false);

        relativeLayout = view.findViewById(R.id.frag_user);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext())); //set manager to recycler view
        textView = view.findViewById(R.id.user_tap);

        mUsers = new ArrayList<>(); //array list for holding result of search

        registerForContextMenu(textView);

        SharedPreferences prefs = this.getActivity().getSharedPreferences("colorPref", Context.MODE_PRIVATE);
        int secondary = prefs.getInt("secondary", -1);
        if(secondary != -1){
            relativeLayout.setBackgroundColor(secondary);
        }

        // searches whenever user types
        search_users = view.findViewById(R.id.search_users);
        search_users.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if( charSequence.length() > 0) // search only if there is a letter
                    searchUsers(charSequence.toString().toLowerCase());
                else { // else set recycler view with empty list
                    mUsers.clear();
                    userAdapter = new UserAdapter(getContext(), mUsers, false);
                    recyclerView.setAdapter(userAdapter);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });

        return view;
    }


    private void searchUsers(String s) {
        // query users that start with the search term
        final FirebaseUser fuser = FirebaseAuth.getInstance().getCurrentUser();
        Query query = FirebaseDatabase.getInstance().getReference("Users")
                .orderByChild("search").startAt(s).endAt(s + "\uf8ff");

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mUsers.clear();
                // add user to array list, not including self
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && fuser != null) {
                        if (!user.getId().equals(fuser.getUid())) {
                            mUsers.add(user);
                        }
                    }
                }
                // create user adapter with the arraylist of users, and set to recycler view
                userAdapter = new UserAdapter(getContext(), mUsers, false);
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

        if(v.getId() == R.id.user_tap) {
            new ColorPickerDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                    .setPreferenceName("MyColorPickerDialog")
                    .setPositiveButton("confirm",
                            new ColorEnvelopeListener() {
                                @Override
                                public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                                    int color = envelope.getColor();
                                    relativeLayout.setBackgroundColor(envelope.getColor());
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




}
