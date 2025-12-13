package com.ahmedy.chat.config;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public final class TrustAllSSL {

    public static void enable() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            // 🔥 Apply globally
            SSLContext.setDefault(sslContext);

            // 🔥 Disable hostname verification
            HttpsURLConnection.setDefaultHostnameVerifier(
                    (hostname, session) -> true
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to enable trust-all SSL", e);
        }
    }
}
