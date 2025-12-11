package com.android.iunoob.bloodbank.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.iunoob.bloodbank.R;
import com.android.iunoob.bloodbank.viewmodels.UserData;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    // Views
    private EditText inputName, inputContact, inputAddress, inputUserEmail, inputPassword, inputPasswordConfirm;
    private Spinner inputSex, inputBloodGroup, inputDivision;
    private CheckBox checkbox;
    private Button btnUpdate;
    private ImageView backBtn;
    private CircleImageView profileImageView;
    private FloatingActionButton fabEditPhoto;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private StorageReference storageRef;
    private FirebaseUser currentUser;

    // State
    private Uri imageUri;
    private String existingImageUrl = null;

    // Activity Result Launchers (Modern way to handle permissions and image picking)
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    pickImageFromGallery();
                } else {
                    Toast.makeText(this, "Permission denied. Cannot access gallery.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    imageUri = result.getData().getData();
                    Glide.with(this).load(imageUri).into(profileImageView);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initializeFirebase();
        initializeViews(); // This was not fully implemented before, a critical error.
        setupListeners(); // This was missing before, a critical error.

        if (currentUser != null) {
            loadUserProfile();
        } else {
            Toast.makeText(this, "User not authenticated. Please login again.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // --- Initialization Methods ---

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        dbRef = FirebaseDatabase.getInstance().getReference("Users");
        storageRef = FirebaseStorage.getInstance().getReference("profile_images");
    }

    private void initializeViews() {
        backBtn = findViewById(R.id.back_btn);
        profileImageView = findViewById(R.id.profile_image);
        fabEditPhoto = findViewById(R.id.fab_edit_photo);
        inputName = findViewById(R.id.input_name);
        inputContact = findViewById(R.id.input_contact);
        inputAddress = findViewById(R.id.input_address);
        inputUserEmail = findViewById(R.id.input_userEmail);
        inputPassword = findViewById(R.id.input_password);
        inputPasswordConfirm = findViewById(R.id.input_password_confirm);
        inputSex = findViewById(R.id.input_sex);
        inputBloodGroup = findViewById(R.id.input_blood_group);
        inputDivision = findViewById(R.id.input_division);
        checkbox = findViewById(R.id.checkbox);
        btnUpdate = findViewById(R.id.btn_update);
    }

    private void setupListeners() {
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish()); // Back button now works.
        }
        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v -> handleUpdateProfile()); // Update button now works.
        }
        if (fabEditPhoto != null) {
            fabEditPhoto.setOnClickListener(v -> checkPermissionAndPickImage());
        }
    }

    // --- Data Loading and Populating ---

    private void loadUserProfile() {
        dbRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    UserData userData = snapshot.getValue(UserData.class);
                    if (userData != null) {
                        populateProfileData(userData);
                    }
                } else {
                    if (currentUser.getEmail() != null) {
                        inputUserEmail.setText(currentUser.getEmail());
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateProfileData(UserData userData) {
        inputName.setText(userData.getName());
        inputContact.setText(userData.getContact());
        inputAddress.setText(userData.getAddress());
        inputUserEmail.setText(userData.getEmail());
        checkbox.setChecked(userData.isDonor());

        existingImageUrl = userData.getProfileImageUrl();
        if (existingImageUrl != null && !existingImageUrl.isEmpty() && getApplicationContext() != null) {
            Glide.with(ProfileActivity.this).load(existingImageUrl).placeholder(R.drawable.ic_default_avatar).into(profileImageView);
        }

        try {
            if (userData.getGender() >= 0) inputSex.setSelection(userData.getGender());
            if (userData.getBloodGroup() >= 0) inputBloodGroup.setSelection(userData.getBloodGroup());
            if (userData.getDivision() >= 0) inputDivision.setSelection(userData.getDivision());
        } catch (Exception e) {
            Log.e("ProfileActivity", "Error setting spinner values", e);
        }
    }

    // --- Update Logic ---

    private void handleUpdateProfile() {
        if (imageUri != null) {
            uploadImageThenUpdateProfile();
        } else {
            updateProfileData(existingImageUrl); // Update with the old image URL
        }
    }

    private void uploadImageThenUpdateProfile() {
        final StorageReference fileReference = storageRef.child(currentUser.getUid() + ".jpg");

        fileReference.putFile(imageUri).addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
            String newImageUrl = uri.toString();
            updateProfileData(newImageUrl);
        })).addOnFailureListener(e -> {
            Toast.makeText(ProfileActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateProfileData(String imageUrl) {
        String name = inputName.getText().toString().trim();
        String contact = inputContact.getText().toString().trim();
        String address = inputAddress.getText().toString().trim();
        String email = inputUserEmail.getText().toString().trim();
        String newPassword = inputPassword.getText().toString().trim();
        String confirmPassword = inputPasswordConfirm.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(contact) || TextUtils.isEmpty(address) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please fill all mandatory profile fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update profile data in Realtime Database
        UserData userData = new UserData();
        userData.setName(name);
        userData.setEmail(email);
        userData.setContact(contact);
        userData.setAddress(address);
        userData.setGender(inputSex.getSelectedItemPosition());
        userData.setBloodGroup(inputBloodGroup.getSelectedItemPosition());
        userData.setDivision(inputDivision.getSelectedItemPosition());
        userData.setDonor(checkbox.isChecked());
        userData.setProfileImageUrl(imageUrl);

        dbRef.child(currentUser.getUid()).setValue(userData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (TextUtils.isEmpty(newPassword)) { // Show success only if password is not being updated
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed to update profile data.", Toast.LENGTH_LONG).show();
            }
        });

        // Update password in Firebase Auth (if provided)
        if (!TextUtils.isEmpty(newPassword)) {
            if (newPassword.length() < 6) {
                inputPassword.setError("Password must be at least 6 characters.");
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                inputPasswordConfirm.setError("Passwords do not match.");
                return;
            }
            updateUserPassword(newPassword);
        }
    }

    private void updateUserPassword(String newPassword) {
        currentUser.updatePassword(newPassword).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ProfileActivity.this, "Password and profile updated successfully!", Toast.LENGTH_SHORT).show();
                inputPassword.setText(""); // Clear fields on success
                inputPasswordConfirm.setText("");
            } else {
                Toast.makeText(ProfileActivity.this, "Failed to update password. Please re-login and try again.", Toast.LENGTH_LONG).show();
                Log.e("ProfileActivity", "Password update failed", task.getException());
            }
        });
    }

    // --- Permission and Image Picking Logic ---

    private void checkPermissionAndPickImage() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageFromGallery();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }
}
