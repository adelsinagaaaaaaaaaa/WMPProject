package com.android.iunoob.bloodbank.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.iunoob.bloodbank.R;
import com.android.iunoob.bloodbank.viewmodels.CustomUserData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BloodRequestAdapter extends RecyclerView.Adapter<BloodRequestAdapter.ViewHolder> {

    private List<CustomUserData> postList;

    private final String[] bloodGroupArray = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};

    private final String[] divisionArray = {"Dhaka", "Chittagong", "Rajshahi", "Khulna",
            "Barisal", "Sylhet", "Rangpur", "Mymensingh"};

    public BloodRequestAdapter(List<CustomUserData> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.blood_request_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomUserData post = postList.get(position);

        String bloodGroup = getBloodGroupString(post.getBloodGroup());
        holder.bloodTypeBadge.setText(bloodGroup);
        holder.bloodTypeBadge.setBackgroundResource(getBloodTypeColor(bloodGroup));

        holder.requestTitle.setText("Needs " + bloodGroup + " Blood Donor!");

        if (post.getName() != null && !post.getName().isEmpty()) {
            holder.postedBy.setText("Posted by: " + post.getName());
        } else {
            holder.postedBy.setText("Posted by: Anonymous");
        }

        String division = getDivisionString(post.getDivision());
        String address = post.getAddress() != null ? post.getAddress() : "";
        holder.locationText.setText(address.isEmpty() ? division : address + ", " + division);

        if (post.getContact() != null && !post.getContact().isEmpty()) {
            holder.contactText.setText(post.getContact());
            holder.contactText.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + post.getContact()));
                v.getContext().startActivity(intent);
            });
        } else {
            holder.contactText.setText("No contact provided");
        }

        if (post.getTimestamp() > 0) {
            holder.postedTime.setText("Posted on: " + formatTimestamp(post.getTimestamp()));
        } else {
            holder.postedTime.setText("Posted recently");
        }

        holder.urgentBadge.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    private String getBloodGroupString(String bloodGroup) {
        if (bloodGroup == null) return "Unknown";
        if (!bloodGroup.matches("\\d+")) return bloodGroup;
        try {
            int index = Integer.parseInt(bloodGroup);
            if (index >= 0 && index < bloodGroupArray.length) return bloodGroupArray[index];
        } catch (Exception ignored) {}
        return bloodGroup;
    }

    private String getDivisionString(String division) {
        if (division == null) return "Unknown";
        if (!division.matches("\\d+")) return division;
        try {
            int index = Integer.parseInt(division);
            if (index >= 0 && index < divisionArray.length) return divisionArray[index];
        } catch (Exception ignored) {}
        return division;
    }

    private int getBloodTypeColor(String bloodGroup) {
        switch (bloodGroup) {
            case "A+": return R.drawable.blood_badge_a_plus;
            case "A-": return R.drawable.blood_badge_a_minus;
            case "B+": return R.drawable.blood_badge_b_plus;
            case "B-": return R.drawable.blood_badge_b_minus;
            case "O+": return R.drawable.blood_badge_o_plus;
            case "O-": return R.drawable.blood_badge_o_minus;
            case "AB+": return R.drawable.blood_badge_ab_plus;
            case "AB-": return R.drawable.blood_badge_ab_minus;
            default: return R.drawable.blood_badge_background;
        }
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(new Date(timestamp));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView bloodTypeBadge, urgentBadge, requestTitle, postedBy, locationText, contactText, postedTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            bloodTypeBadge = itemView.findViewById(R.id.blood_type_badge);
            urgentBadge = itemView.findViewById(R.id.urgent_badge);
            requestTitle = itemView.findViewById(R.id.request_title);
            postedBy = itemView.findViewById(R.id.posted_by);
            locationText = itemView.findViewById(R.id.location_text);
            contactText = itemView.findViewById(R.id.contact_text);
            postedTime = itemView.findViewById(R.id.posted_time);
        }
    }
}
