# Shrinker rules for the release build.
#
# This file has to exist: `app/build.gradle.kts` names it in `proguardFiles`
# with `isMinifyEnabled = true`, and a missing file there fails the release
# build with a path error that looks nothing like the real cause. It was
# missing until the release workflow was written, and nobody had noticed
# because only the debug build had ever been run.
#
# It is deliberately almost empty. AGP already merges the default
# `proguard-android-optimize.txt`, the consumer rules shipped by AndroidX and
# the ones Compose publishes with its own artifacts, and those cover every
# dependency this app has. Rules copied in "just in case" are the usual way a
# shrinker stops shrinking: each one keeps code that nothing calls.

# Keep the line numbers of a crash readable. Without this the stack traces
# that reach the user are renamed frames, and an obfuscated report on an app
# with no crash reporter is a report nobody can act on. `SourceFile` is
# rewritten to a constant so the original file names do not leak.
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
