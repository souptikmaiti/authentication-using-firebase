package com.example.firebaseauthenticationexample;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

public class ProfileActivity extends AppCompatActivity {
    private EditText etDisplayName;
    private ImageView ivProfilePic;
    private Button btnSave;
    private ProgressBar progressBarUpload;
    private TextView tvVerification;
    public static final int IMAGE_CHOOSER_REQUEST = 5;
    private Uri imageUri;
    private String imageDownloadUrl;
    private FirebaseAuth mAuth;
    private StorageTask uploadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        etDisplayName = findViewById(R.id.et_display_name);
        ivProfilePic = findViewById(R.id.iv_profile_pic);
        btnSave = findViewById(R.id.btn_save);
        progressBarUpload = findViewById(R.id.progress_bar_upload);
        tvVerification = findViewById(R.id.tv_verification);
        mAuth = FirebaseAuth.getInstance();

        ivProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchImage();
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(uploadTask!=null && uploadTask.isInProgress()){
                    Toast.makeText(ProfileActivity.this, "upload in progress", Toast.LENGTH_SHORT).show();
                    return;
                }else{
                    saveInFirebaseStorage();
                }
            }
        });
        tvVerification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                emailVerification();
            }
        });
    }

    private void emailVerification() {
        if(mAuth.getCurrentUser()!=null){
            mAuth.getCurrentUser().sendEmailVerification();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if(user == null){
            finish();
            startActivity(new Intent(ProfileActivity.this,MainActivity.class));
        }else if(user.getPhotoUrl() != null && user.getDisplayName()!=null){
            Glide.with(this).load(user.getPhotoUrl()).into(ivProfilePic);
            etDisplayName.setText(user.getDisplayName());
        }
        if(user.isEmailVerified()){
            tvVerification.setText("Verified");
        }else{
            tvVerification.setText("Not Verified (Click here for email verification)");
        }
    }
    private void fetchImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,IMAGE_CHOOSER_REQUEST);
    }
    private String getExtension(Uri uri){
        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cr.getType(uri));
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==IMAGE_CHOOSER_REQUEST && resultCode==RESULT_OK && data!=null && data.getData()!=null){
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(ivProfilePic);
        }
    }
    private void saveInFirebaseStorage() {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference("user_data")
                .child(System.currentTimeMillis() + getExtension(imageUri));
        uploadTask = storageReference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                progressBarUpload.setVisibility(View.GONE);
                Toast.makeText(ProfileActivity.this, "image uploaded successfully", Toast.LENGTH_SHORT).show();
                Task<Uri> downloadUri = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                downloadUri.addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        imageDownloadUrl = uri.toString();
                        setUserInfo();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBarUpload.setVisibility(View.GONE);
                Toast.makeText(ProfileActivity.this, "image upload failed", Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                progressBarUpload.setVisibility(View.VISIBLE);
            }
        });
    }
    private void setUserInfo(){
        FirebaseUser user = mAuth.getCurrentUser();
        String displayName = etDisplayName.getText().toString().trim();
        if(displayName.isEmpty()) {
            etDisplayName.setError("Name required");
            etDisplayName.requestFocus();
            return;
        }
        if(user!=null && !imageDownloadUrl.isEmpty()){
            UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .setPhotoUri(Uri.parse(imageDownloadUrl))
                    .build();
            user.updateProfile(userProfileChangeRequest).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Toast.makeText(ProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_logout,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_logout:
                mAuth.signOut();
                finish();
                startActivity(new Intent(ProfileActivity.this,MainActivity.class));
                break;
        }
        return true;
    }
}
