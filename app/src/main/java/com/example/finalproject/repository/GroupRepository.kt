package com.example.finalproject.repository

import androidx.lifecycle.LiveData
import com.example.finalproject.Model.GroupEntity
import com.example.finalproject.room.AppDatabase
import com.example.finalproject.room.GroupDao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Context

class GroupRepository(private val groupDao: GroupDao) {

    fun syncGroupsFromFirestore(uid: String, context: Context) {
        val db = FirebaseFirestore.getInstance()

        db.collection("groups")
            .whereArrayContains("members", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val groupList = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val name = doc.getString("groupName") ?: return@mapNotNull null
                    val type = doc.getString("groupType") ?: ""
                    val photo = doc.getString("groupPhotoUrl")
                    val createdBy = doc.getString("createdByUid") ?: return@mapNotNull null
                    val members = doc.get("members") as? List<String> ?: listOf(uid)

                    GroupEntity(
                        firestoreId = id,
                        groupName = name,
                        groupType = type,
                        groupPhotoUrl = photo,
                        createdByUid = createdBy,
                        membersJson = Gson().toJson(members)
                    )
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val groupDao = AppDatabase.getDatabase(context).groupDao()
                    groupList.forEach { groupDao.insertGroup(it) }
                }
            }
    }

    fun getGroupsForUser(uid: String): LiveData<List<GroupEntity>> {
        return groupDao.getGroupsForUser(uid)
    }

    fun getGroupById(groupId: String): LiveData<GroupEntity> {
        return groupDao.getGroupById(groupId)
    }

    suspend fun insertGroup(group: GroupEntity) {
        groupDao.insertGroup(group)
    }

    suspend fun deleteGroup(groupId: String) {
        groupDao.deleteGroup(groupId)
    }
}
