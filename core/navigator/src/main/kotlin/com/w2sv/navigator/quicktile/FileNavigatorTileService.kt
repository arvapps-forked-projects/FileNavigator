package com.w2sv.navigator.quicktile

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.service.quicksettings.Tile
import androidx.annotation.IntDef
import com.w2sv.core.navigator.R
import com.w2sv.kotlinutils.coroutines.launchDelayed
import com.w2sv.navigator.FileNavigator
import com.w2sv.navigator.shared.mainActivityIntent
import com.w2sv.navigator.shared.mainActivityPendingIntent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class FileNavigatorTileService : LoggingTileService() {

    @Inject
    internal lateinit var fileNavigatorIsRunning: FileNavigator.IsRunning

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listeningJob: Job? = null

    /**
     * Called every time the quick settings pan is expanded. onStopListening behaves vice-versa.
     */
    override fun onStartListening() {
        super.onStartListening()

        // Launch navigator and tile state synchronization job
        listeningJob?.cancel()
        listeningJob = scope.launch {
            fileNavigatorIsRunning.collect { isRunning ->
                qsTile.updateState(if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE)
            }
        }
    }

    override fun onClick() {
        super.onClick()

        when (qsTile.state) {
            Tile.STATE_ACTIVE -> FileNavigator.stop(this)
            Tile.STATE_INACTIVE -> activateNavigator()
        }
    }

    /**
     * Either
     * - starts the navigator immediately via launch dialog if permissions are granted and the device is unlocked
     * - prompts the user to unlock if permissions are granted but the device is locked (launch dialog is not shown on lock screen)
     * - launches MainActivity if required permissions are missing, which will result in the permissions screen being shown
     */
    private fun activateNavigator() {
        val permissionsGranted = FileNavigator.necessaryPermissionsGranted(this)
        when {
            permissionsGranted && !isLocked -> showDialogAndLaunchNavigator()
            permissionsGranted -> unlockAndRun { showDialogAndLaunchNavigator() }
            else -> startMainActivityAndCollapse()
        }
    }

    /**
     * Starting from Sdk Version 31 (Android 12), foreground services can't be started from the background.
     * Therefore show a Dialog, which promotes the app to a foreground process state, and start the FGS while it's showing.
     *
     * https://developer.android.com/develop/background-work/services/foreground-services#bg-access-restrictions
     * https://stackoverflow.com/questions/77331327/start-a-foreground-service-from-a-quick-tile-on-android-targetsdkversion-34-and
     */
    private fun showDialogAndLaunchNavigator() {
        showDialog(
            Dialog(this)
                .apply {
                    setTheme(R.style.LaunchDialogTheme)
                    setContentView(R.layout.tile_dialog)
                    setOnShowListener {
                        // Dismiss dialog after a small delay to prevent flashing
                        scope.launchDelayed(250) {
                            FileNavigator.start(this@FileNavigatorTileService)
                            dismiss()
                        }
                    }
                }
        )
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun startMainActivityAndCollapse() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(mainActivityPendingIntent(this))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(mainActivityIntent(this))
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        listeningJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

@IntDef(Tile.STATE_ACTIVE, Tile.STATE_INACTIVE, Tile.STATE_UNAVAILABLE)
private annotation class TileState

private fun Tile.updateState(@TileState state: Int) {
    this.state = state
    updateTile()
}
