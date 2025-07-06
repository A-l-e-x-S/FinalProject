package com.example.finalproject.room

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.finalproject.Model.GroupEntity

@Dao
interface GroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Query("SELECT * FROM groups_table")
    fun getAllGroups(): LiveData<List<GroupEntity>>

    @Query("SELECT * FROM groups_table WHERE id = :groupId LIMIT 1")
    fun getGroupById(groupId: Int): LiveData<GroupEntity>

    @Query("SELECT * FROM groups_table WHERE userId = :userId")
    suspend fun getGroupsByUser(userId: Int): List<GroupEntity>

    @Query("DELETE FROM groups_table WHERE userId = :userId")
    suspend fun deleteGroupsByUser(userId: Int)
}
