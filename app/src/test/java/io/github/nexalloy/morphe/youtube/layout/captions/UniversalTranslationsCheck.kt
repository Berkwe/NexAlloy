package io.github.nexalloy.morphe.youtube.layout.captions

/** Standalone JVM checks; no Android or host classes required. */
fun main() {
    val url = "https://www.youtube.com/api/timedtext?v=abc&lang=en&sig=A%2Fb&tlang=de#anchor"
    val template = UniversalTranslations.Track("de", url, "t.en.de")
    val tracks = listOf(template)
    val result = UniversalTranslations.expand(tracks, { it }, { _, track, _ -> track })
    val turkish = result.single { it.code == "tr" }
    check(turkish.url == url.replace("tlang=de", "tlang=tr"))
    check(turkish.vss == "t.en.tr")
    check(result.none { it.code == "en" })
    check(result.map { it.code }.distinct().size == result.size)
    val other = UniversalTranslations.Track("fr", url.replace("tlang=de", "tlang=fr")
        .replace("sig=A%2Fb", "sig=other"), "t.a.en.fr")
    val invalidFirst = template.copy(url = url.replace("youtube.com", "example.com"))
    val mixed = UniversalTranslations.expand(listOf(invalidFirst, other, template), { it }, { _, t, _ -> t })
    check(mixed.containsAll(listOf(invalidFirst, other, template)))
    check(mixed.single { it.code == "tr" }.url.contains("sig=other&tlang=tr"))
    check(listOf("tr", "is", "eu", "cy").all { code -> mixed.any { it.code == code } })
    val empty = emptyList<UniversalTranslations.Track>()
    check(UniversalTranslations.expand(empty, { it }, { _, t, _ -> t }) === empty)
    for (invalid in listOf(url.replace("#anchor", "&tlang=fr#anchor"), url.replace("tlang=de", "%74lang=de"),
        url.replace("youtube.com", "example.com"), url.replace("lang=en", "lang=!"))) {
        val bad = listOf(template.copy(url = invalid))
        check(UniversalTranslations.expand(bad, { it }, { _, t, _ -> t }) === bad)
    }
    check(UniversalTranslations.expand(tracks, { it }, { _, _, _ -> error("copy failed") }) === tracks)
    println("UniversalTranslations: all checks passed; ${result.size} targets")
}
