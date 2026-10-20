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

package com.gridnine.platform.elsa.common.core.l10n;

import com.gridnine.platform.elsa.common.core.model.common.L10nMessage;
import com.gridnine.platform.elsa.common.core.model.domain.CaptionProvider;
import com.gridnine.platform.elsa.common.core.model.domain.EntityReference;
import com.gridnine.platform.elsa.common.core.utils.LocaleUtils;
import com.gridnine.platform.elsa.common.core.utils.TextUtils;
import com.gridnine.platform.elsa.common.meta.common.EnumDescription;
import com.gridnine.platform.elsa.common.meta.domain.DomainMetaRegistry;
import com.gridnine.platform.elsa.common.meta.l10n.L10nMetaRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;


public class Localizer {
    @Autowired
    private L10nMetaRegistry l10nRegistry;

    @Autowired
    private CaptionProvider captionProvider;

    @Autowired
    public DomainMetaRegistry domainMetaRegistry;


    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

    public String toString(String bundleId, String key, Locale locale, Object... params) {
        var result = key;
        if (result == null) {
            return null;
        }
        var bundle = l10nRegistry.getBundles().get(bundleId);
        var messageDescription = bundle == null ? null : bundle.getMessages().get(result);
        if (messageDescription != null) {
            result = LocaleUtils.getLocalizedName(messageDescription.getDisplayNames(), locale, result);
        }
        var size = params.length;
        for (int n = 0; n < size; n++) {
            result = result.replace("{%s}".formatted(n), toString(params[n]));
        }
        return result;
    }

    public String toString(L10nMessage message) {
        return toString(message.getBundle(), message.getKey(), null, message.getParameters().toArray());
    }

    public String toString(Object value) {
        return toString(value, null);
    }

    public String toString(Object value, Locale locale) {
        if (value instanceof Collection<?> c) {
            return TextUtils.join(c.stream().map(it -> toStringInternal(it, locale)).toList(), ",");
        }
        return toStringInternal(value, locale);
    }

    private String toStringInternal(Object value, Locale loc) {
        if (value == null) {
            return "???";
        }
        if (value instanceof EntityReference<?> er) {
            return captionProvider.getCaption(er);
        }
        if (value instanceof Enum<?> en) {
            var res = getLocalizedName(domainMetaRegistry.getEnums(), en, loc);
            return res == null ? en.name() : res;
        }
        if (value instanceof LocalDate ld) {
            return dateFormatter.format(ld);
        }
        if (value instanceof LocalDateTime ldt) {
            return dateTimeFormatter.format(ldt);
        }
        return value.toString();
    }

    private String getLocalizedName(Map<String, EnumDescription> descriptions, Enum<?> value, Locale loc) {
        var ed = descriptions.get(value.getClass().getName());
        if (ed == null) {
            return null;
        }
        var eid = ed.getItems().get(value.name());
        if (eid == null) {
            return value.name();
        }
        return LocaleUtils.getLocalizedName(eid.getDisplayNames(), loc, value.name());
    }

}
