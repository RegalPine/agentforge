package io.github.regalpine.agentforge.model;

/**
 * Interface for structured output models that can produce typed results.
 * <p>
 * Structured output models ensure that the model's output conforms to
 * a specified schema. This is critical for:
 * <ul>
 *   <li>Reliable data extraction</li>
 *   <li>Type-safe model responses</li>
 *   <li>Schema validation</li>
 *   <li>Integration with downstream systems</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * StructuredOutputModel model = ...;
 * OutputSpec spec = OutputSpec.jsonSchema("Person", personSchema);
 * StructuredChatRequest request = StructuredChatRequest.of(messages, spec);
 * StructuredChatResponse response = model.chatStructured(request);
 * Person person = response.asTyped(Person.class);
 * </pre>
 */
public interface StructuredOutputModel {

    /**
     * Process a structured chat request and return a typed response.
     *
     * @param request the structured chat request
     * @return the structured chat response
     * @throws ModelException if the model invocation fails
     * @throws StructuredOutputException if the output does not conform to the schema
     */
    StructuredChatResponse chatStructured(StructuredChatRequest request);

    /**
     * Get the model identifier.
     *
     * @return the model ID
     */
    String modelId();

    /**
     * Get the provider identifier.
     *
     * @return the provider ID
     */
    String providerId();
}
