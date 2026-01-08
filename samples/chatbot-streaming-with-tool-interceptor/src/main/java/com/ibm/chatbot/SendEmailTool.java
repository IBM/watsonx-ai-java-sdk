/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.chat.ExecutableTool;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolArguments;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;

public class SendEmailTool implements ExecutableTool {

    private static final Logger logger = LoggerFactory.getLogger(SendEmailTool.class);
    private static final String TOOL_SCHEMA_NAME = "send_email";
    private static final Tool TOOL_SCHEMA = Tool.of(
        TOOL_SCHEMA_NAME,
        "Send an email",
        JsonSchema.object()
            .property("to", JsonSchema.string("The email address of the recipient"))
            .property("subject", JsonSchema.string("The subject of the email"))
            .property("body", JsonSchema.string("The body of the email"))
            .required("to", "subject", "body")
    );

    @Override
    public String name() {
        return TOOL_SCHEMA_NAME;
    }

    @Override
    public Tool schema() {
        return TOOL_SCHEMA;
    }

    @Override
    public String execute(ToolArguments toolArgs) {

        var to = toolArgs.get("to");
        var subject = toolArgs.get("subject");
        var body = toolArgs.get("body");
        logger.info("""
            Executed send_email function with parameters:
            Email sent to: %s
            Subject: %s
            Body: %s""".formatted(to, subject, body));

        return "Email sent successfully";
    }

}
