package io.lerk.lrkFM.util;

/**
 * Created by lfuelling on 30.03.17.
 */

public class EditablePair<F, S> {
    private F first; //first member of pair
    private S second; //second member of pair

    public EditablePair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public void setFirst(F first) {
        this.first = first;
    }

    public void setSecond(S second) {
        this.second = second;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }
}
