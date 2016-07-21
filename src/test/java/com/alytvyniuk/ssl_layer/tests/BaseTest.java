/*
 * #%L
 * SslLayer
 * %%
 * Copyright (C) 2012 - 2016 LocationProvider
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the LocationProvider nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package com.alytvyniuk.ssl_layer.tests;

import com.alytvyniuk.ssl_layer.SslLayer;
import com.alytvyniuk.ssl_layer.test_facility.ClientThread;
import com.alytvyniuk.ssl_layer.test_facility.NetworkChannel;
import com.alytvyniuk.ssl_layer.test_facility.SSLContextProvider;
import com.alytvyniuk.ssl_layer.test_facility.ServerThread;
import com.mauriciotogneri.trail.Trail;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;


public abstract class BaseTest {

    private static final String BASE_TEST_DIR = "src/test/testResults/";
    private File mTestDir;
    protected File mServerSentFile;
    protected File mServerReceivedFile;
    protected File mClientSentFile;
    protected File mClientReceivedFile;
    protected boolean mIsBidirectional;
    protected File mRequestFile;
    protected File mResponseFile;

    private SSLContextProvider mSSLContextProvider;

    protected void init(boolean isBidirectional, String keyPath, String keypass,
                        String requestFilePath, String responseFilePath) throws IOException {
        mTestDir = new File(BASE_TEST_DIR + getClass().getSimpleName());
        if (!mTestDir.exists()) {
            mTestDir.mkdirs();
        }

        mServerReceivedFile = new File(mTestDir, "serverReceived");
        mClientSentFile = new File(mTestDir, "clientSent");
        mServerReceivedFile.createNewFile();
        mClientSentFile.createNewFile();
        if (isBidirectional) {
            mServerSentFile = new File(mTestDir, "serverSent");
            mClientReceivedFile = new File(mTestDir, "clientReceived");
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

    protected void runRequest() throws IOException {
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
    public void printName() {
        Trail.debug(getClass().getSimpleName());
    }

    @After
    public void cleanup() throws IOException {
        FileUtils.deleteDirectory(mTestDir);
    }
}
