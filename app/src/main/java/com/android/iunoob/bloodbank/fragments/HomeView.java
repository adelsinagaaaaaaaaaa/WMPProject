package com.android.iunoob.bloodbank.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.iunoob.bloodbank.R;
import com.android.iunoob.bloodbank.adapters.BloodRequestAdapter;
import com.android.iunoob.bloodbank.viewmodels.CustomUserData;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeView extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout emptyViewLayout;

    private DatabaseReference postsRef;
    private ValueEventListener postsListener;
    private BloodRequestAdapter bloodRequestAdapter;
    private List<CustomUserData> postList;
    private List<CustomUserData> filteredPostList;

    // Filter containers
    private LinearLayout bloodGroupFilterContainer;
    private LinearLayout divisionFilterContainer;

    // Filter states
    private String selectedBloodGroup = "All";
    private String selectedDivision = "All";

    // Blood groups and divisions
    private final String[] bloodGroups = {"All", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
    private final String[] divisions = {"All", "Dhaka", "Chittagong", "Rajshahi", "Khulna",
            "Barisal", "Sylhet", "Rangpur", "Mymensingh"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_view_fragment, container, false);

        initializeViews(view);
        setupRecyclerView();
        setupFilterChips();
        setupFirebase();

        return view;
    }

    private void initializeViews(View view) {
        if (getActivity() != null) {
            getActivity().setTitle("Blood Requests");
        }
        recyclerView = view.findViewById(R.id.recyleposts);
        progressBar = view.findViewById(R.id.home_progress_bar);
        emptyViewLayout = view.findViewById(R.id.empty_view_layout);
        bloodGroupFilterContainer = view.findViewById(R.id.blood_group_filter_container);
        divisionFilterContainer = view.findViewById(R.id.division_filter_container);
    }

    private void setupRecyclerView() {
        postList = new ArrayList<>();
        filteredPostList = new ArrayList<>();
        bloodRequestAdapter = new BloodRequestAdapter(filteredPostList);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(bloodRequestAdapter);
    }

    private void setupFilterChips() {
        // Setup Blood Group Chips
        for (String bloodGroup : bloodGroups) {
            TextView chip = createFilterChip(bloodGroup, true);
            bloodGroupFilterContainer.addView(chip);
        }

        // Setup Division Chips
        for (String division : divisions) {
            TextView chip = createFilterChip(division, false);
            divisionFilterContainer.addView(chip);
        }

        // Select "All" by default
        if (bloodGroupFilterContainer.getChildCount() > 0) {
            bloodGroupFilterContainer.getChildAt(0).setSelected(true);
        }
        if (divisionFilterContainer.getChildCount() > 0) {
            divisionFilterContainer.getChildAt(0).setSelected(true);
        }
    }

    private TextView createFilterChip(String text, boolean isBloodGroup) {
        TextView chip = (TextView) LayoutInflater.from(getContext())
                .inflate(R.layout.filter_chip, null);
        chip.setText(text);

        chip.setOnClickListener(v -> {
            // Deselect all chips in the same container
            LinearLayout container = isBloodGroup ? bloodGroupFilterContainer : divisionFilterContainer;
            for (int i = 0; i < container.getChildCount(); i++) {
                container.getChildAt(i).setSelected(false);
            }

            // Select clicked chip
            chip.setSelected(true);

            // Update filter
            if (isBloodGroup) {
                selectedBloodGroup = text;
            } else {
                selectedDivision = text;
            }

            // Apply filter
            applyFilters();
        });

        return chip;
    }

    private void setupFirebase() {
        postsRef = FirebaseDatabase.getInstance().getReference("posts");
    }

    @Override
    public void onStart() {
        super.onStart();
        listenForPosts();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (postsListener != null) {
            postsRef.removeEventListener(postsListener);
        }
    }

    private void listenForPosts() {
        setLoading(true);
        Query postsQuery = postsRef.orderByChild("timestamp");

        postsListener = postsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!isAdded()) return;

                postList.clear();

                if (dataSnapshot.exists()) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        try {
                            CustomUserData post = postSnapshot.getValue(CustomUserData.class);
                            if (post != null) {
                                postList.add(post);
                            }
                        } catch (Exception e) {
                            Log.e("HomeView", "Could not parse post.", e);
                        }
                    }
                    Collections.reverse(postList); // Show newest posts on top
                    applyFilters();
                } else {
                    showEmptyView();
                }
                setLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (!isAdded()) return;
                setLoading(false);
                Toast.makeText(getActivity(), "Failed to load posts: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        filteredPostList.clear();

        for (CustomUserData post : postList) {
            boolean matchesBloodGroup = selectedBloodGroup.equals("All") ||
                    matchesBloodGroupFilter(post.getBloodGroup(), selectedBloodGroup);
            boolean matchesDivision = selectedDivision.equals("All") ||
                    matchesDivisionFilter(post.getDivision(), selectedDivision);

            if (matchesBloodGroup && matchesDivision) {
                filteredPostList.add(post);
            }
        }

        if (filteredPostList.isEmpty()) {
            showEmptyView();
        } else {
            showData();
        }

        bloodRequestAdapter.notifyDataSetChanged();
    }

    private boolean matchesBloodGroupFilter(String postBloodGroup, String filterBloodGroup) {
        if (postBloodGroup == null || filterBloodGroup == null) return false;

        // Convert blood group number to string if needed
        String bloodGroupStr = convertBloodGroupToString(postBloodGroup);
        return bloodGroupStr.equalsIgnoreCase(filterBloodGroup);
    }

    private boolean matchesDivisionFilter(String postDivision, String filterDivision) {
        if (postDivision == null || filterDivision == null) return false;

        // Convert division number to string if needed
        String divisionStr = convertDivisionToString(postDivision);
        return divisionStr.equalsIgnoreCase(filterDivision);
    }

    private String convertBloodGroupToString(String bloodGroup) {
        // If already a string, return as is
        if (!bloodGroup.matches("\\d+")) {
            return bloodGroup;
        }

        // Convert number to blood group string
        try {
            int index = Integer.parseInt(bloodGroup);
            String[] bloodGroupArray = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
            if (index >= 0 && index < bloodGroupArray.length) {
                return bloodGroupArray[index];
            }
        } catch (NumberFormatException e) {
            Log.e("HomeView", "Invalid blood group format: " + bloodGroup);
        }
        return bloodGroup;
    }

    private String convertDivisionToString(String division) {
        // If already a string, return as is
        if (!division.matches("\\d+")) {
            return division;
        }

        // Convert number to division string
        try {
            int index = Integer.parseInt(division);
            String[] divisionArray = {"Dhaka", "Chittagong", "Rajshahi", "Khulna",
                    "Barisal", "Sylhet", "Rangpur", "Mymensingh"};
            if (index >= 0 && index < divisionArray.length) {
                return divisionArray[index];
            }
        } catch (NumberFormatException e) {
            Log.e("HomeView", "Invalid division format: " + division);
        }
        return division;
    }

    private void setLoading(boolean isLoading) {
        if(progressBar == null || recyclerView == null || emptyViewLayout == null) return;
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            recyclerView.setVisibility(View.GONE);
            emptyViewLayout.setVisibility(View.GONE);
        }
    }

    private void showData() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyViewLayout.setVisibility(View.GONE);
    }

    private void showEmptyView() {
        recyclerView.setVisibility(View.GONE);
        emptyViewLayout.setVisibility(View.VISIBLE);
    }
}