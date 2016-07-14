package com.alytvyniuk.ssl_layer;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;

public class SslLayer extends SSLHandShaker implements Closeable {

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
        if (!isHandShaken()) {
            SSLEngineResult r = handshake();
            log("HandShake done " + getResultString(r));
        }
        int readLength = 0;
        int unwrapResult = -1;
        while (readLength < maxLength) {
            if (decryptedReadBuffer.remaining() == 0) {
                unwrapResult = unwrap();
            }
            if (decryptedReadBuffer.remaining() > 0) {
                int length = Math.min(buffer.length - offset, decryptedReadBuffer.remaining());
                readLength += length;
                decryptedReadBuffer.get(buffer, offset, length);
                offset += length;
            }
            if (readLength == 0 && unwrapResult == -1) {
                return -1;
            }
        }
        log("readDecrypted length: " + readLength);
        return readLength;
    }

    private int unwrap() throws IOException {
        log("handShakeUnwrap");
        if (readEncryptedByteBuffer.hasRemaining()) {
            decryptedReadBuffer.clear();
            SSLEngineResult r = sslEngine.unwrap(readEncryptedByteBuffer, decryptedReadBuffer);
            log("handShakeUnwrap unwrap " + getResultString(r));
            if (r.getStatus() == SSLEngineResult.Status.OK) {
                decryptedReadBuffer.flip();
                return r.bytesProduced();
            } else if (r.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                readEncryptedByteBuffer.compact();
            }
        } else {
            readEncryptedByteBuffer.clear();
        }
        int count = readableByteChannel.read(readEncryptedByteBuffer);
        readEncryptedByteBuffer.flip();
        log("handShakeUnwrap read " + count);
        return count == -1 ? -1 : unwrap();

    }

    private synchronized void writeDecrypted(byte[] buffer, int offset, final int maxLength) throws IOException {
        checkByteArrayParameters(buffer, offset, maxLength);
        if (!isHandShaken()) {
            SSLEngineResult r = handshake();
            log("HandShake done " + getResultString(r));
        }
        int sentLength = 0;
        while (sentLength < maxLength) {
            if (decryptedWriteByteBuffer.hasRemaining()) {
                sentLength += decryptedWriteByteBuffer.limit();
                wrap(decryptedWriteByteBuffer);
            } else {
                decryptedWriteByteBuffer.clear();
                decryptedWriteByteBuffer.put(buffer, offset,
                        Math.min(maxLength - sentLength, decryptedWriteByteBuffer.remaining()));
                decryptedWriteByteBuffer.flip();
            }
        }
    }

    private void wrap(ByteBuffer byteBuffer) throws IOException {
        limbo.clear();
        SSLEngineResult r = sslEngine.wrap(byteBuffer, limbo);
        limbo.flip();
        writableByteChannel.write(limbo);
        limbo.clear();
    }

    public synchronized void close() throws IOException {
        log("close");
//        if (this.isClosed == false) {
//            this.isClosed = true;
//            try {
//                if (this.encryptedInput != null) this.encryptedInput.close();
//                if (this.encryptedOutput != null) this.encryptedOutput.close();
//            } catch (Exception x) {
//            }
//        }
    }

    private void checkByteArrayParameters(byte[] b, int off, int len) {
        if (b == null) {
            throw new IllegalArgumentException("Input byte array shouldn't be null");
        } else if (len > b.length - off || off < 0 || len <= 0) {
            throw new IllegalArgumentException("Wrong size requested. Buffer length: " + b.length
                    + " offset: " + off + " maxLength " + len);
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