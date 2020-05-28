package com.example.chatapp.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messageList;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_item, parent, false);
        return new MessageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, int position) {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        if (mAuth.getCurrentUser() != null) {
            Message message = messageList.get(position);

            String fromUser = message.getFrom();
            String messageType = message.getType();

            if (fromUser != null) {

                mDatabase.child(fromUser).child("thumbnail").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String profileImage = dataSnapshot.getValue().toString();
                        Picasso.get().load(profileImage).into(holder.profileImage);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                if (messageType.equals("text")) {
                    holder.messageText.setVisibility(View.VISIBLE);
                    holder.messageText.setText(message.getMessage());
                    holder.messageImage.setVisibility(View.GONE);
                    if (fromUser.equals(mAuth.getCurrentUser().getUid())) {
                        holder.messageText.setBackgroundResource(R.drawable.message_background_another);
                        holder.messageText.setTextColor(Color.BLACK);
                    } else {
                        holder.messageText.setBackgroundResource(R.drawable.message_background);
                        holder.messageText.setTextColor(Color.WHITE);
                    }
                }
                if (messageType.equals("image")) {
                    holder.messageImage.setVisibility(View.VISIBLE);
                    holder.messageText.setVisibility(View.GONE);

                    Picasso.get().load(message.getMessage()).into(holder.messageImage);
                }
            }
        }

    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        public TextView messageText;
        public CircleImageView profileImage;
        public ImageView messageImage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.chat_item_message);
            profileImage = itemView.findViewById(R.id.chat_item_image);
            messageImage = itemView.findViewById(R.id.chat_item_pic);
        }
    }
}
