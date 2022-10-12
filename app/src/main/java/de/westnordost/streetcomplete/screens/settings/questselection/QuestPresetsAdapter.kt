package de.westnordost.streetcomplete.screens.settings.questselection

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.visiblequests.QuestPreset
import de.westnordost.streetcomplete.data.visiblequests.QuestPresetsController
import de.westnordost.streetcomplete.data.visiblequests.QuestPresetsSource
import de.westnordost.streetcomplete.databinding.RowQuestPresetBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Adapter for the list in which the user can select which preset of quest selections he wants to
 *  use. */
class QuestPresetsAdapter(
    private val context: Context,
    private val questPresetsController: QuestPresetsController
) : RecyclerView.Adapter<QuestPresetsAdapter.QuestPresetViewHolder>(), DefaultLifecycleObserver {

    private var presets: MutableList<QuestPreset> = mutableListOf()

    private val viewLifecycleScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val questPresetsListener = object : QuestPresetsSource.Listener {
        override fun onSelectedQuestPresetChanged() { viewLifecycleScope.launch {
            notifyDataSetChanged()
        } }

        override fun onAddedQuestPreset(preset: QuestPreset) { viewLifecycleScope.launch {
            presets.add(preset)
            notifyItemInserted(presets.size - 1)
        } }

        override fun onDeletedQuestPreset(presetId: Long) { viewLifecycleScope.launch {
            val deleteIndex = presets.indexOfFirst { it.id == presetId }
            presets.removeAt(deleteIndex)
            notifyItemRemoved(deleteIndex)
        } }
    }

    override fun onStart(owner: LifecycleOwner) {
        presets = mutableListOf()
        presets.add(QuestPreset(0, context.getString(R.string.quest_presets_default_name)))
        presets.addAll(questPresetsController.getAll())

        questPresetsController.addListener(questPresetsListener)
    }

    override fun onStop(owner: LifecycleOwner) {
        questPresetsController.removeListener(questPresetsListener)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        viewLifecycleScope.cancel()
    }

    override fun getItemCount(): Int = presets.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestPresetViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return QuestPresetViewHolder(RowQuestPresetBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: QuestPresetViewHolder, position: Int) {
        holder.onBind(presets[position])
    }

    inner class QuestPresetViewHolder(private val binding: RowQuestPresetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun onBind(with: QuestPreset) {
            binding.presetTitleText.text = with.name
            binding.selectionRadioButton.setOnCheckedChangeListener(null)
            binding.selectionRadioButton.isChecked = questPresetsController.selectedId == with.id
            binding.selectionRadioButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) onSelectQuestPreset(with.id)
            }
            binding.menuButton.isEnabled = true
            binding.menuButton.setOnClickListener { onClickMenuButton(with) }
        }

        private fun onSelectQuestPreset(presetId: Long) {
            viewLifecycleScope.launch(Dispatchers.IO) {
                questPresetsController.selectedId = presetId
            }
        }

        private fun onClickMenuButton(preset: QuestPreset) {
            val popup = PopupMenu(context, binding.menuButton)

            val renameItem = popup.menu.add(R.string.quest_presets_rename)
            renameItem.setIcon(R.drawable.ic_edit_24dp)
            renameItem.setOnMenuItemClickListener { onClickRenamePreset(preset); true }

            val shareItem = popup.menu.add(R.string.quest_presets_share)
            shareItem.setIcon(R.drawable.ic_share_24dp)
            shareItem.setOnMenuItemClickListener { onClickSharePreset(preset); true }

            if (preset.id != 0L) {
                val deleteItem = popup.menu.add(R.string.quest_presets_delete)
                deleteItem.setOnMenuItemClickListener { onClickDeleteQuestPreset(preset); true }
            }

            popup.show()
        }

        private fun onClickRenamePreset(preset: QuestPreset) {
            TODO()
        }

        private fun onClickSharePreset(preset: QuestPreset) {
            TODO()
        }

        private fun onClickDeleteQuestPreset(preset: QuestPreset) {
            AlertDialog.Builder(itemView.context, R.style.Theme_Bubble_Dialog_Alert)
                .setMessage(itemView.context.getString(R.string.quest_presets_delete_message, preset.name))
                .setPositiveButton(R.string.delete_confirmation) { _, _ -> deleteQuestPreset(preset.id) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        private fun deleteQuestPreset(presetId: Long) {
            viewLifecycleScope.launch(Dispatchers.IO) {
                questPresetsController.delete(presetId)
            }
        }
    }
}
