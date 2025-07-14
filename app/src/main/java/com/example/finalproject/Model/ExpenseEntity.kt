package com.example.finalproject.Model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val groupId: String,
    val title: String,
    val description: String? = null,
    val amount: Double,
    val payerUid: String,
    val timestamp: Long,
    val splitBetweenJson: String,
    val photoUrl: String? = null
)