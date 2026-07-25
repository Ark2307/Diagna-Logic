package com.diagna.logic.rag;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Converts between {@code float[]} vectors and the raw little-endian byte
 * layout stored in {@code MeetingChunk.embedding} (BSON BinData) — half the
 * size of storing the same precision as a BSON array of doubles, for the
 * whole corpus's ~6,200 chunks. A fixed byte order is used explicitly
 * (rather than platform-default) so the format is reproducible regardless
 * of what machine wrote or reads it.
 */
public final class EmbeddingCodec {

    private static final int BYTES_PER_FLOAT = 4;

    private EmbeddingCodec() {
    }

    public static byte[] encode(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * BYTES_PER_FLOAT).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : vector) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    public static float[] decode(byte[] bytes) {
        if (bytes.length % BYTES_PER_FLOAT != 0) {
            throw new IllegalArgumentException("Byte length " + bytes.length + " is not a multiple of " + BYTES_PER_FLOAT);
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] vector = new float[bytes.length / BYTES_PER_FLOAT];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = buffer.getFloat();
        }
        return vector;
    }
}
