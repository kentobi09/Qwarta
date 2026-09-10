package com.ledger.iou.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phoneNumber: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
