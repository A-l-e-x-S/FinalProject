package com.example.finalproject.room

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.finalproject.Model.GroupEntity

@Dao
interface GroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Query("SELECT * FROM groups_table WHERE membersJson LIKE '%' || :uid || '%'")
    fun getGroupsForUser(uid: String): LiveData<List<GroupEntity>>

    @Query("SELECT * FROM groups_table WHERE firestoreId = :groupId LIMIT 1")
    fun getGroupById(groupId: String): LiveData<GroupEntity>

    @Query("DELETE FROM groups_table WHERE firestoreId = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("DELETE FROM groups_table WHERE createdByUid = :uid OR membersJson LIKE '%' || :uid || '%'")
    suspend fun deleteGroupsByUser(uid: String)
}
