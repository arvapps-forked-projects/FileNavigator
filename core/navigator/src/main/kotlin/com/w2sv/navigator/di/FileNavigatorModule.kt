package com.w2sv.navigator.di

import android.content.ContentResolver
import android.content.Context
import com.w2sv.navigator.FileNavigator
import com.w2sv.navigator.domain.moving.MoveOperationSummary
import com.w2sv.navigator.observing.MediaIdWithMediaType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

internal typealias MoveSummaryChannel = Channel<MoveOperationSummary>

@InstallIn(SingletonComponent::class)
@Module
internal object FileNavigatorModule {

    @FileNavigatorIsRunning
    @Provides
    fun fileNavigatorIsRunning(status: FileNavigator.Status): StateFlow<Boolean> =
        status.isRunning

    @Singleton
    @Provides
    fun moveOperationSummaryChannel(): MoveSummaryChannel =
        Channel(Channel.BUFFERED)

    @Singleton
    @Provides
    fun mutableBlacklistedMediaUriSharedFlow(): MutableSharedFlow<MediaIdWithMediaType> =
        MutableSharedFlow()

    @Singleton
    @Provides
    fun blacklistedMediaUriSharedFlow(
        mutableBlacklistedMediaUriSharedFlow: MutableSharedFlow<MediaIdWithMediaType>
    ): SharedFlow<MediaIdWithMediaType> =
        mutableBlacklistedMediaUriSharedFlow.asSharedFlow()

    @Provides
    fun contentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver
}
