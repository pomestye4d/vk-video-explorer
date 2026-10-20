/*
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gridnine.platform.elsa.common.meta.common;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

public final class XmlUtils {
    public static XmlNode parseXml(byte[] content) throws Exception {
        var result = new XmlNode();
        var db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc;
        try {
            doc = db.parse(new ByteArrayInputStream(content));
        } finally {
            db.reset();
        }
        updateElm(result, doc.getDocumentElement());
        return result;
    }

    private static void updateElm(XmlNode result, Element elm) {
        result.setName(elm.getTagName());
        var attributes = elm.getAttributes();
        var attributeLength = attributes.getLength();
        for (int i = 0; i < attributeLength; i++) {
            var item = attributes.item(i);
            result.getAttributes().put(item.getNodeName(), item.getNodeValue());
        }
        var children = elm.getChildNodes();
        var childrenLength = children.getLength();
        for (int i = 0; i < childrenLength; i++) {
            var child = children.item(i);
            if (child != null) {
                switch (child.getNodeType()) {
                    case Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> result.setValue(child.getNodeValue());
                    case Node.ELEMENT_NODE -> {
                        var childNode = new XmlNode();
                        updateElm(childNode, (Element) child);
                        result.getChildren().add(childNode);
                    }
                }
            }
        }
    }

    private XmlUtils() {
    }
}
