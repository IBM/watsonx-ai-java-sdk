/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textextraction;

import static java.util.Objects.requireNonNull;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.Type;

/**
 * Utility class for text extraction operations.
 */
public class TextExtractionUtils {

    /**
     * Adds a file extension to a given file name.
     *
     * @param fileName the original file name
     * @param extension the desired file extension
     * @return the file name with the specified extension
     */
    public static String addExtension(String fileName, Type extension) {
        requireNonNull(fileName, "fileName cannot be null");
        requireNonNull(extension, "extension cannot be null");

        var index = fileName.lastIndexOf(".");
        fileName = index > 0 ? fileName.substring(0, index) : fileName;

        return switch(extension) {
            case HTML -> fileName.concat(".html");
            case JSON -> fileName.concat(".json");
            case MD -> fileName.concat(".md");
            case PAGE_IMAGES -> fileName;
            case PLAIN_TEXT -> fileName.concat(".txt");
        };
    }
}
