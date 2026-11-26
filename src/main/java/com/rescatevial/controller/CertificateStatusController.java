package com.rescatevial.controller;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Provides the expiration time of the HTTPS certificate served by the local Nginx. */
@RestController
public class CertificateStatusController {
    private static final String LOCAL_TLS_HOST = "127.0.0.1";
    private static final int TLS_PORT = 443;
    private static final int TIMEOUT_MILLIS = 3000;

    @GetMapping("/api/certificate-status")
    public ResponseEntity<Map<String, String>> certificateStatus() {
        try {
            X509Certificate certificate = readLocalCertificate();
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("expiresAt", certificate.getNotAfter().toInstant().toString()));
        } catch (Exception exception) {
            return ResponseEntity.status(503)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("status", "unavailable"));
        }
    }

    private X509Certificate readLocalCertificate() throws Exception {
        X509TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                // This local check only reads the certificate's public expiration date.
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                // Nginx is contacted through loopback; the browser validates public trust.
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[] {trustManager}, new SecureRandom());

        try (SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket()) {
            socket.connect(new InetSocketAddress(LOCAL_TLS_HOST, TLS_PORT), TIMEOUT_MILLIS);
            socket.setSoTimeout(TIMEOUT_MILLIS);
            socket.startHandshake();
            Certificate[] peerCertificates = socket.getSession().getPeerCertificates();
            if (peerCertificates.length == 0 || !(peerCertificates[0] instanceof X509Certificate certificate)) {
                throw new IllegalStateException("Nginx did not present an X.509 certificate");
            }
            return certificate;
        }
    }
}
