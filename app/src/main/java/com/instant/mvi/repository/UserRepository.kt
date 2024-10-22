package com.instant.mvi.repository

import com.instant.mvi.model.User

// User repository interface
interface UserRepository {
    suspend fun getUsers(): List<User>
    suspend fun addUser(user: User): List<User>
    suspend fun deleteUser(user: User): List<User>
    suspend fun clearUsers(): List<User>
}