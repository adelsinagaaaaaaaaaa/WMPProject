package com.android.iunoob.bloodbank.viewmodels;

import com.google.firebase.database.Exclude;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CustomUserData implements Serializable {
   private String Address, Division, Contact;
   private String Name, BloodGroup;
   private String uid;
   private long timestamp; // Added timestamp field

   public CustomUserData() {

    }

    // Getters and Setters
    public String getAddress() { return Address; }
    public void setAddress(String address) { this.Address = address; }
    public String getDivision() { return Division; }
    public void setDivision(String division) { this.Division = division; }
    public String getContact() { return Contact; }
    public void setContact(String contact) { this.Contact = contact; }
    public String getName() { return Name; }
    public void setName(String name) { this.Name = name; }
    public String getBloodGroup() { return BloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.BloodGroup = bloodGroup; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public long getTimestamp() { return timestamp; } // Added getter
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; } // Added setter

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("name", Name);
        result.put("contact", Contact);
        result.put("address", Address);
        result.put("bloodGroup", BloodGroup);
        result.put("division", Division);

        return result;
    }
}
