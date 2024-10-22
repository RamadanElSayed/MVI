package com.instant.mvi.di

import com.instant.mvi.repository.UserRepository
import com.instant.mvi.repository.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository



//    @Singleton
//    @Provides
//    fun bindUserRepository(): UserRepository
//    {
//        return UserRepositoryImpl()
//    }


}
