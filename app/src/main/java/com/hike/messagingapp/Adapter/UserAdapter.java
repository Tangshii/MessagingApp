package com.hike.messagingapp.Adapter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hike.messagingapp.MessageActivity;
import com.hike.messagingapp.Model.Chat;
import com.hike.messagingapp.Model.User;
import com.hike.messagingapp.R;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private Context mContext;
    private List<User> mUsers;
    private boolean ischat;
    boolean once=false;
    String theLastMessage;
    ProgressDialog loading;

    final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();

    public UserAdapter(Context mContext, List<User> mUsers, boolean ischat) {
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final User user = mUsers.get(position);

        setUsernames(user, holder);

        // set the profile image of the searched users
        if (user.getImageURL().equals("default")) {
            holder.profile_image.setImageResource(R.drawable.account);
        } else {
            Glide.with(mContext).load(user.getImageURL()).into(holder.profile_image);
        }

        if (ischat) { // adapter is for chats tab
            lastMessage(user.getId(), holder.last_msg); // so show last message
            // set online indicator
            if (user.getStatus().equals("online")) {
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


        // Delete show two dialogs and deletes convo fore both sides
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Use the Builder class for convenient dialog construction
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Delete Conversation?\n").setMessage("it will be deleted for both users")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                // second dialog
                                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                                builder.setTitle("Are you sure?\n").setMessage("BOTH sides will be deleted forever")
                                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                loading = new ProgressDialog(mContext);
                                                loading.setMessage("deleting...");
                                                loading.setCanceledOnTouchOutside(false);
                                                loading.show();
                                                once=true;
                                                if(once)
                                                    getReceiver(user.getUsername());
                                                else
                                                    once=false;
                                            }
                                        })
                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {

                                            }
                                        });
                                builder.show();


                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        });
                builder.show();

                return true;
            }


        });




    }


    // sets holder.username and put number of unread messages
    private void setUsernames(final User user, final ViewHolder holder) {
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
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chat chat = snapshot.getValue(Chat.class);
                    if (firebaseUser != null && chat.getReceiver() != null) {
                        // if receiver is user and message is not seen
                        if ((chat.getReceiver().equals(firebaseUser.getUid()) && chat.getSender().equals(user.getId()))
                                && !chat.isIsseen()) {
                            unread++;
                        }
                    }
                }
                // set username with number of unread messages
                if (unread == 0) {
                    holder.username.setText(user.getUsername());
                } else {
                    holder.username.setText(user.getUsername() + " (" + unread + ")");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }


    //check for last message
    private void lastMessage(final String userid, final TextView last_msg) {
        theLastMessage = "default";
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chat chat = snapshot.getValue(Chat.class);
                    if (firebaseUser != null && chat.getReceiver() != null) {
                        if (chat.getReceiver().equals(firebaseUser.getUid()) && chat.getSender().equals(userid) ||
                                chat.getReceiver().equals(userid) && chat.getSender().equals(firebaseUser.getUid())) {
                            theLastMessage = chat.getMessage();
                        }
                    }
                }

                if (theLastMessage == "default")
                    last_msg.setText("No Message");
                else
                    last_msg.setText(theLastMessage);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }


    void getReceiver(final String receiverName){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        //search Chats table
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String receiverId = "";
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if( user.getUsername() == receiverName ){
                        receiverId = user.getId();
                        //Toast.makeText(mContext, receiverId, Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                deleteConvo(receiverId);

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }


    // delete both chatlists
    void deleteConvo(final String receiverId){

        // delete sender convo
        final DatabaseReference chatRefReceiver = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(fUser.getUid()).child(receiverId);

        chatRefReceiver.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        snapshot.getRef().removeValue();
                        break;
                }

                //delete receiver convo
                final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Chatlist")
                        .child(receiverId).child(fUser.getUid());
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            snapshot.getRef().removeValue();
                            break;
                        }
                        loading.dismiss();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                });

                deleteChats(receiverId);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

    }

    // delete actual messages
    void deleteChats(final String receiverId){
        // get Chats table
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
        //search Chats table
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver() != null && chat.getSender()!=null) {

                        if (chat.getSender().equals(fUser.getUid()) && chat.getReceiver().equals(receiverId) ||
                                chat.getReceiver().equals(fUser.getUid()) && chat.getSender().equals(receiverId)) {
                            snapshot.getRef().removeValue();
                            Log.e("DELETEEEEEEEEEEEEEDDDD", snapshot.getKey());
                        }

                    }
                }
                loading.dismiss();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }


    @Override
    public int getItemCount() {
        if (mUsers != null)
            return mUsers.size();
        else
            return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

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

}
