package com.android.iunoob.bloodbank.viewmodels;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class UserData {

    private String Name, Email, Contact, Address, profileImageUrl;
    private int Gender, BloodGroup, Division;
    private boolean isDonor;

    public UserData() {
    }

    // Getters and Setters
    public String getName() { return Name; }
    public void setName(String name) { Name = name; }
    public String getEmail() { return Email; }
    public void setEmail(String email) { Email = email; }
    public String getContact() { return Contact; }
    public void setContact(String contact) { Contact = contact; }
    public String getAddress() { return Address; }
    public void setAddress(String address) { Address = address; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public int getGender() { return Gender; }
    public void setGender(int gender) { Gender = gender; }
    public int getBloodGroup() { return BloodGroup; }
    public void setBloodGroup(int bloodGroup) { BloodGroup = bloodGroup; }
    public int getDivision() { return Division; }
    public void setDivision(int division) { Division = division; }
    public boolean isDonor() { return isDonor; }
    public void setDonor(boolean donor) { isDonor = donor; }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("name", Name);
        result.put("email", Email);
        result.put("contact", Contact);
        result.put("address", Address);
        result.put("profileImageUrl", profileImageUrl);
        result.put("gender", Gender);
        result.put("bloodGroup", BloodGroup);
        result.put("division", Division);
        result.put("donor", isDonor);
        return result;
    }
}
