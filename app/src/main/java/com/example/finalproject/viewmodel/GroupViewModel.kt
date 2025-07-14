package com.example.finalproject.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.finalproject.repository.GroupRepository
import com.example.finalproject.room.AppDatabase
import com.example.finalproject.Model.GroupEntity
import kotlinx.coroutines.launch
import android.content.Context

class GroupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GroupRepository

    init {
        val groupDao = AppDatabase.getDatabase(application).groupDao()
        repository = GroupRepository(groupDao)
    }

    fun getGroupsForUser(uid: String): LiveData<List<GroupEntity>> {
        return repository.getGroupsForUser(uid)
    }

    fun getGroupById(groupId: String): LiveData<GroupEntity> {
        return repository.getGroupById(groupId)
    }

    fun insertGroup(group: GroupEntity) = viewModelScope.launch {
        repository.insertGroup(group)
    }

    fun deleteGroup(groupId: String) = viewModelScope.launch {
        repository.deleteGroup(groupId)
    }

    fun syncGroups(uid: String, context: Context) {
        repository.syncGroupsFromFirestore(uid, context)
    }
}
