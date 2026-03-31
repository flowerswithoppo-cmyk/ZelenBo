package com.zelenbo.app.di

import com.zelenbo.app.data.repository.InMemoryLogsRepository
import com.zelenbo.app.data.repository.local.SettingsRepositoryImpl
import com.zelenbo.app.domain.repository.LogsRepository
import com.zelenbo.app.domain.repository.SettingsRepository
import com.zelenbo.app.domain.repository.VpnGateway
import com.zelenbo.app.service.VpnGatewayImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindLogsRepository(impl: InMemoryLogsRepository): LogsRepository

    @Binds
    abstract fun bindVpnGateway(impl: VpnGatewayImpl): VpnGateway
}

