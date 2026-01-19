package com.w2sv.navigator.observing

import android.content.Context
import com.w2sv.common.di.ApplicationIoScope
import com.w2sv.domain.model.navigatorconfig.NavigatorConfigFlow
import com.w2sv.navigator.domain.notifications.NotificationEventHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow

/**
 * Bundles shared infrastructure needed by all [FileObserver] instances.
 *
 * This avoids large constructor parameter lists and replaces AssistedInject by providing
 * a single, DI-managed environment object that is delegated by each observer.
 */
internal interface FileObserverEnvironment {
    val context: Context
    val scope: CoroutineScope
    val mediaStoreDataProducer: MediaStoreDataProducer
    val blacklistedMediaUris: SharedFlow<MediaIdWithMediaType>
    val notificationEventHandler: NotificationEventHandler
    val navigatorConfigFlow: NavigatorConfigFlow
}

internal class FileObserverEnvironmentImpl @Inject constructor(
    @ApplicationContext override val context: Context,
    @ApplicationIoScope override val scope: CoroutineScope,
    override val mediaStoreDataProducer: MediaStoreDataProducer,
    override val blacklistedMediaUris: SharedFlow<MediaIdWithMediaType>,
    override val notificationEventHandler: NotificationEventHandler,
    override val navigatorConfigFlow: NavigatorConfigFlow
) : FileObserverEnvironment
