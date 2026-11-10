package io.github.regalpine.agentforge.runtime.structured;

import java.util.Map;

/**
 * Stage in the output processing pipeline.
 * <p>
 * Each stage transforms raw output into more structured data.
 * The pipeline runs stages in order:
 * <ol>
 *   <li>Parse — extract JSON/text from raw output</li>
 *   <li>Validate — check against schema</li>
 *   <li>Normalize — normalize field names and types</li>
 *   <li>Map — map to target Java type</li>
 * </ol>
 *
 * @param <I> input type
 * @param <O> output type
 */
public interface PipelineStage<I, O> {

    /**
     * Process the input and produce output.
     *
     * @param input   the input to process
     * @param context the pipeline context
     * @return the processing result
     * @throws PipelineException if processing fails
     */
    O process(I input, PipelineContext context) throws PipelineException;

    /**
     * Get the stage name for reporting.
     *
     * @return the stage name
     */
    String name();
}
