package org.horizontal.tella.mobile.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.horizontal.tella.mobile.domain.repository.feedback.FeedBackRepository
import org.horizontal.tella.mobile.util.StatusProvider

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerDependenciesEntryPoint {
    fun feedbackRepository(): FeedBackRepository
    fun statusProvider(): StatusProvider
}

