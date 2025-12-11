/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tools {

    private static final Logger logger = LoggerFactory.getLogger(Tools.class);

    public static boolean sendEmail(String to, String subject, String body) {
        logger.info("""
            :
            ----------------------------------------
            Email sent to: {}
            Subject: {}
            Body: {}
            ----------------------------------------""", to, subject, body);
        return true;
    }
}
