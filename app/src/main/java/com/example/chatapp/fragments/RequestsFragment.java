package com.example.chatapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.activities.ProfileActivity;
import com.example.chatapp.databinding.FragmentRequestsBinding;
import com.example.chatapp.models.Request;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class RequestsFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseRequests, mDatabaseUsers;

    private FragmentRequestsBinding binding;

    public RequestsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRequestsBinding.inflate(inflater, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabaseRequests = FirebaseDatabase.getInstance().getReference().child("Friend Requests").child(mAuth.getCurrentUser().getUid());
        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("Users");

        mDatabaseRequests.keepSynced(true);
        mDatabaseUsers.keepSynced(true);

        binding.requestsList.setHasFixedSize(true);
        binding.requestsList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.requestsList.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        Query requestsQuery = mDatabaseRequests.orderByChild("request").equalTo("received");

        FirebaseRecyclerAdapter adapter = new FirebaseRecyclerAdapter<Request, RequestsViewHolder>(
                Request.class,
                R.layout.users_item,
                RequestsViewHolder.class,
                requestsQuery
        ) {
            @Override
            protected void populateViewHolder(final RequestsViewHolder requestsViewHolder, Request request, int i) {
                final String userID = getRef(i).getKey();

                mDatabaseUsers.child(userID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String name = dataSnapshot.child("name").getValue().toString();
                        String status = dataSnapshot.child("status").getValue().toString();
                        String thumbnail = dataSnapshot.child("thumbnail").getValue().toString();

                        requestsViewHolder.setName(name);
                        requestsViewHolder.setStatus(status);
                        requestsViewHolder.setThumbnail(thumbnail);

                        requestsViewHolder.view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                                profileIntent.putExtra("userID", userID);
                                startActivity(profileIntent);
                            }
                        });

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        };

        binding.requestsList.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }

    public static class RequestsViewHolder extends RecyclerView.ViewHolder {
        View view;

        public RequestsViewHolder(@NonNull View itemView) {
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
