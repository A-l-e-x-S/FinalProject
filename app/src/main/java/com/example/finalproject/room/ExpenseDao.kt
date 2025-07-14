package com.example.finalproject.room

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.finalproject.Model.ExpenseEntity

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getExpensesForGroup(groupId: String): LiveData<List<ExpenseEntity>>

    @Query("DELETE FROM expenses WHERE groupId = :groupId")
    suspend fun deleteExpensesByGroup(groupId: String)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE payerUid = :uid")
    suspend fun deleteExpensesByUser(uid: String)
}
