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

package com.gridnine.platform.elsa.gradle.parser.adminUi;

import com.gridnine.platform.elsa.gradle.meta.adminUi.AdminUiMetaRegistry;
import com.gridnine.platform.elsa.gradle.meta.adminUi.div.NestedDivDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.div.RootDivDescription;
import com.gridnine.platform.elsa.gradle.meta.common.XmlNode;

public class DivMetadataParser {

    public static NestedDivDescription parseDiv(XmlNode node, boolean root, String rootContainerName, AdminUiMetaRegistry registry) {
        var divDescription =  root? new RootDivDescription(): new NestedDivDescription();
        if (divDescription instanceof  RootDivDescription rtd) {
            rtd.setClassName(AdminUiParserHelper.getClassName(node));
            registry.getContainers().put(rtd.getClassName(), divDescription);
        }
        divDescription.setStyle(node.getAttribute("style"));
        divDescription.setId(node.getAttribute("id"));
        node.getChildren().forEach(child -> {
            divDescription.getChildren().add(AdminUiParserHelper.parse(child, false, rootContainerName, registry));
        });
        return divDescription;
    }
}
