package com.w2sv.navigator.notifications.helper

import com.w2sv.domain.model.filetype.FileAndSourceType
import com.w2sv.domain.repository.NavigatorConfigDataSource
import com.w2sv.navigator.domain.moving.MoveDestination
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class GetQuickMoveDestinations @Inject constructor(private val navigatorConfigDataSource: NavigatorConfigDataSource) {

    suspend operator fun invoke(fileAndSourceType: FileAndSourceType): List<MoveDestination.Directory> =
        navigatorConfigDataSource
            .quickMoveDestinations(
                fileType = fileAndSourceType.fileType,
                sourceType = fileAndSourceType.sourceType
            )
            .map { it.map { localDestinationApi -> MoveDestination.Directory(localDestinationApi) } }
            .first()
}
