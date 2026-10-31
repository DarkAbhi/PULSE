# Get a production APK in Telegram

The **Android release APK** GitHub Actions workflow builds the release variant with
the existing signing key and sends the APK to your Telegram channel or private chat.
It also saves the APK and its SHA-256 checksum as a GitHub Actions artifact for 14 days.
It runs automatically on every push to `migration`. It does not publish a Play
Store or GitHub Release.

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

## Automatic builds from migration

Keep `.github/workflows/android-release.yml` on **migration** and push that branch
to GitHub. Each push automatically builds a signed release APK and sends it to
Telegram. No workflow file on `main`, PR merge, or manual button is required for
this push trigger. See GitHub's
[push event documentation](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#push).

The workflow checks out the exact commit that triggered the push, so a newer push
cannot change the source of an already queued build. APK filenames and Telegram
messages identify that commit. Each push has its own run; if one push contains
multiple commits, the build uses the final commit in that push. There are no path
filters, so pushes that only change documentation also request a build. Separate
commits do not cancel each other's release runs.

1. Commit your changes on `migration` and push them to GitHub.
2. Open **GitHub → DarkAbhi/PULSE → Actions → Android release APK** to see the run.
3. Wait for the build. The bot posts the signed APK with its source branch, commit,
   and run link. Download and install it on your phone.

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

## Rebuild an existing commit

Open a previous **Android release APK** run in GitHub Actions and choose **Re-run
all jobs** to rebuild its original commit. This also attempts Telegram delivery
again. A new source commit is not required to retry a failed build or delivery.

Telegram command handling through Hermes is not part of this workflow; delivery
is automatic after a push or a rerun.
