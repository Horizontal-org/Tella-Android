package org.horizontal.tella.mobile.views.fragment.peertopeer.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.horizontal.tella.mobile.data.peertopeer.TellaPeerToPeerClient
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerToPeerManager
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSession
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState
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

    @Provides
    @Singleton
    fun provideSession(): P2PSession {
        return P2PSession()
    }

    @Provides
    @Singleton
    fun provideP2PServerState(p2PSession: P2PSession): P2PSharedState {
        val p2PSharedState = P2PSharedState()
        p2PSharedState.session = p2PSession
        return p2PSharedState
    }
}