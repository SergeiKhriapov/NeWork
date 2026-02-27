package ru.netology.nework.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.netology.nework.domain.repository.AuthRepository
import ru.netology.nework.domain.repository.PostRepository
import ru.netology.nework.data.repository.AuthRepositoryImpl
import ru.netology.nework.data.repository.PostRepositoryImpl
import ru.netology.nework.data.repository.UserRepositoryImpl
import ru.netology.nework.domain.repository.UserRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPostRepository(impl: PostRepositoryImpl): PostRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}