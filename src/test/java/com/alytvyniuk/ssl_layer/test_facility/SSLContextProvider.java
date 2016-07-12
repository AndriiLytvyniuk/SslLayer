package com.alytvyniuk.ssl_layer.test_facility;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by alytvyniuk on 28.01.16.
 */
public class SSLContextProvider {

    private SSLContext mSSLContext;

    public SSLContextProvider(String keyFilePath, String password) {
        mSSLContext = initSSLContext(keyFilePath, password);
    }

    private SSLContext initSSLContext(String keyFilePath, String password) {
        try {
            //Security.insertProviderAt(new BouncyCastleProvider(), 1);
            KeyStore keystore = KeyStore.getInstance("JKS");
            char[] passphrase = password.toCharArray();
            File keyStoreFile = new File(keyFilePath);
            keystore.load(new FileInputStream(keyStoreFile), passphrase);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, password.toCharArray());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            return sslContext;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public SSLEngine getServerSSLEngine() {
        SSLEngine engine = mSSLContext.createSSLEngine();
        engine.setUseClientMode(false);
        engine.setNeedClientAuth(false);
        engine.setWantClientAuth(false);
        return engine;
    }

    public SSLEngine getClientSSLEngine() {
        SSLEngine engine = mSSLContext.createSSLEngine("client", 8080);
        engine.setUseClientMode(true);
        return engine;
    }
}
