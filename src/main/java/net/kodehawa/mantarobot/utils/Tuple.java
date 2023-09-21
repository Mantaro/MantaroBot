package net.kodehawa.mantarobot.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Tuple<A, B, C>(A first, B second, C third) {
    @JsonCreator
    public Tuple(@JsonProperty("first") A first, @JsonProperty("second") B second, @JsonProperty("third") C third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public static <A, B, C> Tuple<A, B, C> of(A first, B second, C third) {
        return new Tuple<>(first, second, third);
    }
}
