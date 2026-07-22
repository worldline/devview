# Pull Request Guidelines

How to submit pull requests to DevView.

## Before You Submit
- Ensure your branch is up to date with main
- Run all tests and code quality checks
- Review your changes for clarity and completeness
- Update documentation if needed

## PR Title

PR titles **must** follow `type: description` format — this is enforced by CI (`semantic-title` job):

```
feat: add network mock response editor
fix: correct overlay back navigation
docs: update installation guide
refactor: remove dead endpoint selection state
```

Valid types: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `ci`, `perf`, `build`, `revert`.

GitHub auto-suggests a title from the branch name — always verify it matches this format before opening the PR.

## Opening a Pull Request
- Use a clear, descriptive title (see above) and summary
- Reference related issues or discussions
- Assign reviewers if possible
- Mark as draft if not ready for review

## Process
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Write/update tests
5. Update documentation
6. Run code quality checks
7. Submit pull request

## Branch Naming
- feature/description - New features
- fix/description - Bug fixes
- docs/description - Documentation
- refactor/description - Refactoring

## Commit Messages

Individual commits use **gitmoji** format: `:emoji: message`

```
:sparkles: Add network mock response editor
:bug: Fix overlay back navigation
:memo: Update installation guide
:fire: Remove dead endpoint selection code
```

Note: PRs are squash-merged using the PR title (`type: description`) as the final commit message. The gitmoji format is for individual commits on your branch.

## Checklist
- [ ] Tests pass
- [ ] Detekt passes
- [ ] Documentation updated
- [ ] CHANGELOG updated
- [ ] Any `LazyColumn`/`LazyRow` with a `key` argument has a device test rendering 2+ items (see [Code Style](code-style.md#compose-list-keys))

## Review Process
- PRs are reviewed by maintainers. Be responsive to feedback.
- Respond to feedback promptly and constructively
- Make requested changes and push updates
- Resolve merge conflicts as needed

## After Approval
- Squash and merge if appropriate
- Delete your feature branch after merging
- Celebrate your contribution!

## Next Steps
- See [Code Style](code-style.md) for formatting and linting rules
- Review [Development Setup](development.md) for environment configuration
- See [Code of Conduct](code-of-conduct.md) for community standards

---

_If you have questions about pull requests, open an issue or start a discussion on GitHub._
