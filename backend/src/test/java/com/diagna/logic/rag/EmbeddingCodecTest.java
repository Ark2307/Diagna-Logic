package com.diagna.logic.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddingCodecTest {

    @Test
    void roundTripsExactly() {
        float[] original = {0.1f, -0.2f, 3.14159f, -0.0f, 1e10f, -1e-10f};
        assertThat(EmbeddingCodec.decode(EmbeddingCodec.encode(original))).isEqualTo(original);
    }

    @Test
    void producesExpectedByteLength() {
        float[] vector = new float[1536];
        assertThat(EmbeddingCodec.encode(vector)).hasSize(1536 * 4);
    }

    @Test
    void emptyVectorRoundTrips() {
        assertThat(EmbeddingCodec.decode(EmbeddingCodec.encode(new float[0]))).isEmpty();
    }

    @Test
    void rejectsByteLengthNotAMultipleOfFour() {
        assertThatThrownBy(() -> EmbeddingCodec.decode(new byte[]{1, 2, 3})).isInstanceOf(IllegalArgumentException.class);
    }
}
