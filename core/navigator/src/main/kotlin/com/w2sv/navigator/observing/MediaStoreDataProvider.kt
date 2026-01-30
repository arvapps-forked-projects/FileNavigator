package com.w2sv.navigator.observing

import android.content.ContentResolver
import com.w2sv.common.uri.MediaUri
import com.w2sv.navigator.domain.moving.MediaStoreFileData
import com.w2sv.navigator.shared.discardedLog
import javax.inject.Inject

private const val SEEN_FILES_BUFFER_SIZE = 5

internal class MediaStoreDataProvider @Inject constructor() {

    private val seenParametersBuffer = RecentSet<SeenParameters>(SEEN_FILES_BUFFER_SIZE)

    operator fun invoke(mediaUri: MediaUri, contentResolver: ContentResolver): Result {
        // Fetch MediaStoreFileData; exit if impossible
        val columnData = MediaStoreFileData.queryFor(mediaUri, contentResolver)
            ?: return Result.RetrievalUnsuccessful

        // Exit if file is pending or trashed
        if (columnData.isPending) {
            discardedLog { "pending" }
            return Result.FileIsPending
        }
        if (columnData.isTrashed) {
            discardedLog { "trashed" }
            return Result.FileIsTrashed
        }

        val seenParameters = SeenParameters(uri = mediaUri, fileSize = columnData.size)
        if (seenParametersBuffer.contains(seenParameters)) {
            discardedLog { "already seen" }
            return Result.AlreadySeen
        }

        val isUpdateOfSeenFile = seenParametersBuffer.replaceIf(
            predicate = { it.uri == seenParameters.uri },
            element = seenParameters
        )
        return Result.Success(data = columnData, isUpdateOfSeenFile = isUpdateOfSeenFile)
    }

    sealed interface Result {
        data class Success(val data: MediaStoreFileData, val isUpdateOfSeenFile: Boolean) : Result

        data object RetrievalUnsuccessful : Result
        data object FileIsPending : Result
        data object FileIsTrashed : Result
        data object AlreadySeen : Result

        val asSuccessOrNull: Success?
            get() = this as? Success
    }

    private data class SeenParameters(val uri: MediaUri, val fileSize: Long)
}
