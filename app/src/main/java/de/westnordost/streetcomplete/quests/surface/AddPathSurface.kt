package de.westnordost.streetcomplete.quests.surface

import androidx.appcompat.app.AlertDialog
import android.content.Context
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.geometry.ElementGeometry
import de.westnordost.streetcomplete.data.osm.osmquests.OsmFilterQuestType
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement.BICYCLIST
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement.OUTDOORS
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement.PEDESTRIAN
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement.WHEELCHAIR
import de.westnordost.streetcomplete.osm.Tags
import de.westnordost.streetcomplete.osm.changeToSteps
import de.westnordost.streetcomplete.osm.surface.ANYTHING_UNPAVED
import de.westnordost.streetcomplete.osm.surface.INVALID_SURFACES
import de.westnordost.streetcomplete.osm.surface.applyTo
import de.westnordost.streetcomplete.quests.booleanQuestSettingsDialog
import de.westnordost.streetcomplete.quests.questPrefix

class AddPathSurface : OsmFilterQuestType<SurfaceOrIsStepsAnswer>() {

    override val elementFilter = """
        ways with highway ~ path|footway|cycleway|bridleway|steps
        and segregated != yes
        and access !~ private|no
        and (!conveying or conveying = no)
        and (!indoor or indoor = no)
        and (
          !surface
          or surface ~ ${ANYTHING_UNPAVED.joinToString("|")} and surface older today -6 years
          or surface older today -8 years
          or (
            surface ~ ${if (prefs.getBoolean(questPrefix(prefs) + ALLOW_GENERIC_PATH, false)) "" else "paved|unpaved|"}${INVALID_SURFACES.joinToString("|")}
            and !surface:note
            and !note:surface
          )
        )
        and ~path|footway|cycleway|bridleway !~ link
    """
    /* ~paved ways are less likely to change the surface type */

    override val changesetComment = "Specify path surfaces"
    override val wikiLink = "Key:surface"
    override val icon = R.drawable.ic_quest_way_surface
    override val achievements = listOf(PEDESTRIAN, WHEELCHAIR, BICYCLIST, OUTDOORS)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_surface_title

    override fun createForm() = AddPathSurfaceForm()

    override fun applyAnswerTo(answer: SurfaceOrIsStepsAnswer, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        when (answer) {
            is SurfaceAnswer -> {
                answer.value.applyTo(tags)
            }
            is IsActuallyStepsAnswer -> {
                tags.changeToSteps()
            }
            is IsIndoorsAnswer -> {
                tags["indoor"] = "yes"
            }
        }
    }

    override val hasQuestSettings = true

    override fun getQuestSettingsDialog(context: Context): AlertDialog =
        booleanQuestSettingsDialog(context, prefs, questPrefix(prefs) + ALLOW_GENERIC_PATH,
            R.string.quest_generic_surface_message,
            R.string.quest_generic_surface_yes,
            R.string.quest_generic_surface_no
        )
}

const val ALLOW_GENERIC_PATH = "qs_AddPathSurface_allow_generic"
