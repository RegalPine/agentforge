package io.github.regalpine.agentforge.runtime.structured;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pipeline stage that normalizes parsed data.
 * <p>
 * Normalization includes:
 * <ul>
 *   <li>Trimming string values</li>
 *   <li>Removing null entries (optional)</li>
 *   <li>Converting field names to a consistent format</li>
 * </ul>
 */
public final class NormalizerStage implements PipelineStage<Map<String, Object>, Map<String, Object>> {

    private final boolean removeNulls;
    private final boolean trimStrings;

    public NormalizerStage() {
        this(true, true);
    }

    public NormalizerStage(boolean removeNulls, boolean trimStrings) {
        this.removeNulls = removeNulls;
        this.trimStrings = trimStrings;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> process(Map<String, Object> input, PipelineContext context) throws PipelineException {
        if (input == null) {
            throw new PipelineException(name(), "Input map is null");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (removeNulls && value == null) {
                continue;
            }

            if (trimStrings && value instanceof String s) {
                value = s.trim();
            }

            if (value instanceof Map<?, ?> nested) {
                Map<String, Object> normalizedNested = new LinkedHashMap<>();
                for (Map.Entry<?, ?> ne : ((Map<String, Object>) nested).entrySet()) {
                    normalizedNested.put((String) ne.getKey(), ne.getValue());
                }
                value = normalizedNested;
            }

            result.put(key, value);
        }
        return result;
    }

    @Override
    public String name() {
        return "Normalizer";
    }
}
