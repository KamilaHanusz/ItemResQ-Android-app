package com.example.itemresq.model

import com.google.firebase.Timestamp

data class UserModel(
    var id : String = "",
    var email : String = "",
    var addedReportsCount : Int = 0,
    var profilePictureUri : String = "",
    var registrationTime: Timestamp = Timestamp.now(),
)
