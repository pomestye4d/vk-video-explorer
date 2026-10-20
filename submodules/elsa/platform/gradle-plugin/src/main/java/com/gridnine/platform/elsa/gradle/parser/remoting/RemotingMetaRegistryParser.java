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

package com.gridnine.platform.elsa.gradle.parser.remoting;

import com.gridnine.platform.elsa.gradle.meta.common.XmlNode;
import com.gridnine.platform.elsa.gradle.meta.remoting.*;
import com.gridnine.platform.elsa.gradle.parser.common.CommonParserUtils;
import com.gridnine.platform.elsa.gradle.parser.common.MetaDataParsingResult;
import com.gridnine.platform.elsa.gradle.utils.BuildExceptionUtils;

import java.io.File;
import java.util.List;

public class RemotingMetaRegistryParser {

    public void updateMetaRegistry(RemotingMetaRegistry registry, List<File> sources) {
        sources.forEach(it -> BuildExceptionUtils.wrapException(() -> {
            MetaDataParsingResult pr = CommonParserUtils.parse(it);
            XmlNode node = pr.node();

            node.getChildren("enum").forEach(child ->
                    CommonParserUtils.updateEnum(registry.getEnums(), child, pr.localizations()));
            node.getChildren("entity").forEach(child ->
                    CommonParserUtils.updateEntity(registry.getEntities(), child));
            var remotingDescription = registry.getRemotings().computeIfAbsent(CommonParserUtils.getIdAttribute(node), RemotingDescription::new);
            node.getChildren("group").forEach(groupChild -> {
                var groupDescr = remotingDescription.getGroups().computeIfAbsent(CommonParserUtils.getIdAttribute(groupChild), RemotingGroupDescription::new);
                groupChild.getChildren("service").forEach(item -> {
                    String idAttribute = CommonParserUtils.getIdAttribute(item);
                    var sc = groupDescr.getServices().computeIfAbsent(idAttribute,
                            ServiceDescription::new);
                    var methodStr = item.getAttribute("method");
                    var method = methodStr == null? HttpMethod.POST: HttpMethod.valueOf(methodStr);
                    var path = item.getAttribute("path");
                    sc.setMethod(method);
                    sc.setPath(path);
                    var requestChildren = item.getChildren("request");
                    if (requestChildren.size() == 1) {
                        XmlNode requestElm = requestChildren.get(0);
                        sc.setMultipartRequest("true".equals(requestElm.getAttribute("multipart")));
                        sc.setRequestClassName(parseEntity(registry, requestElm));
                    }
                    var requestRefChildren = item.getChildren("request-ref");
                    if (requestRefChildren.size() == 1) {
                        sc.setRequestClassName(CommonParserUtils.getIdAttribute(requestRefChildren.get(0)));
                    }
                    var responseChildren = item.getChildren("response");
                    if (responseChildren.size() == 1) {
                        sc.setResponseClassName(parseEntity(registry, responseChildren.get(0)));
                    }
                    var responseRefChildren = item.getChildren("response-ref");
                    if (responseRefChildren.size() == 1) {
                        sc.setResponseClassName(CommonParserUtils.getIdAttribute(responseRefChildren.get(0)));
                    }
                });
                groupChild.getChildren("subscription").forEach(item -> {
                    var sc = groupDescr.getSubscriptions().computeIfAbsent(CommonParserUtils.getIdAttribute(item),
                            RemotingSubscriptionDescription::new);
                    var requestChildren = item.getChildren("parameter");
                    if (requestChildren.size() == 1) {
                        XmlNode requestElm = requestChildren.get(0);
                        sc.setParameterClassName(parseEntity(registry, requestElm));
                    }
                    var requestRefChildren = item.getChildren("parameter-ref");
                    if (requestRefChildren.size() == 1) {
                        sc.setParameterClassName(CommonParserUtils.getIdAttribute(requestRefChildren.get(0)));
                    }
                    var responseChildren = item.getChildren("event");
                    if (responseChildren.size() == 1) {
                        sc.setEventClassName(parseEntity(registry, responseChildren.get(0)));
                    }
                    var responseRefChildren = item.getChildren("event-ref");
                    if (responseRefChildren.size() == 1) {
                        sc.setEventClassName(CommonParserUtils.getIdAttribute(responseRefChildren.get(0)));
                    }
                });
            });
        }));
    }

    private String parseEntity(RemotingMetaRegistry registry, XmlNode elm) {
        var ett = CommonParserUtils.updateEntity(registry.getEntities(), elm);
        return ett.getId();
    }
}
