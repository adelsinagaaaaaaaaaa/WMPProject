package com.android.iunoob.bloodbank.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.iunoob.bloodbank.R;
import com.android.iunoob.bloodbank.adapters.SearchDonorAdapter;
import com.android.iunoob.bloodbank.viewmodels.DonorData;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchDonorFragment extends Fragment {

    private Spinner bloodGroupSpinner, divisionSpinner;
    private Button btnSearch;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;

    private SearchDonorAdapter searchAdapter;
    private List<DonorData> finalDonorList;

    private DatabaseReference usersRef, donorsRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_donor_fragment, container, false);

        initializeViews(view);
        setupFirebase();
        setupRecyclerView();

        btnSearch.setOnClickListener(v -> searchDonors());

        return view;
    }

    private void initializeViews(View view) {
        if (getActivity() != null) {
            getActivity().setTitle("Find Blood Donor");
        }

        bloodGroupSpinner = view.findViewById(R.id.btngetBloodGroup);
        divisionSpinner = view.findViewById(R.id.btngetDivison);
        btnSearch = view.findViewById(R.id.btnSearch);
        recyclerView = view.findViewById(R.id.showDonorList);
        progressBar = view.findViewById(R.id.search_progress_bar); // Using the modern ProgressBar
    }

    private void setupFirebase() {
        FirebaseDatabase fdb = FirebaseDatabase.getInstance();
        usersRef = fdb.getReference("Users");
        donorsRef = fdb.getReference("donors");
    }

    private void setupRecyclerView() {
        finalDonorList = new ArrayList<>();
        searchAdapter = new SearchDonorAdapter(finalDonorList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(requireActivity(), LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(searchAdapter);
    }

    private void searchDonors() {
        setLoading(true);

        donorsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot donorHistorySnapshot) {
                // DEFINITIVE FIX #1: Safety check before any operation
                if (!isAdded()) return;

                Map<String, DonorData> donorHistoryMap = new HashMap<>();
                if (donorHistorySnapshot.exists()) {
                    for (DataSnapshot donorRecord : donorHistorySnapshot.getChildren()) {
                        try {
                            DonorData history = donorRecord.getValue(DonorData.class);
                            if (history != null) {
                                donorHistoryMap.put(donorRecord.getKey(), history);
                            }
                        } catch (Exception e) {
                            Log.e("SearchDonorFragment", "Skipping corrupted donor history: " + donorRecord.getKey(), e);
                        }
                    }
                }
                filterUsers(donorHistoryMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                setLoading(false);
                showErrorToast("Could not fetch donor history: " + error.getMessage());
            }
        });
    }

    private void filterUsers(Map<String, DonorData> donorHistoryMap) {
        String selectedBlood = bloodGroupSpinner.getSelectedItem().toString();
        String selectedDivision = divisionSpinner.getSelectedItem().toString();

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                // DEFINITIVE FIX #2: Safety check before any operation
                if (!isAdded()) return;

                finalDonorList.clear();

                if (usersSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : usersSnapshot.getChildren()) {
                        String uid = userSnapshot.getKey();
                        try {
                            Boolean isDonor = false;
                            if (userSnapshot.child("donor").exists()) {
                                isDonor = userSnapshot.child("donor").getValue(Boolean.class);
                            } else if (userSnapshot.child("isDonor").exists()) {
                                isDonor = userSnapshot.child("isDonor").getValue(Boolean.class);
                            }

                            if (isDonor == null || !isDonor || uid == null) {
                                continue;
                            }

                            String[] bloodGroupsArray = getResources().getStringArray(R.array.Blood_Group);
                            String[] divisionsArray = getResources().getStringArray(R.array.division_list);

                            String userBloodGroup = getStringValueFromSnapshot(userSnapshot.child("bloodGroup"), bloodGroupsArray);
                            String userDivision = getStringValueFromSnapshot(userSnapshot.child("division"), divisionsArray);

                            if (userBloodGroup == null || userDivision == null) continue;

                            if (userBloodGroup.equals(selectedBlood) && userDivision.equals(selectedDivision)) {
                                String name = userSnapshot.child("name").getValue(String.class);
                                String address = userSnapshot.child("address").getValue(String.class);
                                String contact = userSnapshot.child("contact").getValue(String.class);

                                DonorData history = donorHistoryMap.get(uid);
                                long totalDonate = (history != null) ? history.getTotalDonate() : 0;
                                String lastDonate = (history != null && history.getLastDonate() != null) ? history.getLastDonate() : "Not yet donated!";

                                DonorData displayData = new DonorData(name, address, contact, totalDonate, lastDonate);
                                finalDonorList.add(displayData);
                            }
                        } catch (Exception e) {
                            Log.e("SearchDonorFragment", "Skipping corrupted user profile: " + uid, e);
                        }
                    }
                }

                if (finalDonorList.isEmpty()) {
                    Toast.makeText(getActivity(), "No donors found matching your criteria.", Toast.LENGTH_LONG).show();
                }
                searchAdapter.notifyDataSetChanged();
                setLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (!isAdded()) return;
                setLoading(false);
                showErrorToast("A database error occurred during search.");
            }
        });
    }

    private String getStringValueFromSnapshot(DataSnapshot snapshot, String[] array) {
        Object value = snapshot.getValue();
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Long) {
            int index = ((Long) value).intValue();
            if (index >= 0 && index < array.length) {
                return array[index];
            }
        }
        return null;
    }

    private void showErrorToast(String message) {
        if (isAdded() && getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
        }
        Log.e("SearchDonorFragment", message);
    }

    private void setLoading(boolean isLoading) {
        if (progressBar == null || btnSearch == null) return;
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnSearch.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSearch.setEnabled(true);
        }
    }
}