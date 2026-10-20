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

package com.gridnine.platform.elsa.gradle.parser.l10n;

import com.gridnine.platform.elsa.gradle.meta.common.StandardValueType;
import com.gridnine.platform.elsa.gradle.meta.common.XmlNode;
import com.gridnine.platform.elsa.gradle.meta.l10n.L10nMessageDescription;
import com.gridnine.platform.elsa.gradle.meta.l10n.L10nMessageParameterDescription;
import com.gridnine.platform.elsa.gradle.meta.l10n.L10nMessagesBundleDescription;
import com.gridnine.platform.elsa.gradle.meta.l10n.L10nMetaRegistry;
import com.gridnine.platform.elsa.gradle.parser.common.CommonParserUtils;
import com.gridnine.platform.elsa.gradle.parser.common.MetaDataParsingResult;

import java.io.File;

public class L10nMetaRegistryParser {

    public void updateMetaRegistry(L10nMetaRegistry registry, File source) throws Exception {
        MetaDataParsingResult pr = CommonParserUtils.parse(source);
        XmlNode node = pr.node();
        var bundleDescription = registry.getBundles().computeIfAbsent(CommonParserUtils.getIdAttribute(node),
                L10nMessagesBundleDescription::new);
        node.getChildren("message").forEach(child -> {
            var message = bundleDescription.getMessages().computeIfAbsent(CommonParserUtils.getIdAttribute(child),
                    L10nMessageDescription::new);
            CommonParserUtils.updateLocalizations(message.getDisplayNames(), pr.localizations(), message.getId());
            CommonParserUtils.updateLocalizationFromAttribute(child, "localization", message.getDisplayNames());
            child.getChildren("parameter").forEach(paramChild -> {
                var param = message.getParameters().computeIfAbsent(CommonParserUtils.getIdAttribute(paramChild),
                        L10nMessageParameterDescription::new);
                param.setClassName(paramChild.getAttribute("class-name"));
                param.setCollection("true".equals(paramChild.getAttribute("collection")));
                param.setType(StandardValueType.valueOf(paramChild.getAttribute("type")));
            });
        });
    }


}
