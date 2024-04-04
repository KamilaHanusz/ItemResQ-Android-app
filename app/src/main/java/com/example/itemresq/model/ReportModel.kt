package com.example.itemresq.model

import com.google.firebase.Timestamp

data class ReportModel(
    var reportId: String = "",
    var type: String = "",
    var title: String = "",
    var description: String = "",
    var category: String = "",
    var occurrenceDate: Timestamp = Timestamp.now(),
    var reward: String = "",
    var phoneNumber: String = "",
    var email: String = "",
    var imageUrls: List<String> = emptyList(),
    var latitude : Double = 0.0,
    var longitude : Double = 0.0,
    var reporterId: String = "",
    var createdTime: Timestamp = Timestamp.now(),
)
