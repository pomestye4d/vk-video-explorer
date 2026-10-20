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

package com.gridnine.platform.elsa.core.remoting.standard;

import com.gridnine.platform.elsa.common.core.serialization.meta.ObjectMetadataProvidersFactory;
import com.gridnine.platform.elsa.common.core.serialization.meta.SerializablePropertyType;
import com.gridnine.platform.elsa.common.rest.core.*;
import com.gridnine.platform.elsa.core.remoting.RemotingCallContext;
import com.gridnine.platform.elsa.core.remoting.RestHandler;
import com.gridnine.platform.elsa.server.core.CoreRemotingConstants;
import org.springframework.beans.factory.annotation.Autowired;

public class GetRemotingEntityDescriptionHandler implements RestHandler<GetRemotingEntityDescriptionRequest, GetRemotingEntityDescriptionResponse> {

    @Autowired
    private ObjectMetadataProvidersFactory providersFactory;

    @Override
    public String getId() {
        return CoreRemotingConstants.CORE_META_GET_ENTITY_DESCRIPTION;
    }

    @Override
    public GetRemotingEntityDescriptionResponse service(GetRemotingEntityDescriptionRequest request, RemotingCallContext context) {
        var provider = providersFactory.getProvider(request.getEntityId());
        var ed = new RemotingEntityDescription();
        provider.getAllProperties().forEach(prop -> {
            var pd = new RemotingEntityPropertyDescription();
            pd.setId(prop.id());
            pd.setClassName(prop.className());
            pd.setType(toType(prop.type()));
            pd.setIsAbstract(prop.isAbstract());
            ed.getProperties().add(pd);
        });
        provider.getAllCollections().forEach(coll -> {
            var cd = new RemotingEntityCollectionDescription();
            cd.setId(coll.id());
            cd.setElementClassName(coll.elementClassName());
            cd.setElementType(toType(coll.elementType()));
            cd.setElementIsAbstract(coll.isAbstract());
            ed.getCollections().add(cd);
        });
        provider.getAllMaps().forEach(map -> {
            var md = new RemotingEntityMapDescription();
            md.setId(map.id());
            md.setKeyClassName(map.keyClassName());
            md.setKeyType(toType(map.keyType()));
            md.setKeyIsAbstract(map.keyIsAbstract());
            md.setValueClassName(map.valueClassName());
            md.setValueType(toType(map.valueType()));
            md.setValueIsAbstract(map.valueIsAbstract());
            ed.getMaps().add(md);
        });
        var result = new GetRemotingEntityDescriptionResponse();
        result.setDescription(ed);
        return result;
    }

    private RemotingEntityValueType toType(SerializablePropertyType type) {
        return RemotingEntityValueType.valueOf(type.name());
    }

}
