# DevView UI components

Use `com.worldline.devview.ui` for custom-module controls that match the built-in modules.
The kit inherits `MaterialTheme` colours, typography and shapes; it owns layout and interaction patterns.

## Section headings and grouped items

`DevViewSectionHeader(text, modifier)` shows a sentence-case group name. Supply human-readable text,
not an uppercase enum name. `Modifier.devViewGroupedItem(index, count)` clips and fills each item using
large outer corners and 4dp inner corners. Apply it before the click/selection modifier to clip the ripple.
Use `DevViewGroupedDivider(modifier)` between rows and inset it to the text column.

```kotlin
Column {
    DevViewSectionHeader(text = "Available responses")
    responses.forEachIndexed { index, response ->
        Text(
            text = response.name,
            modifier = Modifier
                .devViewGroupedItem(index = index, count = responses.size)
                .clickable { select(response) }
                .padding(16.dp)
        )
    }
}
```

## Pinned search and filter panel

`DevViewFilterBar` keeps search visible while filters are collapsed. The badge counts non-search filters;
its toggle announces the count. State belongs to the caller: save it with `rememberSaveable` or keep it
in an entry-scoped ViewModel. The Clear action resets panel filters; query reset is a separate caller decision.
Set `clearFiltersEnabled = false` while a reset cannot complete, such as during preference loading,
a pending save or a persistence error. The default is `true`; the action also requires an active filter.

```kotlin
Scaffold(
    modifier = Modifier.imePadding(),
    bottomBar = {
        DevViewFilterBar(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Search events",
            activeFilterCount = selectedCategories.size,
            expanded = expanded,
            onExpandedChange = { expanded = it },
            onClearFilters = { selectedCategories = emptyList() },
            bottomPadding = bottomPadding
        ) {
            DevViewSectionHeader(text = "Categories")
            // Your module's filter chips.
        }
    }
) { padding ->
    EventList(modifier = Modifier.padding(padding))
}
```

Pass the `bottomPadding` received by `Module.registerContent`, and handle IME insets around the bar.
Expanded controls scroll vertically within a bounded panel so the search field stays reachable.

## Empty states

`DevViewEmptyState(icon, title, message, modifier, action)` explains why content is absent and optionally
supplies a recovery control. Distinguish an empty collection from an active filter with no matches.
The icon is decorative; the title and message carry its meaning.

```kotlin
DevViewEmptyState(
    icon = Icons.Rounded.Search,
    title = "No matching events",
    message = "Try another query or clear the filters.",
    action = { TextButton(onClick = ::clearFilters) { Text("Clear filters") } }
)
```

## Copying text

`rememberCopyToClipboard()` returns a `(String) -> Unit` callback for plain text on Android and iOS.
It uses the Compose clipboard asynchronously; leaving the calling composition cancels pending copies.
Platform clipboard types stay internal to DevView.

```kotlin
val copy = rememberCopyToClipboard()
TextButton(onClick = { copy(body) }) { Text("Copy body") }
```

For home-screen tiles and explicit colour overrides, see [Theming](theming.md).
