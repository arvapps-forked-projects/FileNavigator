package com.w2sv.navigator.di

import com.w2sv.navigator.FileNavigator
import com.w2sv.navigator.domain.moving.MediaIdWithMediaType
import com.w2sv.navigator.domain.moving.MoveOperationSummary
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

internal typealias MoveOperationSummaryChannel = Channel<MoveOperationSummary>

@InstallIn(SingletonComponent::class)
@Module
internal object FileNavigatorModule {

    @FileNavigatorIsRunning
    @Provides
    fun fileNavigatorIsRunning(state: FileNavigator.State): StateFlow<Boolean> =
        state.isRunning

    @Singleton
    @Provides
    fun moveOperationSummaryChannel(): MoveOperationSummaryChannel =
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
}
