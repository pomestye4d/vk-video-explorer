/*****************************************************************
 * Gridnine AB http://www.gridnine.com
 * Project: LDocs
 *****************************************************************/

package com.gridnine.platform.elsa.common.core.ssl;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.X509Certificate;


public final class TrustAllTrustManager extends X509ExtendedTrustManager {
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] arg0,
            final String arg1) {
        // noop
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] arg0,
            final String arg1) {
        // noop
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] arg0,
            final String arg1, final Socket arg2) {
        // noop
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] arg0,
            final String arg1, final Socket arg2) {
        // noop
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] arg0,
            final String arg1, final SSLEngine arg2) {
        // noop
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] arg0,
            final String arg1, final SSLEngine arg2) {
        // noop
    }
}