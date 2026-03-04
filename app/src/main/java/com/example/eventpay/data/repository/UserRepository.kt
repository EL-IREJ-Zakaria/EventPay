package com.example.eventpay.data.repository

import com.example.eventpay.data.local.dao.UserDao
import com.example.eventpay.data.model.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    val allUsers: Flow<List<User>> = userDao.getAllUsers()

    suspend fun getUserById(userId: String): User? {
        return userDao.getUserById(userId)
    }

    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }

    suspend fun login(email: String, password: String): User? {
        return userDao.login(email, password)
    }

    suspend fun register(user: User) {
        userDao.insertUser(user)
    }
    
    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    suspend fun updateWalletBalance(userId: String, balance: Double) {
        userDao.updateWalletBalance(userId, balance)
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }
}
