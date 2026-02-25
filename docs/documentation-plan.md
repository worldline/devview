# DevView Documentation Overhaul Plan

_Last updated: February 25, 2026_

## 1. Audience & Tone
- **Audience:** Developers integrating DevView into their Kotlin Multiplatform (KMP) projects.
- **Tone:** Professional, concise, and developer-friendly. Neutral branding, but with a clear, modern, and slightly opinionated identity (consistent use of callouts, icons, and code highlighting). Use British English and an experienced developer tone—clear, to the point, and detailed when necessary. Documentation should be accessible to junior developers, explaining intermediate or complex concepts as needed.

## 2. Visuals & Media
- **Screenshots:** Add Compose UI screenshots where relevant (e.g., FeatureFlip UI, Analytics UI). Use device frames where it adds clarity, but keep minimal if not needed. If screenshots are not available, add placeholders with descriptions so they can be added later.
- **No onboarding videos or animations**—focus on clear written guides, diagrams, and screenshots.

## 3. Branding & Style
- **Neutral, modern, and accessible:** Use the mkdocs-material theme (already configured), with a teal accent.
- **Identity:** Use consistent callouts (e.g., “Tip”, “Warning”, “Note”), icons, and code formatting. Consider a subtle motif (e.g., developer tools, modularity) in section headers or callouts.

## 4. API Reference
- **Dokka-generated docs:** Link to or embed Dokka HTML output for API reference. No need for manual API docs for now.
- **Navigation:** Ensure API docs are discoverable from the main docs.

## 5. Examples & Integration Scenarios
- **Sample app:** Reference the sample/ directory for full working examples.
- **Examples:** Provide practical, real-world code snippets for each module (FeatureFlip, Analytics, NetworkMock), including advanced usage and troubleshooting. Use complete code snippets where possible; partial snippets only when necessary.
- **Analytics:** Show how to use the Analytics module with a generic analytics provider.
- **NetworkMock:** Focus on Ktor integration, but mention extensibility for future providers.

## 6. Localization
- **English primary:** If mkdocs-material’s i18n features are easy to enable, set up the structure for future translations, but keep content in English for now.

## 7. Structure

```
Home
  - Overview, key features, screenshots, badges, What's New/Release Highlights
Getting Started
  - Prerequisites
  - Installation (KMP, Android, iOS)
  - Quick Start (full code, step-by-step)
  - Configuration (advanced options, edge cases)
  - Troubleshooting & FAQ
Modules
  - Overview (architecture diagram, module relationships)
  - FeatureFlip (usage, UI screenshots, advanced scenarios)
  - Analytics (usage, event tracking, integration tips)
  - NetworkMock (usage, Ktor plugin, UI previews)
  - Creating Custom Modules (step-by-step, best practices)
Guides
  - Integration Guide (end-to-end, platform-specific)
  - Module Development (custom modules, navigation, theming)
  - Navigation (deep links, type-safe navigation)
  - Theming (customising UI, dark mode, branding)
  - Best Practices (patterns, anti-patterns, performance tips)
  - Common Pitfalls & Solutions
API Reference
  - Link/embed Dokka HTML output
Examples
  - Platform Setup (Android, iOS, KMP)
  - Feature Examples (flags, analytics, network mocking)
  - Advanced Examples (custom modules, integration)
  - Sample Projects (reference sample/ directory)
Contributing
  - How to contribute
  - Development setup
  - Code style
  - Pull request guidelines
  - Code of conduct
Changelog & Licence
```

## 8. Add-ons & Enhancements

### Already in Use
- **mkdocs-material**: Modern, responsive UI, tabs, callouts, code highlighting.
- **pymdownx** extensions: For code, tabs, details, mermaid diagrams, etc.
- **search, tags, mike, social**: For navigation, versioning, and sharing.

### Additional Recommendations
- **mkdocs-glightbox**: For image lightboxes/screenshots. _Why:_ Allows users to click screenshots and see them enlarged, improving UI documentation.
- **mkdocs-awesome-pages-plugin**: For custom navigation and ordering if navigation grows more complex.
- **Localization/i18n**: Enable mkdocs-material i18n structure if easy, for future translation support.

### Not Included
- **Onboarding videos/animations**: Not relevant for this library’s context.
- **Live demos/playgrounds**: Not relevant; sample app is sufficient.
- **Migration Guide**: Not needed until a major version change occurs.

## 9. Implementation Plan

1. **Content Audit & Gap Analysis**
   - Review all current docs for missing sections, incomplete examples, and unclear explanations.
   - Identify where screenshots, diagrams, or callouts would add value.

2. **Content Refactoring & Expansion**
   - Fill gaps in module docs (add UI screenshots, advanced usage, troubleshooting).
   - Expand Getting Started with more detailed, step-by-step integration flows.
   - Add a Troubleshooting & FAQ section.
   - Add advanced examples and reference the sample app.

3. **Navigation & Structure**
   - Update mkdocs.yml to match the new structure.
   - Use navigation features (tabs, sticky nav, etc.) for better UX.

4. **Visual Enhancements**
   - Add screenshots for UI modules (use placeholders with descriptions if screenshots are not available).
   - Add or improve diagrams (architecture, module relationships).
   - Use callouts and icons for tips, warnings, and notes.

5. **API Reference Integration**
   - Ensure Dokka HTML output is generated and linked from the API Reference section.

6. **Localization Prep**
   - If feasible, set up the structure for future translations (e.g., docs/en/, docs/fr/).

7. **Testing & Review**
   - Build the docs locally, review for clarity, completeness, and navigation.
   - Polish language, formatting, and accessibility.

---

## Notes on Add-ons

The previously suggested add-ons not listed above (e.g., mkdocs-mermaid2, mkdocs-git-revision-date-localized-plugin) are either already covered by pymdownx extensions or not strictly necessary for the current scope. They can be revisited if the documentation grows in complexity or if specific needs arise (e.g., revision tracking, more advanced diagrams).

---

## To Do
- [ ] Complete content audit
- [ ] Expand and refactor docs as per plan
- [ ] Add screenshots and diagrams (use placeholders where needed)
- [ ] Update mkdocs.yml navigation
- [ ] Integrate Dokka API docs
- [ ] Prepare for localisation (if feasible)
- [ ] Review and polish

---

## Process & Workflow

1. **Section-by-Section, File-by-File Approach**
   - Work through each section (e.g., Home, Getting Started, etc.) one at a time.
   - Within each section, handle files one by one.
   - After each file is completed, pause for review and approval.
   - Once all files in a section are done, conduct a global review for that section.

2. **Review & Approval**
   - Provide a summary of changes for each file.
   - User may review the file in detail if desired.

3. **Task Tracking**
   - Maintain a docs/docs-tasks.md file to track progress and outstanding tasks.

---

This plan should be updated as the project evolves.
