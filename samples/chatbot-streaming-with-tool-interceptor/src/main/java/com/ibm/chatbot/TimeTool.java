/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.chat.ExecutableTool;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolArguments;

public class TimeTool implements ExecutableTool {

    private static final Logger logger = LoggerFactory.getLogger(TimeTool.class);
    private static final String TOOL_SCHEMA_NAME = "get_current_time";
    private static final Tool TOOL_SCHEMA = Tool.of(TOOL_SCHEMA_NAME, "Get the current time");

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
        logger.info("Executed get_current_time function");
        return "The current time is " + LocalTime.now();
    }
}
