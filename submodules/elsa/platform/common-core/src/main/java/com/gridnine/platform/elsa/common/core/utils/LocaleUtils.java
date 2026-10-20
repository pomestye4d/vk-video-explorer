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

package com.gridnine.platform.elsa.common.core.utils;

import java.util.Locale;
import java.util.Map;

public class LocaleUtils {
    public static final Locale ruLocale = new Locale.Builder().setLanguage("ru").build();

    private final static ThreadLocal<Locale> currentLocale = new ThreadLocal<>();

    public static Locale getLocale(String language, String countryCode) {
        if ("ru".equals(language)) {
            return ruLocale;
        }
        if ("en".equals(language)) {
            return Locale.ENGLISH;
        }
        return new Locale.Builder().setLanguage(language).setVariant(countryCode).build();
    }

    public static void setCurrentLocale(Locale locale) {
        currentLocale.set(locale);
    }

    public static Locale getCurrentLocale() {
        var locale = currentLocale.get();
        return locale == null ? ruLocale : locale;
    }

    public static void resetCurrentLocale() {
        currentLocale.remove();
    }

    public static String getLocalizedName(Map<Locale, String> localizations, Locale loc, String defaultValue) {
        var result = localizations.get(loc == null ? getCurrentLocale() : loc);
        if (result == null) {
            result = localizations.get(ruLocale);
        }
        if (result == null && !localizations.isEmpty()) {
            result = localizations.values().iterator().next();
        }
        return result == null ? defaultValue : result;
    }

    public static String getLocalizedName(String[] locales, String[] names, String defaultValue) {
        var currentLocale = getCurrentLocale();
        var idx = -1;
        for (int n = 0; n < locales.length; n++){
            var loc = locales[n];
            if(loc.startsWith(currentLocale.getLanguage())){
                return names[n];
            }
        }
        for (int n = 0; n < locales.length; n++){
            var loc = locales[n];
            if(loc.startsWith("en")){
                return names[n];
            }
        }
        if(defaultValue != null){
            return defaultValue;
        }
        if(locales.length > 0){
            return locales[0];
        }
        return null;
    }
}
