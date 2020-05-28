package com.example.chatapp.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.activities.ChatActivity;
import com.example.chatapp.activities.ProfileActivity;
import com.example.chatapp.databinding.FragmentFriendsBinding;
import com.example.chatapp.models.Friend;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendsFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseFriends, mDatabaseUsers;

    private FragmentFriendsBinding binding;

    public FriendsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFriendsBinding.inflate(inflater, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabaseFriends = FirebaseDatabase.getInstance().getReference().child("Friends").child(mAuth.getCurrentUser().getUid());
        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("Users");

        mDatabaseFriends.keepSynced(true);
        mDatabaseUsers.keepSynced(true);

        binding.friendsList.setHasFixedSize(true);
        binding.friendsList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.friendsList.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerAdapter adapter = new FirebaseRecyclerAdapter<Friend, FriendsViewHolder>(
                Friend.class,
                R.layout.users_item,
                FriendsViewHolder.class,
                mDatabaseFriends

        ) {
            @Override
            protected void populateViewHolder(final FriendsViewHolder friendsViewHolder, final Friend friend, int i) {
                friendsViewHolder.setDate(friend.getDate());

                final String userID = getRef(i).getKey();

                mDatabaseUsers.child(userID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String name = dataSnapshot.child("name").getValue().toString();
                        String imageUri = dataSnapshot.child("thumbnail").getValue().toString();
                        String online;

                        if (dataSnapshot.hasChild("online")) {
                            online = dataSnapshot.child("online").getValue().toString();
                        } else {
                            online = "false";
                        }


                        friendsViewHolder.setName(name);
                        friendsViewHolder.setThumbnail(imageUri);
                        friendsViewHolder.setOnline(online);

                        friendsViewHolder.view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                CharSequence[] options = {"View Profile", "Send Message"};
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                builder.setTitle("Select Option: ");

                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        if (i == 0) {
                                            Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                                            profileIntent.putExtra("userID", userID);
                                            startActivity(profileIntent);
                                        }

                                        if (i == 1) {
                                            Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                            chatIntent.putExtra("userID", userID);
                                            chatIntent.putExtra("name", name);
                                            startActivity(chatIntent);
                                        }
                                    }
                                });

                                builder.show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
            }
        };

        binding.friendsList.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder {
        View view;

        public FriendsViewHolder(@NonNull View itemView) {
            super(itemView);

            this.view = itemView;
        }

        public void setDate(String date) {
            TextView friendDate = view.findViewById(R.id.user_status);
            friendDate.setText(date);
        }

        public void setName(String name) {
            TextView userName = view.findViewById(R.id.user_name);
            userName.setText(name);
        }

        public void setThumbnail(String imageUri) {
            CircleImageView userThumbnail = view.findViewById(R.id.user_image);
            Picasso.get().load(imageUri).into(userThumbnail);
        }

        public void setOnline(String online) {
            ImageView friendOnline = view.findViewById(R.id.user_online);

            if (online.equals("true")) {
                friendOnline.setVisibility(View.VISIBLE);
            } else {
                friendOnline.setVisibility(View.GONE);
            }
        }
    }
}
