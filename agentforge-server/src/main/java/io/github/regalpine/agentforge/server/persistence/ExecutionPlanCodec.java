package io.github.regalpine.agentforge.server.persistence;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.github.regalpine.agentforge.graph.EdgeType;
import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.graph.ir.ExecutionNode;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.graph.ir.RoutingEntry;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * JSON codec for {@link ExecutionPlan} serialization.
 * <p>
 * Uses Jackson streaming API to handle the complex nested IR types
 * (EntityId, Version, NodeType, EdgeType) without requiring annotations
 * on kernel types.
 */
final class ExecutionPlanCodec {

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private ExecutionPlanCodec() {}

    static String encode(ExecutionPlan plan) {
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = JSON_FACTORY.createGenerator(sw)) {
            writePlan(g, plan);
            g.flush();
            return sw.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode ExecutionPlan", e);
        }
    }

    static ExecutionPlan decode(String json) {
        try (JsonParser p = JSON_FACTORY.createParser(json)) {
            p.nextToken();
            return readPlan(p);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to decode ExecutionPlan", e);
        }
    }

    // ── Write ──

    private static void writePlan(JsonGenerator g, ExecutionPlan plan) throws IOException {
        g.writeStartObject();
        g.writeStringField("graphId", plan.graphId().value());
        g.writeStringField("graphVersion", plan.graphVersion().value());
        g.writeStringField("fingerprint", plan.fingerprint());
        g.writeStringField("entryNode", plan.entryNode().value());

        g.writeFieldName("nodes");
        g.writeStartArray();
        for (ExecutionNode node : plan.nodes()) {
            writeNode(g, node);
        }
        g.writeEndArray();

        g.writeFieldName("routing");
        g.writeStartObject();
        for (var entry : plan.routing().entrySet()) {
            g.writeFieldName(entry.getKey().value());
            g.writeStartArray();
            for (RoutingEntry re : entry.getValue()) {
                writeRoutingEntry(g, re);
            }
            g.writeEndArray();
        }
        g.writeEndObject();

        g.writeFieldName("policies");
        writeMap(g, plan.policies());

        g.writeEndObject();
    }

    private static void writeNode(JsonGenerator g, ExecutionNode node) throws IOException {
        g.writeStartObject();
        g.writeStringField("id", node.id().value());
        g.writeStringField("type", node.type().name());
        g.writeFieldName("config");
        writeMap(g, node.config());
        g.writeFieldName("resolvedReferences");
        writeMap(g, node.resolvedReferences());
        g.writeEndObject();
    }

    private static void writeRoutingEntry(JsonGenerator g, RoutingEntry re) throws IOException {
        g.writeStartObject();
        g.writeStringField("target", re.target().value());
        g.writeStringField("edgeType", re.edgeType().name());
        if (re.condition() != null) {
            g.writeStringField("condition", re.condition());
        }
        g.writeEndObject();
    }

    private static void writeMap(JsonGenerator g, Map<String, Object> map) throws IOException {
        g.writeStartObject();
        for (var entry : new TreeMap<>(map).entrySet()) {
            g.writeFieldName(entry.getKey());
            writeValue(g, entry.getValue());
        }
        g.writeEndObject();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(JsonGenerator g, Object value) throws IOException {
        if (value == null) {
            g.writeNull();
        } else if (value instanceof String s) {
            g.writeString(s);
        } else if (value instanceof Integer i) {
            g.writeNumber(i);
        } else if (value instanceof Long l) {
            g.writeNumber(l);
        } else if (value instanceof Double d) {
            g.writeNumber(d);
        } else if (value instanceof Float f) {
            g.writeNumber(f);
        } else if (value instanceof Boolean b) {
            g.writeBoolean(b);
        } else if (value instanceof Map<?, ?> m) {
            writeMap(g, (Map<String, Object>) m);
        } else if (value instanceof List<?> list) {
            g.writeStartArray();
            for (Object item : list) {
                writeValue(g, item);
            }
            g.writeEndArray();
        } else {
            g.writeString(value.toString());
        }
    }

    // ── Read ──

    private static ExecutionPlan readPlan(JsonParser p) throws IOException {
        EntityId graphId = null;
        Version graphVersion = null;
        String fingerprint = null;
        EntityId entryNode = null;
        List<ExecutionNode> nodes = new ArrayList<>();
        Map<EntityId, List<RoutingEntry>> routing = new LinkedHashMap<>();
        Map<String, Object> policies = Map.of();

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String field = p.currentName();
            p.nextToken();
            switch (field) {
                case "graphId" -> graphId = new EntityId(p.getText());
                case "graphVersion" -> graphVersion = new Version(p.getText());
                case "fingerprint" -> fingerprint = p.getText();
                case "entryNode" -> entryNode = new EntityId(p.getText());
                case "nodes" -> {
                    while (p.nextToken() != JsonToken.END_ARRAY) {
                        nodes.add(readNode(p));
                    }
                }
                case "routing" -> {
                    while (p.nextToken() != JsonToken.END_OBJECT) {
                        EntityId key = new EntityId(p.currentName());
                        List<RoutingEntry> entries = new ArrayList<>();
                        p.nextToken(); // START_ARRAY
                        while (p.nextToken() != JsonToken.END_ARRAY) {
                            entries.add(readRoutingEntry(p));
                        }
                        routing.put(key, entries);
                    }
                }
                case "policies" -> policies = readMap(p);
                default -> p.skipChildren();
            }
        }

        return new ExecutionPlan(graphId, graphVersion, fingerprint, entryNode, nodes, routing, policies);
    }

    private static ExecutionNode readNode(JsonParser p) throws IOException {
        EntityId id = null;
        NodeType type = null;
        Map<String, Object> config = Map.of();
        Map<String, Object> resolvedReferences = Map.of();

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String field = p.currentName();
            p.nextToken();
            switch (field) {
                case "id" -> id = new EntityId(p.getText());
                case "type" -> type = NodeType.valueOf(p.getText());
                case "config" -> config = readMap(p);
                case "resolvedReferences" -> resolvedReferences = readMap(p);
                default -> p.skipChildren();
            }
        }

        return new ExecutionNode(id, type, config, resolvedReferences);
    }

    private static RoutingEntry readRoutingEntry(JsonParser p) throws IOException {
        EntityId target = null;
        EdgeType edgeType = EdgeType.NORMAL;
        String condition = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String field = p.currentName();
            p.nextToken();
            switch (field) {
                case "target" -> target = new EntityId(p.getText());
                case "edgeType" -> edgeType = EdgeType.valueOf(p.getText());
                case "condition" -> condition = p.getText();
                default -> p.skipChildren();
            }
        }

        return new RoutingEntry(target, edgeType, condition);
    }

    private static Map<String, Object> readMap(JsonParser p) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String key = p.currentName();
            p.nextToken();
            map.put(key, readValue(p));
        }
        return Map.copyOf(map);
    }

    private static Object readValue(JsonParser p) throws IOException {
        return switch (p.currentToken()) {
            case VALUE_STRING -> p.getText();
            case VALUE_NUMBER_INT -> p.getLongValue();
            case VALUE_NUMBER_FLOAT -> p.getDoubleValue();
            case VALUE_TRUE -> true;
            case VALUE_FALSE -> false;
            case VALUE_NULL -> null;
            case START_OBJECT -> readMap(p);
            case START_ARRAY -> {
                List<Object> list = new ArrayList<>();
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    list.add(readValue(p));
                }
                yield List.copyOf(list);
            }
            default -> {
                p.skipChildren();
                yield null;
            }
        };
    }
}
