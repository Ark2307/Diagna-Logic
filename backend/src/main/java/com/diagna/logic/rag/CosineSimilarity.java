package com.diagna.logic.rag;

/**
 * Exact cosine similarity between two equal-length vectors. "Exact" is the
 * point: because retrieval is always scoped to one meeting's chunk set (at
 * most ~90 vectors — see CLAUDE.md), a brute-force scan here is sub-millisecond
 * and avoids the approximation an ANN index would introduce for no benefit
 * at this scale.
 */
public final class CosineSimilarity {

    private CosineSimilarity() {
    }

    /**
     * @return the cosine similarity in {@code [-1, 1]}, or {@code 0.0} if either vector is all-zero
     *         (rather than {@code NaN} from a division by zero)
     * @throws IllegalArgumentException if the vectors have different lengths
     */
    public static double of(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vector dimension mismatch: " + a.length + " vs " + b.length);
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
