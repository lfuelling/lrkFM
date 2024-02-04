package io.lerk.lrkFM.exceptions

import java.io.Serializable

/**
 * [NoAccessException].
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class NoAccessException(message: String?) : Exception(message), Serializable {
    companion object {
        const val serialVersionUID = 10L
    }
}
