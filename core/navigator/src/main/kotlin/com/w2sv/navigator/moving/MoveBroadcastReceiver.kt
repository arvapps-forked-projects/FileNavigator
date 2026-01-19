package com.w2sv.navigator.moving

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.w2sv.androidutils.content.intent
import com.w2sv.common.di.ApplicationIoScope
import com.w2sv.navigator.di.MoveSummaryChannel
import com.w2sv.navigator.domain.moving.MoveOperation
import com.w2sv.navigator.domain.moving.MoveOperationSummary
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class MoveBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var moveSummaryChannel: MoveSummaryChannel

    @Inject
    @ApplicationIoScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val operation = MoveOperation<MoveOperation>(intent)

        scope.launch {
            operation.file.moveTo(destination = operation.destination, context = context) { result ->
                moveSummaryChannel.trySend(MoveOperationSummary(result, operation))
            }
        }
    }

    companion object {
        fun sendBroadcast(operation: MoveOperation, context: Context) {
            context.sendBroadcast(
                intent(
                    moveBundle = operation,
                    context = context
                )
            )
        }

        fun intent(moveBundle: MoveOperation, context: Context): Intent =
            intent<MoveBroadcastReceiver>(context)
                .putExtra(MoveOperation.EXTRA, moveBundle)
    }
}
