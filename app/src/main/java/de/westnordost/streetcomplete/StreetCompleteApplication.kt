package de.westnordost.streetcomplete

import android.app.ActivityManager
import android.app.ActivityManager.MemoryInfo
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import de.westnordost.streetcomplete.data.CacheTrimmer
import de.westnordost.streetcomplete.data.CleanerWorker
import de.westnordost.streetcomplete.data.Preloader
import de.westnordost.streetcomplete.data.dbModule
import de.westnordost.streetcomplete.data.download.downloadModule
import de.westnordost.streetcomplete.data.download.tiles.DownloadedTilesController
import de.westnordost.streetcomplete.data.edithistory.EditHistoryController
import de.westnordost.streetcomplete.data.edithistory.editHistoryModule
import de.westnordost.streetcomplete.data.maptiles.maptilesModule
import de.westnordost.streetcomplete.data.messages.messagesModule
import de.westnordost.streetcomplete.data.meta.metadataModule
import de.westnordost.streetcomplete.data.osm.created_elements.createdElementsModule
import de.westnordost.streetcomplete.data.osm.edits.elementEditsModule
import de.westnordost.streetcomplete.data.osm.geometry.elementGeometryModule
import de.westnordost.streetcomplete.data.osm.mapdata.mapDataModule
import de.westnordost.streetcomplete.data.osm.osmquests.osmQuestModule
import de.westnordost.streetcomplete.data.osmApiModule
import de.westnordost.streetcomplete.data.osmnotes.edits.noteEditsModule
import de.westnordost.streetcomplete.data.osmnotes.notequests.osmNoteQuestModule
import de.westnordost.streetcomplete.data.osmnotes.notesModule
import de.westnordost.streetcomplete.data.externalsource.externalSourceModule
import de.westnordost.streetcomplete.data.overlays.overlayModule
import de.westnordost.streetcomplete.data.quest.questModule
import de.westnordost.streetcomplete.data.upload.uploadModule
import de.westnordost.streetcomplete.data.urlconfig.urlConfigModule
import de.westnordost.streetcomplete.data.user.UserLoginStatusController
import de.westnordost.streetcomplete.data.user.achievements.achievementsModule
import de.westnordost.streetcomplete.data.user.statistics.statisticsModule
import de.westnordost.streetcomplete.data.user.userModule
import de.westnordost.streetcomplete.data.visiblequests.questPresetsModule
import de.westnordost.streetcomplete.overlays.custom.getCustomOverlayIndices
import de.westnordost.streetcomplete.overlays.custom.getIndexedCustomOverlayPref
import de.westnordost.streetcomplete.overlays.overlaysModule
import de.westnordost.streetcomplete.quests.oneway_suspects.data.trafficFlowSegmentsModule
import de.westnordost.streetcomplete.quests.osmose.PREF_OSMOSE_ITEMS
import de.westnordost.streetcomplete.quests.questsModule
import de.westnordost.streetcomplete.screens.main.mainModule
import de.westnordost.streetcomplete.screens.main.map.mapModule
import de.westnordost.streetcomplete.screens.measure.arModule
import de.westnordost.streetcomplete.screens.settings.ResurveyIntervalsUpdater
import de.westnordost.streetcomplete.screens.settings.oldQuestNames
import de.westnordost.streetcomplete.screens.settings.renameUpdateQuests
import de.westnordost.streetcomplete.screens.settings.settingsModule
import de.westnordost.streetcomplete.util.CrashReportExceptionHandler
import de.westnordost.streetcomplete.util.Log
import de.westnordost.streetcomplete.util.getDefaultTheme
import de.westnordost.streetcomplete.util.getSelectedLocale
import de.westnordost.streetcomplete.util.getSystemLocales
import de.westnordost.streetcomplete.util.ktx.addedToFront
import de.westnordost.streetcomplete.util.ktx.nowAsEpochMilliseconds
import de.westnordost.streetcomplete.util.setDefaultLocales
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

class StreetCompleteApplication : Application() {

    private val preloader: Preloader by inject()
    private val crashReportExceptionHandler: CrashReportExceptionHandler by inject()
    private val resurveyIntervalsUpdater: ResurveyIntervalsUpdater by inject()
    private val downloadedTilesController: DownloadedTilesController by inject()
    private val prefs: SharedPreferences by inject()
    private val editHistoryController: EditHistoryController by inject()
    private val userLoginStatusController: UserLoginStatusController by inject()
    private val cacheTrimmer: CacheTrimmer by inject()

    private val applicationScope = CoroutineScope(SupervisorJob() + CoroutineName("Application"))

    override fun onCreate() {
        super.onCreate()

        deleteDatabase(ApplicationConstants.OLD_DATABASE_NAME)

        startKoin {
            androidContext(this@StreetCompleteApplication)
            workManagerFactory()
            modules(
                achievementsModule,
                appModule,
                createdElementsModule,
                dbModule,
                downloadModule,
                editHistoryModule,
                elementEditsModule,
                elementGeometryModule,
                mapDataModule,
                mapModule,
                mainModule,
                maptilesModule,
                metadataModule,
                noteEditsModule,
                notesModule,
                messagesModule,
                osmApiModule,
                osmNoteQuestModule,
                osmQuestModule,
                questModule,
                questPresetsModule,
                questsModule,
                settingsModule,
                statisticsModule,
                trafficFlowSegmentsModule,
                uploadModule,
                userModule,
                arModule,
                overlaysModule,
                overlayModule,
                urlConfigModule,
                externalSourceModule,
            )
        }

        applicationScope.launch {
            editHistoryController.deleteSyncedOlderThan(nowAsEpochMilliseconds() - ApplicationConstants.MAX_UNDO_HISTORY_AGE)
            preloader.preload()
        }

        /* Force log out users who use the old OAuth consumer key+secret because it does not exist
           anymore. Trying to use that does not result in a "not authorized" API response, but some
           response the app cannot handle */
        if (!prefs.getBoolean(Prefs.OSM_LOGGED_IN_AFTER_OAUTH_FUCKUP, false)) {
            if (userLoginStatusController.isLoggedIn) {
                userLoginStatusController.logOut()
            }
        }

        setDefaultLocales()

        preferences = prefs

        crashReportExceptionHandler.install()

        enqueuePeriodicCleanupWork()

        setDefaultTheme()

        resurveyIntervalsUpdater.update()

        val lastVersion = prefs.getString(Prefs.LAST_VERSION_DATA, null)
        if (BuildConfig.VERSION_NAME != lastVersion) {
            prefs.edit { putString(Prefs.LAST_VERSION_DATA, BuildConfig.VERSION_NAME) }
            if (lastVersion != null) {
                onNewVersion()
                if (lastVersion.endsWith("_ee"))
                    // adjust osmose ignores, this is necessary because they may now contain comma
                    prefs.all.filterKeys { it.contains(PREF_OSMOSE_ITEMS) }.forEach { (key, value) ->
                        if (value is String)
                            prefs.edit { putString(key, value.replace(",", "§§")) }
                    }
            }
            // update custom overlay to the indexed version
            if (prefs.contains("custom_overlay_filter") || prefs.contains("custom_overlay_color_key")) {
                val indices = if (prefs.contains(Prefs.CUSTOM_OVERLAY_INDICES)) getCustomOverlayIndices(prefs)  else emptyList()
                val newIndex = indices.maxOrNull() ?: 0
                prefs.edit {
                    if (prefs.contains("custom_overlay_filter"))
                        putString(getIndexedCustomOverlayPref(Prefs.CUSTOM_OVERLAY_IDX_FILTER, newIndex), prefs.getString("custom_overlay_filter", "")!!)
                    if (prefs.contains("custom_overlay_color_key"))
                        putString(getIndexedCustomOverlayPref(Prefs.CUSTOM_OVERLAY_IDX_COLOR_KEY, newIndex), prefs.getString("custom_overlay_color_key", "")!!)
                    remove("custom_overlay_filter")
                    remove("custom_overlay_color_key")
                    putString(Prefs.CUSTOM_OVERLAY_INDICES, (indices + newIndex).sorted().joinToString(","))
                }
            }
            // update prefs referring to renamed quests
            val prefsToRename = prefs.all.filter { pref ->
                val v = pref.value
                oldQuestNames.any { pref.key.contains(it) || (v is String && v.contains(it)) }
            }
            val e = prefs.edit()
            prefsToRename.forEach {
                e.remove(it.key)
                when (it.value) {
                    is String -> e.putString(it.key.renameUpdateQuests(), (it.value as String).renameUpdateQuests())
                    is Boolean -> e.putBoolean(it.key.renameUpdateQuests(), it.value as Boolean)
                    is Int -> e.putInt(it.key.renameUpdateQuests(), it.value as Int)
                    is Long -> e.putLong(it.key.renameUpdateQuests(), it.value as Long)
                    is Float -> e.putFloat(it.key.renameUpdateQuests(), it.value as Float)
                    is Set<*> -> e.putStringSet(it.key.renameUpdateQuests(), it.value as? Set<String>?)
                }
            }
            e.apply()
        }

    }

    private fun onNewVersion() {
        // on each new version, invalidate quest cache
        downloadedTilesController.invalidateAll()
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE, ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // very low on memory -> drop caches
                cacheTrimmer.clearCaches()
                Log.i("StreetCompleteApplication", "onTrimMemory, level $level: ${getMemString()}")
            }
            ComponentCallbacks2.TRIM_MEMORY_MODERATE, ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                // memory needed, but not critical -> trim only
                Log.i("StreetCompleteApplication", "onTrimMemory, level $level: ${getMemString()}")
                cacheTrimmer.trimCaches()
            }
        }
    }

    private fun getMemString(): String {
        val memInfo = MemoryInfo()
        getSystemService<ActivityManager>()?.getMemoryInfo(memInfo)
        return "${memInfo.availMem / 0x100000L} MB of ${memInfo.totalMem / 0x100000L} available, mem low: ${memInfo.lowMemory}, mem low threshold: ${memInfo.threshold / 0x100000L} MB"
    }

    private fun setDefaultLocales() {
        val locale = getSelectedLocale(this)
        if (locale != null) {
            setDefaultLocales(getSystemLocales().addedToFront(locale))
        }
    }

    private fun setDefaultTheme() {
        val theme = Prefs.Theme.valueOf(prefs.getString(Prefs.THEME_SELECT, getDefaultTheme())!!)
        AppCompatDelegate.setDefaultNightMode(theme.appCompatNightMode)
    }

    private fun enqueuePeriodicCleanupWork() {
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "Cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequest.Builder(
                CleanerWorker::class.java,
                1, TimeUnit.DAYS,
                1, TimeUnit.DAYS,
            ).setInitialDelay(1, TimeUnit.HOURS).build()
        )
    }

    companion object {
        lateinit var preferences: SharedPreferences
    }
}
