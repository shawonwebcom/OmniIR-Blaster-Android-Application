package com.example.data

import kotlinx.coroutines.flow.Flow

class IrRepository(private val irDao: IrDao) {
    val allRemotes: Flow<List<IrRemote>> = irDao.getAllRemotes()

    fun getRemoteById(id: Long): Flow<IrRemote?> = irDao.getRemoteById(id)

    fun getButtonsForRemote(remoteId: Long): Flow<List<IrButton>> = irDao.getButtonsForRemote(remoteId)

    suspend fun insertRemote(remote: IrRemote): Long = irDao.insertRemote(remote)

    suspend fun updateRemote(remote: IrRemote) = irDao.updateRemote(remote)

    suspend fun deleteRemote(remote: IrRemote) = irDao.deleteRemote(remote)

    suspend fun insertButton(button: IrButton): Long = irDao.insertButton(button)

    suspend fun updateButton(button: IrButton) = irDao.updateButton(button)

    suspend fun deleteButton(button: IrButton) = irDao.deleteButton(button)

    suspend fun deleteRemoteWithButtons(remoteId: Long) = irDao.deleteRemoteWithButtons(remoteId)
}
