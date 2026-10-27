# Get a production APK in Telegram

The **Android release APK** GitHub Actions workflow builds the release variant with
the existing signing key and sends the APK to your Telegram channel or private chat.
It also saves the APK and its SHA-256 checksum as a GitHub Actions artifact for 14 days.
It runs only when requested. It does not publish a Play Store or GitHub Release.

## One-time GitHub configuration

Under **Settings → Secrets and variables → Actions → Secrets**, add these four
repository secrets:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 of `pulse-android/.signing/pulse-release.p12` |
| `ANDROID_SIGNING_PROPERTIES_BASE64` | Base64 of `pulse-android/.signing/release.properties` |
| `TELEGRAM_BOT_TOKEN` | Token provided by Telegram's `@BotFather` for your bot |
| `TELEGRAM_CHAT_ID` | Your channel's numeric ID (typically `-100…`), public `@channelusername`, or your private chat's numeric ID |

The signing properties already contain `storePassword`, `keyAlias`, and
`keyPassword`; you do not need separate secrets for those. The workflow restores
the two ignored files with restricted permissions and overrides `storeFile` with
the runner's relative path. It removes the files at the end and disables Gradle
configuration/build caching for this job. Keep the original key backed up: it is
required to update your existing release installation.

From PowerShell at the repository root, copy each encoded value to the clipboard
and paste it into the corresponding GitHub secret. Run these **one at a time**:

```powershell
# Paste into ANDROID_KEYSTORE_BASE64, then run the next command.
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path 'pulse-android/.signing/pulse-release.p12').Path)) | Set-Clipboard

# Paste into ANDROID_SIGNING_PROPERTIES_BASE64.
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path 'pulse-android/.signing/release.properties').Path)) | Set-Clipboard

# Clear the clipboard after saving both secrets.
Set-Clipboard -Value ''
```

Base64 is an encoding, not encryption. Store both values as **secrets**, never as
ordinary repository variables or committed files. GitHub supplies the runner's
Java and Android SDK paths; no local `JAVA_HOME`, `ANDROID_HOME`, `sdk.dir`, backend
login credentials, or personal GitHub token need to be copied into this workflow.

Optionally add a repository **variable** named `PULSE_RELEASE_API_URL` to override
the production API address. Leave it unset to retain the app's current default,
`http://100.115.134.90:3100/api/`. Keep the trailing `/api/`. The installed app still
needs Tailscale access to this address; the GitHub runner does not need Tailscale
to compile the APK. Other hosts require HTTPS under the app's existing policy.

## One-time Telegram configuration

1. Open the official `@BotFather` in Telegram and use `/newbot` to create a bot
   (or use an existing bot). Save its token as `TELEGRAM_BOT_TOKEN`.
2. For a channel, add the bot as an administrator with permission to post messages.
   For delivery directly to yourself, open the bot's chat and press **Start**.
3. Set `TELEGRAM_CHAT_ID`. A public channel can use `@channelusername`. For a
   private channel, use its numeric ID. To discover the ID, add the bot, create a
   new channel post, and inspect `channel_post.chat.id` from Telegram's
   `getUpdates` method. A direct message appears under `message.chat.id` instead.
   If the bot is already connected to Hermes by webhook, obtain the chat ID from
   that integration: `getUpdates` cannot run while a webhook is configured.

Use a dedicated delivery bot if that makes setup easier; this workflow only sends
messages and does not poll for commands or change a bot's webhook.

The official [Telegram tutorial](https://core.telegram.org/bots/tutorial) explains
bot creation. The [Bot API](https://core.telegram.org/bots/api#getupdates) documents
chat IDs and update delivery.

## Request a build

After this workflow is committed and pushed to the repository's **default branch**:

1. Open **GitHub → DarkAbhi/PULSE → Actions → Android release APK**.
2. Click **Run workflow**, select the branch to build, and confirm **Run workflow**.
3. Wait for the build. The bot posts the signed APK with its commit and run link.
   Download and install it on your phone.

The APK uses the version name and code declared in `app/build.gradle.kts` (currently
`1.0.1`, code `2`). Increment them when cutting a new app version; the workflow
does not change source version numbers. APK filenames include the commit, run
number, and attempt so rebuilds can be distinguished.

If delivery fails, the run reports a failure and the APK remains available under
**Artifacts** on that run. Telegram's hosted Bot API currently limits document
uploads to 50 MB; oversized APKs also remain downloadable from the GitHub artifact.
The existing local release APK was approximately 25 MB when this was added.
See [sendDocument](https://core.telegram.org/bots/api#senddocument).

Signature verification runs before upload. This is a packaging workflow, not a
device test suite; follow the README's release cold-launch guidance before relying
on a new version.

## Trigger the same build from Hermes later

Hermes can invoke this exact workflow using an authenticated GitHub CLI:

```sh
gh workflow run android-release.yml --repo DarkAbhi/PULSE --ref <branch>
```

Replace `<branch>` with the branch you want built. This dispatches asynchronously;
Telegram delivery happens when the workflow completes. Authenticate the CLI on the
Hermes host with repository access and permission to run Actions (a fine-grained
token needs **Actions: write**). That credential belongs on the Hermes host, not
in this build workflow. See GitHub's
[manual workflow instructions](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow).

A Telegram `/apk` command requires a bot or Hermes command handler to authenticate
the requesting user and call that dispatch. The button works without operating
such a service, so command handling is left for a later integration.
