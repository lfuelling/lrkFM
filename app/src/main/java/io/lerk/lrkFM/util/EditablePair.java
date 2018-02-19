package io.lerk.lrkFM.util;

/**
 * Editable pair. Such mystery.
 * @author Lukas Fülling (lukas@k40s.net)
 */
public class EditablePair<F, S> {

    /**
     * First member of pair.
     */
    private F first;

    /**
     * Second member of pair.
     */
    private S second;

    /**
     * Constructor.
     * @param first First member.
     * @param second Second member.
     */
    public EditablePair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Setter for first member of pair.
     * @param first replacement first.
     */
    public void setFirst(F first) {
        this.first = first;
    }

    /**
     * Setter for second member of pair.
     * @param second replacement second.
     */
    public void setSecond(S second) {
        this.second = second;
    }

    /**
     * Getter for first member.
     * @return first member of pair.
     */
    public F getFirst() {
        return first;
    }

    /**
     * Getter for second member.
     * @return second member of pair.
     */
    public S getSecond() {
        return second;
    }
}
