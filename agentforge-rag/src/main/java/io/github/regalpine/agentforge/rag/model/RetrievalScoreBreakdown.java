package io.github.regalpine.agentforge.rag.model;

/**
 * Score breakdown for a retrieval result.
 * AE-09 §37: Score Breakdown.
 *
 * <p>Explains why a chunk was selected by showing
 * its individual scores from keyword, semantic, and rerank stages.
 */
public record RetrievalScoreBreakdown(
    Double keywordScore,
    Double semanticScore,
    Double rerankScore
) {

    public static RetrievalScoreBreakdown keyword(double score) {
        return new RetrievalScoreBreakdown(score, null, null);
    }

    public static RetrievalScoreBreakdown semantic(double score) {
        return new RetrievalScoreBreakdown(null, score, null);
    }

    public static RetrievalScoreBreakdown of(double keyword, double semantic) {
        return new RetrievalScoreBreakdown(keyword, semantic, null);
    }

    public static RetrievalScoreBreakdown full(double keyword, double semantic, double rerank) {
        return new RetrievalScoreBreakdown(keyword, semantic, rerank);
    }

    /**
     * Compute the combined score as a weighted average of available scores.
     */
    public double combined() {
        double sum = 0.0;
        int count = 0;
        if (keywordScore != null) { sum += keywordScore; count++; }
        if (semanticScore != null) { sum += semanticScore; count++; }
        if (rerankScore != null) { sum += rerankScore; count++; }
        return count == 0 ? 0.0 : sum / count;
    }
}
