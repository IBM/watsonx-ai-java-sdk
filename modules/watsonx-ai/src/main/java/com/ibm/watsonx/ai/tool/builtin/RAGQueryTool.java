/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool.builtin;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.util.HashMap;
import java.util.List;
import com.ibm.watsonx.ai.chat.ExecutableTool;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolArguments;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.core.Experimental;
import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolService;

/**
 * Tool to search and retrieve relevant information from documents stored in vector indexes using semantic similarity.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * var ragQueryTool = RAGQueryTool.builder()
 *     .toolService(toolService)
 *     .projectId("my-project-id")
 *     .vectorIndexIds("index-1")
 *     .description("The document contains information about the watsonx.ai project.")
 *     .build();
 *
 * String result = ragQueryTool.query("Tell me more about the project");
 * }</pre>
 *
 */
@Experimental
public class RAGQueryTool implements ExecutableTool {

    private static final String TOOL_SCHEMA_NAME = "rag_query";
    private final Tool TOOL_SCHEMA;

    private final ToolService toolService;
    private final String projectId;
    private final String spaceId;
    private final List<String> vectorIndexIds;
    private final String description;

    private RAGQueryTool(Builder builder) {
        toolService = requireNonNull(builder.toolService, "ToolService cannot be null");
        vectorIndexIds = requireNonNull(builder.vectorIndexIds, "vectorIndexIds cannot be null");
        projectId = builder.projectId;
        spaceId = builder.spaceId;

        if (isNull(projectId) && isNull(spaceId))
            throw new IllegalArgumentException("Either projectId or spaceId must be provided");

        if (vectorIndexIds.isEmpty())
            throw new IllegalArgumentException("At least one vector index id must be provided");

        if (nonNull(builder.description))
            description = """
                Use this tool to retrieve relevant information from indexed documents using semantic search.
                %s""".formatted(builder.description);
        else
            description = "Use this tool to retrieve relevant information from indexed documents using semantic search.";

        TOOL_SCHEMA = Tool.of(
            TOOL_SCHEMA_NAME,
            description,
            JsonSchema.object()
                .property("input", JsonSchema.string("User question or search query optimized for semantic similarity search in vector databases"))
                .required("input")
                .build());
    }

    @Override
    public String name() {
        return TOOL_SCHEMA_NAME;
    }

    @Override
    public Tool schema() {
        return TOOL_SCHEMA;
    }

    @Override
    public String execute(ToolArguments args) {

        if (isNull(args) || !args.contains("input"))
            throw new IllegalArgumentException("input argument is required");

        return query(args.get("input"));
    }

    /**
     * Queries the vector indexes to retrieve relevant information based on the provided input.
     *
     * @param input the user's question or query text
     * @return a string containing the retrieved relevant information from the indexed documents
     */
    public String query(String input) {

        requireNonNull(input, "input can't be null");

        var config = new HashMap<String, Object>();

        if (nonNull(projectId))
            config.put("projectId", projectId);
        else
            config.put("spaceId", spaceId);

        if (vectorIndexIds.size() == 1)
            config.put("vectorIndexId", vectorIndexIds.get(0));
        else
            config.put("vectorIndexIds", vectorIndexIds);

        return toolService.run(ToolRequest.unstructuredInput("RAGQuery", input, config));
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * var ragQueryTool = RAGQueryTool.builder()
     *     .toolService(toolService)
     *     .projectId("my-project-id")
     *     .vectorIndexIds("index-1")
     *     .description("The document contains information about the watsonx.ai project.")
     *     .build();
     *
     * String result = ragQueryTool.query("Tell me more about the project");
     * }</pre>
     *
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link RAGQueryTool} instances with configurable parameters.
     */
    public static final class Builder {
        private ToolService toolService;
        private String projectId;
        private String spaceId;
        private List<String> vectorIndexIds;
        private String description;

        private Builder() {}

        /**
         * Sets the {@link ToolService} used to execute RAG queries.
         *
         * @param toolService the tool service instance
         */
        public Builder toolService(ToolService toolService) {
            this.toolService = toolService;
            return this;
        }

        /**
         * Sets the project id where the vector indexes are located.
         * <p>
         * Either {@code projectId} or {@code spaceId} must be provided.
         *
         * @param projectId the project id
         */
        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        /**
         * Sets the space id where the vector indexes are located.
         * <p>
         * Either {@code projectId} or {@code spaceId} must be provided.
         *
         * @param spaceId the space id
         */
        public Builder spaceId(String spaceId) {
            this.spaceId = spaceId;
            return this;
        }

        /**
         * Sets the vector index ids to query.
         * <p>
         * At least one vector index id must be provided.
         *
         * @param vectorIndexIds one or more vector index ids
         */
        public Builder vectorIndexIds(String... vectorIndexIds) {
            return vectorIndexIds(List.of(vectorIndexIds));
        }

        /**
         * Sets the vector index ids to query.
         * <p>
         * At least one vector index id must be provided.
         *
         * @param vectorIndexIds one or more vector index ids
         */
        public Builder vectorIndexIds(List<String> vectorIndexIds) {
            this.vectorIndexIds = vectorIndexIds;
            return this;
        }

        /**
         * Sets a custom description for the tool.
         * <p>
         * This description will be appended to the base description to provide additional context about the indexed documents.
         *
         * @param description additional context about the indexed documents
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds a {@link RAGQueryTool} instance using the configured parameters.
         *
         * @return a new instance of {@link RAGQueryTool}
         */
        public RAGQueryTool build() {
            return new RAGQueryTool(this);
        }
    }
}
