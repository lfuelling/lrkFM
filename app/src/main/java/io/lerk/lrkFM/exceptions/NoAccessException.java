package io.lerk.lrkFM.exceptions;

import java.io.Serializable;

/**
 * {@link NoAccessException}.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class NoAccessException extends Exception implements Serializable {
    static final long serialVersionUID = 10L;

    public NoAccessException(String message) {
        super(message);
    }
}
