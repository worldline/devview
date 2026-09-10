#!/usr/bin/env python3

import os
import sys
import re
import shutil
import subprocess
from pathlib import Path
from datetime import date

GRADLEW = ".\\gradlew.bat" if os.name == "nt" else "./gradlew"


def run(cmd: list[str], check: bool = True) -> None:
    print(f"  Running: {' '.join(cmd)}")
    subprocess.run(cmd, check=check)


def get_property(key: str, file: Path) -> str:
    for line in file.read_text(encoding="utf-8").splitlines():
        if line.startswith(f"{key}="):
            return line.split("=", 1)[1].strip()
    raise ValueError(f"Property {key} not found in {file}")


def set_property(key: str, value: str, file: Path) -> None:
    content = file.read_text(encoding="utf-8")
    content = re.sub(rf"^{re.escape(key)}=.*$", f"{key}={value}", content, flags=re.MULTILINE)
    file.write_text(content, encoding="utf-8", newline="\n")


def find_published_api_files() -> list[Path]:
    api_files = []
    for api_txt in Path(".").rglob("api/api.txt"):
        module_dir = api_txt.parent.parent
        props_file = module_dir / "gradle.properties"
        if props_file.exists():
            content = props_file.read_text(encoding="utf-8")
            if re.search(r"^POM_ARTIFACT_ID=", content, re.MULTILINE):
                api_files.append(api_txt)
                print(f"  Found published module: {module_dir}")
            else:
                print(f"  Skipping unpublished module: {module_dir}")
    return sorted(api_files)


def _unreleased_body_start(lines: list[str], unreleased_index: int) -> int:
    """Index of the first non-blank line under '## [Unreleased]', or len(lines) if none."""
    body_index = unreleased_index + 1
    while body_index < len(lines) and not lines[body_index].strip():
        body_index += 1
    return body_index


def extract_changelog_section(version: str, changelog: Path) -> str:
    content = changelog.read_text(encoding="utf-8")
    start = content.find(f"## [{version}]")
    if start == -1:
        return ""

    next_section = content.find("\n## [", start + 1)
    link_match = re.compile(r"^\[[^\]\n]+\]: ", re.MULTILINE).search(content, start + 1)
    stops = [i for i in (next_section, link_match.start() if link_match else -1) if i != -1]
    end = min(stops) if stops else -1
    section = content[start:end].strip() if end != -1 else content[start:].strip()

    # Drop the release header line so docs/index.md can render the section body only.
    lines = section.splitlines()[1:]
    return "\n".join(lines).strip()


def update_whats_new(version: str, changelog_section: str, index: Path) -> None:
    content = index.read_text(encoding="utf-8")
    start = content.find("## What's New\n")
    if start == -1:
        raise ValueError(f"{index} missing '## What's New' anchor")

    end = content.find("\n---\n", start)
    if end == -1:
        raise ValueError(f"{index} missing '---' terminator after What's New")

    # Convert Keep a Changelog sub-sections into a compact docs home summary. Bullets are
    # hard-wrapped in CHANGELOG.md, so continuation lines get folded back into the bullet
    # they belong to.
    lines: list[str] = []
    for line in changelog_section.splitlines():
        line = re.sub(r"\]\(docs/", "](", line)  # CHANGELOG.md links are repo-root relative
        if line.startswith("### "):
            lines.append(f"\n**{line[4:]}**\n")
        elif line.startswith("- "):
            lines.append(line)
        elif line.strip() and lines and lines[-1].startswith("- "):
            lines[-1] += " " + line.strip()

    new_section = f"## What's New\n\n### v{version}\n\n" + "\n".join(lines).strip()
    # A blank line must precede the "---" terminator, or Markdown reads the preceding
    # paragraph/list-item text as a Setext heading underline instead of a section break.
    content = content[:start] + new_section + "\n" + content[end:]
    index.write_text(content, encoding="utf-8", newline="\n")


def update_changelog(version: str, changelog: Path) -> None:
    content = changelog.read_text(encoding="utf-8")
    today = date.today().strftime("%Y-%m-%d")
    ends_with_newline = content.endswith("\n")

    if "## [Unreleased]" not in content:
        raise ValueError("CHANGELOG.md missing '## [Unreleased]' section")
    if "[Unreleased]:" not in content:
        raise ValueError("CHANGELOG.md missing [Unreleased] reference link")

    release_header = f"## [{version}] - {today}"

    lines = content.splitlines()
    try:
        unreleased_index = lines.index("## [Unreleased]")
    except ValueError as exc:
        raise ValueError("CHANGELOG.md malformed") from exc

    body_index = _unreleased_body_start(lines, unreleased_index)
    rebuilt_lines = lines[: unreleased_index + 1] + ["", release_header, ""] + lines[body_index:]
    content = "\n".join(rebuilt_lines)
    if ends_with_newline:
        content += "\n"

    # Pull the current version from the existing [Unreleased] comparison link and rewrite
    # the bottom reference block so the new release sits between the previous tag and HEAD.
    # Tags in this repo are bare (0.1.5, not v0.1.5) -- `v?` tolerates the one legacy v0.1.0 link.
    link_re = re.compile(
        r"^\[Unreleased\]: (https://github\.com/[^/]+/[^/]+)/compare/v?(.+?)\.\.\.HEAD$",
        re.MULTILINE,
    )
    match = link_re.search(content)
    if not match:
        raise ValueError("CHANGELOG.md missing compare link for [Unreleased]")

    base_url = match.group(1)
    prev_version = match.group(2)

    rewritten_links = link_re.sub(
        f"[Unreleased]: {base_url}/compare/{version}...HEAD\n"
        f"[{version}]: {base_url}/compare/{prev_version}...{version}",
        content,
        count=1,
    )

    changelog.write_text(rewritten_links, encoding="utf-8", newline="\n")


def preflight(new_version: str, changelog: Path) -> None:
    """Everything checked here must pass before Step 1 mutates anything -- pushing the tag
    in Step 6 fires publish.yml and is effectively irreversible."""
    branch = subprocess.run(
        ["git", "rev-parse", "--abbrev-ref", "HEAD"], capture_output=True, text=True, check=True,
    ).stdout.strip()
    if branch != "main":
        print(f"Error: Must release from 'main', currently on '{branch}'")
        sys.exit(1)

    status = subprocess.run(
        ["git", "status", "--porcelain"], capture_output=True, text=True, check=True,
    ).stdout
    if status.strip():
        print("Error: Working tree has uncommitted or staged changes")
        sys.exit(1)

    run(["git", "fetch", "origin", "main", "--tags"])
    local_head = subprocess.run(
        ["git", "rev-parse", "HEAD"], capture_output=True, text=True, check=True,
    ).stdout.strip()
    remote_head = subprocess.run(
        ["git", "rev-parse", "origin/main"], capture_output=True, text=True, check=True,
    ).stdout.strip()
    if local_head != remote_head:
        print("Error: Local 'main' is not up to date with 'origin/main'")
        sys.exit(1)

    tag_exists = subprocess.run(
        ["git", "rev-parse", "-q", "--verify", f"refs/tags/{new_version}"], capture_output=True,
    ).returncode == 0
    if tag_exists:
        print(f"Error: Tag {new_version} already exists")
        sys.exit(1)

    content = changelog.read_text(encoding="utf-8")
    if "## [Unreleased]" not in content:
        print("Error: CHANGELOG.md missing '## [Unreleased]' section")
        sys.exit(1)
    if "[Unreleased]:" not in content:
        print("Error: CHANGELOG.md missing [Unreleased] reference link")
        sys.exit(1)

    lines = content.splitlines()
    unreleased_index = lines.index("## [Unreleased]")
    body_index = _unreleased_body_start(lines, unreleased_index)
    if body_index >= len(lines) or lines[body_index].startswith(("## [", "[")):
        print("Error: '## [Unreleased]' section is empty -- nothing to release")
        sys.exit(1)


def main():
    if len(sys.argv) < 2:
        print("Usage: release.py <release-version> [next-snapshot-version]")
        print("  e.g. python3 release.py 2.0.0")
        print("  e.g. python3 release.py 2.0.0 2.1.0-SNAPSHOT")
        sys.exit(1)

    new_version = sys.argv[1]
    gradle_props = Path("gradle.properties")
    changelog = Path("CHANGELOG.md")

    cur_snapshot = get_property("VERSION_NAME", gradle_props)

    if not cur_snapshot.endswith("-SNAPSHOT"):
        print(f"Error: Current VERSION_NAME ({cur_snapshot}) is not a -SNAPSHOT version")
        sys.exit(1)

    if new_version.endswith("-SNAPSHOT"):
        print(f"Error: Release version ({new_version}) must not be a -SNAPSHOT version")
        sys.exit(1)

    if len(sys.argv) >= 3:
        next_snapshot = sys.argv[2]
    elif "-" in new_version:
        # Pre-release (e.g. 0.3.0-alpha01): stay on the same base version so the next
        # alpha/beta/final can still be cut from the same slot.
        next_snapshot = f"{new_version.split('-', 1)[0]}-SNAPSHOT"
    else:
        parts = new_version.split(".")
        try:
            major, minor, patch = parts
            next_snapshot = f"{major}.{minor}.{int(patch) + 1}-SNAPSHOT"
        except ValueError:
            print(f"Error: Cannot infer next snapshot from '{new_version}'; pass it explicitly.")
            sys.exit(1)

    print("=" * 50)
    print(f"  Releasing:     {new_version}")
    print(f"  From:          {cur_snapshot}")
    print(f"  Next snapshot: {next_snapshot}")
    print("=" * 50)

    print("\nPreflight checks...")
    preflight(new_version, changelog)

    print("\nStep 1: Bumping version in gradle.properties...")
    set_property("VERSION_NAME", new_version, gradle_props)

    print("\nStep 2: Regenerating api.txt...")
    run([GRADLEW, "metalavaGenerateSignature", "-Pandroidx.baselineprofile.skipgeneration"])

    print("\nStep 3: Copying api.txt snapshots for published modules...")
    api_files = find_published_api_files()

    if not api_files:
        print("  Warning: No published modules with api.txt found")

    for api_txt in api_files:
        version_txt = api_txt.parent / f"{new_version}.txt"
        shutil.copy2(api_txt, version_txt)
        print(f"  Copied {api_txt} -> {version_txt}")

    print("\nStep 4: Updating CHANGELOG.md and docs/index.md...")
    update_changelog(new_version, changelog)

    docs_index = Path("docs/index.md")
    section = extract_changelog_section(new_version, changelog)
    if section:
        update_whats_new(new_version, section, docs_index)
    else:
        print("  Warning: Could not find changelog section for this version")

    print("\nStep 5: Committing and tagging...")
    run(["git", "add", str(gradle_props), str(changelog), str(docs_index)])
    for api_txt in api_files:
        version_txt = api_txt.parent / f"{new_version}.txt"
        run(["git", "add", str(version_txt)])

    run(["git", "commit", "-m", f"Prepare for release {new_version}"])
    run(["git", "tag", new_version])

    print("\nStep 6: Pushing release commit and tag...")
    run(["git", "push", "--atomic", "origin", "HEAD:refs/heads/main", new_version])

    print(f"\nStep 7: Setting next snapshot version ({next_snapshot})...")
    set_property("VERSION_NAME", next_snapshot, gradle_props)
    run(["git", "add", str(gradle_props)])
    run(["git", "commit", "-m", "Prepare next development version"])
    run(["git", "push"])

    print("\n" + "=" * 50)
    print(f"  Release {new_version} complete!")
    print(f"  Next snapshot: {next_snapshot}")
    print("=" * 50)


if __name__ == "__main__":
    main()
