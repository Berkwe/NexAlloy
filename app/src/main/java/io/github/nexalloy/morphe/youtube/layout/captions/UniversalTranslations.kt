package io.github.nexalloy.morphe.youtube.layout.captions

import java.net.URI
import java.net.URLDecoder
import java.text.Collator
import java.util.Locale

/** Only native translation templates are accepted; never reconstruct a signed URL. */
internal object UniversalTranslations {
    val languages = "aa ab af ak am ar as ay az ba be bg bho bn bo br bs ca ceb co crs cs cy da de dv dz ee el en eo es et eu fa fi fil fj fo fr fy ga gaa gd gl gn gu gv ha haw hi hmn hr ht hu hy id ig is it iu iw ja jv ka kha kk kl km kn ko kri ku ky la lb lg ln lo lt lua luo lv mfe mg mi mk ml mn mr ms mt my ne new nl no nso ny oc om or os pa pam pl ps pt pt-PT qu rn ro ru rw sa sd sg si sk sl sm sn so sq sr ss st su sv sw ta te tg th ti tk tn to tr ts tt tum ug uk ur uz ve vi war wo xh yi yo zh-Hans zh-Hant zu".split(" ")
    data class Track(val code: String, val url: String, val vss: String)
    private val language = Regex("[a-z]{2,8}(-[A-Za-z0-9]{2,8})*")
    private fun key(code: String) = Locale.forLanguageTag(code).toLanguageTag().lowercase(Locale.ROOT)
    private data class Source(val language: String, val offset: Int)
    private fun source(track: Track): Source {
        require(language.matches(track.code))
        val uri = URI(track.url)
        val host = uri.host.lowercase(Locale.ROOT)
        require(uri.scheme.equals("https", true) && (host == "youtube.com" || host.endsWith(".youtube.com")))
        require(uri.userInfo == null && uri.port in listOf(-1, 443) && uri.rawPath == "/api/timedtext")
        val fields = mutableMapOf<String, String>()
        var offset = track.url.indexOf('?') + 1
        var target = -1
        for (part in requireNotNull(uri.rawQuery).split('&')) {
            val rawKey = part.substringBefore('=')
            val decoded = URLDecoder.decode(rawKey, "UTF-8")
            if (decoded in listOf("v", "lang", "kind", "name", "tlang")) {
                require(decoded == rawKey && !fields.containsKey(decoded))
                fields[decoded] = URLDecoder.decode(part.substringAfter('=', ""), "UTF-8")
                if (decoded == "tlang") {
                    require(part == "tlang=" + track.code)
                    target = offset + 6
                }
            }
            offset += part.length + 1
        }
        require(target >= 0 && fields["tlang"] == track.code)
        val source = requireNotNull(fields["lang"])
        require(language.matches(source))
        fields["v"]?.let { require(Regex("[A-Za-z0-9_-]{1,128}").matches(it)) }
        require(track.vss.startsWith("t") && track.vss.endsWith("." + track.code))
        return Source(source, target)
    }

    fun <T : Any> expand(tracks: List<T>, read: (T) -> Track,
                        copy: (T, Track, String) -> T, locale: Locale = Locale.getDefault()): List<T> {
        if (tracks.isEmpty()) return tracks
        return try {
            val candidates = tracks.mapNotNull { native ->
                runCatching { native to read(native) }.getOrNull()
            }
            val (nativeTemplate, template, source) = candidates.firstNotNullOfOrNull { (native, track) ->
                runCatching { Triple(native, track, source(track)) }.getOrNull()
            } ?: return tracks
            val existing = candidates.map { key(it.second.code) }.toMutableSet()
            val added = languages.filter { key(it) != key(source.language) && existing.add(key(it)) }
                .map { code ->
                    val label = Locale.forLanguageTag(if (code == "pt") "pt-BR" else code).getDisplayName(locale)
                    val translated = Track(code,
                        template.url.substring(0, source.offset) + code +
                            template.url.substring(source.offset + template.code.length),
                        template.vss.dropLast(template.code.length) + code)
                    val native = copy(nativeTemplate, translated, label.ifEmpty { code })
                    require(read(native) == translated)
                    native to label.ifEmpty { code }
                }
            if (added.isEmpty()) tracks else {
                val collator = Collator.getInstance(locale)
                (tracks.map { it to it.toString() } + added)
                    .sortedWith { a, b -> collator.compare(a.second, b.second) }.map { it.first }
            }
        } catch (_: Exception) { tracks }
    }
}
