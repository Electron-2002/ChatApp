package com.example.chatapp.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.chatapp.R;
import com.example.chatapp.adapters.MessageAdapter;
import com.example.chatapp.application.GetTimeAgo;
import com.example.chatapp.databinding.ActivityChatBinding;
import com.example.chatapp.models.Message;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private static final int ITEMS_TO_LOAD = 10;
    private static final int GALLERY_PICK = 1;
    private ActivityChatBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorage;
    private String chatFriendID, chatFriendName;
    private List<Message> messageList;
    private MessageAdapter adapter;
    private LinearLayoutManager linearLayoutManager;
    private int currPage = 1;
    private int itemPos = 0;
    private String lastKey = "";
    private String prevKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        chatFriendID = getIntent().getStringExtra("userID");
        chatFriendName = getIntent().getStringExtra("name");

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorage = FirebaseStorage.getInstance().getReference();

        messageList = new ArrayList<>();

        Toolbar toolbar = findViewById(R.id.chat_tool_bar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setTitle("");

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = inflater.inflate(R.layout.chat_custom_bar, null);

        actionBar.setCustomView(actionBarView);

        TextView friendName = findViewById(R.id.custom_bar_name);
        friendName.setText(chatFriendName);

        binding.chatsList.setHasFixedSize(true);
        linearLayoutManager = new LinearLayoutManager(this);
        binding.chatsList.setLayoutManager(linearLayoutManager);

        adapter = new MessageAdapter(messageList);
        loadMessages();
        binding.chatsList.setAdapter(adapter);

        final TextView friendSeen = findViewById(R.id.custom_bar_last_seen);
        final CircleImageView friendThumbnail = findViewById(R.id.custom_bar_image);

        mDatabase.child("Users").child(chatFriendID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String online = dataSnapshot.child("online").getValue().toString();
                String thumbnail = dataSnapshot.child("thumbnail").getValue().toString();

                if (online.equals("true")) {
                    friendSeen.setText("Online");
                } else {
                    GetTimeAgo getTimeAgo = new GetTimeAgo();
                    long lastTime = Long.parseLong(online);
                    String lastSeen = GetTimeAgo.getTimeAgo(lastTime, ChatActivity.this);
                    friendSeen.setText(lastSeen);
                }

                Picasso.get().load(thumbnail).into(friendThumbnail);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mDatabase.child("Chat").child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild(chatFriendID)) {
                    Map<String, Object> chatMap = new HashMap<String, Object>();
                    chatMap.put("seen", false);
                    chatMap.put("timestamp", ServerValue.TIMESTAMP);

                    Map<String, Object> chatUserMap = new HashMap<>();
                    chatUserMap.put("Chat/" + mAuth.getCurrentUser().getUid() + "/" + chatFriendID, chatMap);
                    chatUserMap.put("Chat/" + chatFriendID + "/" + mAuth.getCurrentUser().getUid(), chatMap);

                    mDatabase.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if (databaseError == null) {

                            }
                        }
                    });
                } else {
                    mDatabase.child("Chat").child(mAuth.getCurrentUser().getUid()).child(chatFriendID).child("seen").setValue(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        binding.chatSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        binding.chatsRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                currPage++;
                itemPos = 0;
                loadMoreMessages();
            }
        });

        binding.chatImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {

            Uri imageUri = data.getData();
            final String currUserRef = "Messages/" + mAuth.getCurrentUser().getUid() + "/" + chatFriendID;
            final String chatFriendRef = "Messages/" + chatFriendID + "/" + mAuth.getCurrentUser().getUid();

            DatabaseReference messagePush = mDatabase.child("Messages").child(mAuth.getCurrentUser().getUid()).child(chatFriendID).push();

            final String pushKey = messagePush.getKey();

            mStorage.child("Message Images").child(pushKey).putFile(imageUri)
                    .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                mStorage.child("Message Images").child(pushKey).getDownloadUrl()
                                        .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {

                                                Map<String, Object> messageMap = new HashMap<>();
                                                messageMap.put("message", uri.toString());
                                                messageMap.put("seen", false);
                                                messageMap.put("type", "image");
                                                messageMap.put("time", ServerValue.TIMESTAMP);
                                                messageMap.put("from", mAuth.getCurrentUser().getUid());

                                                Map<String, Object> messageUserMap = new HashMap<>();
                                                messageUserMap.put(currUserRef + "/" + pushKey, messageMap);
                                                messageUserMap.put(chatFriendRef + "/" + pushKey, messageMap);

                                                binding.chatMessage.setText("");

                                                mDatabase.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                                                    @Override
                                                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                                        if (databaseError == null) {

                                                        }
                                                    }
                                                });
                                            }
                                        });
                            }
                        }
                    });


        }
    }

    private void loadMoreMessages() {
        DatabaseReference messageRef = mDatabase.child("Messages").child(mAuth.getCurrentUser().getUid()).child(chatFriendID);

        Query messageQuery = messageRef.orderByKey().endAt(lastKey).limitToLast(ITEMS_TO_LOAD);
        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Message message = dataSnapshot.getValue(Message.class);

                if (!prevKey.equals(dataSnapshot.getKey())) {
                    messageList.add(itemPos++, message);
                } else {
                    prevKey = lastKey;
                }

                if (itemPos == 1) {
                    lastKey = dataSnapshot.getKey();
                }
                adapter.notifyDataSetChanged();

                binding.chatsRefresh.setRefreshing(false);
                linearLayoutManager.scrollToPositionWithOffset(10, 0);
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


    }

    private void loadMessages() {

        DatabaseReference messageRef = mDatabase.child("Messages").child(mAuth.getCurrentUser().getUid()).child(chatFriendID);

        Query messageQuery = messageRef.limitToLast(currPage * ITEMS_TO_LOAD);
        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Message message = dataSnapshot.getValue(Message.class);

                messageList.add(message);
                adapter.notifyDataSetChanged();

                itemPos++;
                if (itemPos == 1) {
                    lastKey = dataSnapshot.getKey();
                    prevKey = dataSnapshot.getKey();
                }

                binding.chatsList.scrollToPosition(messageList.size() - 1);
                binding.chatsRefresh.setRefreshing(false);
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
    }

    private void sendMessage() {
        String message = binding.chatMessage.getText().toString();

        if (!TextUtils.isEmpty(message)) {

            String currUserRef = "Messages/" + mAuth.getCurrentUser().getUid() + "/" + chatFriendID;
            String chatFriendRef = "Messages/" + chatFriendID + "/" + mAuth.getCurrentUser().getUid();

            DatabaseReference messagePush = mDatabase.child("messages").child(mAuth.getCurrentUser().getUid()).child(chatFriendID).push();
            String pushKey = messagePush.getKey();

            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", mAuth.getCurrentUser().getUid());

            Map<String, Object> messageUserMap = new HashMap<>();
            messageUserMap.put(currUserRef + "/" + pushKey, messageMap);
            messageUserMap.put(chatFriendRef + "/" + pushKey, messageMap);

            binding.chatMessage.setText("");
            mDatabase.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                    if (databaseError == null) {
                    }
                }
            });
        }
    }
}
