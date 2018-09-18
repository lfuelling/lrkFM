package io.lerk.lrkFM.exceptions;

import java.io.Serializable;

/**
 * {@link EmptyDirectoryException}.
 */
public class EmptyDirectoryException extends Exception implements Serializable {
    static final long serialVersionUID = 10L;

    public EmptyDirectoryException(String message) {
        super(message);
    }
}
