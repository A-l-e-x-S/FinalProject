package com.example.finalproject.repository

import androidx.lifecycle.LiveData
import com.example.finalproject.room.ExpenseDao
import com.example.finalproject.Model.ExpenseEntity

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    fun getExpensesForGroup(groupId: String): LiveData<List<ExpenseEntity>> {
        return expenseDao.getExpensesForGroup(groupId)
    }

    suspend fun insertExpense(expense: ExpenseEntity) {
        expenseDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
    }
}