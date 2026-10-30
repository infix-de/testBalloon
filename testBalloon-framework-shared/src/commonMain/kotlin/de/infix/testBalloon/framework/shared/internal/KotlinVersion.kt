package de.infix.testBalloon.framework.shared.internal

/**
 * A restrictive interpretation of a Kotlin version, using heuristics to determine the order of [extension].
 */
@TestBalloonInternalApi
public class KotlinVersion(
    private val major: Int,
    private val minor: Int,
    private val patch: Int,
    private val extension: String?
) : Comparable<KotlinVersion> {

    /**
     * A version's maturity matching a lowercased `extension` in ascending order (later is newer).
     */
    private enum class Maturity(lowercaseRegexText: String?) {
        // EAP versions
        ALPHA("""alpha[\d-]*"""),
        BETA("""beta[\d-]*"""),
        RC("""rc[\d-]*"""),

        // Dev versions
        // In reality, dev versions like 2.4.20-dev1234 can be interspersed with EAP versions (like 2.4.20-Beta1),
        // leading to a precedence of 2.4.20-dev123 < 2.4.20-Beta1 < 2.4.20-dev255. We ignore this, implying that we can
        // only support dev versions which are actually more recent than the latest EAP version.
        DEV("""dev-\d+"""),

        // Special versions embedded into IntelliJ IDEA, assuming these are more recent than any other non-stable
        // version.
        INTELLIJ_IDEA("""ij\d+-\d+"""),

        // stable versions
        STABLE(null);

        val lowercaseRegex by lazy { lowercaseRegexText?.let { Regex(it) } }
    }

    private val maturity: Maturity = run {
        val lowercaseClassifier = extension?.lowercase() ?: return@run Maturity.STABLE
        for (maturity in Maturity.entries) {
            if (maturity.lowercaseRegex?.matches(lowercaseClassifier) == true) return@run maturity
        }
        // If we cannot determine the maturity:
        Maturity.INTELLIJ_IDEA
    }

    private val buildNumber = extension?.dropWhile { !it.isDigit() }?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0

    override fun compareTo(other: KotlinVersion): Int {
        (major - other.major).let { if (it != 0) return it }
        (minor - other.minor).let { if (it != 0) return it }
        (patch - other.patch).let { if (it != 0) return it }
        (maturity.ordinal - other.maturity.ordinal).let { if (it != 0) return it }
        (buildNumber - other.buildNumber).let { if (it != 0) return it }
        return 0
    }

    override fun toString(): String = "$major.$minor.$patch${extension?.let { "-$it" } ?: ""}"
}

@TestBalloonInternalApi
public fun String.asKotlinVersion(): KotlinVersion {
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
