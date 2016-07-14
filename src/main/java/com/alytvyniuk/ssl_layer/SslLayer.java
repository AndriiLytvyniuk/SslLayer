package com.alytvyniuk.ssl_layer;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

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

    public InputStream getInputStream() {
        return decryptedInputStream;
    }

    public OutputStream getOutputStream() {
        return decryptedOutputStream;
    }

    /**
     * Reads data to buffer
     * @param buffer
     * @param offset
     * @param maxLength
     * @return size of read bytes or -1 if nothing to read
     * @throws IOException
     */
    private synchronized int readDecrypted(byte[] buffer, int offset, final int maxLength) throws IOException {
        checkByteArrayParameters(buffer, offset, maxLength);
        throwIfClosed();
        if (!isHandShaken()) {
            SSLEngineResult r = handshake();
            log("HandShake done " + getResultString(r));
        }
        int readLength = 0;
        int unwrapResult = -1;
        //while (readLength < maxLength) {
            if (readDecryptedByteBuffer.remaining() == 0) {
                unwrapResult = unwrap();
            }
            if (readDecryptedByteBuffer.remaining() > 0) {
                readLength = Math.min(buffer.length - offset, readDecryptedByteBuffer.remaining());
                //readLength += length;
                readDecryptedByteBuffer.get(buffer, offset, readLength);
                //offset += length;
            }
        log("readDecrypted length: " + readLength);
            if (readLength == 0 && unwrapResult == -1) {
                return -1;
            }
        //}
        return readLength;
    }

    private int unwrap() throws SSLException {
        log("handShakeUnwrap");
        if (readEncryptedByteBuffer.hasRemaining()) {
            readDecryptedByteBuffer.clear();
            SSLEngineResult r = sslEngine.unwrap(readEncryptedByteBuffer, readDecryptedByteBuffer);
            log("handShakeUnwrap unwrap " + getResultString(r));
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
            log("handShakeUnwrap read " + count);
            return count == -1 ? -1 : unwrap();
        } catch (IOException e) {
            close();
            return -1;
        }
    }

    private synchronized void writeDecrypted(byte[] buffer, int offset, final int maxLength) throws IOException {
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
                wrap(writeDecryptedByteBuffer);
            } else {
                writeDecryptedByteBuffer.clear();
                writeDecryptedByteBuffer.put(buffer, offset,
                        Math.min(maxLength - sentLength, writeDecryptedByteBuffer.remaining()));
                writeDecryptedByteBuffer.flip();
            }
        }
    }

    private void wrap(ByteBuffer byteBuffer) throws IOException {
        writeEncryptedByteBuffer.clear();
        SSLEngineResult r = sslEngine.wrap(byteBuffer, writeEncryptedByteBuffer);
        writeEncryptedByteBuffer.flip();
        writableByteChannel.write(writeEncryptedByteBuffer);
        writeEncryptedByteBuffer.clear();
    }

    private synchronized void close() {
        log("close");
        isClosed = true;
        try {
            writableByteChannel.close();
            readableByteChannel.close();
        } catch (IOException e) {
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
            if (count == -1) return -1;
            else if (count != 1) throw new IOException("Unexpected read count: " + count);
            return singleByte[0];
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