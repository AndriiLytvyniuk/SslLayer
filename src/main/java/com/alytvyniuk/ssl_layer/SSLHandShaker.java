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
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    protected ByteBuffer decryptedReadBuffer;
    protected final WritableByteChannel writableByteChannel;
    protected final ReadableByteChannel readableByteChannel;
    protected final ByteBuffer readEncryptedByteBuffer;
    protected final ByteBuffer decryptedWriteByteBuffer;

    private boolean isHandShakeFinished;

    protected SSLHandShaker(SSLEngine sslEngine, InputStream encryptedInput, OutputStream encryptedOutput) {
        this.sslEngine = sslEngine;
        isClient = sslEngine.getUseClientMode();
        SSLSession session = sslEngine.getSession();
        int appBufferMax = session.getApplicationBufferSize();
        decryptedReadBuffer = ByteBuffer.allocate(appBufferMax + 50);
        int netBufferMax = session.getPacketBufferSize();
        limbo = ByteBuffer.allocate(netBufferMax);
        readEncryptedByteBuffer = ByteBuffer.allocate(netBufferMax);
        decryptedWriteByteBuffer = ByteBuffer.allocate(netBufferMax);
        decryptedWriteByteBuffer.limit(0);
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
            onHandShakeFinished();
        }
        return r;
    }

    private void onHandShakeFinished() {
        isHandShakeFinished = true;
        decryptedReadBuffer.position(0);
        decryptedReadBuffer.limit(0);
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
        limbo.clear();
        SSLEngineResult r = sslEngine.wrap(EMPTY_BUFFER, limbo);
        limbo.flip();
        log("handShakeWrap write " + limbo.remaining() + " " + getResultString(r));
        writableByteChannel.write(limbo);
        limbo.clear();
        return continueHandshake(r);
    }

    protected SSLEngineResult handShakeUnwrap() throws IOException {
        log("handShakeUnwrap");
        if (readEncryptedByteBuffer.hasRemaining()) {
            SSLEngineResult r = sslEngine.unwrap(readEncryptedByteBuffer, decryptedReadBuffer);
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

    protected boolean isHandShaken() {
        return isHandShakeFinished;
    }
}
