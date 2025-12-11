package com.android.iunoob.bloodbank.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.iunoob.bloodbank.R;
import com.android.iunoob.bloodbank.viewmodels.DonorData;

import java.util.List;

public class SearchDonorAdapter extends RecyclerView.Adapter<SearchDonorAdapter.PostHolder> {

    private List<DonorData> postLists;
    private Context context;

    public SearchDonorAdapter(List<DonorData> postLists) {
        this.postLists = postLists;
    }

    @NonNull
    @Override
    public PostHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        context = viewGroup.getContext();
        View listitem = LayoutInflater.from(context)
                .inflate(R.layout.search_donor_item, viewGroup, false);
        return new PostHolder(listitem);
    }

    @Override
    public void onBindViewHolder(@NonNull PostHolder postHolder, int i) {
        DonorData donorData = postLists.get(i);

        postHolder.Name.setText(donorData.getName());
        postHolder.contact.setText("Contact: " + donorData.getContact());
        postHolder.Address.setText("Address: " + donorData.getAddress());
        postHolder.totaldonate.setText("Total Donation: " + donorData.getTotalDonate() + " times");
        postHolder.posted.setText("Last Donation: " + donorData.getLastDonate());

        // --- Call Button Logic ---
        postHolder.btnCall.setOnClickListener(v -> {
            String phoneNumber = donorData.getContact();
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + phoneNumber));
                context.startActivity(callIntent);
            } else {
                Toast.makeText(context, "Contact number not available.", Toast.LENGTH_SHORT).show();
            }
        });

        // --- Share Button Logic ---
        postHolder.btnShare.setOnClickListener(v -> {
            try {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareBody = "Blood Donor Found!\n" +
                        "Name: " + donorData.getName() + "\n" +
                        "Contact: " + donorData.getContact() + "\n" +
                        "Address: " + donorData.getAddress() + "\n" +
                        "(Shared from BloodBank App)";
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                context.startActivity(Intent.createChooser(shareIntent, "Share donor via"));
            } catch (Exception e) {
                Toast.makeText(context, "Cannot share donor details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return postLists.size();
    }

    public class PostHolder extends RecyclerView.ViewHolder {
        TextView Name, Address, contact, posted, totaldonate;
        ImageButton btnCall, btnShare;

        public PostHolder(@NonNull View itemView) {
            super(itemView);

            Name = itemView.findViewById(R.id.donorName);
            contact = itemView.findViewById(R.id.donorContact);
            totaldonate = itemView.findViewById(R.id.totaldonate);
            Address = itemView.findViewById(R.id.donorAddress);
            posted = itemView.findViewById(R.id.lastdonate);
            btnCall = itemView.findViewById(R.id.btn_call);
            btnShare = itemView.findViewById(R.id.btn_share);
        }
    }
}
