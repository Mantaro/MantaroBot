package net.kodehawa.mantarobot.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Pair<L, R> {
    private final L left;
    private final R right;

    @JsonCreator
    public Pair(@JsonProperty("left") L left, @JsonProperty("right") R right) {
        this.left = left;
        this.right = right;
    }

    public static <L, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }
}
