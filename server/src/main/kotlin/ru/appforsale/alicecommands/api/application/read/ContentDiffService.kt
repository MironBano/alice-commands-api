package ru.appforsale.alicecommands.api.application.read

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.ChecklistItem
import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.CommandGroup
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.ScenarioTemplate
import ru.appforsale.alicecommands.api.domain.ports.BundleStorage
import ru.appforsale.alicecommands.api.domain.ports.DraftRepository
import ru.appforsale.alicecommands.api.domain.ports.ManifestRepository
import ru.appforsale.alicecommands.api.domain.ports.SchemaValidator

@Serializable
data class FieldDiff(
    val old: String? = null,
    val new: String? = null,
)

@Serializable
data class EntityDiff(
    val id: String,
    val change: String,
    val field_diffs: Map<String, FieldDiff> = emptyMap(),
    val title_ru: String? = null,
    val category_id: String? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
data class DiffSummary(
    val added: Int = 0,
    val removed: Int = 0,
    val changed: Int = 0,
    val unchanged: Int = 0,
)

@Serializable
data class EntityDiffSection(
    val summary: DiffSummary,
    val items: List<EntityDiff>,
)

@Serializable
data class ContentDiffResponse(
    val base: String,
    val base_content_version: Int? = null,
    val summary: DiffSummary,
    val categories: EntityDiffSection,
    val command_groups: EntityDiffSection,
    val commands: EntityDiffSection,
    val scenario_templates: EntityDiffSection,
    val checklist_items: EntityDiffSection,
)

class ContentDiffService(
    private val draftRepository: DraftRepository,
    private val manifestRepository: ManifestRepository,
    private val bundleStorage: BundleStorage,
    private val schemaValidator: SchemaValidator,
) {
    fun previewImport(jsonText: String): ContentDiffResponse {
        schemaValidator.validateJson(jsonText)
        val incoming = BundleCodec.json.decodeFromString<ContentBundle>(jsonText)
        val published = loadPublishedBundle()
        return diffBundles(published.first, published.second, incoming)
    }

    fun draftVsPublished(): ContentDiffResponse {
        val published = loadPublishedBundle()
        val draft = draftRepository.loadFull()
        return diffBundles(published.first, published.second, draft)
    }

    private fun loadPublishedBundle(): Pair<String, ContentBundle> {
        val current = manifestRepository.getCurrent()
        if (current == null) {
            return "empty" to emptyBundle()
        }
        val bytes = bundleStorage.read(current.bundlePath)
            ?: return "empty" to emptyBundle()
        val bundle = BundleCodec.json.decodeFromString<ContentBundle>(BundleCodec.gunzip(bytes))
        return "published_v${current.contentVersion}" to bundle
    }

    private fun emptyBundle(): ContentBundle = ContentBundle(
        published_at = "1970-01-01T00:00:00Z",
        categories = emptyList(),
        command_groups = emptyList(),
        commands = emptyList(),
        scenario_templates = emptyList(),
        checklist_items = emptyList(),
    )

    private fun diffBundles(baseLabel: String, base: ContentBundle, incoming: ContentBundle): ContentDiffResponse {
        val categories = diffCategories(base.categories, incoming.categories)
        val commandGroups = diffCommandGroups(base.command_groups, incoming.command_groups)
        val commands = diffCommands(base.commands, incoming.commands)
        val scenarios = diffScenarios(base.scenario_templates, incoming.scenario_templates)
        val checklist = diffChecklist(base.checklist_items, incoming.checklist_items)

        val summary = DiffSummary(
            added = categories.summary.added + commandGroups.summary.added + commands.summary.added +
                scenarios.summary.added + checklist.summary.added,
            removed = categories.summary.removed + commandGroups.summary.removed + commands.summary.removed +
                scenarios.summary.removed + checklist.summary.removed,
            changed = categories.summary.changed + commandGroups.summary.changed + commands.summary.changed +
                scenarios.summary.changed + checklist.summary.changed,
            unchanged = categories.summary.unchanged + commandGroups.summary.unchanged + commands.summary.unchanged +
                scenarios.summary.unchanged + checklist.summary.unchanged,
        )

        return ContentDiffResponse(
            base = baseLabel,
            base_content_version = manifestRepository.getCurrent()?.contentVersion,
            summary = summary,
            categories = categories,
            command_groups = commandGroups,
            commands = commands,
            scenario_templates = scenarios,
            checklist_items = checklist,
        )
    }

    private fun diffCategories(base: List<Category>, incoming: List<Category>): EntityDiffSection =
        diffEntities(
            base.associateBy { it.id },
            incoming.associateBy { it.id },
            fieldDiffs = { old, new ->
                mapDiffs(
                    "title_ru" to (old?.title_ru to new?.title_ru),
                    "title_kk" to (old?.title_kk to new?.title_kk),
                    "sort_order" to (old?.sort_order?.toString() to new?.sort_order?.toString()),
                    "source_url" to (old?.source_url to new?.source_url),
                    "description_ru" to (old?.description_ru to new?.description_ru),
                    "featured" to (old?.featured?.toString() to new?.featured?.toString()),
                    "icon_key" to (old?.icon_key to new?.icon_key),
                    "icon_url" to (old?.icon_url to new?.icon_url),
                    "accent_color" to (old?.accent_color to new?.accent_color),
                    "accent_color_dark" to (old?.accent_color_dark to new?.accent_color_dark),
                    "device_types" to (old?.device_types?.sorted()?.toString() to new?.device_types?.sorted()?.toString()),
                )
            },
            toEntity = { id, change, diffs, item -> EntityDiff(id, change, diffs, item?.title_ru) },
        )

    private fun diffCommandGroups(base: List<CommandGroup>, incoming: List<CommandGroup>): EntityDiffSection =
        diffEntities(
            base.associateBy { it.id },
            incoming.associateBy { it.id },
            fieldDiffs = { old, new ->
                mapDiffs(
                    "title_ru" to (old?.title_ru to new?.title_ru),
                    "category_id" to (old?.category_id to new?.category_id),
                    "sort_order" to (old?.sort_order?.toString() to new?.sort_order?.toString()),
                    "description_ru" to (old?.description_ru to new?.description_ru),
                    "icon_key" to (old?.icon_key to new?.icon_key),
                    "icon_url" to (old?.icon_url to new?.icon_url),
                    "accent_color" to (old?.accent_color to new?.accent_color),
                    "accent_color_dark" to (old?.accent_color_dark to new?.accent_color_dark),
                    "featured" to (old?.featured?.toString() to new?.featured?.toString()),
                    "preview_command_ids" to (old?.preview_command_ids?.sorted()?.toString() to new?.preview_command_ids?.sorted()?.toString()),
                )
            },
            toEntity = { id, change, diffs, item -> EntityDiff(id, change, diffs, item?.title_ru, item?.category_id) },
        )

    private fun diffCommands(base: List<Command>, incoming: List<Command>): EntityDiffSection =
        diffEntities(
            base.associateBy { it.id },
            incoming.associateBy { it.id },
            fieldDiffs = { old, new ->
                mapDiffs(
                    "title_ru" to (old?.title_ru to new?.title_ru),
                    "category_id" to (old?.category_id to new?.category_id),
                    "phrases" to (old?.phrases?.sorted()?.toString() to new?.phrases?.sorted()?.toString()),
                    "effect_description_ru" to (old?.effect_description_ru to new?.effect_description_ru),
                    "requires_alice_word" to (old?.requires_alice_word?.toString() to new?.requires_alice_word?.toString()),
                    "requires_plus" to (old?.requires_plus?.toString() to new?.requires_plus?.toString()),
                    "device_types" to (old?.device_types?.sorted()?.toString() to new?.device_types?.sorted()?.toString()),
                    "related_command_ids" to (old?.related_command_ids?.sorted()?.toString() to new?.related_command_ids?.sorted()?.toString()),
                    "source_url" to (old?.source_url to new?.source_url),
                    "tags" to (old?.tags?.sorted()?.toString() to new?.tags?.sorted()?.toString()),
                    "group_id" to (old?.group_id to new?.group_id),
                    "sort_order" to (old?.sort_order?.toString() to new?.sort_order?.toString()),
                    "variant_label_ru" to (old?.variant_label_ru to new?.variant_label_ru),
                    "is_primary_in_group" to (old?.is_primary_in_group?.toString() to new?.is_primary_in_group?.toString()),
                    "search_aliases" to (old?.search_aliases?.sorted()?.toString() to new?.search_aliases?.sorted()?.toString()),
                )
            },
            toEntity = { id, change, diffs, item ->
                EntityDiff(id, change, diffs, item?.title_ru, item?.category_id, item?.tags ?: emptyList())
            },
        )

    private fun diffScenarios(base: List<ScenarioTemplate>, incoming: List<ScenarioTemplate>): EntityDiffSection =
        diffEntities(
            base.associateBy { it.id },
            incoming.associateBy { it.id },
            fieldDiffs = { old, new ->
                mapDiffs(
                    "title_ru" to (old?.title_ru to new?.title_ru),
                    "trigger_ru" to (old?.trigger_ru to new?.trigger_ru),
                    "actions_ru" to (old?.actions_ru?.sorted()?.toString() to new?.actions_ru?.sorted()?.toString()),
                    "example_phrases" to (old?.example_phrases?.sorted()?.toString() to new?.example_phrases?.sorted()?.toString()),
                    "audience" to (old?.audience to new?.audience),
                    "deep_link_hint" to (old?.deep_link_hint to new?.deep_link_hint),
                    "source_url" to (old?.source_url to new?.source_url),
                )
            },
            toEntity = { id, change, diffs, item -> EntityDiff(id, change, diffs, item?.title_ru) },
        )

    private fun diffChecklist(base: List<ChecklistItem>, incoming: List<ChecklistItem>): EntityDiffSection =
        diffEntities(
            base.associateBy { it.id },
            incoming.associateBy { it.id },
            fieldDiffs = { old, new ->
                mapDiffs(
                    "order" to (old?.order?.toString() to new?.order?.toString()),
                    "command_id" to (old?.command_id to new?.command_id),
                    "hint_ru" to (old?.hint_ru to new?.hint_ru),
                )
            },
            toEntity = { id, change, diffs, item -> EntityDiff(id, change, diffs, title_ru = item?.command_id) },
        )

    private fun <T> diffEntities(
        baseMap: Map<String, T>,
        incomingMap: Map<String, T>,
        fieldDiffs: (T?, T?) -> Map<String, FieldDiff>,
        toEntity: (String, String, Map<String, FieldDiff>, T?) -> EntityDiff,
    ): EntityDiffSection {
        var added = 0
        var removed = 0
        var changed = 0
        var unchanged = 0
        val items = mutableListOf<EntityDiff>()

        for (id in (baseMap.keys + incomingMap.keys).sorted()) {
            val old = baseMap[id]
            val new = incomingMap[id]
            when {
                old == null && new != null -> {
                    added++
                    items.add(toEntity(id, "added", fieldDiffs(null, new), new))
                }
                old != null && new == null -> {
                    removed++
                    items.add(toEntity(id, "removed", emptyMap(), old))
                }
                old != null && new != null -> {
                    val diffs = fieldDiffs(old, new)
                    if (diffs.isEmpty()) {
                        unchanged++
                    } else {
                        changed++
                        items.add(toEntity(id, "changed", diffs, new))
                    }
                }
            }
        }

        return EntityDiffSection(DiffSummary(added, removed, changed, unchanged), items)
    }

    private fun mapDiffs(vararg pairs: Pair<String, Pair<String?, String?>>): Map<String, FieldDiff> =
        pairs.mapNotNull { (field, values) ->
            val (o, n) = values
            if (o == n) null else field to FieldDiff(old = o, new = n)
        }.toMap()
}
