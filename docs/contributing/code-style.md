# Code Style Guide

Code style guidelines for DevView.

## Formatting
- Use Kotlin's official style guide
- Indent with 4 spaces
- Limit lines to 120 characters
- Use blank lines to separate logical sections

## Naming Conventions
- Use camelCase for variables and functions
- Use PascalCase for classes and objects
- Use UPPER_SNAKE_CASE for constants
- Prefix private fields with an underscore if needed
- Use descriptive names and be consistent

## Linting & Static Analysis

Detekt (with ktlint bundled) enforces style automatically. Run the full analysis across all modules:

=== "Windows"

    ```bat
    .\gradlew.bat detektFull
    ```

=== "macOS / Linux"

    ```bash
    ./gradlew detektFull
    ```

The pre-commit hook runs this automatically on every `git commit`, so issues are caught before they reach CI. See [Development Setup](development.md) for hook installation.

## Documentation
- Use KDoc for all public classes, functions, and properties
- Document module interfaces and key architectural decisions
- Add comments for complex logic and non-obvious code
- Include code examples and use proper formatting

## Best Practices
- Write clear, descriptive commit messages
- Refactor code for readability and maintainability
- Remove unused code and imports
- Prefer immutable data structures

## Compose List Keys

Every `LazyColumn` or `LazyRow` with a `key` argument must follow these rules:

1. **Unique within the list** — the key must be distinct for every item visible simultaneously. `hashCode()` and `timestamp` are not safe (`hashCode` can collide; millisecond timestamps are not unique under rapid logging). Use a domain identifier: a stable name, a composite key, a database ID.
2. **Stable under state changes** — the key must not change when the item's mutable state changes. Keying a feature on `feature.hashCode()` changes when `isEnabled` changes; `feature.name` does not.
3. **Semantically meaningful** — the key should communicate what uniquely identifies the item, not an implementation detail.

**Rule of thumb:** if the key field doubles as a DataStore preference key, database primary key, or URL path segment, it is the right choice.

Every `LazyColumn` with a `key` argument must have at least one device test that renders two or more items and asserts all items are visible (using `waitUntilTagCount` or `assertIsDisplayed`). This test catches duplicate-key crashes before they reach production.

## Next Steps
- See [Development Setup](development.md) for environment configuration
- Review [Pull Requests](pull-requests.md) for contribution process

---

_If you have questions about code style, open an issue or start a discussion on GitHub._
