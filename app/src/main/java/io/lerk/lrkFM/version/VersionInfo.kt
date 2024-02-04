package io.lerk.lrkFM.version



/**
 * Version singleton.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class VersionInfo private constructor(val current: Version, val latest: Version) {

    companion object {
        private var instance: VersionInfo? = null
        fun parse(callback: (VersionInfo?) -> Unit, current: String, latest: String) {
            if (instance == null) {
                instance = VersionInfo(
                    Version.Companion.fromString(current),
                    Version.Companion.fromString(latest)
                )
            }
            callback(instance)
        }
    }
}
