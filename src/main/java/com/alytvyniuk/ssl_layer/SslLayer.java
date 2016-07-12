package com.alytvyniuk.ssl_layer;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

public class SslLayer implements Closeable {

    private boolean isClosed = false;
    private javax.net.ssl.SSLEngine sslEngine;
    private InputStream encryptedInput;
    private OutputStream encryptedOutput;
    private static final int BLOCK_SIZE = 1500;
    private byte[] tmp = new byte[BLOCK_SIZE];
    private SSLSession session;
    private int appBufferMax;
    private int netBufferMax;
    private ByteBuffer emptyBuffer = java.nio.ByteBuffer.allocate(0);
    private ByteBuffer serverIn;
    private ByteBuffer limbo;
    private final boolean mIsClient;
    private WritableByteChannel mByteChannel;
    boolean isHandShakeFinised;

    public SslLayer(SSLEngine sslEngine, InputStream encryptedInput, OutputStream encryptedOutput) {
        this.sslEngine = sslEngine;
        mIsClient = sslEngine.getUseClientMode();
        this.encryptedInput = encryptedInput;
        this.encryptedOutput = encryptedOutput;
        this.session = sslEngine.getSession();
        this.appBufferMax = session.getApplicationBufferSize();
        this.netBufferMax = session.getPacketBufferSize();
        this.serverIn = java.nio.ByteBuffer.allocate(appBufferMax + 50);
        this.serverIn.limit(0);
        this.limbo = java.nio.ByteBuffer.allocate(netBufferMax);
        mByteChannel = Channels.newChannel(encryptedOutput);
    }

    public synchronized void write(byte[] buffer, int offset, int length) throws IOException {
        ByteBuffer writeBuffer = ByteBuffer.wrap(buffer, offset, length);
        if (!isHandShakeFinised) {
            doWrap(emptyBuffer);
        }
        this.doWrap(writeBuffer);
    }

    public java.io.OutputStream getDecryptedOutput() {
        return new DecryptedOutput();
    }

    public synchronized int read(byte[] buffer, int offset, int maxLength) throws IOException {
        while (this.serverIn.remaining() == 0) {
            if (this.isClosed) return -1;
            try {
                doUnwrap();
            } catch (java.io.EOFException onClosed) {
                this.close();
                return -1;
            }
            if (this.isClosed) return -1;
        }
        int limit = Math.min(maxLength, this.serverIn.remaining());

        //System.err.println("Reading "+limit+" bytes from serverIn, position before: "+this.serverIn.position());
        this.serverIn.get(buffer, offset, limit);
        //System.err.println("Reading "+limit+" bytes from serverIn, position after: "+this.serverIn.position());

        //System.err.println("Reading "+limit+" bytes from serverIn: "+serverIn);

        return limit;
    }

    public java.io.InputStream getDecryptedInput() {
        return new DecryptedInput();
    }

    private void doUnwrap() throws IOException {
        //System.err.println("doUnwrap()");
        if (this.serverIn.remaining() == 0) {
            this.serverIn.clear();
        } else {
            this.serverIn.flip();
        }

        //limbo.flip();
        log("do Unwrap read " + encryptedInput.available());
        int count = encryptedInput.read(tmp, 0, tmp.length);
        log("do Unwrap read2 " + count);
        if (count == -1) {
            this.close();
            return;
        }
        javax.net.ssl.SSLEngineResult r = sslEngine.unwrap(java.nio.ByteBuffer.wrap(tmp, 0, count), serverIn);
        int consumed = r.bytesConsumed();
        while ((r.getStatus() == Status.BUFFER_UNDERFLOW || consumed < count) && count != -1) {
            if (r.getStatus() == Status.BUFFER_UNDERFLOW) {
                log("do Unwrap consumed = " + consumed + " , " + count);
                byte[] newTmp = new byte[tmp.length + BLOCK_SIZE];
                System.arraycopy(tmp, 0, newTmp, 0, count);
                tmp = newTmp;
                count += encryptedInput.read(tmp, count, tmp.length - count);
            } else if (r.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                Runnable runnable = sslEngine.getDelegatedTask();
                if (runnable != null) runnable.run();
            }
            ByteBuffer tempBuf = ByteBuffer.wrap(tmp, consumed, count - consumed);
            log("temp " + tempBuf);
            r = sslEngine.unwrap(tempBuf, serverIn);
            consumed += r.bytesConsumed();

            log("do Unwrap consumed = " + consumed + " , " + count + ", r=" + r);
        }
        log("Unwrap " + getResultString(r));
        if (r.getHandshakeStatus() == HandshakeStatus.FINISHED) {
            isHandShakeFinised = true;
        }

        serverIn.flip();
        //limbo.clear();
        int length = r.bytesProduced();
        if (length > 0) {
            return;
        }

        if (r.getStatus() == Status.CLOSED) {
            throw new java.io.EOFException("End Of Stream");
        } else if (r.getStatus() != Status.OK) {
            throw new java.io.IOException("Unhandled Status: " + r.getStatus());
        }
        if (r.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
            continueHandshake(r);
        }
    }

    private void doWrap(java.nio.ByteBuffer serverOut) throws IOException {
        //System.err.println("doWrap()");
        javax.net.ssl.SSLEngineResult r = sslEngine.wrap(serverOut, limbo);
        log("Wrap " + getResultString(r));
        if (r.getHandshakeStatus() == HandshakeStatus.FINISHED) {
            isHandShakeFinised = true;
        }
        limbo.flip();
        log("wrap before write " + limbo);
        mByteChannel.write(limbo);
        //encryptedOutput.write(limbo.array(), limbo.arrayOffset() + limbo.position(), limbo.remaining());
        log("Wrap after write " + limbo);
        limbo.clear();

        if (r.getStatus() == Status.CLOSED) {
            throw new java.io.EOFException("End Of Stream");
        } else if (r.getStatus() != Status.OK) {
            throw new java.io.IOException("Unhandled Status: " + r.getStatus());
        }
        if (r.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
            continueHandshake(r);
        }
    }

    private void doTask() throws IOException {
        //System.err.println("doTask()");
        Runnable runnable = sslEngine.getDelegatedTask();
        if (runnable != null) runnable.run();
        //  typically needs to send data after task
        doWrap(emptyBuffer);
    }

    private void continueHandshake(javax.net.ssl.SSLEngineResult r) throws IOException {
        //System.err.println("continueHandshake: "+r.getHandshakeStatus());
        switch (r.getHandshakeStatus()) {
            case NEED_TASK:
                doTask();
                break;
            case NEED_WRAP:
                doWrap(emptyBuffer);
                break;
            case NEED_UNWRAP:
                doUnwrap();
                break;
            default:
                break;
        }
    }

    public static short readShort(InputStream in) throws java.io.IOException {
        int b = in.read();
        if (b == -1) throw new java.io.EOFException("End Of Stream");
        short s = (short) (b << 8);
        b = in.read();
        if (b == -1) throw new java.io.EOFException("End Of Stream");
        s += b;
        return s;
    }

    public synchronized void close() throws IOException {
        log("close");
        if (this.isClosed == false) {
            this.isClosed = true;
            try {
                if (this.encryptedInput != null) this.encryptedInput.close();
                if (this.encryptedOutput != null) this.encryptedOutput.close();
            } catch (Exception x) {
            }
        }
    }

    public class DecryptedInput extends InputStream {
        private byte[] singleByte = new byte[1];

        @Override
        public int read() throws IOException {
            int count = SslLayer.this.read(singleByte, 0, 1);
            if (count == -1) return -1;
            else if (count != 1) throw new IOException("Unexpected read count: " + count);
            return singleByte[0];
        }

        @Override
        public int read(byte[] b) throws IOException {
            return SslLayer.this.read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return SslLayer.this.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            SslLayer.this.close();
        }
    }

    public class DecryptedOutput extends OutputStream {
        private byte[] singleByte = new byte[1];

        @Override
        public void write(int b) throws IOException {
            singleByte[0] = (byte) b;
            SslLayer.this.write(singleByte, 0, 1);
        }

        @Override
        public void write(byte[] b) throws IOException {
            SslLayer.this.write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            SslLayer.this.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            SslLayer.this.close();
        }
    }

    private String getResultString(SSLEngineResult r) {
        return "consumed = " + r.bytesConsumed() + " produced = " + r.bytesProduced()
                + " status = " + r.getStatus() + " handshakeStatus " + r.getHandshakeStatus();
    }

    private void log(String message) {
        System.out.println((mIsClient ? "Client" : "Server") + " " + message);
    }
}