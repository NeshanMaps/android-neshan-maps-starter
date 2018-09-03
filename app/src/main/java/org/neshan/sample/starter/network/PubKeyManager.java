package org.neshan.sample.starter.network;

import android.util.Log;

import java.math.BigInteger;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public final class PubKeyManager implements X509TrustManager {

    private String publicKey;

    public PubKeyManager(String publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null) {
            throw new IllegalArgumentException("checkServerTrusted: X509Certificate array is null");
        }
        if (!(chain.length > 0)) {
            throw new IllegalArgumentException("checkServerTrusted: X509Certificate is empty");
        }

        // Perform customary SSL/TLS checks
        TrustManagerFactory tmf;
        try {
            tmf = TrustManagerFactory.getInstance("X509");
            tmf.init((KeyStore) null);

            for (TrustManager trustManager : tmf.getTrustManagers()) {
                ((X509TrustManager) trustManager).checkServerTrusted(
                        chain, authType);
            }

        } catch (Exception e) {
            throw new CertificateException(e.toString());
        }

        // Hack ahead: BigInteger and toString(). We know a DER encoded Public
        // Key starts with 0x30 (ASN.1 SEQUENCE and CONSTRUCTED), so there is
        // no leading 0x00 to drop.
        RSAPublicKey pubkey = (RSAPublicKey) chain[0].getPublicKey();
        String encoded = new BigInteger(1 /* positive */, pubkey.getEncoded())
                .toString(16);

        Log.d("cert", "publikKey: " + publicKey);
        Log.d("cert", "pubkey: " + pubkey);
        Log.d("cert", "encoded: " + encoded);


        // Pin it!
        final boolean expected = publicKey.equalsIgnoreCase(encoded);
        // fail if expected public key is different from our public key
        if (!expected) {
            throw new CertificateException("Not trusted");
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
