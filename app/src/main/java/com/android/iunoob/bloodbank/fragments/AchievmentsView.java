package com.android.iunoob.bloodbank.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.iunoob.bloodbank.R;
import com.android.iunoob.bloodbank.viewmodels.DonorData;
import com.android.iunoob.bloodbank.viewmodels.UserData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AchievmentsView extends Fragment {

    private ProgressDialog pd;
    private DatabaseReference userRef, donorsRef;
    private FirebaseAuth mAuth;

    private TextView totalDonateText, lastDonateText, nextDonateText, donateInfoText, notDonorText, impactText;
    private LinearLayout donorLayout, yesNoLayout;
    private RelativeLayout progressLayout;
    private ProgressBar circularProgressBar;
    private Button btnYes, btnNo, btnShareAchievement;
    private ImageView badgeBronze, badgeSilver, badgeGold, badgePlatinum, badgeFirstDrop, badgeLifeSaver;
    private View view;

    private long currentTotalDonations = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.user_achievment_fragment, container, false);

        initializeViews();
        setupFirebase();

        if (mAuth.getCurrentUser() != null) {
            loadUserData();
        } else {
            showNotDonorMessage("You are not logged in.");
        }

        return view;
    }

    private void initializeViews() {
        pd = new ProgressDialog(getActivity());
        pd.setMessage("Loading Achievements...");
        pd.setCancelable(false);

        if (getActivity() != null) {
            getActivity().setTitle("My Achievements");
        }

        totalDonateText = view.findViewById(R.id.settotalDonate);
        lastDonateText = view.findViewById(R.id.setLastDonate);
        nextDonateText = view.findViewById(R.id.nextDonate);
        donateInfoText = view.findViewById(R.id.donateInfo);
        notDonorText = view.findViewById(R.id.ShowInof);
        impactText = view.findViewById(R.id.impact_text);
        donorLayout = view.findViewById(R.id.donorAchiev);
        yesNoLayout = view.findViewById(R.id.yesnolayout);
        progressLayout = view.findViewById(R.id.progress_layout);
        circularProgressBar = view.findViewById(R.id.progress_bar);
        btnYes = view.findViewById(R.id.btnYes);
        btnNo = view.findViewById(R.id.btnNo);
        
        badgeBronze = view.findViewById(R.id.badge_bronze);
        badgeSilver = view.findViewById(R.id.badge_silver);
        badgeGold = view.findViewById(R.id.badge_gold);
        badgePlatinum = view.findViewById(R.id.badge_platinum);
        badgeFirstDrop = view.findViewById(R.id.badge_first_drop);
        badgeLifeSaver = view.findViewById(R.id.badge_life_saver);
        btnShareAchievement = view.findViewById(R.id.btn_share_achievement);

        btnShareAchievement.setOnClickListener(v -> shareAchievement());
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase fdb = FirebaseDatabase.getInstance();
        userRef = fdb.getReference("Users");
        donorsRef = fdb.getReference("donors");
    }

    private void loadUserData() {
        pd.show();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            pd.dismiss();
            return;
        }

        userRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                if (!snapshot.exists()) {
                    pd.dismiss();
                    showNotDonorMessage("User profile not found. Please create your profile.");
                    return;
                }

                UserData userData = snapshot.getValue(UserData.class);
                if (userData == null || !userData.isDonor()) {
                    pd.dismiss();
                    showNotDonorMessage("You are not registered as a donor. Please update your profile to see achievements.");
                    return;
                }

                checkDonorAchievements(currentUser.getUid(), userData.getBloodGroup());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                pd.dismiss();
                Toast.makeText(getActivity(), "Failed to load profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkDonorAchievements(String uid, int bloodGroupIndex) {
        donorsRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                pd.dismiss();
                donorLayout.setVisibility(View.VISIBLE);
                notDonorText.setVisibility(View.GONE);

                long totalDonations = 0;
                String lastDonationDate = "";

                if (snapshot.exists()) {
                    DonorData donorData = snapshot.getValue(DonorData.class);
                    if (donorData != null) {
                        totalDonations = donorData.getTotalDonate();
                        lastDonationDate = donorData.getLastDonate();
                    }
                }

                currentTotalDonations = totalDonations;
                totalDonateText.setText(totalDonations + " times");
                updateImpactMetric(totalDonations);
                displayBadges(totalDonations, bloodGroupIndex);

                if (lastDonationDate == null || lastDonationDate.isEmpty() || totalDonations == 0) {
                    lastDonateText.setText("Not yet donated!");
                    handleDonationEligibility(999, totalDonations, uid);
                } else {
                    lastDonateText.setText(lastDonationDate);
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        Date date = sdf.parse(lastDonationDate);
                        long diffInMillis = new Date().getTime() - date.getTime();
                        long daysSinceDonation = TimeUnit.MILLISECONDS.toDays(diffInMillis);
                        handleDonationEligibility(daysSinceDonation, totalDonations, uid);
                    } catch (Exception e) {
                        handleDonationEligibility(999, totalDonations, uid);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                pd.dismiss();
                Toast.makeText(getActivity(), "Failed to load achievements: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleDonationEligibility(long daysSinceDonation, long currentTotalDonations, String uid) {
        long daysRequired = 120;

        if (daysSinceDonation >= daysRequired) {
            donateInfoText.setText("You are eligible to donate again!");
            progressLayout.setVisibility(View.GONE);
            yesNoLayout.setVisibility(View.VISIBLE);
            btnYes.setOnClickListener(v -> updateUserDonation(currentTotalDonations, uid));
            btnNo.setOnClickListener(v -> {
                yesNoLayout.setVisibility(View.GONE);
                donateInfoText.setText("Thank you for your response.");
            });
        } else {
            long daysRemaining = daysRequired - daysSinceDonation;
            donateInfoText.setText("Next donation available in:");
            nextDonateText.setText(daysRemaining + "\ndays");
            int progress = (int) (daysRequired - daysRemaining);
            circularProgressBar.setProgress(progress);
            progressLayout.setVisibility(View.VISIBLE);
            yesNoLayout.setVisibility(View.GONE);
        }
    }

    private void updateUserDonation(long currentTotalDonations, String uid) {
        pd.show();
        String todayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        DatabaseReference donorRecord = donorsRef.child(uid);
        donorRecord.child("lastDonate").setValue(todayDate);
        donorRecord.child("totalDonate").setValue(currentTotalDonations + 1)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    pd.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(getActivity(), "Achievement updated! Thank you, hero!", Toast.LENGTH_SHORT).show();
                        loadUserData();
                    } else {
                        Toast.makeText(getActivity(), "Failed to update. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateImpactMetric(long totalDonations) {
        if (totalDonations > 0) {
            long livesSaved = totalDonations * 3;
            impactText.setText("You have helped save up to " + livesSaved + " lives!");
            impactText.setVisibility(View.VISIBLE);
        } else {
            impactText.setVisibility(View.GONE);
        }
    }

    private void displayBadges(long totalDonations, int bloodGroupIndex) {
        badgeBronze.setVisibility(View.GONE);
        badgeSilver.setVisibility(View.GONE);
        badgeGold.setVisibility(View.GONE);
        badgePlatinum.setVisibility(View.GONE);
        badgeFirstDrop.setVisibility(View.GONE);
        badgeLifeSaver.setVisibility(View.GONE);

        if (totalDonations >= 1) badgeFirstDrop.setVisibility(View.VISIBLE);
        if (totalDonations >= 1) badgeBronze.setVisibility(View.VISIBLE);
        if (totalDonations >= 5) badgeSilver.setVisibility(View.VISIBLE);
        if (totalDonations >= 10) badgeGold.setVisibility(View.VISIBLE);
        if (totalDonations >= 25) badgePlatinum.setVisibility(View.VISIBLE);

        String[] bloodGroups = getResources().getStringArray(R.array.Blood_Group);
        List<String> rareBloodTypes = Arrays.asList("O-", "AB+", "AB-");
        if (bloodGroupIndex >= 0 && bloodGroupIndex < bloodGroups.length) {
            String userBloodGroup = bloodGroups[bloodGroupIndex];
            if (rareBloodTypes.contains(userBloodGroup)) {
                badgeLifeSaver.setVisibility(View.VISIBLE);
            }
        }
    }

    private void shareAchievement() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            long livesSaved = currentTotalDonations * 3;
            String shareBody = "I've just checked my achievements on the BloodBank App!\n" +
                    "I have donated " + currentTotalDonations + " times and helped save up to " + livesSaved + " lives!\n\n" +
                    "Join me in saving lives. Download BloodBank App today!";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(shareIntent, "Share your achievement via"));
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Cannot share at the moment.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showNotDonorMessage(String message) {
        donorLayout.setVisibility(View.GONE);
        notDonorText.setVisibility(View.VISIBLE);
        notDonorText.setText(message);
    }
}
