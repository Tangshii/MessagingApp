package com.hike.messagingapp.Adapter;

import android.content.Context;
import android.content.Intent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
import com.hike.messagingapp.MessageActivity;
import com.hike.messagingapp.Model.Chat;
import com.hike.messagingapp.Model.User;
import com.hike.messagingapp.R;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private Context mContext;
    private List<User> mUsers;
    private boolean ischat;

    String theLastMessage;

    public UserAdapter(Context mContext, List<User> mUsers, boolean ischat){
        this.mUsers = mUsers;
        this.mContext = mContext;
        this.ischat = ischat;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate view with user item xml
        View view = LayoutInflater.from(mContext).inflate(R.layout.user_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final User user = mUsers.get(position);

        // sets holder.username.setText(user.getUsername());
        setUsername(user, holder);

        // set the profile image of the searched users
        if (user.getImageURL().equals("default")){
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(mContext).load(user.getImageURL()).into(holder.profile_image);
        }

        if (ischat){ // adapter is for chats tab
            lastMessage(user.getId(), holder.last_msg); // so show last message

            // set online indicator
            if (user.getStatus().equals("online")){
                holder.img_on.setVisibility(View.VISIBLE);
                holder.img_off.setVisibility(View.GONE);
            } else {
                holder.img_on.setVisibility(View.GONE);
                holder.img_off.setVisibility(View.VISIBLE);
            }
        } else { // adapter is for users search tab so don't show it
            holder.last_msg.setVisibility(View.GONE);
            holder.img_on.setVisibility(View.GONE);
            holder.img_off.setVisibility(View.GONE);
        }

        // when taps a searched user, start a message activity
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, MessageActivity.class);
                intent.putExtra("userid", user.getId()); //put the receiver uid
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (mUsers != null)
            return mUsers.size();
        else
            return 0;
    }

    public  class ViewHolder extends RecyclerView.ViewHolder{

        public TextView username;
        public ImageView profile_image;
        private ImageView img_on;
        private ImageView img_off;
        private TextView last_msg;

        public ViewHolder(View itemView) {
            super(itemView);

            username = itemView.findViewById(R.id.username);
            profile_image = itemView.findViewById(R.id.profile_image);
            img_on = itemView.findViewById(R.id.img_on);
            img_off = itemView.findViewById(R.id.img_off);
            last_msg = itemView.findViewById(R.id.last_msg);
        }
    }


    // sets holder.username and put number of unread messages
    private void setUsername(final User user, final ViewHolder holder){
        // get firebase user
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        // get Chats table
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
        //search Chats table
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int unread = 0;
                // loop thru each Chats
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    if (firebaseUser != null && chat != null) {
                        // if receiver is user and message is not seen
                        if ( ( chat.getReceiver().equals(firebaseUser.getUid()) && chat.getSender().equals(user.getId())  )
                                && !chat.isIsseen()) {
                            unread++;
                        }
                    }
                }
                // set username with number of unread messages
                if (unread == 0){
                    holder.username.setText(user.getUsername());
                } else {
                    holder.username.setText(user.getUsername() + " ("+ unread+")");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }



    //check for last message
    private void lastMessage(final String userid, final TextView last_msg){
        theLastMessage = "default";

        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                   if (firebaseUser != null && chat != null) {
                        if (chat.getReceiver().equals(firebaseUser.getUid()) && chat.getSender().equals(userid) ||
                                chat.getReceiver().equals(userid) && chat.getSender().equals(firebaseUser.getUid())) {
                            theLastMessage = chat.getMessage();
                        }
                    }
                }

                if( theLastMessage == "default" )
                    last_msg.setText("No Message");
                else {
                        if(theLastMessage.startsWith("http")){
                            last_msg.setText("Image");
                        }
                        else
                            last_msg.setText(theLastMessage);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
