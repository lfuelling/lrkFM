package io.lerk.lrkFM.version

/**
 * The app version.
 *
 *
 * Example: 1.0.0-rc.1
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class Version private constructor(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val sub: Int,
    val beta: Boolean,
    val rc: Boolean
) {

    fun newerThan(v: Version?): Boolean {
        if (major > v!!.major) {
            return true
        } else if (major == v.major) {
            if (minor > v.minor) {
                return true
            } else if (minor == v.minor) {
                if (patch > v.patch) {
                    return true
                } else if (patch == v.patch) {
                    return sub > v.sub
                }
            }
        }
        return false
    }

    override fun toString(): String {
        return "$major.$minor.$patch" +
                if (beta) SPLIT_BETA +
                        (if (sub > 9) sub else "0$sub") else if (rc) SPLIT_RC +
                        (if (sub > 9) sub else "0$sub") else "latest"
        // nosleep
    }

    companion object {
        private const val SPLIT_BETA = "b"
        private const val SPLIT_RC = "rc"
        @Throws(NumberFormatException::class)
        fun fromString(versionString: String): Version {
            val versionSplit =
                versionString.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val patchSplit: Array<String>
            val beta = versionString.split(SPLIT_BETA.toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray().size > 1
            val rc = versionString.split(SPLIT_RC.toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray().size > 1
            patchSplit = try {
                if (beta) {
                    versionSplit[if (versionSplit.size > 2) 2 else 1].split(SPLIT_BETA.toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                } else if (rc) {
                    versionSplit[if (versionSplit.size > 2) 2 else 1].split(SPLIT_RC.toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                } else {
                    arrayOf("0", "0")
                }
            } catch (e: IndexOutOfBoundsException) {
                arrayOf("0", "0")
            }
            return Version(
                versionSplit[0].toInt(),
                versionSplit[1].split(SPLIT_BETA.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[0].split(SPLIT_RC.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[0].toInt(),
                patchSplit[0].toInt(),
                patchSplit[1].toInt(),
                beta,
                rc
            )
        }
    }
}
