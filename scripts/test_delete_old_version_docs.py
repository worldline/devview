"""Exercise docs pruning without changing gh-pages or invoking a real mike."""

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).with_name("delete_old_version_docs.sh").resolve()
BASH = shutil.which("bash")
if os.name == "nt":
    git_bash = Path(os.environ["LOCALAPPDATA"]) / "Programs/Git/bin/bash.exe"
    if git_bash.exists():
        BASH = str(git_bash)


@unittest.skipUnless(BASH, "Bash is required")
class DocsPruningTest(unittest.TestCase):
    def prune(self, versions, list_status=0):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "versions").write_text(versions, encoding="utf-8")
            (root / "mike").write_text(
                '#!/bin/bash\n'
                'if [[ "$1" == list ]]; then\n'
                '  cat "$TEST_ROOT/versions"\n'
                '  exit "$LIST_STATUS"\n'
                'fi\n'
                'printf "%s\\n" "$*" >> "$TEST_ROOT/deleted"\n',
                encoding="utf-8",
            )
            (root / "mike").chmod(0o755)
            result = subprocess.run(
                [BASH, "-c", 'if command -v cygpath >/dev/null; then '
                 'TEST_ROOT=$(cygpath -u "$TEST_ROOT"); export TEST_ROOT; fi; '
                 'export PATH="$TEST_ROOT:$PATH"; bash "$TEST_SCRIPT"'],
                env={**os.environ, "TEST_ROOT": root.as_posix(),
                     "TEST_SCRIPT": SCRIPT.as_posix(), "LIST_STATUS": str(list_status)},
                capture_output=True, text=True, check=False,
            )
            deleted = root / "deleted"
            return result, deleted.read_text().splitlines() if deleted.exists() else []

    def test_stable_supersedes_all_prerelease_spellings_in_any_order(self):
        prereleases = ["0.2.0-alpha08", "0.2.0-beta02", "0.2.0-rc01",
                       "0.2.0-RC02", "0.2.0-alpha.1"]
        for stable_first in (True, False):
            with self.subTest(stable_first=stable_first):
                versions = ["0.2.0 [latest]", *prereleases]
                if not stable_first:
                    versions.reverse()
                result, deleted = self.prune("\n".join(["dev", "0.1.5", *versions]))
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual(set(deleted), {f"delete {v} --push" for v in prereleases})

    def test_future_prerelease_and_each_minor_line_survive(self):
        result, deleted = self.prune(
            "dev\n0.2.0 [latest]\n0.2.1-alpha01\n0.2.1-alpha02\n"
            "0.3.0-beta01\n0.3.0-beta02\n0.1.4\n0.1.5\npreview\n"
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(set(deleted), {
            "delete 0.2.1-alpha01 --push", "delete 0.3.0-beta01 --push",
            "delete 0.1.4 --push",
        })

    def test_dev_alias_is_protected(self):
        result, deleted = self.prune("0.2.0-alpha01 [dev]\n0.2.0 [latest]\n")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(deleted, [])

    def test_similar_aliases_do_not_replace_protected_holders(self):
        result, deleted = self.prune(
            "0.2.0 [latest]\n0.2.0-alpha08 [latest-preview]\n"
            "0.2.0-alpha01 [dev]\n0.2.0-alpha02 [dev-preview]\n"
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(set(deleted), {
            "delete 0.2.0-alpha08 --push", "delete 0.2.0-alpha02 --push",
        })

    def test_stable_without_latest_survives_its_prerelease(self):
        result, deleted = self.prune("0.2.0-alpha08\n0.2.0\n")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(deleted, ["delete 0.2.0-alpha08 --push"])

    def test_homepage_section_labels_have_complete_markdown(self):
        home = SCRIPT.parent.parent / "docs/index.md"
        text = home.read_text(encoding="utf-8")
        section = text.split("## What's New\n", 1)[1].split("\n---\n", 1)[0]
        for label in ("Added", "Changed", "Removed", "Fixed"):
            self.assertIn(f"**{label}**\n\n- ", section)

    def test_prerelease_history_enables_markdown_rendering(self):
        changelog = SCRIPT.parent.parent / "CHANGELOG.md"
        text = changelog.read_text(encoding="utf-8")
        self.assertIn('<details markdown="1">\n<summary>Prerelease history</summary>', text)

    def test_failed_list_stops_without_deletion(self):
        result, deleted = self.prune("0.1.4\n0.1.5\n", list_status=1)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(deleted, [])


if __name__ == "__main__":
    unittest.main()
