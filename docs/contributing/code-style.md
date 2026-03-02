# Code Style Guide

Code style guidelines for DevView.

> _[Placeholder: Insert screenshot or diagram of code style checks in action. Use a device frame if relevant.]_

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
- Run ktlint and detekt before submitting changes
```bash
./gradlew ktlintCheck
./gradlew detekt
```
- Fix all reported issues before opening a pull request

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

## Next Steps
- See [Development Setup](development.md) for environment configuration
- Review [Pull Requests](pull-requests.md) for contribution process

---

_If you have questions about code style, open an issue or start a discussion on GitHub._
