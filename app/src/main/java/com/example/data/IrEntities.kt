package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "ir_remotes")
data class IrRemote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "TV", // TV, AC, Custom, audio, etc.
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ir_buttons",
    foreignKeys = [
        ForeignKey(
            entity = IrRemote::class,
            parentColumns = ["id"],
            childColumns = ["remoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["remoteId"])]
)
data class IrButton(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: Long,
    val label: String,
    val protocol: String = "NEC", // NEC, SONY, RC5, PRONTO, RAW
    val hexCode: String = "", // Used for NEC, SONY, RC5 (e.g., "0x20DF10EF")
    val rawPattern: String = "", // Used for RAW input (comma separated)
    val frequency: Int = 38040, // Usually 38000 (38kHz) and similar
    val bitLength: Int = 32, // NEC default is 32, Sony default is 12, etc.
    val iconName: String = "rounded_corner", // play, pause, power, volume_up, etc.
    val colorHex: String = "#2196F3", // color coding for button (Default Material Blue)
    val orderIndex: Int = 0
)
