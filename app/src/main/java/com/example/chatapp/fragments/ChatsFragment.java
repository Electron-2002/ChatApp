package com.example.chatapp.fragments;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.activities.ChatActivity;
import com.example.chatapp.databinding.FragmentChatsBinding;
import com.example.chatapp.models.Conv;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseMessages, mDatabaseUsers, mDatabaseConv;

    private FragmentChatsBinding binding;

    public ChatsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentChatsBinding.inflate(inflater, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabaseConv = FirebaseDatabase.getInstance().getReference().child("Chat").child(mAuth.getCurrentUser().getUid());
        mDatabaseMessages = FirebaseDatabase.getInstance().getReference().child("Messages").child(mAuth.getCurrentUser().getUid());
        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("Users");

        mDatabaseConv.keepSynced(true);
        mDatabaseUsers.keepSynced(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        binding.chatsList.setHasFixedSize(true);
        binding.chatsList.setLayoutManager(linearLayoutManager);
        binding.chatsList.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        Query conversationQuery = mDatabaseConv.orderByChild("timestamp");

        FirebaseRecyclerAdapter adapter = new FirebaseRecyclerAdapter<Conv, ConvViewHolder>(
                Conv.class,
                R.layout.users_item,
                ConvViewHolder.class,
                conversationQuery

        ) {
            @Override
            protected void populateViewHolder(final ConvViewHolder convViewHolder, final Conv conv, int i) {
                final String userID = getRef(i).getKey();

                final Query lastMessageQuery = mDatabaseMessages.child(userID).limitToLast(1);
                lastMessageQuery.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        String data = dataSnapshot.child("message").getValue().toString();
                        String type = dataSnapshot.child("type").getValue().toString();
                        convViewHolder.setMessage(data, conv.isSeen(), type);
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                mDatabaseUsers.child(userID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String userName = dataSnapshot.child("name").getValue().toString();
                        String thumbnail = dataSnapshot.child("thumbnail").getValue().toString();

                        if (dataSnapshot.hasChild("online")) {
                            String userOnline = dataSnapshot.child("online").getValue().toString();
                            convViewHolder.setUserOnline(userOnline);
                        }

                        convViewHolder.setName(userName);
                        convViewHolder.setThumbnail(thumbnail);

                        convViewHolder.view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                chatIntent.putExtra("userID", userID);
                                chatIntent.putExtra("name", userName);
                                startActivity(chatIntent);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        };

        binding.chatsList.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }

    public static class ConvViewHolder extends RecyclerView.ViewHolder {

        View view;

        public ConvViewHolder(@NonNull View itemView) {
            super(itemView);
            this.view = itemView;
        }

        public void setMessage(String message, boolean isSeen, String type) {
            TextView userStatus = view.findViewById(R.id.user_status);
            if (type.equals("image"))
                message = "Sent an image";

            userStatus.setText(message);

            if (!isSeen) {
                userStatus.setTypeface(userStatus.getTypeface(), Typeface.BOLD);
            } else {
                userStatus.setTypeface(userStatus.getTypeface(), Typeface.NORMAL);
            }
        }

        public void setName(String name) {
            TextView userName = view.findViewById(R.id.user_name);
            userName.setText(name);
        }

        public void setThumbnail(String imageUri) {
            CircleImageView userThumbnail = view.findViewById(R.id.user_image);
            Picasso.get().load(imageUri).into(userThumbnail);
        }

        public void setUserOnline(String onlineStatus) {
            ImageView userOnline = view.findViewById(R.id.user_online);

            if (onlineStatus.equals("true")) {
                userOnline.setVisibility(View.VISIBLE);
            } else {
                userOnline.setVisibility(View.GONE);
            }
        }
    }
}
