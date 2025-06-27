/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textextraction;

import static java.util.Objects.requireNonNull;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.Type;

public class FileUtils {

  public static String addExtension(String fileName, Type extension) {
    requireNonNull(fileName, "fileName cannot be null");
    requireNonNull(extension, "extension cannot be null");

    if (fileName.endsWith(extension.value()))
      return fileName;

    var index = fileName.lastIndexOf(".");
    fileName = index > 0 ? fileName.substring(0, index).concat(".") : fileName.concat(".");

    return switch(extension) {
      case HTML -> fileName.concat("html");
      case JSON -> fileName.concat("json");
      case MD -> fileName.concat("md");
      case PAGE_IMAGES -> fileName;
      case PLAIN_TEXT -> fileName.concat("txt");
    };
  }

  public static String extractFileName(String path) {
    requireNonNull(path, "path cannot be null");
    var index = path.lastIndexOf("/");
    return index > 0 ? path.substring(index + 1) : path;
  }
}
