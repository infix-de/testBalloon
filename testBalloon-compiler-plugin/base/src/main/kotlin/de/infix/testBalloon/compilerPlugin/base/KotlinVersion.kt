package de.infix.testBalloon.compilerPlugin.base

/**
 * A restrictive interpretation of a Kotlin version, assuming that dev versions are always newer than EAP versions.
 *
 * NOTE: In reality, dev versions (like 2.4.20-dev1234) can be interspersed with EAP versions (like 2.4.20-Beta1),
 * leading to a precedence of 2.4.20-dev123 < 2.4.20-Beta1 < 2.4.20-dev255. We ignore this, implying that we can
 * only support dev versions which match the highest available compiler plugin adapter version.
 */
class KotlinVersion(val major: Int, val minor: Int, val patch: Int, val extension: String?) :
    Comparable<KotlinVersion> {

    enum class Maturity(private val lowercaseRegexText: String?) {
        // EAP versions
        ALPHA("""alpha\d*"""),
        BETA("""beta\d*"""),
        RC("""rc\d*"""),

        // dev versions
        DEV("""dev-\d+"""),

        // stable versions
        STABLE(null);

        val lowercaseRegex by lazy { lowercaseRegexText?.let { Regex(it) } }
    }

    val maturity: Int = run {
        val lowercaseClassifier = extension?.lowercase() ?: return@run Maturity.STABLE.ordinal
        for (maturity in Maturity.entries) {
            if (maturity.lowercaseRegex?.matches(lowercaseClassifier) == true) return@run maturity.ordinal
        }
        throw IllegalArgumentException(
            "Cannot determine maturity for classifier '$extension' in ${this@KotlinVersion}"
        )
    }

    val buildNumber = extension?.dropWhile { !it.isDigit() }?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0

    override fun compareTo(other: KotlinVersion): Int {
        (major - other.major).let { if (it != 0) return it }
        (minor - other.minor).let { if (it != 0) return it }
        (patch - other.patch).let { if (it != 0) return it }
        (maturity - other.maturity).let { if (it != 0) return it }
        (buildNumber - other.buildNumber).let { if (it != 0) return it }
        return 0
    }

    override fun toString(): String = "$major.$minor.$patch${extension?.let { "-$it" } ?: ""}"
}

fun String.asKotlinVersion(): KotlinVersion {
    val segments = split("-", limit = 2)
    val components = segments.getOrNull(0)?.split('.')
    val classifier = segments.getOrNull(1)

    val major = components?.getOrNull(0)?.toIntOrNull()
        ?: throw IllegalArgumentException("'$this' is missing a major version")
    val minor = components.getOrNull(1)?.toIntOrNull()
        ?: throw IllegalArgumentException("'$this' is missing a minor version")
    val micro = components.getOrNull(2)?.toIntOrNull() ?: 0

    return KotlinVersion(major = major, minor = minor, patch = micro, extension = classifier)
}
