package com.w2sv.navigator

import com.w2sv.navigator.di.MoveSummaryChannel
import com.w2sv.navigator.moving.MoveSummaryListener
import javax.inject.Inject
import kotlinx.coroutines.flow.consumeAsFlow

internal class MoveResultCollector @Inject constructor(
    private val moveSummaryListener: MoveSummaryListener,
    private val moveSummaryChannel: MoveSummaryChannel
) {
    suspend fun startCollecting() {
        moveSummaryChannel
            .consumeAsFlow()
            .collect(moveSummaryListener::onMoveResult)
    }
}
