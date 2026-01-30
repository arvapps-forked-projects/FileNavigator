package com.w2sv.navigator.observing

import android.os.Handler
import com.w2sv.domain.model.filetype.FileAndSourceType
import com.w2sv.domain.model.filetype.FileType
import com.w2sv.domain.model.filetype.SourceType
import com.w2sv.navigator.domain.moving.MediaStoreFileData

internal class MediaFileObserver(
    private val fileType: FileType,
    private val enabledSourceTypes: Collection<SourceType>,
    handler: Handler,
    environment: FileObserverEnvironment
) : FileObserver(
    mediaType = fileType.mediaType,
    handler = handler,
    environment = environment
) {
    override val logIdentifier: String
        get() = "${super.logIdentifier}.${fileType.logIdentifier}"

    override fun matchingFileAndSourceTypeOrNull(mediaStoreFileData: MediaStoreFileData): FileAndSourceType? {
        val sourceType = mediaStoreFileData.sourceType()

        return if (enabledSourceTypes.contains(sourceType)) {
            FileAndSourceType(fileType, sourceType)
        } else {
            null
        }
    }
}
