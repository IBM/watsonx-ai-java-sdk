/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

public class XmlUtilsTest {

    @Test
    void should_parse_well_formed_xml() {
        Document doc = XmlUtils.parse("<Error><Code>NoSuchKey</Code></Error>");
        assertEquals("NoSuchKey", doc.getElementsByTagName("Code").item(0).getTextContent());
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
