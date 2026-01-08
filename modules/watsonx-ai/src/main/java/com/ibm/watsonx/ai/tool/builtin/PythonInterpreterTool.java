/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool.builtin;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import java.util.Map;
import com.ibm.watsonx.ai.chat.ExecutableTool;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolArguments;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.core.Experimental;
import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolService;

/**
 * Tool to execute Python code in a remote runtime environment.
 * <p>
 * This tool runs Python code and returns the console output. It is intended for isolated calculations, computations, data manipulation, or
 * visualizations.
 * <p>
 * The runtime provides access to common scientific and data analysis libraries such as:
 * <ul>
 * <li><b>numpy</b>, <b>pandas</b>, <b>scipy</b>, <b>sympy</b> for data processing and computations</li>
 * <li><b>matplotlib</b> for plotting charts</li>
 * </ul>
 * Other Python libraries may also be available, but it is recommended to prefer the ones above.
 * <p>
 * Usage guidelines:
 * <ul>
 * <li>Always provide the full code to run in a single call; do not invoke the tool multiple times in a row.</li>
 * <li>Use qualified imports (e.g. {@code import numpy as np; np.array([...])}) rather than direct symbol imports.</li>
 * <li>Do not attempt to install libraries manually, it will not work in the runtime.</li>
 * <li>If execution fails, regenerate valid Python code and retry.</li>
 * <li>If the tool output starts with {@code IMAGE(}, follow the returned instructions to render the image.</li>
 * </ul>
 * <p>
 * <b>Deployment ID:</b> A {@code pythonInterpreterDeploymentId} must be provided at construction time. This identifier specifies the remote
 * deployment in which the Python code will be executed.
 */
@Experimental
public class PythonInterpreterTool implements ExecutableTool {

    private static final String TOOL_SCHEMA_NAME = "python_interpreter";
    private static final Tool TOOL_SCHEMA = Tool.of(
        TOOL_SCHEMA_NAME, """
            Executes Python code and returns the console output. \
            Use this tool for calculations, data manipulation, or visualizations.
            Important: Only printed output (via print()) is returned. \
            Results not explicitly printed will be ignored.""",
        JsonSchema.object()
            .property("code", JsonSchema.string("The Python code to execute"))
            .required("code")
            .build());

    private final ToolService toolService;
    private final String deploymentId;

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
        if (isNull(args) || !args.contains("code"))
            throw new IllegalArgumentException("code argument is required");

        return run(args.get("code"));
    }

    /**
     * Constructs a new {@code PythonInterpreterTool} with the specified {@link ToolService} and deployment identifier for the Python interpreter
     * runtime.
     *
     * @param toolService the service used to execute the tool calls
     * @param pythonInterpreterDeploymentId the identifier of the Python interpreter deployment where the code will be executed
     */
    public PythonInterpreterTool(ToolService toolService, String pythonInterpreterDeploymentId) {
        this.toolService = requireNonNull(toolService, "ToolService can't be null");
        this.deploymentId = pythonInterpreterDeploymentId;
    }

    /**
     * Runs the Python code.
     *
     * @param code the Python code to run
     * @return the console output produced by the execution, including any printed text
     */
    public String run(String code) {

        requireNonNull(code, "code can't be null");

        var structuredInput = Map.<String, Object>of("code", code);
        var config = Map.<String, Object>of("deploymentId", deploymentId);

        return toolService.run(ToolRequest.structuredInput("PythonInterpreter", structuredInput, config));
    }
}
