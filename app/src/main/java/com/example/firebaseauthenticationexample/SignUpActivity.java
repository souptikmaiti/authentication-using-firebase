package com.example.firebaseauthenticationexample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class SignUpActivity extends AppCompatActivity {
    private EditText etSignupEmail, etSignupPassword;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etSignupEmail = findViewById(R.id.et_signup_email);
        etSignupPassword = findViewById(R.id.et_signup_password);
        progressBar = findViewById(R.id.progress_bar);

        mAuth = FirebaseAuth.getInstance();
    }

    public void doSignup(View v) {
        String email = etSignupEmail.getText().toString().trim();
        String password = etSignupPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etSignupEmail.setError("please enter a valid email");
            etSignupEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etSignupPassword.setError("please enter password");
            etSignupPassword.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etSignupEmail.setError("invalid email");
            etSignupEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etSignupPassword.setError("password length can't be lesser than 6");
            etSignupPassword.requestFocus();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    finish();
                    startActivity(new Intent(SignUpActivity.this,ProfileActivity.class));
                } else if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                    Toast.makeText(SignUpActivity.this, "Already registered", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(SignUpActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void goToLogin(View v) {
        finish();
        startActivity(new Intent(this, MainActivity.class));
    }
}
