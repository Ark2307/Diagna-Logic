package com.diagna.logic.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sha256Test {

    @Test
    void sameInputAlwaysProducesTheSameHash() {
        assertThat(Sha256.hex("hello")).isEqualTo(Sha256.hex("hello"));
    }

    @Test
    void differentInputsProduceDifferentHashes() {
        assertThat(Sha256.hex("hello")).isNotEqualTo(Sha256.hex("world"));
    }

    @Test
    void producesA64CharacterLowercaseHexString() {
        String hash = Sha256.hex("some content");
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void matchesTheKnownVectorForAnEmptyString() {
        // Well-known SHA-256 of the empty string.
        assertThat(Sha256.hex("")).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }
}
