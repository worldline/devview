# Publishing a Release

This guide walks through publishing DevView to Maven Central from scratch — no prior GPG keys, Sonatype account, or Maven Central experience required.

## How It Works

The publish pipeline is already wired:

- **Trigger**: pushing a tag matching `v*` (e.g. `v0.1.0`) fires `.github/workflows/publish.yml`
- **What it does**: runs build + detekt + konsist checks, then calls `./gradlew publish` using the [Vanniktech Maven Publish plugin](https://github.com/vanniktech/gradle-maven-publish-plugin)
- **Signing**: all artifacts are GPG-signed before upload (`RELEASE_SIGNING_ENABLED=true` in `gradle.properties`)
- **Target**: the new [Maven Central Portal](https://central.sonatype.com) (`SONATYPE_HOST=CENTRAL_PORTAL`)

You need to complete the five setup steps below once, then publishing is a single script call.

---

## Step 1: Create a Sonatype Central Portal Account

1. Go to [https://central.sonatype.com](https://central.sonatype.com)
2. Click **Sign in** and choose **Sign in with GitHub** (recommended — links your GitHub identity)
3. Complete registration if prompted

This is the new Central Portal. It replaced the legacy Nexus/OSSRH workflow.

---

## Step 2: Verify the Namespace

DevView publishes under `com.worldline.devview`. Sonatype must verify you own this namespace before any upload is accepted.

1. In the Central Portal, go to **Namespaces** in the left sidebar
2. Click **Add Namespace** and enter `com.worldline` (the parent namespace covers all sub-paths)
3. Sonatype shows you a verification method — for a GitHub-hosted project the easiest is **GitHub repository verification**:
    - It asks you to create a public GitHub repository named after the verification token (e.g. `worldline/CENTRAL-12345678`)
    - Create the empty repo, then click **Verify** in the portal
    - Delete the temp repo once verified
4. Verification usually completes within a few minutes

If `com.worldline` is already verified by your organisation, you can skip this step.

---

## Step 3: Generate a GPG Signing Key

All Maven Central artifacts must be GPG-signed. If you don't have GPG installed:

- **macOS**: `brew install gnupg`
- **Windows**: Download [Gpg4win](https://gpg4win.org)
- **Linux**: `apt install gnupg` or equivalent

### Generate the key

```bash
gpg --full-generate-key
```

When prompted:
- **Key type**: `ECC (sign and encrypt)` (option 9, default — recommended by JetBrains)
- **Elliptic curve**: `Curve 25519` (option 1, default)
- **Expiry**: `0` (does not expire) — or set a date if preferred
- **Name/email**: your name and the email associated with your Sonatype account
- **Passphrase**: choose a strong passphrase — you'll need it for `GPG_KEY_PASSWORD`

### Find your key ID

```bash
gpg --list-secret-keys --keyid-format=long
```

Output looks like:

```
sec   rsa4096/AABBCCDD11223344 2026-07-15 [SC]
      FFFF0000AAAA1111BBBB2222CCCC3333DDDD4444
uid           [ultimate] Your Name <you@example.com>
```

The **key ID** is the 8 hex characters after the `/` on the `sec` line: `11223344`. This is your `GPG_KEY_ID` secret.

### Export the private key

```bash
gpg --armor --export-secret-keys 11223344
```

This prints a block starting with a PGP private key header. Copy the **entire output** including the header and footer lines — this is your `GPG_KEY` secret. <!-- gitleaks:allow -->

### Publish the public key

```bash
gpg --keyserver hkps://keyserver.ubuntu.com --send-keys 11223344
```

> Use `hkps://` (HTTPS, port 443) — plain `hkp://` (port 11371) is often blocked by corporate firewalls. If this fails, try `hkps://keys.openpgp.org` instead.

Maven Central validators check public keyservers to verify artifact signatures. This step is required.

---

## Step 4: Create a Maven Central API Token

Do **not** use your portal login credentials in CI. Generate a dedicated token instead:

1. In the Central Portal, click your username (top-right) → **View User Tokens**
2. Click **Generate User Token**
3. A popup appears — fill in:
   - **Token name**: something descriptive, e.g. `devview-github-actions`
   - **Expiration**: 1 year (recommended — set a calendar reminder to rotate the GitHub secret before it expires)
4. The portal shows a **username** and **password** — copy both immediately, the password is only shown once

These become `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD`.

---

## Step 5: Configure GitHub Repository Secrets

In the GitHub repository, go to **Settings → Secrets and variables → Actions → New repository secret** and add all five:

| Secret name | Value |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Token username from Step 4 |
| `MAVEN_CENTRAL_PASSWORD` | Token password from Step 4 |
| `GPG_KEY` | Full ASCII-armored private key from Step 3 (include the `-----BEGIN/END PGP PRIVATE KEY BLOCK-----` lines) |
| `GPG_KEY_ID` | 8-character short key ID from Step 3 (e.g. `11223344`) |
| `GPG_KEY_PASSWORD` | Passphrase chosen during key generation in Step 3 |

The `publish.yml` workflow passes these to Gradle as `ORG_GRADLE_PROJECT_*` environment variables, which the Vanniktech plugin picks up automatically.

---

## Step 6: Run the Release

Ensure you are on `main` with a clean working tree, then run:

```bash
./scripts/release.sh 0.1.0 0.2.0-SNAPSHOT
```

On Windows, run this from Git Bash (not PowerShell — the script uses `sed` and bash features).

The script does the following automatically:

1. Replaces `VERSION_NAME` in `gradle.properties` with `0.1.0`
2. Commits: `Prepare for release 0.1.0`
3. Creates tag `v0.1.0` — **this tag push triggers the publish workflow**
4. Replaces `VERSION_NAME` with `0.2.0-SNAPSHOT`
5. Commits: `Prepare next development version`
6. Pushes both commits and the tag (`git push && git push --tags`)

---

## Step 7: Verify

1. Go to the **Actions** tab in GitHub and watch the `Publish` workflow
2. The jobs run in order: `build`, `detekt`, `konsist` (parallel) → `publish` → `github-release`
3. When `publish` succeeds, open [https://central.sonatype.com](https://central.sonatype.com) → **Deployments** — your deployment appears there
4. Artifacts propagate to the public Maven Central search at [https://central.sonatype.com/search](https://central.sonatype.com/search) within 15–30 minutes
5. The `github-release` job automatically creates a GitHub Release with generated release notes

---

## Troubleshooting

**Signing error in CI**
Make sure `GPG_KEY` contains the complete armored block including the PGP private key header and footer lines. A truncated key causes silent signing failures. <!-- gitleaks:allow -->

**Namespace not verified**
The upload will be rejected with a namespace error. Complete Step 2 and wait for verification to go green before retrying.

**Deployment stuck in "Pending" on the Portal**
This can happen if POM metadata is incomplete. Check that all `POM_*` fields in the root `gradle.properties` are filled in.

**`--no-configuration-cache` in the workflow**
The publish step runs with `--no-configuration-cache` because the Vanniktech plugin is not fully compatible with Gradle configuration cache during publishing. This is intentional and expected.
