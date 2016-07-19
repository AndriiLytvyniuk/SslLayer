package com.alytvyniuk.ssl_layer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

public class SslLayer extends SSLHandShaker {

    private final InputStream decryptedInputStream = new DecryptedInputStream();
    private final OutputStream decryptedOutputStream = new DecryptedOutputStream();

    public SslLayer(SSLEngine sslEngine, InputStream encryptedInput, OutputStream encryptedOutput) {
        super(sslEngine, encryptedInput, encryptedOutput);
        readEncryptedByteBuffer.limit(0);
    }

    public InputStream getDecryptedInputStream() {
        return decryptedInputStream;
    }

    public OutputStream getDecryptedOutputStream() {
        return decryptedOutputStream;
    }

    private synchronized int readDecrypted(byte[] buffer, int offset, final int maxLength) throws IOException {
        checkByteArrayParameters(buffer, offset, maxLength);
        throwIfClosed();
        if (!isHandShaken()) {
            SSLEngineResult r = handshake();
            log("HandShake done " + getResultString(r));
        }
        int readLength = 0;
        int unwrapResult = -1;
        if (readDecryptedByteBuffer.remaining() == 0) {
            unwrapResult = unwrap();
        }
        if (readDecryptedByteBuffer.remaining() > 0) {
            readLength = Math.min(buffer.length - offset, readDecryptedByteBuffer.remaining());
            readDecryptedByteBuffer.get(buffer, offset, readLength);
        }
        log("readDecrypted length: " + readLength);
        return readLength == 0 && unwrapResult == -1 ? -1 : readLength;
    }

    private int unwrap() throws SSLException {
        log("Unwrap");
        if (readEncryptedByteBuffer.hasRemaining()) {
            readDecryptedByteBuffer.clear();
            SSLEngineResult r = sslEngine.unwrap(readEncryptedByteBuffer, readDecryptedByteBuffer);
            log("unwrap " + getResultString(r));
            if (r.getStatus() == SSLEngineResult.Status.OK) {
                readDecryptedByteBuffer.flip();
                return r.bytesProduced();
            } else if (r.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                readEncryptedByteBuffer.compact();
            }
        } else {
            readEncryptedByteBuffer.clear();
        }
        try {
            int count = readableByteChannel.read(readEncryptedByteBuffer);
            readEncryptedByteBuffer.flip();
            log("Unwrap read " + count);
            return count == -1 ? -1 : unwrap();
        } catch (IOException e) {
            close();
            return -1;
        }
    }

    private synchronized void writeDecrypted(byte[] buffer, int offset, final int maxLength) throws IOException {
        log("Wrap");
        checkByteArrayParameters(buffer, offset, maxLength);
        throwIfClosed();
        if (!isHandShaken()) {
            SSLEngineResult r = handshake();
            log("HandShake done " + getResultString(r));
        }
        int sentLength = 0;
        while (sentLength < maxLength) {
            if (writeDecryptedByteBuffer.hasRemaining()) {
                sentLength += writeDecryptedByteBuffer.limit();
                offset += sentLength;
                wrap();
            } else {
                writeDecryptedByteBuffer.clear();
                writeDecryptedByteBuffer.put(buffer, offset,
                        Math.min(maxLength - sentLength, writeDecryptedByteBuffer.remaining()));
                writeDecryptedByteBuffer.flip();
            }
        }
    }

    private void wrap() throws IOException {
        while (writeDecryptedByteBuffer.hasRemaining()) {
            log("wrap " + writeDecryptedByteBuffer.remaining());
            SSLEngineResult r = sslEngine.wrap(writeDecryptedByteBuffer, writeEncryptedByteBuffer);
            writeEncryptedByteBuffer.flip();
            log("write " + getResultString(r) + " written: " + writeEncryptedByteBuffer.remaining() + ", left " + writeDecryptedByteBuffer.remaining());
            writableByteChannel.write(writeEncryptedByteBuffer);
            writeEncryptedByteBuffer.clear();
        }
    }

    private void checkByteArrayParameters(byte[] b, int off, int len) {
        if (b == null) {
            throw new IllegalArgumentException("Input byte array shouldn't be null");
        } else if (len > b.length - off || off < 0 || len <= 0) {
            throw new IllegalArgumentException("Wrong size requested. Buffer length: " + b.length
                    + " offset: " + off + " maxLength " + len);
        }
    }

    private void throwIfClosed() throws IOException {
        if (isClosed) {
            throw new IOException("SSL Layer is Closed");
        }
    }

    private class DecryptedInputStream extends InputStream {
        private byte[] singleByte = new byte[1];

        @Override
        public int read() throws IOException {
            int count = readDecrypted(singleByte, 0, 1);
            return count == 1 ? singleByte[0] : -1;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return readDecrypted(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return readDecrypted(b, off, len);
        }

        @Override
        public void close() throws IOException {
            SslLayer.this.close();
        }
    }

    private class DecryptedOutputStream extends OutputStream {
        private byte[] singleByte = new byte[1];

        @Override
        public void write(int b) throws IOException {
            singleByte[0] = (byte) b;
            writeDecrypted(singleByte, 0, 1);
        }

        @Override
        public void write(byte[] b) throws IOException {
            writeDecrypted(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            writeDecrypted(b, off, len);
        }

        @Override
        public void close() throws IOException {
            SslLayer.this.close();
        }
    }
}