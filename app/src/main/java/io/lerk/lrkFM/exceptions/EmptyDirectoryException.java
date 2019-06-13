package io.lerk.lrkFM.exceptions;

import java.io.Serializable;

/**
 * {@link EmptyDirectoryException}.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class EmptyDirectoryException extends Exception implements Serializable {
    static final long serialVersionUID = 10L;

    public EmptyDirectoryException(String message) {
        super(message);
    }
}
