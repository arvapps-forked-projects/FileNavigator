package com.w2sv.navigator.di

import javax.inject.Qualifier

/**
 * Qualifier for the corresponding StateFlow<Boolean>.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FileNavigatorIsRunning
