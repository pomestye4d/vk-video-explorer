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
import com.gridnine.platform.elsa.gradle.meta.adminUi.tabs.NestedTabsDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.tabs.RootTabsDescription;
import com.gridnine.platform.elsa.gradle.meta.adminUi.tabs.TabDescription;
import com.gridnine.platform.elsa.gradle.meta.common.XmlNode;
import com.gridnine.platform.elsa.gradle.parser.common.CommonParserUtils;

public class TabsMetadataParser {

    public static NestedTabsDescription parseTabs(XmlNode node, boolean root, String rootContainerName, AdminUiMetaRegistry registry) {
        var tabsDescription = root? new RootTabsDescription(): new NestedTabsDescription();
        if (tabsDescription instanceof RootTabsDescription rtb) {
            rtb.setClassName(AdminUiParserHelper.getClassName(node));
            registry.getContainers().put(rtb.getClassName(), tabsDescription);
        }
        node.getChildren().forEach(tabNode -> {
            if (!"tab".equals(tabNode.getName())) {
                return;
            }
            var tabDescription = new TabDescription();
            tabDescription.setId(CommonParserUtils.getIdAttribute(tabNode));
            tabDescription.setDedicatedAcl("true".equals(tabNode.getAttribute("dedicated-acl")));
            CommonParserUtils.updateLocalizationFromAttribute(tabNode, "title", tabDescription.getTitle());
            if(!tabNode.getChildren().isEmpty()){
                tabDescription.setContent(AdminUiParserHelper.parse(tabNode.getChildren().get(0), false, rootContainerName, registry));
            }
            tabsDescription.getTabs().add(tabDescription);
        });
        return tabsDescription;
    }
}
