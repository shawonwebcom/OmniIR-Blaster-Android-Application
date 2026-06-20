package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IrDao {
    @Query("SELECT * FROM ir_remotes ORDER BY timestamp DESC")
    fun getAllRemotes(): Flow<List<IrRemote>>

    @Query("SELECT * FROM ir_remotes WHERE id = :remoteId LIMIT 1")
    fun getRemoteById(remoteId: Long): Flow<IrRemote?>

    @Query("SELECT * FROM ir_buttons WHERE remoteId = :remoteId ORDER BY orderIndex ASC, id ASC")
    fun getButtonsForRemote(remoteId: Long): Flow<List<IrButton>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemote(remote: IrRemote): Long

    @Update
    suspend fun updateRemote(remote: IrRemote)

    @Delete
    suspend fun deleteRemote(remote: IrRemote)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertButton(button: IrButton): Long

    @Update
    suspend fun updateButton(button: IrButton)

    @Delete
    suspend fun deleteButton(button: IrButton)

    @Transaction
    @Query("DELETE FROM ir_remotes WHERE id = :remoteId")
    suspend fun deleteRemoteWithButtons(remoteId: Long)
}
