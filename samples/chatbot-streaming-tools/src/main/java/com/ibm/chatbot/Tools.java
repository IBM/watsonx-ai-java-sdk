/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

public class Tools {


    public record SendEmailArguments(String email, String subject, String body) {};

    public static boolean sendEmail(String to, String subject, String body) {
        System.out.println("""

            ----------------------------------------
            Executed sendEmail function with parameters:
            Email sent to: %s
            Subject: %s
            Body: %s
            ----------------------------------------""".formatted(to, subject, body));
        return true;
    }
}
