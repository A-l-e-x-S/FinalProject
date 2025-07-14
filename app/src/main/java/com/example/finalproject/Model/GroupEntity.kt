package com.example.finalproject.Model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups_table")
data class GroupEntity(
    @PrimaryKey val firestoreId: String,
    val groupName: String,
    val groupType: String,
    val groupPhotoUrl: String? = null,
    val createdByUid: String,
    val membersJson: String
)