package ru.appforsale.alicecommands.api.application.read

import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.ChecklistItem
import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.CommandGroup
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.EntityDeltaSection
import ru.appforsale.alicecommands.api.domain.ScenarioTemplate

object ContentBundleDiffer {

    fun diffCategories(base: List<Category>, incoming: List<Category>): EntityDeltaSection<Category> =
        diffEntities(base, incoming) { it.id }

    fun diffCommandGroups(base: List<CommandGroup>, incoming: List<CommandGroup>): EntityDeltaSection<CommandGroup> =
        diffEntities(base, incoming) { it.id }

    fun diffCommands(base: List<Command>, incoming: List<Command>): EntityDeltaSection<Command> =
        diffEntities(base, incoming) { it.id }

    fun diffScenarios(base: List<ScenarioTemplate>, incoming: List<ScenarioTemplate>): EntityDeltaSection<ScenarioTemplate> =
        diffEntities(base, incoming) { it.id }

    fun diffChecklist(base: List<ChecklistItem>, incoming: List<ChecklistItem>): EntityDeltaSection<ChecklistItem> =
        diffEntities(base, incoming) { it.id }

    fun diffCommandOfDay(
        base: ru.appforsale.alicecommands.api.domain.CommandOfDay?,
        incoming: ru.appforsale.alicecommands.api.domain.CommandOfDay?,
    ): ru.appforsale.alicecommands.api.domain.CommandOfDay? =
        if (base == incoming) null else incoming

    private fun <T> diffEntities(
        base: List<T>,
        incoming: List<T>,
        idOf: (T) -> String,
    ): EntityDeltaSection<T> {
        val baseMap = base.associateBy(idOf)
        val incomingMap = incoming.associateBy(idOf)
        val added = mutableListOf<T>()
        val updated = mutableListOf<T>()
        val removed = mutableListOf<String>()

        incomingMap.forEach { (id, item) ->
            val old = baseMap[id]
            when {
                old == null -> added += item
                old != item -> updated += item
            }
        }
        baseMap.keys.filter { it !in incomingMap }.forEach { removed += it }

        return EntityDeltaSection(added, updated, removed)
    }
}
