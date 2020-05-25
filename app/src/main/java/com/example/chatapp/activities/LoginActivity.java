package com.example.chatapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.chatapp.CustomDialog;
import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivityLoginBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private static final String TAG = "LoginActivity";

    private CustomDialog dialog;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        dialog = new CustomDialog(LoginActivity.this);

        Toolbar toolbar = findViewById(R.id.login_page_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Login to your Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginUser();
            }
        });
    }

    private void loginUser() {
        String email = binding.userEmail.getEditText().getText().toString().trim();
        String password = binding.userPassword.getEditText().getText().toString().trim();

        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            dialog.startDialog();
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "createUserWithEmail:success");
                                dialog.dismissDialog();
                                Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                                mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(mainIntent);
                                finish();
                            } else {
                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                dialog.dismissDialog();
                                Snackbar.make(binding.layout, "Authentication Failed", Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }
}
