package com.android.iunoob.bloodbank.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.iunoob.bloodbank.R;
import com.android.iunoob.bloodbank.viewmodels.CustomUserData;
import com.android.iunoob.bloodbank.viewmodels.UserData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

public class PostActivity extends AppCompatActivity {

    private EditText inputContact, inputLocation;
    private Spinner spinnerBloodGroup, spinnerDivision;
    private Button btnPost;
    private ProgressBar progressBar;

    private DatabaseReference userRef, postsRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Post Blood Request");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeFirebase();
        checkUserAuthentication();
        initializeViews();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase fdb = FirebaseDatabase.getInstance();
        userRef = fdb.getReference("Users");
        postsRef = fdb.getReference("posts");
    }

    private void checkUserAuthentication() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to post.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(PostActivity.this, LoginActivity.class));
            finish();
        }
    }

    private void initializeViews() {
        inputContact = findViewById(R.id.getMobile);
        inputLocation = findViewById(R.id.getLocation);
        spinnerBloodGroup = findViewById(R.id.SpinnerBlood);
        spinnerDivision = findViewById(R.id.SpinnerDivision);
        btnPost = findViewById(R.id.postbtn);
        progressBar = findViewById(R.id.post_progress_bar);

        btnPost.setOnClickListener(v -> publishPost());
    }

    private boolean validateInput(String contact, String location) {
        if (TextUtils.isEmpty(contact)) {
            inputContact.setError("Contact number is required.");
            return false;
        }
        if (TextUtils.isEmpty(location)) {
            inputLocation.setError("Location is required.");
            return false;
        }
        return true;
    }

    private void publishPost() {
        String contact = inputContact.getText().toString().trim();
        String location = inputLocation.getText().toString().trim();

        if (!validateInput(contact, location)) {
            return;
        }

        setLoading(true);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return; // Should not happen due to check on create

        userRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    setLoading(false);
                    Toast.makeText(PostActivity.this, "User profile not found. Please create it first.", Toast.LENGTH_LONG).show();
                    return;
                }

                UserData userData = snapshot.getValue(UserData.class);
                if (userData == null) {
                    setLoading(false);
                    Toast.makeText(PostActivity.this, "Failed to read user profile.", Toast.LENGTH_SHORT).show();
                    return;
                }

                createAndSavePost(userData, contact, location);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                Toast.makeText(PostActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createAndSavePost(UserData userData, String contact, String location) {
        DatabaseReference newPostRef = postsRef.push();

        CustomUserData post = new CustomUserData();
        post.setUid(mAuth.getCurrentUser().getUid());
        post.setName(userData.getName());
        post.setContact(contact);
        post.setAddress(location);
        post.setBloodGroup(spinnerBloodGroup.getSelectedItem().toString());
        post.setDivision(spinnerDivision.getSelectedItem().toString());
        
        // Using a Map to include a server-side timestamp for better sorting
        java.util.Map<String, Object> postValues = post.toMap();
        postValues.put("timestamp", ServerValue.TIMESTAMP);

        newPostRef.setValue(postValues).addOnCompleteListener(task -> {
            setLoading(false);
            if (task.isSuccessful()) {
                Toast.makeText(PostActivity.this, "Request posted successfully!", Toast.LENGTH_SHORT).show();
                finish(); // Go back to the previous screen (Dashboard)
            } else {
                Toast.makeText(PostActivity.this, "Failed to post request. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnPost.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnPost.setEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
