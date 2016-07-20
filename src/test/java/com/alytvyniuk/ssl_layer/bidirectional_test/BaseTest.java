package com.alytvyniuk.ssl_layer.bidirectional_test;


import com.alytvyniuk.ssl_layer.SslLayer;
import com.alytvyniuk.ssl_layer.test_facility.NetworkChannel;
import com.alytvyniuk.ssl_layer.test_facility.SSLContextProvider;

import org.junit.Before;

import java.io.File;
import java.io.IOException;


public abstract class BaseTest {

    protected File mServerSentFile;
    protected File mServerReceivedFile;
    protected File mClientSentFile;
    protected File mClientReceivedFile;
    protected boolean mIsBidirectional;
    protected File mRequestFile;
    protected File mResponseFile;

    private SSLContextProvider mSSLContextProvider;

    protected void init(String baseDir, boolean isBidirectional, String keyPath, String keypass,
                        String requestFilePath, String responseFilePath) throws IOException {
        mServerReceivedFile = new File(baseDir + "serverReceived");
        mClientSentFile = new File(baseDir + "clientSent");
        mServerReceivedFile.createNewFile();
        mClientSentFile.createNewFile();
        if (isBidirectional) {
            mServerSentFile = new File(baseDir + "serverSent");
            mClientReceivedFile = new File(baseDir + "clientReceived");
            mServerSentFile.createNewFile();
            mClientReceivedFile.createNewFile();
        } else {
            mServerSentFile = null;
            mClientReceivedFile = null;
        }
        mIsBidirectional = isBidirectional;
        mSSLContextProvider = new SSLContextProvider(keyPath, keypass);

        mRequestFile = new File(requestFilePath);
        if (!mRequestFile.exists()) {
            throw new IllegalArgumentException("Wrong request file path");
        }
        mResponseFile = null;
        if (responseFilePath != null) {
            mResponseFile = new File(requestFilePath);
            if (!mResponseFile.exists()) {
                throw new IllegalArgumentException("Wrong response file path");
            }
        }
    }

    protected void runRequest(int serverBufferSize) throws IOException {
        NetworkChannel serverToClientChannel = new NetworkChannel();
        NetworkChannel clientToServerChannel = new NetworkChannel();

        SslLayer clientSslLayer = new SslLayer(mSSLContextProvider.getClientSSLEngine(),
                clientToServerChannel.getInputStream(),
                serverToClientChannel.getOutputStream());

        SslLayer serverSslLayer = new SslLayer(mSSLContextProvider.getServerSSLEngine(),
                serverToClientChannel.getInputStream(),
                clientToServerChannel.getOutputStream());

        Thread clientThread = new ClientThread(clientSslLayer, mRequestFile, mClientSentFile, mClientReceivedFile, mIsBidirectional);
        Thread serverThread = new ServerThread(serverSslLayer, mResponseFile, mServerSentFile, mServerReceivedFile, mIsBidirectional);

        clientThread.start();
        serverThread.start();
        try {
            clientThread.join();
            serverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        onConnectionFinished();
    }

    protected abstract void onConnectionFinished();

    @Before
    public void before() throws IOException {
    }
}
