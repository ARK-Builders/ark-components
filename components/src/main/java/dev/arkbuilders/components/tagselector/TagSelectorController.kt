package dev.arkbuilders.components.tagselector

import android.util.Log
import dev.arkbuilders.components.utils.Popularity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.data.meta.Kind
import dev.arkbuilders.arklib.data.meta.MetadataProcessor
import dev.arkbuilders.arklib.data.stats.StatsEvent
import dev.arkbuilders.arklib.data.tags.Tag
import dev.arkbuilders.arklib.data.tags.TagStorage

sealed class TagItem {
    data class PlainTagItem(val tag: Tag) : TagItem()
    data class KindTagItem(val kind: Kind) : TagItem()
}

enum class QueryMode {
    NORMAL, FOCUS
}

enum class TagsSorting {
    POPULARITY, QUERIED_TS, QUERIED_N, LABELED_TS, LABELED_N
}


data class TagSelectorState(
    var filteredIncludedAndExcluded: List<TagItem>,
    var filteredAvailableTags: List<TagItem>,
    var filteredUnavailableTags: List<TagItem>,
    var includedTagItems: MutableSet<TagItem>,
    var excludedTagItems: MutableSet<TagItem>,
    var availableTagItems: List<TagItem>,
    var unavailableTagItems: List<TagItem>,
    var isClearBtnEnabled: Boolean,
    var queryMode: QueryMode,
    var filterEnabled: Boolean,
    var sorting: TagsSorting,
    var sortingAscending: Boolean,
    var filter: String,
    var showKindTags: Boolean,
    var collectTagUsage: Boolean,
    val actionsHistory: ArrayDeque<TagsSelectorAction>,
) {
    companion object {
        fun initial() = TagSelectorState(
            filteredIncludedAndExcluded = emptyList(),
            filteredAvailableTags = emptyList(),
            filteredUnavailableTags = emptyList(),
            includedTagItems = mutableSetOf(),
            excludedTagItems = mutableSetOf(),
            availableTagItems = emptyList(),
            unavailableTagItems = emptyList(),
            isClearBtnEnabled = false,
            queryMode = QueryMode.NORMAL,
            filterEnabled = false,
            sorting = TagsSorting.POPULARITY,
            sortingAscending = true,
            filter = "",
            showKindTags = false,
            collectTagUsage = false,
            actionsHistory = ArrayDeque<TagsSelectorAction>()
        )
    }
}

sealed class TagSelectorSideEffect {
    class SwitchKindTagsSilent(val enabled: Boolean): TagSelectorSideEffect()
}

class TagSelectorController(
    val scope: CoroutineScope,
    val kindToString: (Kind) -> String,
    val tagSortCriteria: (TagsSorting) -> Map<Tag, Comparable<Any>>,
    val onSelectionChangeListener: suspend (
        mode: QueryMode,
        normalModeSelection: Set<ResourceId>,
        focusModeSelection: Set<ResourceId>?
    ) -> Unit,
    val onStatsEvent: (StatsEvent) -> Unit,
    val onKindTagsChanged: suspend (Boolean) -> Unit,
    val onQueryModeChangedCB: (QueryMode) -> Unit
) : ContainerHost<TagSelectorState, TagSelectorSideEffect> {

    override val container: Container<TagSelectorState, TagSelectorSideEffect> =
        scope.container(TagSelectorState.initial())

    private lateinit var index: ResourceIndex
    private lateinit var tagsStorage: TagStorage
    private lateinit var metadataStorage: MetadataProcessor

    var selection = setOf<ResourceId>()
        private set

    suspend fun init(
        index: ResourceIndex,
        tagsStorage: TagStorage,
        metadataStorage: MetadataProcessor,
        kindTagsEnabled: Boolean,
        sorting: TagsSorting,
        sortingAscending: Boolean,
        collectTagUsage: Boolean,
        sortingFlow: Flow<TagsSorting>,
        sortingAscFlow: Flow<Boolean>
    ) {
        this.index = index
        this.tagsStorage = tagsStorage
        this.metadataStorage = metadataStorage
        intent {
            postSideEffect(TagSelectorSideEffect.SwitchKindTagsSilent(kindTagsEnabled))
            reduce {
                state.copy(
                    showKindTags = kindTagsEnabled,
                    sorting = sorting,
                    sortingAscending = sortingAscending,
                    collectTagUsage = collectTagUsage
                )
            }
        }
        sortingFlow.onEach { newSorting ->
            intent {
                if (state.sorting == newSorting) return@intent
                val newState = state.copy()
                newState.sorting = newSorting
                calculateTagsAndSelection(newState)
            }
        }.launchIn(scope)
        sortingAscFlow.onEach { newSortingAscending ->
            intent {
                if (state.sortingAscending == newSortingAscending) return@intent
                val newState = state.copy()
                newState.sortingAscending = newSortingAscending
                calculateTagsAndSelection(newState)
            }
        }.launchIn(scope)
    }

    fun onTagItemClick(item: TagItem) = intent {
        val newState = state.copy()
        newState.apply {
            when {
                excludedTagItems.contains(item) -> {
                    actionsHistory.addLast(TagsSelectorAction.UncheckExcluded(item))
                    uncheckTag(newState, item)
                }

                includedTagItems.contains(item) -> {
                    actionsHistory.addLast(TagsSelectorAction.UncheckIncluded(item))
                    uncheckTag(newState, item)
                }

                else -> {
                    actionsHistory.addLast(TagsSelectorAction.Include(item))
                    if (state.filterEnabled) resetFilter()
                    includeTag(newState, item)
                }
            }
        }
    }

    fun onTagItemLongClick(item: TagItem) = intent {
        val newState = state.copy()
        newState.apply {
            when {
                includedTagItems.contains(item) -> {
                    actionsHistory.addLast(TagsSelectorAction.UncheckAndExclude(item))
                    excludeTag(newState, item)
                }

                !excludedTagItems.contains(item) -> {
                    actionsHistory.addLast(TagsSelectorAction.Exclude(item))
                    if (filterEnabled) resetFilter()
                    excludeTag(newState, item)
                }

                else -> {
                    actionsHistory.addLast(TagsSelectorAction.UncheckExcluded(item))
                    uncheckTag(newState, item)
                }
            }
        }
    }

    fun onKindTagsToggle(kindTagsEnabled: Boolean) = intent {
        val newState = state.copy()
        newState.showKindTags = kindTagsEnabled
        onKindTagsChanged(kindTagsEnabled)
        calculateTagsAndSelection(newState)
    }

    suspend fun onBackClick(): Boolean {
        val newState = container.stateFlow.value.copy()
        if (newState.actionsHistory.isEmpty())
            return false

        val action = findLastActualAction(newState) ?: return false

        newState.apply {
            when (action) {
                is TagsSelectorAction.Include -> {
                    includedTagItems.remove(action.item)
                }

                is TagsSelectorAction.Exclude -> {
                    excludedTagItems.remove(action.item)
                }

                is TagsSelectorAction.UncheckIncluded -> {
                    includedTagItems.add(action.item)
                }

                is TagsSelectorAction.UncheckExcluded -> {
                    excludedTagItems.add(action.item)
                }

                is TagsSelectorAction.UncheckAndExclude -> {
                    excludedTagItems.remove(action.item)
                    includedTagItems.add(action.item)
                }

                is TagsSelectorAction.Clear -> {
                    includedTagItems = action.included.toMutableSet()
                    excludedTagItems = action.excluded.toMutableSet()
                }
            }
        }

        newState.actionsHistory.removeLast()

        calculateTagsAndSelection(newState)

        return true
    }

    fun onTagExternallySelect(tag: Tag) = intent {
        val newState = state.copy()
        includeTag(newState, TagItem.PlainTagItem(tag))
    }

    fun onClearClick() = intent {
        val newState = state.copy()
        state.apply {
            actionsHistory.addLast(
                TagsSelectorAction.Clear(
                    includedTagItems.toSet(),
                    excludedTagItems.toSet()
                )
            )
            includedTagItems.clear()
            excludedTagItems.clear()
        }
        calculateTagsAndSelection(newState)
    }

    fun onFilterChanged(filter: String) = intent {
        val newState = state.copy()
        newState.filter = filter
        filterTags(newState)
        reduce { newState }
    }

    fun onQueryModeChanged(mode: QueryMode) = intent {
        val newState = state.copy()
        newState.queryMode = mode
        onQueryModeChangedCB(mode)
        calculateTagsAndSelection(newState)
    }

    fun onFilterToggle(enabled: Boolean) = intent {
        val newState = state.copy()
        if (newState.filterEnabled != enabled) {
            newState.filterEnabled = enabled
            if (enabled) {
                filterTags(newState)
            } else {
                resetTags(newState)
            }
            reduce { newState }
        }
    }

    fun calculateTagsAndSelection(_newState: TagSelectorState? = null) = intent {
        val newState = _newState ?: state.copy()

        newState.apply {
            Log.d(TAGS_SELECTOR, "calculating tags and selection")

            val tagItemsByResources = provideTagItemsByResources(newState)
            val allItemsTags = tagItemsByResources.values.flatten().toSet()

            // some tags could have been removed from storage
            excludedTagItems =
                excludedTagItems.intersect(allItemsTags).toMutableSet()
            includedTagItems =
                includedTagItems.intersect(allItemsTags).toMutableSet()

            val selectionAndComplementWithTags = tagItemsByResources
                .toList()
                .groupBy { (_, tags) ->
                    tags.containsAll(includedTagItems) &&
                            !excludedTagItems.any { tags.contains(it) }
                }

            val selectionWithTags = (
                    selectionAndComplementWithTags[true] ?: emptyList()
                    ).toMap()
            val complementWithTags = (
                    selectionAndComplementWithTags[false] ?: emptyList()
                    ).toMap()

            selection = selectionWithTags.keys
            val tagsOfSelectedResources = selectionWithTags.values.flatten()
            val tagsOfUnselectedResources = complementWithTags.values.flatten()

            availableTagItems = (
                    tagsOfSelectedResources.toSet() -
                            includedTagItems -
                            excludedTagItems
                    ).toList()
            unavailableTagItems = (
                    allItemsTags -
                            availableTagItems.toSet() -
                            includedTagItems -
                            excludedTagItems
                    ).toList()

            sort(
                newState,
                tagsOfSelectedResources,
                tagsOfUnselectedResources,
                tagItemsByResources
            )

            if (filterEnabled) filterTags(newState)
            else resetTags(newState)

            Log.d(TAGS_SELECTOR, "tags included: $includedTagItems")
            Log.d(TAGS_SELECTOR, "tags excluded: $excludedTagItems")
            Log.d(TAGS_SELECTOR, "tags available: $availableTagItems")
            Log.d(TAGS_SELECTOR, "tags unavailable: $unavailableTagItems")

            isClearBtnEnabled = includedTagItems.isNotEmpty() ||
                    excludedTagItems.isNotEmpty()

            reduce {
                this@apply
            }

            notifySelectionChanged(tagItemsByResources)
        }
    }


    private suspend fun notifySelectionChanged(
        tagItemsByResources: Map<ResourceId, Set<TagItem>>
    ) = intent {
        if (state.queryMode == QueryMode.NORMAL) {
            onSelectionChangeListener(
                state.queryMode,
                selection,
                null
            )
            return@intent
        }

        val normalModeSelection = selection
        selection = selection.filter { id ->
            tagItemsByResources[id] == state.includedTagItems
        }.toSet()

        onSelectionChangeListener(state.queryMode, normalModeSelection, selection)
    }

    private fun resetFilter() = intent {
        reduce {
            state.copy(filter = "")
        }
    }

    private fun resetTags(newState: TagSelectorState) = newState.apply {
        filteredAvailableTags = availableTagItems
        filteredUnavailableTags = unavailableTagItems
    }

    private fun filterTags(newState: TagSelectorState) = newState.apply {
        filteredAvailableTags = availableTagItems
            .filter { filterTagItem(it, filter) }

        filteredUnavailableTags = unavailableTagItems
            .filter { filterTagItem(it, filter) }
    }

    private fun filterTagItem(item: TagItem, filter: String) = when (item) {
        is TagItem.PlainTagItem -> {
            item.tag.startsWith(filter, false)
        }

        is TagItem.KindTagItem -> {
            kindToString(item.kind).startsWith(filter, false)
        }
    }

    private suspend fun includeTag(
        newState: TagSelectorState,
        item: TagItem
    ) = newState.apply {
        Log.d(TAGS_SELECTOR, "including tag $item")
        val event = when (item) {
            is TagItem.KindTagItem -> StatsEvent.KindTagUsed(item.kind)
            is TagItem.PlainTagItem -> StatsEvent.PlainTagUsed(item.tag)
        }

        onStatsEvent(event)

        includedTagItems.add(item)
        excludedTagItems.remove(item)

        calculateTagsAndSelection(newState)
    }

    private suspend fun excludeTag(newState: TagSelectorState, item: TagItem) =
        newState.apply {
            Log.d(TAGS_SELECTOR, "excluding tag $item")

            excludedTagItems.add(item)
            includedTagItems.remove(item)

            calculateTagsAndSelection(newState)
        }

    private suspend fun uncheckTag(
        newState: TagSelectorState,
        item: TagItem,
        needToCalculate: Boolean = true
    ) = newState.apply {
        Log.d(TAGS_SELECTOR, "un-checking tag $item")

        if (includedTagItems.contains(item) && excludedTagItems.contains(item)) {
            throw AssertionError("The tag is both included and excluded")
        }
        if (!includedTagItems.contains(item) && !excludedTagItems.contains(item)) {
            throw AssertionError("The tag is neither included nor excluded")
        }

        if (!includedTagItems.remove(item)) {
            excludedTagItems.remove(item)
        }

        if (needToCalculate)
            calculateTagsAndSelection(newState)
    }

    private fun sort(
        newState: TagSelectorState,
        tagsOfSelectedResources: List<TagItem>,
        tagsOfUnselectedResources: List<TagItem>,
        tagItemsByResources: Map<ResourceId, Set<TagItem>>
    ) = newState.apply {
        if (sorting == TagsSorting.POPULARITY) {
            sortByPopularity(
                newState,
                tagsOfSelectedResources,
                tagsOfUnselectedResources,
                tagItemsByResources
            )
        } else
            sortByStatsCriteria(newState)

        if (!sortingAscending) {
            availableTagItems = availableTagItems.reversed()
            unavailableTagItems = unavailableTagItems.reversed()
            filteredIncludedAndExcluded =
                filteredIncludedAndExcluded.reversed()
        }
    }


    private fun sortByPopularity(
        newState: TagSelectorState,
        tagsOfSelectedResources: List<TagItem>,
        tagsOfUnselectedResources: List<TagItem>,
        tagItemsByResources: Map<ResourceId, Set<TagItem>>
    ) = newState.apply {
        val tagsOfSelectedResPopularity = Popularity
            .calculate(tagsOfSelectedResources)

        val tagsOfUnselectedResPopularity = Popularity
            .calculate(tagsOfUnselectedResources)

        val tagsPopularity = Popularity
            .calculate(tagItemsByResources.values.flatten())
        availableTagItems = availableTagItems.sortedBy {
            tagsOfSelectedResPopularity[it]
        }
        unavailableTagItems = unavailableTagItems.sortedBy {
            tagsOfUnselectedResPopularity[it]
        }

        filteredIncludedAndExcluded = (includedTagItems + excludedTagItems)
            .sortedBy { tagsPopularity[it] }
    }

    private fun sortByStatsCriteria(
        newState: TagSelectorState
    ) = newState.apply {
        val sortCriteria = tagSortCriteria(sorting)

        val kinds = Kind.values().associateBy { it.name }
        val tagItemsToCriteria = sortCriteria.map { (tag, criteria) ->
            val item = if (kinds.containsKey(tag))
                TagItem.KindTagItem(kinds[tag]!!)
            else
                TagItem.PlainTagItem(tag)
            item to criteria
        }.toMap()
        availableTagItems = availableTagItems.sortedBy {
            tagItemsToCriteria[it]
        }

        unavailableTagItems = unavailableTagItems.sortedBy {
            tagItemsToCriteria[it]
        }

        val includedAnExcluded = includedTagItems + excludedTagItems
        filteredIncludedAndExcluded = includedAnExcluded.sortedBy {
            tagItemsToCriteria[it]
        }
    }

    private fun provideTagItemsByResources(
        state: TagSelectorState
    ): Map<ResourceId, Set<TagItem>> {
        val resources = index.allIds()
        val tagItemsByResources: Map<ResourceId, Set<TagItem>> =
            tagsStorage.groupTagsByResources(resources).map {
                it.key to it.value.map { tag -> TagItem.PlainTagItem(tag) }.toSet()
            }.toMap()

        if (!state.showKindTags) return tagItemsByResources

        return tagItemsByResources.map { (id, tags) ->
            val _tags = tags.toMutableSet()
            metadataStorage
                .retrieve(id)
                .onSuccess {
                    _tags.add(TagItem.KindTagItem(it.kind))
                }
            id to _tags
        }.toMap()
    }

    private suspend fun findLastActualAction(newState: TagSelectorState): TagsSelectorAction? {
        val tagItems = provideTagItemsByResources(newState).values.flatten().toSet()

        while (newState.actionsHistory.lastOrNull() != null) {
            val lastAction = newState.actionsHistory.last()
            if (isActionActual(lastAction, tagItems)) {
                return lastAction
            } else
                newState.actionsHistory.removeLast()
        }

        return null
    }

    private fun isActionActual(
        actions: TagsSelectorAction,
        allTagItems: Set<TagItem>
    ): Boolean = when (actions) {
        is TagsSelectorAction.Exclude -> allTagItems.contains(actions.item)
        is TagsSelectorAction.Include -> allTagItems.contains(actions.item)
        is TagsSelectorAction.UncheckAndExclude -> allTagItems.contains(actions.item)
        is TagsSelectorAction.UncheckExcluded -> allTagItems.contains(actions.item)
        is TagsSelectorAction.UncheckIncluded -> allTagItems.contains(actions.item)
        is TagsSelectorAction.Clear -> {
            actions.excluded.intersect(allTagItems).isNotEmpty() ||
                    actions.included.intersect(allTagItems).isNotEmpty()
        }
    }
}

const val TAGS_SELECTOR: String = "tags-selector"
