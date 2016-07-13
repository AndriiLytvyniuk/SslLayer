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
 * Created by andrii on 13.07.16.
 */
public abstract class SSLHandShaker {

    private final boolean isClient;
    protected SSLEngine sslEngine;
    protected ByteBuffer limbo;
    //protected InputStream encryptedInput;
    //private OutputStream encryptedOutput;
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    protected ByteBuffer decryptedBuffer;
    protected final WritableByteChannel writableByteChannel;
    private final ReadableByteChannel readableByteChannel;

    protected static final int ENCRYPTED_BUFFER_SIZE = 1500;
    //protected byte[] readEncryptedBuffer = new byte[ENCRYPTED_BUFFER_SIZE];
    private final ByteBuffer readEncryptedByteBuffer = ByteBuffer.allocate(ENCRYPTED_BUFFER_SIZE);

    private boolean isHandShakeFinished;

    protected SSLHandShaker(SSLEngine sslEngine, InputStream encryptedInput, OutputStream encryptedOutput) {
        this.sslEngine = sslEngine;
        //this.encryptedInput = encryptedInput;
        //this.encryptedOutput = encryptedOutput;
        isClient = sslEngine.getUseClientMode();
        SSLSession session = sslEngine.getSession();
        int appBufferMax = session.getApplicationBufferSize();
        int netBufferMax = session.getPacketBufferSize();
        decryptedBuffer = ByteBuffer.allocate(appBufferMax + 50);
        limbo = ByteBuffer.allocate(netBufferMax);
        writableByteChannel = Channels.newChannel(encryptedOutput);
        readableByteChannel = Channels.newChannel(encryptedInput);
        readEncryptedByteBuffer.limit(0);
    }

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
            isHandShakeFinished = true;
        }
        return r;
    }

    protected SSLEngineResult continueHandshake(SSLEngineResult r) throws IOException {
        log("continueHandshake " + getResultString(r));
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

    protected SSLEngineResult handShakeWrap() throws IOException {
        log("handShakeWrap");
        SSLEngineResult r = sslEngine.wrap(EMPTY_BUFFER, limbo);
        limbo.flip();
        log("handShakeWrap write " + limbo.remaining() + " " + getResultString(r));
        writableByteChannel.write(limbo);
        limbo.clear();
        continueHandshake(r);
        return r;
    }

    protected SSLEngineResult handShakeUnwrap() throws IOException {
        log("handShakeUnwrap");
        if (readEncryptedByteBuffer.hasRemaining()) {
            SSLEngineResult r = sslEngine.unwrap(readEncryptedByteBuffer, decryptedBuffer);
            log("handShakeUnwrap unwrap " + getResultString(r));
            return continueHandshake(r);
        } else {
            readEncryptedByteBuffer.clear();
            int count = readableByteChannel.read(readEncryptedByteBuffer);
            readEncryptedByteBuffer.flip();
            // = encryptedInput.read(readEncryptedBuffer, 0, readEncryptedBuffer.length);
            log("handShakeUnwrap read " + count);
            if (count == -1) {
                throw new IOException("Not enough data from network for handshake");
            }
            //readEncryptedByteBuffer.put(readEncryptedBuffer, 0, count);
            return handShakeUnwrap();
        }
    }

    protected SSLEngineResult handShakeDoTask() throws IOException {
        log("continueHandshake");
        Runnable runnable = sslEngine.getDelegatedTask();
        if (runnable != null) {
            runnable.run();
        }
        return handShakeWrap();
    }

    protected String getResultString(SSLEngineResult r) {
        return "consumed = " + r.bytesConsumed() + " produced = " + r.bytesProduced()
                + " status = " + r.getStatus() + " handshakeStatus " + r.getHandshakeStatus();
    }

    protected void log(String message) {
        System.out.println((isClient ? "Client" : "Server") + " " + message);
    }

    protected boolean isHandShakeFinished() {
        return isHandShakeFinished;
    }
}
