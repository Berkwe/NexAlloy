@file:Suppress("DEPRECATION")

package io.github.nexalloy.morphe.youtube.layout.captions

import android.content.Context
import app.morphe.extension.shared.Utils
import app.morphe.extension.shared.settings.Setting
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

/** Native templates only: no tracks or menu entries are fabricated for empty lists. */
val UniversalCaptions = patch(
    name = "Universal captions",
    description = "Adds native auto-translate targets using existing caption templates.",
) {
    val languageField = ::universalLanguageField.field
    val vssField = ::universalVssField.field
    val urlField = ::universalUrlField.field
    val labelField = ::universalLabelField.field
    val constructor = UniversalCaptionTrackBuilderCopyConstructorFingerprint.constructor
    val build = UniversalCaptionTrackBuilderBuildFingerprint.method
    val setLanguage = universalLanguageSetter.method
    val setUrl = universalUrlSetter.method
    val setVss = universalVssSetter.method
    require(constructor.parameterTypes.single() == build.returnType)
    require(labelField.declaringClass == constructor.declaringClass)

    PreferenceScreen.PLAYER.addPreferences(
        SwitchPreference("nexalloy_universal_captions", tag = android.preference.SwitchPreference::class.java)
    )

    UniversalCaptionTracksManagerAutoTranslateTracksFingerprint.hookMethod {
        after { param ->
            // Do not intercept exceptions or replace null/non-list results.
            if (param.hasThrowable()) return@after
            val original = param.result as? List<*> ?: return@after
            try {
                if (!Utils.getContext().getSharedPreferences(Setting.preferences.name, Context.MODE_PRIVATE)
                        .getBoolean("nexalloy_universal_captions", false)) return@after
                if (original.any { it == null }) return@after
                @Suppress("UNCHECKED_CAST")
                val tracks = original as List<Any>
                val expanded = UniversalTranslations.expand(tracks, read = { track ->
                    // Language and vss fields belong to the builder; read a detached copy.
                    val builder = constructor.newInstance(track)
                    UniversalTranslations.Track(languageField.get(builder) as String,
                        urlField.get(track) as String, vssField.get(builder) as String)
                }, copy = { track, translation, label ->
                    val builder = constructor.newInstance(track)
                    setLanguage.invoke(builder, translation.code)
                    setUrl.invoke(builder, translation.url)
                    setVss.invoke(builder, translation.vss)
                    labelField.set(builder, label)
                    requireNotNull(build.invoke(builder))
                })
                if (expanded !== original) param.result = expanded
            } catch (_: Exception) {
                // Fail closed: leave the exact original result object untouched.
            }
        }
    }
}
