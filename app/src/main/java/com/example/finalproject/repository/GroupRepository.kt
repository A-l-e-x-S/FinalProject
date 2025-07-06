package com.example.finalproject.repository

import androidx.lifecycle.LiveData
import com.example.finalproject.Model.GroupEntity
import com.example.finalproject.room.GroupDao

class GroupRepository(private val groupDao: GroupDao) {

    val allGroups: LiveData<List<GroupEntity>> = groupDao.getAllGroups()

    suspend fun insertGroup(group: GroupEntity): Long {
        return groupDao.insertGroup(group)
    }

    fun getGroupById(groupId: Int): LiveData<GroupEntity> {
        return groupDao.getGroupById(groupId)
    }
}
