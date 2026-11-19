# NexAlloy source review

Date: 2026-11-19. Read-only repository inspection; no local build, commit, push, or submodule manipulation. No applicable AGENTS.md found in ancestor hierarchy or repository scan. Only this report was written.

## Verified refs (GitHub API)

- origin: https://github.com/Berkwe/NexAlloy.git; upstream: https://github.com/NexAlloy/NexAlloy.git. Never push upstream.
- NexAlloy/NexAlloy default main: a0687707a821f0e2844be84b71d94f0a7c67fb05.
- NexAlloy/morphe-patches default nexalloy: b155450216c05350c14b8892bd35f23a16b24edb; matches repository gitlink. API tags response had no first tag.
- morphe-patches-library gitlink: 054ac94a3bd4c3016e431bf4cb3ea7710272266b (local tree only).
- MorpheApp/morphe-patches default main: 31234a8465683a389bcfba25ad636fd1cd38fd02. First API tag: v2.4.0-dev.5 at ac80ff66e66d9cb5818eee44e69e6206aa26c23b. First tag is not asserted to be latest stable.
- NexAlloy first API tag autobuild: 852290b1b0a5bbdfae1d1d2e75007b9fc5907c58.

## Runtime Universal Captions first

The inspected upstream catalog has a public patch named `Captions`, not a file named UniversalCaptions. Treat Universal Captions as the requested runtime feature scope, not a verified upstream patch name.

Local youtube/layout/captions/AutoCaptionsPatch.kt already ports auto-caption policy, using caption selection reason, forced-caption owner fields, scoped subtitle-disable option interception, new-video lifecycle and mute-auto-caption flag. It is not the complete modern captions bundle.

Upstream CaptionsPatch.kt at the pinned Morphe SHA depends on 13 pieces: autoCaptionsPatch, preferredCaptionLanguagePatch, autoTranslateCaptionsPatch, captionCookiesPatch, transcriptPatch, translatedCaptionPensPatch, captionTrackCapturePatch, captionWindowMeasurePatch, stableCaptionBlocksPatch, captionViewportPatch, captionsInPipPatch, captionsAfterSeekPatch. The upstream description limits stable/bilingual captions to YouTube 21.15+ and Android 10+.

Recommended sequence:
1. Preserve existing AutoCaptions hook behavior and add captions settings grouping without changing defaults.
2. Port preferred/secondary-language selection, then additional auto-translation choices. Existing video-start and video-information hooks can be reused.
3. Port track capture and transcript plumbing before stable blocks, bilingual rendering, viewport/measurement and PiP. Keep all new options disabled by default until device-tested.
4. Handle seek/new-video/PiP lifecycle and track invalidation explicitly; retain original captions as fallback.

Important runtime blocker: upstream PreferredCaptionLanguagePatch injects CaptionTrackInterface and SubtitleManagerInterface plus helper methods into obfuscated target classes using addPatchInterface/addHelperMethod. An ordinary Xposed hook cannot simply add these interfaces/methods to already loaded classes. Adapt extension calls to a runtime reflection/adapter bridge with per-class cached DexKit resolution, or a supported proxy design; copying upstream Java extensions unchanged will not suffice. Version branches include 21.02 and 21.10 behavior and require separate fingerprint coverage. This makes the complete feature medium-to-hard, not a small settings-only patch.

Acceptance still required: compatible APK fingerprint tests; on-device stock/translated/forced/disabled captions; preferred-language fallback; seeking; video switches; PiP; no-caption videos; Android/YouTube version gates. No device compatibility claims are made here.

## Catalog comparison and portability

Local filenames show broad YouTube coverage: ad/layout controls, seeking/swipes, SponsorBlock, DeArrow, Return YouTube Dislike, captions auto-policy, sharing/privacy, codec selection, quality and speed memory. Music coverage is much narrower (ads, navigation, background playback, debugging, sharing sanitization and infrastructure). Reddit has ads, guest mode, sharing sanitization and version checks. Shared infrastructure includes audio, request, Litho, proto and text hooks. Filenames are not an exact public feature count; infrastructure patches and registration affect actual availability.

Current Morphe catalog contains substantially more Music features, including audio ducking, repeat/shuffle memory, crossfade, pause-when-muted, lyrics, orientation, refresh rate and QUIC options. Corresponding local patch paths were absent from the inspected local Morphe patch file listing. This is a path-level gap, not proof no equivalent behavior exists anywhere else.

Safe easy/medium candidates for follow-up source review:
- Music disable audio ducking: small policy-hook candidate; confirm source fingerprint and audio focus behavior first.
- Music force portrait orientation: narrow UI preference candidate; validate activity-specific behavior.
- Music remember repeat/shuffle state: medium state/lifecycle work; avoid overwriting account/server choices unexpectedly.
- Music pause when muted: medium lifecycle/audio-state work; opt-in and preserve explicit user playback intent.
- Caption language selection: medium/high due to runtime bridge above, but priority over unrelated additions.

Avoid presenting compile-time resource replacement, target class/interface injection or arbitrary instruction insertion as directly portable hooks. Spoof/signature/GmsCore paths are not recommended as easy missing features. This review does not recommend entitlement unlocking.

## Actual Gradle configuration

- Root settings declares AGP/com.android.settings 9.4.1, compileSdk 37, minSdk 27; wrapper declares Gradle 9.8.1 with SHA-256 validation.
- Existing workflows select Temurin JDK 17. Java source/target and Kotlin JVM target are 17. These are source observations, not verification that this AGP/Gradle/JDK combination executes successfully. Java bytecode target is distinct from required Gradle runtime JDK; verify official requirements before changing CI JDK.
- Modules: :app, :stub; app build types debug/release, no product flavors seen.
- Existing workflow commands: ./gradlew assembleDebug and ./gradlew assembleRelease. Recommended explicit scoped equivalents: ./gradlew :app:assembleDebug and :app:assembleRelease.
- Custom tasks registered per variant: generateStringsDebug, generateStringsRelease, copyResourcesDebug, copyResourcesRelease. Generated resources/assets attach via variant.sources; no separate manual generation step should be necessary.
- Main Java/Kotlin sources consume both submodules; proto sources consume Morphe YouTube and shared-youtube paths. gradle.properties in morphe-patches is read during configuration.
- Required local binary dependency exists: libs/dexkit-android.aar (and sources jar).
- JavaCompile excludes HideRelatedVideosPatch.java, PrioritizeVideoQualityPatch.java, OAuth2Preference.java, SpoofVideoStreamsSignInPreference.java, SpoofSignaturePatch.java. Do not assume those extensions are available at runtime merely because present upstream.
- Test tasks use JUnit Platform, native DexKit libraries, app/binaries APK inputs and reports/fingerprints/<task>.json. A generic CI unit-test command is not an adequate fingerprint test unless these inputs/native libraries are supplied.

## CI recommendations for Berkwe/NexAlloy

Separate read-only validation from publishing:

1. Validation: pull_request, workflow_dispatch and push to intended fork branch; permissions contents: read. Checkout recursive submodules pinned to gitlinks (never update to remote heads during reproducible build). Keep wrapper checksum validation and cache Gradle dependencies.
2. Install Android SDK platform 37 explicitly and use a runner/runtime JDK supported by the declared AGP/Gradle versions after official compatibility verification. Do not confuse JVM target 17 with runtime requirement.
3. Run :app:assembleDebug without writing signing.properties and upload app/build/outputs/apk/debug/*.apk with missing-artifact failure. This does not require release signing secrets.
4. Run fingerprint tests only in a separate job with matching DexKit native binaries and authorized APK fixtures; record tested app versions. Run :app:testDebugUnitTest only once those prerequisites are available.
5. Release job: protected/manual or tag-triggered; contents: write only here; provision signing material only here, use environment variables and quoted shell expansions, clean temporary keys with a trap. Never print secrets. Upload signed APK and mapping file with restricted retention/access as appropriate.
6. Existing android.yml currently writes signing.properties and key.jks before debug/release, grants write permission to entire job, and updates an `autobuild` release. On a new fork the release/tag may not exist: bootstrap deliberately in the release job or disable publishing until configured. Existing workflow assumes app-release.apk; without release signing configured this name must not be assumed (unsigned release output may differ).
7. Existing tag workflow creates a prerelease for *.*.* tags. Keep it scoped to the fork and intentional tags. Avoid checkout of untrusted PR code in a privileged signing/publishing job.

## Follow-up: new development workflow and concrete blockers

Main agent created .github/workflows/build.yml; inspected only, not modified. It uses JDK 21, recursive checkout, :app:tasks --all followed by :app:assembleDebug :app:testDebugUnitTest :app:lintDebug, read-only permissions and artifact v7. gradle/gradle-daemon-jvm.properties explicitly declares toolchainVersion=21: JDK 21 is therefore the repository's daemon criterion, while source bytecode remains 17. This resolves the earlier JDK uncertainty at project-configuration level (not an execution compatibility test).

Tests: app/binaries currently contains only .gitignore. FilePathArgumentsProvider resolves binaries relative to the test project working directory, throws IllegalStateException when the directory is absent, otherwise recursively includes every non-hidden regular file (not just .apk). Thus current fixtures yield an empty argument stream. FingerprintsKtTest is an ArgumentsSource parameterized class; absent an explicit allow-zero override this should cause JUnit parameterized-class configuration failure, not a meaningful fingerprint pass. This outcome is source-derived, not executed. Ensure only authorized valid APK fixtures are placed there; do not suppress the error or alter tests to get green CI. Native DexKit binaries are present for Linux default and linux-arm64, plus Windows/macOS; runtime loading itself remains untested.

SDK: settings.gradle.kts requires compileSdk 37; the new workflow neither runs setup-android nor explicitly installs platforms;android-37. The project proves the requirement, not availability on ubuntu-latest. Existing runner image contents and Android repository availability have not been checked. A focused CI preflight should show sdkmanager --list_installed and install/verify platforms;android-37, failing clearly if unavailable. Do not silently lower compileSdk. The APK upload step currently runs only after all build/check tasks succeed, so empty-fixture test failure prevents publishing an otherwise produced debug APK; split validation jobs or use an intentional failure-aware upload condition if diagnostic APK delivery is wanted. Do not mark tests successful without fixtures.

### Portability summary

| Feature | Runtime effort | Main constraint |
|---|---|---|
| Existing auto-caption policy | Already ported | Regression/device checks still needed |
| Preferred/secondary caption language | Medium/high | Injected upstream interfaces need runtime adapter bridge |
| Stable/bilingual captions and PiP | High | Track/transcript/rendering lifecycle; version gates |
| Music disable audio ducking | Easy/medium candidate | Fingerprint and audio-focus semantics require validation |
| Music portrait orientation | Easy/medium candidate | Activity-specific policy; source review still needed |
| Music repeat/shuffle memory | Medium candidate | State persistence and lifecycle |
| Music pause when muted | Medium candidate | Audio state and user-intent preservation |

## Verification and blockers

Repository status was clean during inspection. No build or Gradle task listing was executed, per instruction. Custom task names/build variants above are confirmed from source, standard AGP task commands are recommended from that configuration, not execution-verified.

morphe-patches directory was unpopulated except .git while the separate submodule download was running. Did not interfere. Consequently extension-library contents and full compilation cannot be validated locally in this review. GitHub API provided upstream catalog and selected source at pinned Morphe SHA.

The rg executable was absent; inspection continued with grep and sed. No secrets were read or exposed.

Primary sources: local app/build.gradle.kts, settings.gradle.kts, gradle/libs.versions.toml, gradle/wrapper/gradle-wrapper.properties, .github/workflows/android.yml and release.yml, local AutoCaptionsPatch.kt, DEVELOPMENT.md, git tree/remotes; GitHub REST repositories/commits/tags/tree/contents APIs for repositories and SHAs named above.
