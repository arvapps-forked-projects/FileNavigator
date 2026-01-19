package com.w2sv.navigator.observing

import android.os.Handler
import com.anggrayudi.storage.media.MediaType
import com.w2sv.domain.model.filetype.FileAndSourceType
import com.w2sv.domain.model.filetype.FileType
import com.w2sv.domain.model.filetype.SourceType
import com.w2sv.navigator.domain.moving.MediaStoreFileData

internal class NonMediaFileObserver(private val fileTypes: Collection<FileType>, handler: Handler, environment: FileObserverEnvironment) :
    FileObserver(
        mediaType = MediaType.DOWNLOADS,
        handler = handler,
        environment = environment
    ) {
    override fun matchingFileAndSourceTypeOrNull(mediaStoreFileData: MediaStoreFileData): FileAndSourceType? =
        fileTypes
            .firstOrNull { it.fileExtensions.contains(mediaStoreFileData.extension) }
            ?.let { fileType -> FileAndSourceType(fileType, SourceType.Download) }
}
