package com.w2sv.navigator.moving.activity

import android.app.Activity
import com.w2sv.navigator.di.MoveSummaryChannel
import com.w2sv.navigator.domain.moving.MoveOperationSummary
import com.w2sv.navigator.domain.moving.MoveResult
import javax.inject.Inject

class MoveActivityFinisher @Inject constructor(private val moveSummaryChannel: MoveSummaryChannel) {

    fun finishOnError(activity: Activity, error: MoveResult) {
        moveSummaryChannel.trySend(MoveOperationSummary(error))
        activity.finishAndRemoveTask()
    }
}
