package com.meetingiq.platform.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

class CosineSimilarityTest {

    @Test
    void identicalVectorsScoreOne() {
        float[] v = {1f, 2f, 3f};
        assertThat(CosineSimilarity.of(v, v)).isCloseTo(1.0, offset(1e-9));
    }

    @Test
    void orthogonalVectorsScoreZero() {
        float[] a = {1f, 0f};
        float[] b = {0f, 1f};
        assertThat(CosineSimilarity.of(a, b)).isCloseTo(0.0, offset(1e-9));
    }

    @Test
    void oppositeVectorsScoreNegativeOne() {
        float[] a = {1f, 0f};
        float[] b = {-1f, 0f};
        assertThat(CosineSimilarity.of(a, b)).isCloseTo(-1.0, offset(1e-9));
    }

    @Test
    void zeroVectorScoresZeroRatherThanNaN() {
        float[] zero = {0f, 0f, 0f};
        float[] other = {1f, 2f, 3f};
        assertThat(CosineSimilarity.of(zero, other)).isZero();
        assertThat(CosineSimilarity.of(zero, zero)).isZero();
    }

    @Test
    void differentDimensionsThrow() {
        float[] a = {1f, 2f};
        float[] b = {1f, 2f, 3f};
        assertThatThrownBy(() -> CosineSimilarity.of(a, b)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scaleInvariant() {
        float[] a = {1f, 2f, 3f};
        float[] scaled = {2f, 4f, 6f};
        assertThat(CosineSimilarity.of(a, scaled)).isCloseTo(1.0, offset(1e-9));
    }
}
