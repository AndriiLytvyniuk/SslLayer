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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

/**
 * Class that performs all operations related to single SSL connection.
 * It requires:
 * 1) I/O with encrypted data from one side: ({@link SSLHandShaker#readEncryptedChannel} and {@link SSLHandShaker#writeEncryptedChannel})
 * 2) I/O with decrypted data from other ({@link #decryptedInputStream} and {@link #decryptedOutputStream})
 * 3) {@link SSLEngine}, client or server
 * It provides one session connection: handshake + data exchange.
 * After SslLayer is connected, just try to write or receive information from decrypted channels,
 * all handshake operations will be performed automatically. If handshake is not successful, SSLException will be thrown
 */
public class SslLayer extends SSLHandShaker {

    private final InputStream decryptedInputStream = new DecryptedInputStream();
    private final OutputStream decryptedOutputStream = new DecryptedOutputStream();

    /**
     * Constructor for SslLayer
     * @param sslEngine client or server type. supplied with all needed Trust and Key Managers
     *                 If configured incorrectly, SSLException will be thrown
     * @param encryptedInput InputStream with encrypted data
     * @param encryptedOutput OutputStream for encrypted data
     */
    public SslLayer(SSLEngine sslEngine, InputStream encryptedInput, OutputStream encryptedOutput) {
        super(sslEngine, encryptedInput, encryptedOutput);
        readEncryptedByteBuffer.limit(0);
    }

    /**
     * @return InputStream with decrypted data
     */
    public InputStream getDecryptedInputStream() {
        return decryptedInputStream;
    }

    /**
     * @return OutputStream for decrypted data
     */
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
            log("unwrap done " + getResultString(r));
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
            int count = readEncryptedChannel.read(readEncryptedByteBuffer);
            readEncryptedByteBuffer.flip();
            log("read " + count);
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
                sentLength += writeDecryptedByteBuffer.remaining();
                offset = sentLength;
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
        log("Wrap");
        while (writeDecryptedByteBuffer.hasRemaining()) {
            SSLEngineResult r = sslEngine.wrap(writeDecryptedByteBuffer, writeEncryptedByteBuffer);
            log("wrap done" + getResultString(r));
            writeEncryptedByteBuffer.flip();
            log("write " + writeEncryptedByteBuffer.remaining() + ", left " + writeDecryptedByteBuffer.remaining());
            writeEncryptedChannel.write(writeEncryptedByteBuffer);
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