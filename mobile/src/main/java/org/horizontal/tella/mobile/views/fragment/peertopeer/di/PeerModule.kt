package org.horizontal.tella.mobile.views.fragment.peertopeer.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerToPeerManager
import org.horizontal.tella.mobile.data.peertopeer.TellaPeerToPeerClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PeerModule {

    @Provides
    @Singleton
    fun providePeerClient(): TellaPeerToPeerClient {
        return TellaPeerToPeerClient()
    }

    @Provides
    @Singleton
    fun providePeerToPeerManager(): PeerToPeerManager {
        return PeerToPeerManager()
    }
}