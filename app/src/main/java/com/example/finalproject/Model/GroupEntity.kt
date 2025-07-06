package com.example.finalproject.Model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups_table")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupName: String,
    val groupType: String,
    val groupPhotoUrl: String? = null,
    val userId: Int
)
