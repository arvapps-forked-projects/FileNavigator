package com.w2sv.navigator.observing

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import com.anggrayudi.storage.media.MediaType
import com.w2sv.common.logging.log
import com.w2sv.common.uri.MediaId
import com.w2sv.common.uri.MediaUri
import com.w2sv.common.uri.mediaUri
import com.w2sv.domain.model.filetype.FileAndSourceType
import com.w2sv.domain.model.navigatorconfig.AutoMoveConfig
import com.w2sv.kotlinutils.coroutines.flow.collectOn
import com.w2sv.kotlinutils.coroutines.launchDelayed
import com.w2sv.navigator.domain.moving.DestinationSelectionManner
import com.w2sv.navigator.domain.moving.MediaStoreFileData
import com.w2sv.navigator.domain.moving.MoveDestination
import com.w2sv.navigator.domain.moving.MoveFile
import com.w2sv.navigator.domain.moving.MoveOperation
import com.w2sv.navigator.domain.notifications.NotificationEvent
import com.w2sv.navigator.moving.MoveBroadcastReceiver
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import slimber.log.i

// TODO naming
private data class MoveFileWithProcedureJob(val moveFile: MoveFile, val procedureJob: Job)

// TODO increase
private const val CANCEL_PERIOD_MILLIS = 300L

private enum class FileChangeOperation(private val flag: Int?) {

    // Note: don't change the enum entry order, as the working of determine(Int): FileChangeOperation depends on it!!!
    Update(ContentResolver.NOTIFY_UPDATE),
    Insert(ContentResolver.NOTIFY_INSERT),
    Delete(ContentResolver.NOTIFY_DELETE),
    SyncToNetwork(ContentResolver.NOTIFY_SYNC_TO_NETWORK),
    Unclassified(null);

    companion object {
        // TODO what if flags are combined
        operator fun invoke(notifyFlags: Int): FileChangeOperation =
            entries.first { it.flag == null || it.flag and notifyFlags != 0 }
    }
}

internal abstract class FileObserver(val mediaType: MediaType, handler: Handler, environment: FileObserverEnvironment) :
    ContentObserver(handler),
    FileObserverEnvironment by environment {

    private val mediaIdBlacklist = RecentSet<MediaId>(3)
    private var moveFileWithProcedureJob: MoveFileWithProcedureJob? = null

    open val logIdentifier: String
        get() = this::class.java.simpleName

    init {
        collectBlacklistedMediaIds()
    }

    final override fun deliverSelfNotifications(): Boolean =
        false

    private fun collectBlacklistedMediaIds() {
        blacklistedMediaUris
            .filter { it.mediaType == mediaType }
            .map { it.mediaId }
            .collectOn(scope) { mediaId ->
                i { "Collected $mediaId" }
                mediaIdBlacklist.add(mediaId)
                if (moveFileWithProcedureJob?.moveFile?.mediaUri?.id() == mediaId) {
                    cancelAndResetMoveFileProcedureJob()
                }
            }
    }

    private fun cancelAndResetMoveFileProcedureJob() {
        moveFileWithProcedureJob?.procedureJob?.cancel()
        moveFileWithProcedureJob = null
    }

    override fun onChange(selfChange: Boolean, uri: Uri?, flags: Int) {
        // TODO log flags, not just parsed operation
        val operation = FileChangeOperation(flags)
        emitOnChangeLog(uri, operation)

        when (operation) {
            FileChangeOperation.Update -> onChangeCore(uri)
            FileChangeOperation.Delete -> cancelAndResetMoveFileProcedureJob()
            else -> Unit
        }
    }

    // TODO needed?
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        emitOnChangeLog(uri, FileChangeOperation.Unclassified)
        onChangeCore(uri)
    }

    private fun emitOnChangeLog(uri: Uri?, fileChangeOperation: FileChangeOperation) {
        i { "$logIdentifier ${fileChangeOperation.name} $uri | Blacklist: $mediaIdBlacklist" }
    }

    private fun onChangeCore(uri: Uri?) {
        val mediaUri = uri?.mediaUri ?: return discard("mediaUri null")
        val mediaId = mediaUri.id() ?: return discard("mediaId null")
        if (mediaIdBlacklist.contains(mediaId)) return discard("mediaId blacklisted")

        val mediaStoreDataRetrievalResult = mediaStoreDataProvider(
            mediaUri = mediaUri,
            contentResolver = context.contentResolver
        )
            .asSuccessOrNull ?: return

        cancelExistingJobIfUpdate(isUpdateOfAlreadySeenFile = mediaStoreDataRetrievalResult.isUpdateOfSeenFile, mediaUri = mediaUri)

        matchingFileAndSourceTypeOrNull(mediaStoreDataRetrievalResult.data)?.let { fileAndSourceType ->
            val moveFile = MoveFile(
                mediaUri = mediaUri,
                mediaStoreData = mediaStoreDataRetrievalResult.data,
                fileAndSourceType = fileAndSourceType
            )
                .log { "Created $it" }
            scope.launch { onNewMoveFile(moveFile) }
        }
    }

    private fun discard(reason: String) {
        i { "$logIdentifier: discarding because $reason" }
    }

    private fun cancelExistingJobIfUpdate(isUpdateOfAlreadySeenFile: Boolean, mediaUri: MediaUri) {
        moveFileWithProcedureJob?.run {
            val (moveFile, procedureJob) = this
            if (isUpdateOfAlreadySeenFile && mediaUri == moveFile.mediaUri) {
                procedureJob.cancel()
            }
        }
    }

    private suspend fun onNewMoveFile(moveFile: MoveFile) {
        // TODO maybe cache via StateFlows
        val autoMoveDestination = navigatorConfigFlow
            .first()
            .autoMoveConfig(moveFile.fileType, moveFile.sourceType)
            .enabledDestinationOrNull

        moveFileWithProcedureJob = MoveFileWithProcedureJob(
            moveFile = moveFile,
            procedureJob = scope.launchDelayed(CANCEL_PERIOD_MILLIS) {
                autoMoveDestination
                    // TODO Perform the moving directly instead of starting a receiver with parceling
                    ?.let { sendAutoMoveBroadcast(moveFile, it) }
                    ?: notificationEventHandler(NotificationEvent.PostMoveFile(moveFile))
            }
        )
    }

    private fun sendAutoMoveBroadcast(moveFile: MoveFile, destination: MoveDestination.Directory) {
        MoveBroadcastReceiver.sendBroadcast(
            MoveOperation.AutoMove(moveFile, destination, DestinationSelectionManner.Auto),
            context
        )
    }

    /**
     * This method determines whether the observer will fire for the received [mediaStoreFileData] or not.
     */
    protected abstract fun matchingFileAndSourceTypeOrNull(mediaStoreFileData: MediaStoreFileData): FileAndSourceType?
}

private val AutoMoveConfig.enabledDestinationOrNull: MoveDestination.Directory?
    get() = if (enabled && destination != null) MoveDestination.Directory(destination!!) else null
