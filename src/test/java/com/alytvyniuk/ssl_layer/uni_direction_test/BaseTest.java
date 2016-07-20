package com.alytvyniuk.ssl_layer.uni_direction_test;


import com.alytvyniuk.ssl_layer.SslLayer;
import com.alytvyniuk.ssl_layer.test_facility.NetworkChannel;
import com.alytvyniuk.ssl_layer.test_facility.SSLContextProvider;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;

import java.io.File;
import java.io.IOException;


public abstract class BaseTest {

    private SSLContextProvider mSSLContextProvider = new SSLContextProvider("src/test/res/ssl_converter_test/keys/ssl_proxy.jks", "ssl_proxy");
    private File mServerReceivedFile = new File("src/test/res/ssl_converter_test/ServerReceived");
    private File mClientSentFile = new File("src/test/res/ssl_converter_test/ClientSent");

    protected void runRequest(String requestFilePath, int serverBufferSize) throws IOException {
        File requestFile = new File(requestFilePath);
        if (!requestFile.exists()) {
            throw new IllegalArgumentException("Wrong request file path");
        }

        NetworkChannel serverToClientChannel = new NetworkChannel();
        NetworkChannel clientToServerChannel = new NetworkChannel();

        SslLayer clientSslConverter = new SslLayer(mSSLContextProvider.getClientSSLEngine(),
                clientToServerChannel.getInputStream(),
                serverToClientChannel.getOutputStream());

        SslLayer serverSslConverter = new SslLayer(mSSLContextProvider.getServerSSLEngine(),
                serverToClientChannel.getInputStream(),
                clientToServerChannel.getOutputStream());

        Thread clientThread = new ClientThread(clientSslConverter.getDecryptedOutputStream(), requestFile, mClientSentFile);
        Thread serverThread = new ServerThread(serverSslConverter.getDecryptedInputStream(), mServerReceivedFile, serverBufferSize);

        clientThread.start();
        serverThread.start();
        try {
            clientThread.join();
            serverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        boolean isEqual = FileUtils.contentEquals(mClientSentFile, mServerReceivedFile);
        Assert.assertTrue(isEqual);
        mClientSentFile.delete();
        mServerReceivedFile.delete();
    }

    @Before
    public void before() throws IOException {
        mServerReceivedFile.createNewFile();
        mClientSentFile.createNewFile();
    }
}
