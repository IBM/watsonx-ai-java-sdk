/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

public class XmlUtilsTest {

    @Test
    void should_parse_well_formed_xml() {
        Document doc = XmlUtils.parse("<Error><Code>NoSuchKey</Code></Error>");
        assertEquals("NoSuchKey", doc.getElementsByTagName("Code").item(0).getTextContent());
    }

    @Test
    @ResourceLock(Resources.SYSTEM_ERR)
    void should_preserve_malformed_xml_failure_without_writing_to_standard_error() {
        var originalError = System.err;
        var output = new ByteArrayOutputStream();
        try (var capturedError = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setErr(capturedError);
            var exception = assertThrows(RuntimeException.class, () -> XmlUtils.parse("<Error><Code>broken</Error>"));
            assertInstanceOf(SAXParseException.class, exception.getCause());
            assertEquals("", output.toString(StandardCharsets.UTF_8));
        } finally {
            System.setErr(originalError);
        }
    }

    @Test
    void should_reject_doctype_to_prevent_xxe() {
        String malicious = """
            <?xml version="1.0"?>
            <!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
            <Error><Code>&xxe;</Code></Error>""";
        assertThrows(RuntimeException.class, () -> XmlUtils.parse(malicious));
    }

    @Test
    void should_reject_entity_expansion_to_prevent_dos() {
        String billionLaughs = """
            <?xml version="1.0"?>
            <!DOCTYPE lolz [
              <!ENTITY lol "lol">
              <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;">
            ]>
            <lolz>&lol2;</lolz>""";
        assertThrows(RuntimeException.class, () -> XmlUtils.parse(billionLaughs));
    }
}
