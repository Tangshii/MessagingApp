package com.hike.messagingapp.Adapter;

import android.content.Context;
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
import com.hike.messagingapp.Model.Chat;
import com.hike.messagingapp.R;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;

    private Context mContext;
    private List<Chat> mChat; // list of messages
    private String imageurl; // receiver profile pic
    private String userImg; // sender profile pic


    FirebaseUser fuser;

    public MessageAdapter(Context mContext, List<Chat> mChat, String imageurl, String userImg){
        this.mChat = mChat;
        this.mContext = mContext;
        this.imageurl = imageurl;
        this.userImg = userImg;
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_RIGHT) { //message is from sender
            // inflate recycler view with chat item right
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_right, parent, false);
            return new ViewHolder(view);
        } else { //message is from receiver
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_left, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {

        fuser = FirebaseAuth.getInstance().getCurrentUser();
        Chat chat = mChat.get(position);
        String messageType = chat.getType();
        if(messageType == "image"){
                if(mChat.get(position).getSender().equals(fuser.getUid())) {
                    holder.messageSenderPicture.setVisibility(View.VISIBLE);
                    Glide.with(mContext).load(chat.getMessage()).into(holder.messageSenderPicture);

                }
                else{
                    holder.messageReceiverPicture.setVisibility(View.VISIBLE);
                    Glide.with(mContext).load(chat.getMessage()).into(holder.messageReceiverPicture);

                }
            holder.show_message.setVisibility(View.GONE);

        }
        else{

            holder.show_message.setText(chat.getMessage());
            holder.show_message.setVisibility(View.VISIBLE);

        }


        // set profile image for the receiver messages
        if (imageurl.equals("default"))
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        else
            Glide.with(mContext).load(imageurl).into(holder.profile_image);

        // set profile image for the sender messages
        if (userImg.equals("default"))
            holder.userImg.setImageResource(R.mipmap.ic_launcher);
        else
            Glide.with(mContext).load(userImg).into(holder.userImg);

        // set sent or read text, if messages is last one
        if (position == mChat.size()-1){
            if (chat.isIsseen()){
                holder.txt_seen.setText("Read");
            } else {
                holder.txt_seen.setText("Sent");
            }
        } else {
            holder.txt_seen.setVisibility(View.GONE);
        }

    }

    @Override
    public int getItemCount() {
        return mChat.size();
    }

    public  class ViewHolder extends RecyclerView.ViewHolder{

        public TextView show_message;
        public ImageView profile_image;
        public ImageView userImg;
        public TextView txt_seen;
        public ImageView messageSenderPicture, messageReceiverPicture;

        public ViewHolder(View itemView) {
            super(itemView);

            show_message = itemView.findViewById(R.id.show_message);
            profile_image = itemView.findViewById(R.id.profile_image);
            userImg = itemView.findViewById(R.id.userImg);
            txt_seen = itemView.findViewById(R.id.txt_seen);
            messageReceiverPicture = itemView.findViewById(R.id.message_receiver_image_view);
            messageSenderPicture = itemView.findViewById(R.id.message_sender_image_view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        if (mChat.get(position).getSender().equals(fuser.getUid())){
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }
}