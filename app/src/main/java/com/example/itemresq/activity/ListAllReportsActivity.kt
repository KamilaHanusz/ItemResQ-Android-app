package com.example.itemresq.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.itemresq.R
import com.example.itemresq.adapter.AdapterClassForRecyclerView
import com.example.itemresq.databinding.ActivityListAllReportsBinding
import com.example.itemresq.model.ReportModel
import com.example.itemresq.util.UiUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ListAllReportsActivity : AppCompatActivity() {

    lateinit var binding : ActivityListAllReportsBinding
    private lateinit var recyclerView: RecyclerView
    private var reportsCollection = FirebaseFirestore.getInstance().collection("reports")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListAllReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavBar.menu.findItem(R.id.bottom_menu_list_reports).isChecked = true
        binding.bottomNavBar.menu.findItem(R.id.unchecked_item).isVisible = false

        binding.bottomNavBar.setOnItemSelectedListener {menuItem->
            when(menuItem.itemId) {
                R.id.bottom_menu_main ->{
                    startActivity(Intent(this, MainActivity::class.java))
                }
                R.id.bottom_menu_map ->{
                    startActivity(Intent(this, MapActivity::class.java))
                }
                R.id.bottom_menu_add_report ->{
                    startActivity(Intent(this, AddReportActivity::class.java))
                }
            }
            false
        }

        recyclerView = findViewById(R.id.recycleView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid

        if (intent.getBooleanExtra("SELECT_LOST_REPORTS", false)) {
            binding.lostBtn.isChecked = true
            fetchFilteredReports("Zagubione")
        } else if (intent.getBooleanExtra("SELECT_FOUND_REPORTS", false)) {
            binding.foundBtn.isChecked = true
            fetchFilteredReports("Znalezione")
        } else if (intent.getBooleanExtra("SELECT_MY_REPORTS", false)) {
            binding.myBtn.isChecked = true
            fetchMyReports(userId!!)
        } else {
            fetchAllReports()
        }

        binding.optionButtons.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.all_btn -> {
                    fetchAllReports()
                }
                R.id.lost_btn -> {
                    fetchFilteredReports("Zagubione")
                }
                R.id.found_btn -> {
                    fetchFilteredReports("Znalezione")
                }
                R.id.my_btn -> {
                    fetchMyReports(userId!!)
                }
            }
        }

        binding.goBackBtn.setOnClickListener {
            finish()
        }
    }

    private fun fetchAllReports() {
        reportsCollection.get().addOnSuccessListener { documents ->
            val reportsList = mutableListOf<ReportModel>()
            for (document in documents) {
                val report = document.toObject(ReportModel::class.java)
                reportsList.add(report)
            }
            recyclerView.adapter = AdapterClassForRecyclerView(reportsList.reversed())
        } .addOnFailureListener { exception ->
            UiUtil.showToast(this, "Błąd pobierania danych. Spróbuj ponownie później")
        }
    }

    private fun fetchFilteredReports(reportType: String) {
        // Fetch reports based on the type
        reportsCollection.whereEqualTo("type", reportType)
            .get()
            .addOnSuccessListener { documents ->
                val filteredReports = mutableListOf<ReportModel>()
                for (document in documents) {
                    val report = document.toObject(ReportModel::class.java)
                    filteredReports.add(report)
                }
                recyclerView.adapter = AdapterClassForRecyclerView(filteredReports.reversed())
            }.addOnFailureListener { exception ->
                UiUtil.showToast(this, "Błąd pobierania danych. Spróbuj ponownie później")
            }
    }

    private fun fetchMyReports(userId: String) {
        // Fetch reports based on the user ID
        reportsCollection.whereEqualTo("reporterId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val myReports = mutableListOf<ReportModel>()
                for (document in documents) {
                    val report = document.toObject(ReportModel::class.java)
                    myReports.add(report)
                }
                recyclerView.adapter = AdapterClassForRecyclerView(myReports.reversed())
            }.addOnFailureListener { exception ->
                UiUtil.showToast(this, "Błąd pobierania danych. Spróbuj ponownie później")
            }
    }
}