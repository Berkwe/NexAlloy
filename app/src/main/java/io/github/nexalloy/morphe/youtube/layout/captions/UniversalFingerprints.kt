package io.github.nexalloy.morphe.youtube.layout.captions

import io.github.nexalloy.morphe.*
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately

internal object UniversalCaptionTracksManagerDirectTracksFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/util/List;",
    parameters = listOf(),
    filters = listOf(
        string("AUTO_TRANSLATE_CAPTIONS_OPTION")
    )
)


internal object UniversalCaptionTracksManagerAutoTranslateTracksFingerprint : Fingerprint(
    classFingerprint = UniversalCaptionTracksManagerDirectTracksFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/util/List;",
    parameters = listOf(),
    filters = listOf(
        // Url of the source caption track.
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/lang/String;"
        ),
        methodCall(
            smali = "Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;",
            location = MatchAfterImmediately()
        ),
        string("&tlang=", location = MatchAfterImmediately())
    )
)


internal object UniversalCaptionTrackBuilderBuildFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf(),
    filters = listOf(
        // Display name.
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/CharSequence;"
        ),
        string(" languageCode"),
        string(" vssId"),
        string(" url")
    )
)


internal object UniversalCaptionTrackBuilderCopyConstructorFingerprint : Fingerprint(
    classFingerprint = UniversalCaptionTrackBuilderBuildFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/CharSequence;"
        )
    )
)


internal class UniversalCaptionTrackBuilderSetterFingerprint(nullValueMessage: String) : Fingerprint(
    classFingerprint = UniversalCaptionTrackBuilderBuildFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        string(nullValueMessage)
    )
)

internal val universalUrlField = findFieldDirect {
    UniversalCaptionTracksManagerAutoTranslateTracksFingerprint.instructionMatches[0].instruction.fieldRef!!
}
internal val universalLabelField = findFieldDirect {
    UniversalCaptionTrackBuilderBuildFingerprint.instructionMatches[0].instruction.fieldRef!!
}
internal fun universalTrackField(setter: Fingerprint) = findFieldDirect {
    setter.match().method.instructions.filter {
        it.opcode == Opcode.IPUT_OBJECT.ordinal
    }.mapNotNull { it.fieldRef }.distinct().single()
}

internal val universalLanguageSetter = UniversalCaptionTrackBuilderSetterFingerprint("Null languageCode")
internal val universalUrlSetter = UniversalCaptionTrackBuilderSetterFingerprint("Null url")
internal val universalVssSetter = UniversalCaptionTrackBuilderSetterFingerprint("Null vssId")
internal val universalLanguageField = universalTrackField(universalLanguageSetter)
internal val universalVssField = universalTrackField(universalVssSetter)
