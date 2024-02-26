package dev.arkbuilders.components.tagselector

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.chip.Chip
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.skydoves.balloon.Balloon
import dev.arkbuilders.components.utils.closeKeyboard
import dev.arkbuilders.components.utils.placeCursorToEnd
import dev.arkbuilders.components.utils.showKeyboard
import org.orbitmvi.orbit.viewmodel.observe
import dev.arkbuilders.arklib.data.meta.Kind
import dev.arkbuilders.components.tagselector.databinding.ItemTagBinding
import dev.arkbuilders.components.tagselector.databinding.PopupTagSelectorTagMenuBinding
import dev.arkbuilders.components.tagselector.databinding.TagSelectorDragHandlerBinding
import dev.arkbuilders.components.tagselector.databinding.TagSelectorTagsLayoutBinding

class TagSelector(
    val ctx: Context,
    val controller: TagSelectorController,
    val lifecycleOwner: LifecycleOwner,
    val kindToString: (Kind) -> String,
) {
    private val tagsAdapter = ItemAdapter<TagItemView>()
    private val filterTagsAdapter = ItemAdapter<TagItemView>()

    var dragHandlerBinding: TagSelectorDragHandlerBinding? = null
    var tagsLayoutBinding: TagSelectorTagsLayoutBinding? = null

    private var clearChip: Chip? = null

    fun init(
        dragHandlerBinding: TagSelectorDragHandlerBinding,
        tagsLayoutBinding: TagSelectorTagsLayoutBinding
    ) {
        this.dragHandlerBinding = dragHandlerBinding
        this.tagsLayoutBinding = tagsLayoutBinding
        clearChip = dragHandlerBinding.btnClear
        tagsLayoutBinding.rvTags.apply {
            layoutManager = FlexboxLayoutManager(ctx)
            adapter = FastAdapter.with(tagsAdapter)
            itemAnimator = null
        }
        tagsLayoutBinding.rvTagsFilter.apply {
            layoutManager = FlexboxLayoutManager(ctx)
            adapter = FastAdapter.with(filterTagsAdapter)
            itemAnimator = null
        }
        setupListeners()

        controller.observe(
            lifecycleOwner,
            state = ::render,
            sideEffect = ::handleSideEffect
        )
    }

    fun onDestroyView() {
        dragHandlerBinding = null
        tagsLayoutBinding = null
    }

    private fun setupListeners() {
        dragHandlerBinding!!.switchKind.setOnCheckedChangeListener { _, isChecked ->
            controller.onKindTagsToggle(isChecked)
        }
        dragHandlerBinding!!.btnClear.setOnClickListener {
            controller.onClearClick()
        }
        tagsLayoutBinding!!.etTagsFilter.doAfterTextChanged {
            controller.onFilterChanged(it.toString())
        }
    }

    private fun render(state: TagSelectorState) = with(state) {
        drawTags(state)
        setTagsFilterEnabled(filterEnabled)
        setTagsSortingVisibility(state.collectTagUsage)
        tagsLayoutBinding!!.etTagsFilter.setText(filter)
    }

    private fun setTagsSortingVisibility(
        visible: Boolean
    ) {
        dragHandlerBinding!!.btnTagsSorting.isVisible = visible
        val params =
            dragHandlerBinding!!.btnClear.layoutParams as ConstraintLayout.LayoutParams
        params.horizontalBias = if (visible) 1f else 0.5f
        dragHandlerBinding!!.btnClear.layoutParams = params
    }

    private fun setTagsFilterEnabled(enabled: Boolean) {
        tagsLayoutBinding!!.layoutInput.isVisible = enabled
        tagsLayoutBinding!!.rvTagsFilter.isVisible = enabled
        if (enabled) {
            tagsLayoutBinding!!.etTagsFilter.placeCursorToEnd()
            tagsLayoutBinding!!.etTagsFilter.showKeyboard()
        } else
            tagsLayoutBinding!!.etTagsFilter.closeKeyboard()
    }

    private fun drawTags(state: TagSelectorState) = with(state) {
        drawClearChip(state)

        if (checkTagsEmpty(state)) {
            tagsLayoutBinding!!.tvTagsSelectorHint.isVisible = true
            return@with
        } else
            tagsLayoutBinding!!.tvTagsSelectorHint.isVisible = false

        val includedTagItemView = state.includedTagItems.map {
            createTagItemView(it, TagSelectType.Included)
        }
        val excludedTagItemView = state.excludedTagItems.map {
            createTagItemView(it, TagSelectType.Excluded)
        }
        val availableTagItemView = state.filteredAvailableTags.map {
            createTagItemView(it, TagSelectType.Available)
        }
        val unavailableTagItemView = state.filteredUnavailableTags.map {
            createTagItemView(it, TagSelectType.Unavailable)
        }

        var includedAndExcluded = includedTagItemView + excludedTagItemView
        includedAndExcluded =
            state.filteredIncludedAndExcluded.map { tagItem ->
                includedAndExcluded.find { it.tagItem == tagItem }
                    ?: error("TagsSelectorPresenter: Tag inconsistency detected")
            }

        if (state.filterEnabled) {
            filterTagsAdapter.setNewList(includedAndExcluded)
            tagsAdapter.setNewList(
                availableTagItemView +
                        unavailableTagItemView
            )
        } else {
            filterTagsAdapter.setNewList(emptyList())
            tagsAdapter.setNewList(
                includedAndExcluded +
                        availableTagItemView +
                        unavailableTagItemView
            )
        }
    }

    private fun handleSideEffect(effect: TagSelectorSideEffect) {
        when (effect) {
            is TagSelectorSideEffect.SwitchKindTagsSilent -> {
                if (effect.enabled) {
                    dragHandlerBinding!!.switchKind.isChecked = true
                    dragHandlerBinding!!.switchKind.jumpDrawablesToCurrentState()
                }
            }
        }
    }

    private fun checkTagsEmpty(state: TagSelectorState): Boolean = with(state) {
        return@with includedTagItems.isEmpty() &&
                excludedTagItems.isEmpty() &&
                filteredAvailableTags.isEmpty() &&
                filteredUnavailableTags.isEmpty()
    }

    private fun drawClearChip(state: TagSelectorState) = clearChip!!.apply {
        if (state.isClearBtnEnabled) {
            isEnabled = true
            chipIconTint = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.black)
            )
        } else {
            isEnabled = false
            chipIconTint = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.grayTransparent)
            )
        }
    }

    private fun createTagItemView(tagItem: TagItem, tagType: TagSelectType) =
        TagItemView(
            ctx,
            controller,
            kindToString,
            tagItem,
            tagType
        )
}

private enum class TagSelectType {
    Included, Excluded, Available, Unavailable
}

private class TagItemView(
    private val ctx: Context,
    private val controller: TagSelectorController,
    private val kindToString: (Kind) -> String,
    val tagItem: TagItem,
    private val tagType: TagSelectType
) : AbstractBindingItem<ItemTagBinding>() {

    override val type = com.mikepenz.fastadapter.R.id.fastadapter_item

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = ItemTagBinding.inflate(inflater, parent, false)

    override fun bindView(
        binding: ItemTagBinding,
        payloads: List<Any>
    ) = with(binding.chipTag) {

        resetTagView(binding)

        setOnClickListener {
            controller.onTagItemClick(tagItem)
        }

        setOnLongClickListener {
            showTagMenuPopup(tagItem, it)
            true
        }

        when (tagItem) {
            is TagItem.PlainTagItem -> {
                chipBackgroundColor =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.grayTransparent)
                    )
                text = tagItem.tag
            }

            is TagItem.KindTagItem -> {
                chipBackgroundColor =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.blue)
                    )
                text = kindToString(tagItem.kind)
            }
        }

        when (tagType) {
            TagSelectType.Included -> {
                setTextColor(Color.BLUE)
                isChecked = true
            }

            TagSelectType.Excluded -> {
                setTextColor(Color.RED)
                isLongClickable = false
            }

            TagSelectType.Available -> {}
            TagSelectType.Unavailable -> {
                setTextColor(Color.GRAY)
                isLongClickable = false
                isClickable = false
                isCheckable = false
            }
        }
    }

    private fun resetTagView(binding: ItemTagBinding) = binding.chipTag.apply {
        isClickable = true
        isLongClickable = true
        isCheckable = true
        isChecked = false
        setTextColor(Color.BLACK)
    }

    private fun showTagMenuPopup(tag: TagItem, tagView: View) {
        val menuBinding = PopupTagSelectorTagMenuBinding
            .inflate(LayoutInflater.from(ctx))

        val balloon = Balloon.Builder(tagView.context)
            .setLayout(menuBinding)
            .setBackgroundColorResource(R.color.white)
            .setArrowSize(0)
            .setLifecycleOwner(tagView.findViewTreeLifecycleOwner())
            .build()

        menuBinding.apply {
            btnInvert.setOnClickListener {
                controller.onTagItemLongClick(tag)
                balloon.dismiss()
            }
        }

        balloon.showAsDropDown(tagView)
    }
}
