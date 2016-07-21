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
package com.alytvyniuk.ssl_layer;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;

/**
 * Provides SSL HandShake operations
 */
public abstract class SSLHandShaker {

    private final boolean isClient;
    protected SSLEngine sslEngine;
    protected final WritableByteChannel writeEncryptedChannel;
    protected final ReadableByteChannel readEncryptedChannel;
    protected final ByteBuffer writeEncryptedByteBuffer;
    protected final ByteBuffer readEncryptedByteBuffer;
    protected final ByteBuffer writeDecryptedByteBuffer;
    protected final ByteBuffer readDecryptedByteBuffer;
    protected boolean isClosed;
    private boolean isHandShakeFinished;

    private boolean isLoggingEnabled;

    /**
     *
     * @param sslEngine
     * @param encryptedInput input channel stream with ssl encrypted data
     * @param encryptedOutput output channel stream to write ssl encrypted data
     */
    protected SSLHandShaker(SSLEngine sslEngine, InputStream encryptedInput, OutputStream encryptedOutput) {
        this.sslEngine = sslEngine;
        isClient = sslEngine.getUseClientMode();
        SSLSession session = sslEngine.getSession();
        int appBufferMax = session.getApplicationBufferSize();
        readDecryptedByteBuffer = ByteBuffer.allocate(appBufferMax + 50);
        int netBufferMax = session.getPacketBufferSize();
        writeEncryptedByteBuffer = ByteBuffer.allocate(netBufferMax);
        readEncryptedByteBuffer = ByteBuffer.allocate(netBufferMax);
        writeDecryptedByteBuffer = ByteBuffer.allocate(netBufferMax);
        writeDecryptedByteBuffer.limit(0);
        writeEncryptedChannel = Channels.newChannel(encryptedOutput);
        readEncryptedChannel = Channels.newChannel(encryptedInput);
        readEncryptedByteBuffer.limit(0);
    }

    /**
     * Makes all handshake operations, blocking method, calls several transactions
     * from client to server and backwards
     * @return result of handshaking, should be {@link javax.net.ssl.SSLEngineResult.HandshakeStatus#FINISHED}
     * @throws IOException
     */
    protected SSLEngineResult handshake() throws IOException {
        SSLEngineResult r;
        if (isClient) {
            r = handShakeWrap();
        } else {
            r = handShakeUnwrap();
        }
        if (r.getStatus() == SSLEngineResult.Status.CLOSED) {
            throw new EOFException("End Of Stream");
        } else if (r.getStatus() != SSLEngineResult.Status.OK) {
            throw new IOException("Unhandled Status: " + r.getStatus());
        }
        if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
            onHandShakeFinished();
        }
        return r;
    }

    private void onHandShakeFinished() {
        isHandShakeFinished = true;
        readDecryptedByteBuffer.position(0);
        readDecryptedByteBuffer.limit(0);
    }

    private SSLEngineResult continueHandshake(SSLEngineResult r) throws IOException {
        switch (r.getHandshakeStatus()) {
            case NEED_TASK:
                return handShakeDoTask();
            case NEED_WRAP:
                return handShakeWrap();
            case NEED_UNWRAP:
                return handShakeUnwrap();
            default:
                return r;
        }
    }

    private SSLEngineResult handShakeWrap() throws IOException {
        log("handShakeWrap");
        writeEncryptedByteBuffer.clear();
        SSLEngineResult r = sslEngine.wrap(writeDecryptedByteBuffer, writeEncryptedByteBuffer);
        writeEncryptedByteBuffer.flip();
        log("handShakeWrap write " + writeEncryptedByteBuffer.remaining() + " " + getResultString(r));
        writeEncryptedChannel.write(writeEncryptedByteBuffer);
        writeEncryptedByteBuffer.clear();
        return continueHandshake(r);
    }

    private SSLEngineResult handShakeUnwrap() throws IOException {
        log("handShakeUnwrap");
        if (readEncryptedByteBuffer.hasRemaining()) {
            SSLEngineResult r = sslEngine.unwrap(readEncryptedByteBuffer, readDecryptedByteBuffer);
            log("handShakeUnwrap unwrap " + getResultString(r));
            return continueHandshake(r);
        } else {
            readEncryptedByteBuffer.clear();
            int count = readEncryptedChannel.read(readEncryptedByteBuffer);
            readEncryptedByteBuffer.flip();
            log("handShakeUnwrap read " + count);
            if (count == -1) {
                throw new IOException("Not enough data from network for handshake");
            }
            return handShakeUnwrap();
        }
    }

    private SSLEngineResult handShakeDoTask() throws IOException {
        log("continueHandshake");
        Runnable runnable = sslEngine.getDelegatedTask();
        if (runnable != null) {
            runnable.run();
        }
        return handShakeWrap();
    }

    /**
     * Utility method. Returns convenient representation of {@link SSLEngineResult}
     * @param r
     * @return
     */
    protected String getResultString(SSLEngineResult r) {
        return "consumed = " + r.bytesConsumed() + " produced = " + r.bytesProduced()
                + " status = " + r.getStatus() + " handshakeStatus " + r.getHandshakeStatus();
    }

    /**
     * Utility method for convenient logging
     * @param message
     */
    protected void log(String message) {
        if (isLoggingEnabled) {
            System.out.println((isClient ? "Client" : "Server") + " " + message);
        }
    }

    /**
     * @return true if handshake was performed
     */
    protected boolean isHandShaken() {
        return isHandShakeFinished;
    }

    /**
     * Closes all network side channels
     */
    protected synchronized void close() {
        log("close");
        isClosed = true;
        try {
            writeEncryptedChannel.close();
        } catch (IOException e) {
            //ignore
        }
        try {
            readEncryptedChannel.close();
        } catch (IOException e) {
            //ignore
        }
    }

    /**
     * @param loggingEnabled if true - enables logging for SslLayer
     */
    public void setLoggingEnabled(boolean loggingEnabled) {
        isLoggingEnabled = loggingEnabled;
    }
}
