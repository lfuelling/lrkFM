package io.lerk.lrkFM.exceptions

import java.io.Serializable

/**
 * [EmptyDirectoryException].
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class EmptyDirectoryException(message: String?) : Exception(message), Serializable {
    companion object {
        const val serialVersionUID = 10L
    }
}
