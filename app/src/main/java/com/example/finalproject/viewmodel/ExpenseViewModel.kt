package com.example.finalproject.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.finalproject.repository.ExpenseRepository
import com.example.finalproject.room.AppDatabase
import com.example.finalproject.Model.ExpenseEntity
import kotlinx.coroutines.launch

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    init {
        val expenseDao = AppDatabase.getDatabase(application).expenseDao()
        repository = ExpenseRepository(expenseDao)
    }

    fun getExpensesForGroup(groupId: Int) = repository.getExpensesForGroup(groupId)

    fun insertExpense(expense: ExpenseEntity) = viewModelScope.launch {
        repository.insertExpense(expense)
    }
}
