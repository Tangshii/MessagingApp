package com.hike.messagingapp.Adapter;

import android.content.Context;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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

    private OnItemClickListener mOnItemClickListener;

    FirebaseUser fuser;

    //constructor for the adapter, taking in context, arraylist of Chats class instance,
    // receiver image url, sender image url, and an onClickListener for translation
    public MessageAdapter(Context mContext, List<Chat> mChat, String imageurl, String userImg, OnItemClickListener onItemClickListener){
        this.mChat = mChat;
        this.mContext = mContext;
        this.imageurl = imageurl;
        this.userImg = userImg;
        this.mOnItemClickListener= onItemClickListener;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @NonNull
    @Override

    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_RIGHT) { //message is from sender
            // inflate recycler view with chat item right
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_right, parent, false);
            // set color of views
            SharedPreferences colorPref = mContext.getSharedPreferences("colorPref", Context.MODE_PRIVATE);
            int primary = colorPref.getInt("primary", -1);
            if(primary != -1){
                view.findViewById(R.id.show_message).setBackgroundTintList(ColorStateList.valueOf(primary));
            }

            return new ViewHolder(view, mOnItemClickListener);
        } else { //message is from receiver
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_left, parent, false);
            return new ViewHolder(view, mOnItemClickListener);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        // get position of message for onClickItemListener
        Chat chat = mChat.get(position);

        holder.show_message.setText(chat.getMessage());

        // set profile image for the receiver messages
        if (imageurl.equals("default"))
            holder.profile_image.setImageResource(R.drawable.account);
        else
            Glide.with(mContext).load(imageurl).into(holder.profile_image);

        // set profile image for the sender messages
        if (userImg.equals("default"))
            holder.userImg.setImageResource(R.drawable.account);
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

    @Override
    public int getItemViewType(int position) {
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        if (mChat.get(position).getSender().equals(fuser.getUid())){
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public TextView show_message;
        public ImageView profile_image;
        public ImageView userImg;
        public TextView txt_seen;

        OnItemClickListener onItemClickListener;

        public ViewHolder(View itemView, OnItemClickListener onItemClickListener) {
            super(itemView);

            show_message = itemView.findViewById(R.id.show_message);
            profile_image = itemView.findViewById(R.id.profile_image);
            userImg = itemView.findViewById(R.id.userImg);
            txt_seen = itemView.findViewById(R.id.txt_seen);
            this.onItemClickListener = onItemClickListener;
            show_message.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            onItemClickListener.onItemClick(getAdapterPosition());

        }
    }

    public interface OnItemClickListener {
        void onItemClick(int postition);
    }

}