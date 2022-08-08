package de.westnordost.streetcomplete.screens.main.map

import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.RectF
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import de.westnordost.streetcomplete.Prefs
import de.westnordost.streetcomplete.data.osm.geometry.ElementPointGeometry
import de.westnordost.streetcomplete.data.download.tiles.TilesRect
import de.westnordost.streetcomplete.data.download.tiles.enclosingTilesRect
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.quest.DayNightCycle
import de.westnordost.streetcomplete.data.quest.OsmNoteQuestKey
import de.westnordost.streetcomplete.data.quest.OsmQuestKey
import de.westnordost.streetcomplete.data.quest.Quest
import de.westnordost.streetcomplete.data.quest.QuestKey
import de.westnordost.streetcomplete.data.quest.QuestType
import de.westnordost.streetcomplete.data.quest.QuestTypeRegistry
import de.westnordost.streetcomplete.data.quest.VisibleQuestsSource
import de.westnordost.streetcomplete.data.visiblequests.QuestTypeOrderSource
import de.westnordost.streetcomplete.screens.main.map.components.Pin
import de.westnordost.streetcomplete.screens.main.map.components.PinsMapComponent
import de.westnordost.streetcomplete.screens.main.map.tangram.KtMapController
import de.westnordost.streetcomplete.util.isDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Manages the layer of quest pins in the map view:
 *  Gets told by the QuestsMapFragment when a new area is in view and independently pulls the quests
 *  for the bbox surrounding the area from database and holds it in memory. */
class QuestPinsManager(
    private val ctrl: KtMapController,
    private val pinsMapComponent: PinsMapComponent,
    private val questTypeOrderSource: QuestTypeOrderSource,
    private val questTypeRegistry: QuestTypeRegistry,
    private val resources: Resources,
    private val visibleQuestsSource: VisibleQuestsSource,
    private val prefs: SharedPreferences,
) : DefaultLifecycleObserver {

    // draw order in which the quest types should be rendered on the map
    private val questTypeOrders: MutableMap<QuestType, Int> = mutableMapOf()
    // last displayed rect of (zoom 16) tiles
    private var lastDisplayedRect: TilesRect? = null
    // quests in current view: key -> [pin, ...]
    private val questsInView: MutableMap<QuestKey, List<Pin>> = mutableMapOf()
    var reversedOrder = false
        private set
    // list of pins to set, stored in a var as an attempt to avoid setting outdated pins
    // is volatile actually helping here?
    @Volatile private var pinsToSet = emptyList<Pin>()



    private val viewLifecycleScope: CoroutineScope = CoroutineScope(SupervisorJob())

    /** Switch active-ness of quest pins layer */
    var isActive: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) start() else stop() }

    private val visibleQuestsListener = object : VisibleQuestsSource.Listener {
        override fun onUpdatedVisibleQuests(added: Collection<Quest>, removed: Collection<QuestKey>) {
            viewLifecycleScope.launch { updateQuestPins(added, removed) }
        }

        override fun onVisibleQuestsInvalidated() {
            invalidate()
        }
    }

    private val questTypeOrderListener = object : QuestTypeOrderSource.Listener {
        override fun onQuestTypeOrderAdded(item: QuestType, toAfter: QuestType) {
            reinitializeQuestTypeOrders()
        }

        override fun onQuestTypeOrdersChanged() {
            reinitializeQuestTypeOrders()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stop()
        viewLifecycleScope.cancel()
    }

    private fun start() {
        initializeQuestTypeOrders()
        onNewScreenPosition()
        visibleQuestsSource.addListener(visibleQuestsListener)
        questTypeOrderSource.addListener(questTypeOrderListener)
    }

    private fun stop() {
        viewLifecycleScope.coroutineContext.cancelChildren()
        clear()
        visibleQuestsSource.removeListener(visibleQuestsListener)
        questTypeOrderSource.removeListener(questTypeOrderListener)
    }

    private fun invalidate() {
        clear()
        onNewScreenPosition()
    }

    private fun clear() {
        synchronized(questsInView) { questsInView.clear() }
        lastDisplayedRect = null
        viewLifecycleScope.launch { pinsMapComponent.clear() }
    }

    fun getQuestKey(properties: Map<String, String>): QuestKey? =
        properties.toQuestKey()

    fun onNewScreenPosition() {
        if (!isActive) return
        val zoom = ctrl.cameraPosition.zoom
        if (zoom < TILES_ZOOM) return
        val displayedArea = ctrl.screenAreaToBoundingBox(RectF()) ?: return
        val tilesRect = displayedArea.enclosingTilesRect(TILES_ZOOM)
        // area too big -> skip (performance)
        if (tilesRect.size > 16) return
        if (lastDisplayedRect?.contains(tilesRect) != true) {
            lastDisplayedRect = tilesRect
            onNewTilesRect(tilesRect)
        }
    }

    private fun onNewTilesRect(tilesRect: TilesRect) {
        val bbox = tilesRect.asBoundingBox(TILES_ZOOM)
        viewLifecycleScope.launch {
            val quests = withContext(Dispatchers.IO) { visibleQuestsSource.getAllVisible(bbox) }
            setQuestPins(quests)
        }
    }

    private fun setQuestPins(quests: List<Quest>) {
        pinsToSet = synchronized(questsInView) {
            questsInView.clear()
            quests.forEach { questsInView[it.key] = createQuestPins(it) }
            questsInView.values.flatten()
        }
        setPins()
    }

    private fun updateQuestPins(added: Collection<Quest>, removed: Collection<QuestKey>) {
        pinsToSet = synchronized(questsInView) {
            added.forEach { questsInView[it.key] = createQuestPins(it) }
            removed.forEach { questsInView.remove(it) }
            questsInView.values.flatten()
        }
        setPins()
    }

    private fun setPins() {
        val pins = pinsToSet // copy list for later comparison
        synchronized(pinsMapComponent) { // waiting may take considerable time
            if (pins !== pinsToSet) return // list of pins was changed while waiting for synchronized
            pinsMapComponent.set(pins)
        }
    }

    fun reverseQuestOrder() {
        reversedOrder = !reversedOrder
        reinitializeQuestTypeOrders()
    }

    private fun initializeQuestTypeOrders() {
        // this needs to be reinitialized when the quest order changes
        val sortedQuestTypes = if (reversedOrder) questTypeRegistry.asReversed().toMutableList() else questTypeRegistry.toMutableList()
        // move specific quest types to front if set by preference
        val moveToFront = if (Prefs.DayNightBehavior.valueOf(prefs.getString(Prefs.DAY_NIGHT_BEHAVIOR, "IGNORE")!!) == Prefs.DayNightBehavior.PRIORITY)
            if (isDay(ctrl.cameraPosition.position))
                sortedQuestTypes.filter { it.dayNightCycle == DayNightCycle.ONLY_DAY }
            else
                sortedQuestTypes.filter { it.dayNightCycle == DayNightCycle.ONLY_NIGHT }
        else
            emptyList()
        moveToFront.asReversed().forEach {
            sortedQuestTypes.remove(it)
            sortedQuestTypes.add(0, it)
        }
        questTypeOrderSource.sort(sortedQuestTypes)
        synchronized(questTypeOrders) {
            questTypeOrders.clear()
            sortedQuestTypes.forEachIndexed { index, questType ->
                questTypeOrders[questType] = index
            }
        }
    }

    private fun createQuestPins(quest: Quest): List<Pin> {
        val iconName = resources.getResourceEntryName(quest.type.icon)
        val props = quest.key.toProperties()
        val color = quest.type.dotColor
        val importance = getQuestImportance(quest)
        val geometry = if (prefs.getBoolean(Prefs.QUEST_GEOMETRIES, false)
                && quest.geometry !is ElementPointGeometry && color == "no")
            quest.geometry
        else
            null
        return quest.markerLocations.map { Pin(it, iconName, props, importance, geometry, color) }
    }

    /** returns values from 0 to 100000, the higher the number, the more important */
    private fun getQuestImportance(quest: Quest): Int = synchronized(questTypeOrders) {
        val questTypeOrder = questTypeOrders[quest.type] ?: 0
        val freeValuesForEachQuest = 100000 / questTypeOrders.size
        /* position is used to add values unique to each quest to make ordering consistent
           freeValuesForEachQuest is an int, so % freeValuesForEachQuest will fit into int */
        val hopefullyUniqueValueForQuest = quest.position.hashCode() % freeValuesForEachQuest
        return 100000 - questTypeOrder * freeValuesForEachQuest + hopefullyUniqueValueForQuest
    }

    private fun reinitializeQuestTypeOrders() {
        initializeQuestTypeOrders()
        invalidate()
    }

    companion object {
        private const val TILES_ZOOM = 16
    }
}

private const val MARKER_QUEST_GROUP = "quest_group"

private const val MARKER_ELEMENT_TYPE = "element_type"
private const val MARKER_ELEMENT_ID = "element_id"
private const val MARKER_QUEST_TYPE = "quest_type"
private const val MARKER_NOTE_ID = "note_id"

private const val QUEST_GROUP_OSM = "osm"
private const val QUEST_GROUP_OSM_NOTE = "osm_note"

private fun QuestKey.toProperties(): List<Pair<String, String>> = when (this) {
    is OsmNoteQuestKey -> listOf(
        MARKER_QUEST_GROUP to QUEST_GROUP_OSM_NOTE,
        MARKER_NOTE_ID to noteId.toString()
    )
    is OsmQuestKey -> listOf(
        MARKER_QUEST_GROUP to QUEST_GROUP_OSM,
        MARKER_ELEMENT_TYPE to elementType.name,
        MARKER_ELEMENT_ID to elementId.toString(),
        MARKER_QUEST_TYPE to questTypeName
    )
}

private fun Map<String, String>.toQuestKey(): QuestKey? = when (get(MARKER_QUEST_GROUP)) {
    QUEST_GROUP_OSM_NOTE ->
        OsmNoteQuestKey(getValue(MARKER_NOTE_ID).toLong())
    QUEST_GROUP_OSM ->
        OsmQuestKey(
            getValue(MARKER_ELEMENT_TYPE).let { ElementType.valueOf(it) },
            getValue(MARKER_ELEMENT_ID).toLong(),
            getValue(MARKER_QUEST_TYPE)
        )
    else -> null
}
