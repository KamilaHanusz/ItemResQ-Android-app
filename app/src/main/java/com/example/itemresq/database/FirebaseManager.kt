package com.example.itemresq.database

import com.example.itemresq.model.ReportModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseManager {

    private val reportsCollection = FirebaseFirestore.getInstance().collection("reports")
    private val usersCollection = FirebaseFirestore.getInstance().collection("users")
    private val auth = FirebaseAuth.getInstance()

    fun fetchReportsForMainActivity( onSuccess: (List<ReportModel>, List<ReportModel>, List<ReportModel>) -> Unit, onFailure: (String) -> Unit) {
        reportsCollection.get()
            .addOnSuccessListener { documents ->

                val reportsList = mutableListOf<ReportModel>()
                val reportsLostList = mutableListOf<ReportModel>()
                val reportsFoundList = mutableListOf<ReportModel>()

                for (document in documents) {
                    val report = document.toObject(ReportModel::class.java)
                    reportsList.add(report)
                    if (report.type == "Zagubione") {
                        reportsLostList.add(report)
                    } else {
                        reportsFoundList.add(report)
                    }
                }

                onSuccess(reportsList, reportsLostList, reportsFoundList)

            }
            .addOnFailureListener { exception ->
                onFailure("Error in fetching data")
            }
    }

    fun fetchUserInfoForMainActivity(userId: String, onSuccess: (String, String) -> Unit, onFailure: () -> Unit) {
        usersCollection.document(userId).get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val userName = documentSnapshot.getString("email")?.substringBefore("@")
                val profilePictureUri = documentSnapshot.getString("profilePictureUri") ?: ""

                onSuccess(userName ?: "", profilePictureUri)
            } else {
                onFailure()
            }
        }
    }

    fun deleteAccountAndReports(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val user = auth.currentUser
        val userId = user?.uid

        if (userId != null) {
            deleteReportsForUser(userId)

            usersCollection.document(userId).get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    usersCollection.document(userId).delete()
                }
            }

            auth.signOut()

            user.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess("Konto zostało usunięte pomyślnie.")
                    } else {
                        onFailure("Nie udało się usunąć konta. Spróbuj ponownie później.")
                    }
                }
        }
    }

    fun deleteReportsForUser(userId: String) {
        reportsCollection
            .whereEqualTo("reporterId", userId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val reportId = document.id
                    reportsCollection.document(reportId).delete()
                }
            }
    }
}
