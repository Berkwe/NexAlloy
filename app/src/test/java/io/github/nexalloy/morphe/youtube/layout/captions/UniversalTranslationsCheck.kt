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
    val empty = emptyList<UniversalTranslations.Track>()
    check(UniversalTranslations.expand(empty, { it }, { _, t, _ -> t }) === empty)
    for (invalid in listOf(url.replace("#anchor", "&tlang=fr#anchor"), url.replace("tlang=de", "%74lang=de"),
        url.replace("youtube.com", "example.com"), url.replace("lang=en", "lang=fr"))) {
        val bad = listOf(template.copy(url = invalid))
        check(UniversalTranslations.expand(bad, { it }, { _, t, _ -> t }) === bad)
    }
    check(UniversalTranslations.expand(tracks, { it }, { _, _, _ -> error("copy failed") }) === tracks)
    println("UniversalTranslations: all checks passed; ${result.size} targets")
}
