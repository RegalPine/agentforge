package io.github.regalpine.agentforge.runtime.structured;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Output Processing Pipeline for structured output.
 * <p>
 * Runs a sequence of pipeline stages to transform raw LLM output
 * into validated, structured data:
 * <pre>
 *   Raw Output → Parse → Validate → Normalize → StructuredResult
 * </pre>
 * Each stage can add errors/warnings to the pipeline context.
 * If errors are accumulated, the result will be marked as failed.
 */
public final class OutputProcessingPipeline {

    private final List<PipelineStage<?, ?>> stages;
    private final int maxRepairAttempts;

    private OutputProcessingPipeline(List<PipelineStage<?, ?>> stages, int maxRepairAttempts) {
        this.stages = List.copyOf(stages);
        this.maxRepairAttempts = maxRepairAttempts;
    }

    /**
     * Process raw output through the pipeline.
     *
     * @param rawOutput the raw LLM output
     * @return the structured result
     */
    public StructuredResult process(String rawOutput) {
        Objects.requireNonNull(rawOutput, "rawOutput cannot be null");

        PipelineContext context = new PipelineContext();
        Object current = rawOutput;

        for (PipelineStage stage : stages) {
            try {
                current = stage.process(current, context);
            } catch (PipelineException e) {
                context.addError(e.getMessage());
            } catch (Exception e) {
                context.addError("Stage '" + stage.name() + "' threw unexpected error: " + e.getMessage());
            }

            if (context.hasErrors()) {
                return StructuredResult.failure(rawOutput, context.toReport());
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = current instanceof Map ? (Map<String, Object>) current : Map.of();
        return StructuredResult.success(data, rawOutput, context.toReport());
    }

    /**
     * Create a new builder.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a default pipeline with JSON parsing and validation.
     *
     * @return a default pipeline
     */
    public static OutputProcessingPipeline defaults() {
        return builder()
            .addStage(new JsonParserStage())
            .addStage(new NormalizerStage())
            .build();
    }

    /**
     * Builder for OutputProcessingPipeline.
     */
    public static class Builder {
        private final List<PipelineStage<?, ?>> stages = new ArrayList<>();
        private int maxRepairAttempts = 3;

        public Builder addStage(PipelineStage<?, ?> stage) {
            stages.add(Objects.requireNonNull(stage, "stage cannot be null"));
            return this;
        }

        public Builder maxRepairAttempts(int maxRepairAttempts) {
            this.maxRepairAttempts = maxRepairAttempts;
            return this;
        }

        public OutputProcessingPipeline build() {
            return new OutputProcessingPipeline(stages, maxRepairAttempts);
        }
    }
}
