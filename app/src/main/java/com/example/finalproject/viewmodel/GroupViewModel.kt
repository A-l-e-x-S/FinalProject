package com.example.finalproject.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.finalproject.repository.GroupRepository
import com.example.finalproject.room.AppDatabase
import com.example.finalproject.Model.GroupEntity
import kotlinx.coroutines.launch

class GroupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GroupRepository
    val allGroups: LiveData<List<GroupEntity>>

    init {
        val groupDao = AppDatabase.getDatabase(application).groupDao()
        repository = GroupRepository(groupDao)
        allGroups = repository.allGroups
    }

    suspend fun insertGroup(group: GroupEntity): Long {
        return repository.insertGroup(group)
    }

    fun getGroupById(groupId: Int): LiveData<GroupEntity> {
        return repository.getGroupById(groupId)
    }
}
