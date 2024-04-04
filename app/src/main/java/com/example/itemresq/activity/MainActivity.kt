package com.example.itemresq.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.itemresq.R
import com.example.itemresq.adapter.AdapterClassForRecyclerView
import com.example.itemresq.database.FirebaseManager
import com.example.itemresq.databinding.ActivityMainBinding
import com.example.itemresq.util.MapUtil
import com.example.itemresq.util.UiUtil
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var binding : ActivityMainBinding
    private lateinit var mMap : GoogleMap
    private var boundsBuilder = LatLngBounds.Builder()
    private lateinit var recyclerViewLost: RecyclerView
    private lateinit var recyclerViewFound: RecyclerView
    private val limit = 3
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUiForUserInfo()

        binding.sideMenuBtn.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.sideNavView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.my_profile_btn -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, MyProfileActivity::class.java))
                    false
                }
                R.id.my_reports_btn -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    val intent = Intent(this, ListAllReportsActivity::class.java)
                    intent.putExtra("SELECT_MY_REPORTS", true)
                    startActivity(intent)
                    false
                }
                R.id.change_password_btn -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, ChangePasswordActivity::class.java))
                    false
                }
                R.id.log_out_btn -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    false
                }
                R.id.delete_account_btn -> {
                    UiUtil.showDeleteAccountDialog(this)
                    false
                }
                else -> false
            }
        }

        binding.bottomNavBar.menu.findItem(R.id.bottom_menu_main).isChecked = true
        binding.bottomNavBar.menu.findItem(R.id.unchecked_item).isVisible = false

        binding.bottomNavBar.setOnItemSelectedListener {menuItem->
            when(menuItem.itemId) {
                R.id.bottom_menu_map ->{
                    startActivity(Intent(this, MapActivity::class.java))
                }
                R.id.bottom_menu_add_report ->{
                    startActivity(Intent(this, AddReportActivity::class.java))
                }
                R.id.bottom_menu_list_reports ->{
                    startActivity(Intent(this, ListAllReportsActivity::class.java))
                }
            }
            false
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.seeAllLostBtn.setOnClickListener {
            val intent = Intent(this, ListAllReportsActivity::class.java)
            intent.putExtra("SELECT_LOST_REPORTS", true)
            startActivity(intent)
        }

        binding.seeAllFoundBtn.setOnClickListener {
            val intent = Intent(this, ListAllReportsActivity::class.java)
            intent.putExtra("SELECT_FOUND_REPORTS", true)
            startActivity(intent)
        }

        binding.mapToClick.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        recyclerViewLost = findViewById(R.id.recyclerViewLost)
        recyclerViewLost.layoutManager = LinearLayoutManager(this)
        recyclerViewLost.setHasFixedSize(true)

        recyclerViewFound = findViewById(R.id.recyclerViewFound)
        recyclerViewFound.layoutManager = LinearLayoutManager(this)
        recyclerViewFound.setHasFixedSize(true)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.setAllGesturesEnabled(false)
        mMap.uiSettings.isMyLocationButtonEnabled = false
        setUpMap()
    }

    private fun setUpMap() {
        FirebaseManager.fetchReportsForMainActivity(
            onSuccess = { reportsList, reportsLostList, reportsFoundList ->
                if (reportsList.isNotEmpty()) {
                    MapUtil.populateMapWithMarkersAndSetCamera(mMap, boundsBuilder, reportsList, 80)
                }

                if (reportsLostList.isEmpty()) {
                    binding.recentlyLostHeader.visibility = View.GONE
                }
                if (reportsFoundList.isEmpty()) {
                    binding.recentlyFoundHeader.visibility = View.GONE
                }
                if (reportsLostList.isEmpty() && reportsFoundList.isEmpty()) {
                    binding.noReportsInfo.visibility = View.VISIBLE
                }

                val limitedReportsLostList = reportsLostList.takeLast(limit).reversed()
                val limitedReportsFoundList = reportsFoundList.takeLast(limit).reversed()
                recyclerViewLost.adapter = AdapterClassForRecyclerView(limitedReportsLostList)
                recyclerViewFound.adapter = AdapterClassForRecyclerView(limitedReportsFoundList)
            },
            onFailure = { error ->
                UiUtil.showToast(this, error)
            }
        )
    }

    private fun setUiForUserInfo() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid

        if (userId != null) {
            FirebaseManager.fetchUserInfoForMainActivity(userId,
                onSuccess = { userName, profilePictureUri ->
                    val headerView : View = binding.sideNavView.getHeaderView(0)
                    val helloUserTextView: TextView = headerView.findViewById(R.id.hello_user)
                    val profilePictureImageView: ImageView = headerView.findViewById(R.id.profile_picture)
                    helloUserTextView.text = "Witaj $userName"
                    if (profilePictureUri.isEmpty()) {
                        profilePictureImageView.setImageResource(R.drawable.icon_profile_background)
                    } else {
                        Picasso.get().load(profilePictureUri)
                            .placeholder(R.drawable.icon_time_background)
                            .error(R.drawable.icon_error_background)
                            .into(profilePictureImageView)
                    }
                },
                onFailure = {
                    UiUtil.showToast(this, "Wystąpił błąd. Użytkownik nie istnieje.")
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            )
        }
    }
}
