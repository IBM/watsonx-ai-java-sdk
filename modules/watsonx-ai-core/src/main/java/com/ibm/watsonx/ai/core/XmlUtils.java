/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Utility class for XML parsing and manipulation.
 */
public class XmlUtils {

    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    /**
     * Prevents direct instantiation of the {@code Builder}.
     */
    protected XmlUtils() {}

    /**
     * Parses the given XML string into a Document object.
     *
     * @param xml The XML string to be parsed.
     * @return A Document object representing the XML structure.
     */
    public static Document parse(String xml) {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
