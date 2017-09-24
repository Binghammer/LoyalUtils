package com.chadbingham.loyautils.fire

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseReferenceProvider {
    var database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val reference: DatabaseReference
        get() = database.reference
}
