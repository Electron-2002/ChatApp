package com.example.chatapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivityUsersBinding;
import com.example.chatapp.models.User;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersActivity extends AppCompatActivity {

    private ActivityUsersBinding binding;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        Toolbar toolbar = findViewById(R.id.users_tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("All Users");

        binding.usersList.setHasFixedSize(true);
        binding.usersList.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerAdapter adapter = new FirebaseRecyclerAdapter<User, UsersViewHolder>(
                User.class,
                R.layout.users_item,
                UsersViewHolder.class,
                mDatabase
        ) {
            @Override
            protected void populateViewHolder(UsersViewHolder usersViewHolder, User user, int i) {
                usersViewHolder.setName(user.getName());
                usersViewHolder.setStatus(user.getStatus());
                usersViewHolder.setThumbnail(user.getThumbnail());

                final String clickedUserID = getRef(i).getKey();

                usersViewHolder.view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent profileIntent = new Intent(UsersActivity.this, ProfileActivity.class);
                        profileIntent.putExtra("userID", clickedUserID);
                        startActivity(profileIntent);
                    }
                });
            }
        };

        binding.usersList.setAdapter(adapter);
    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder {
        View view;

        public UsersViewHolder(@NonNull View itemView) {
            super(itemView);

            this.view = itemView;
        }

        public void setName(String name) {
            TextView userName = view.findViewById(R.id.user_name);
            userName.setText(name);
        }

        public void setStatus(String status) {
            TextView userStatus = view.findViewById(R.id.user_status);
            userStatus.setText(status);
        }

        public void setThumbnail(String imageUri) {
            CircleImageView userThumbnail = view.findViewById(R.id.user_image);
            Picasso.get().load(imageUri).into(userThumbnail);
        }
    }
}
