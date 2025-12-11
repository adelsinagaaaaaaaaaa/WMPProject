package com.android.iunoob.bloodbank.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.android.iunoob.bloodbank.R;
import com.android.iunoob.bloodbank.fragments.AboutUs;
import com.android.iunoob.bloodbank.fragments.AchievmentsView;
import com.android.iunoob.bloodbank.fragments.BloodInfo;
import com.android.iunoob.bloodbank.fragments.HomeView;
import com.android.iunoob.bloodbank.fragments.NearByHospitalActivity;
import com.android.iunoob.bloodbank.fragments.SearchDonorFragment;
import com.android.iunoob.bloodbank.viewmodels.UserData;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class Dashboard extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth mAuth;
    private TextView getUserName, getUserEmail;
    private FirebaseDatabase user_db;
    private FirebaseUser cur_user;
    private DatabaseReference userdb_ref;

    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        pd = new ProgressDialog(this);
        pd.setMessage("Loading...");
        pd.setCancelable(false);

        mAuth = FirebaseAuth.getInstance();
        user_db = FirebaseDatabase.getInstance();
        cur_user = mAuth.getCurrentUser();
        userdb_ref = user_db.getReference("Users");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view ->
                startActivity(new Intent(Dashboard.this, PostActivity.class))
        );

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View header = navigationView.getHeaderView(0);
        getUserEmail = header.findViewById(R.id.UserEmailView);
        getUserName = header.findViewById(R.id.UserNameView);

        loadUserData();

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentcontainer, new HomeView())
                    .commit();

            navigationView.getMenu().getItem(0).setChecked(true);
        }
    }

    private void loadUserData() {
        if (cur_user == null) return;

        Query singleuser = userdb_ref.child(cur_user.getUid());
        pd.show();

        singleuser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // --- FIX: Dismiss the dialog at the beginning ---
                if (pd.isShowing()) {
                    pd.dismiss();
                }
                // --- End of Fix ---

                try {
                    UserData user = snapshot.getValue(UserData.class);
                    if (user != null && getUserName != null) {
                        getUserName.setText(user.getName());
                    }
                    if (cur_user != null && getUserEmail != null) {
                        getUserEmail.setText(cur_user.getEmail());
                    }
                } catch (Exception e) {
                    Log.e("DashboardError", "Error setting user data", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // --- FIX: Dismiss the dialog on error too ---
                if (pd.isShowing()) {
                    pd.dismiss();
                }
                 // --- End of Fix ---
                Log.d("User", error.getMessage());
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.donateinfo) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentcontainer, new BloodInfo()).commit();
        } else if (id == R.id.devinfo) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentcontainer, new AboutUs()).commit();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.home) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentcontainer, new HomeView()).commit();

        } else if (id == R.id.userprofile) {
            startActivity(new Intent(this, ProfileActivity.class));

        } else if (id == R.id.user_achiev) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentcontainer, new AchievmentsView()).commit();

        } else if (id == R.id.logout) {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));

        } else if (id == R.id.blood_storage) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentcontainer, new SearchDonorFragment()).commit();

        } else if (id == R.id.nearby_hospital) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentcontainer, new NearByHospitalActivity()).commit();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }
}
