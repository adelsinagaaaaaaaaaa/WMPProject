package com.android.iunoob.bloodbank.viewmodels;

import java.io.Serializable;

public class DonorData implements Serializable {

    private String name, address, contact, lastDonate;
    private long totalDonate;

    public DonorData() {
    }

    // Constructor to map from UserData
    public DonorData(String name, String address, String contact, long totalDonate, String lastDonate) {
        this.name = name;
        this.address = address;
        this.contact = contact;
        this.totalDonate = totalDonate;
        this.lastDonate = lastDonate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getLastDonate() {
        return lastDonate;
    }

    public void setLastDonate(String lastDonate) {
        this.lastDonate = lastDonate;
    }

    public long getTotalDonate() {
        return totalDonate;
    }

    public void setTotalDonate(long totalDonate) {
        this.totalDonate = totalDonate;
    }
}
