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
import com.gridnine.platform.elsa.gradle.meta.adminUi.BaseAdminUiElementDescription;
import com.gridnine.platform.elsa.gradle.meta.common.XmlNode;
import com.gridnine.platform.elsa.gradle.meta.webApp.CustomContainerWebElementDescription;
import com.gridnine.platform.elsa.gradle.meta.webApp.CustomWebElementDescription;
import com.gridnine.platform.elsa.gradle.parser.common.CommonParserUtils;
import com.gridnine.platform.elsa.gradle.parser.custom.CustomMetaRegistryParser;

public class AdminUiParserHelper {


    public static String getClassName(XmlNode node) {
        return  node.getAttribute("class-name");
    }

    public static BaseAdminUiElementDescription parse(XmlNode node, boolean root, String rootClassName, AdminUiMetaRegistry registry) {
        if("form".equals(node.getName())){
            return FormMetadataParser.parseForm(node, root, rootClassName, registry);
        }
        if("grid".equals(node.getName())){
            return GridMetadataParser.parseForm(node, root, rootClassName, registry);
        }
        if("tabs".equals(node.getName())){
            return TabsMetadataParser.parseTabs(node, root, rootClassName, registry);
        }
        if("rich-text-editor".equals(node.getName())){
            return StandaloneElementsMetadataParser.parse(node);
        }
        if("editable-table".equals(node.getName())){
            return StandaloneElementsMetadataParser.parseEditableTable(node, rootClassName, registry);
        }
        if("button".equals(node.getName())){
            return StandaloneElementsMetadataParser.parseButton(node);
        }
        if("div".equals(node.getName())){
            return DivMetadataParser.parseDiv(node, root, rootClassName, registry);
        }
        if("text-field".equals(node.getName())){
            return StandaloneElementsMetadataParser.parseTextField(node);
        }
        throw new RuntimeException("unsupported tag name " + node.getName());
    }


}
