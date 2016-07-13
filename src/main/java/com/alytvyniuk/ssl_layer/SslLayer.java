package com.alytvyniuk.ssl_layer;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

public class SslLayer extends SSLHandShaker implements Closeable {

    private final InputStream decryptedInputStream = new DecryptedInputStream();
    private final OutputStream decryptedOutputStream = new DecryptedOutputStream();

    public SslLayer(SSLEngine sslEngine, InputStream encryptedInput, OutputStream encryptedOutput) {
        super(sslEngine, encryptedInput, encryptedOutput);
    }

    public InputStream getInputStream() {
        return decryptedInputStream;
    }

    public OutputStream getOutputStream() {
        return decryptedOutputStream;
    }

    private synchronized void writeDecrypted(byte[] buffer, int offset, int length) throws IOException {
        ByteBuffer writeBuffer = ByteBuffer.wrap(buffer, offset, length);
        if (!isHandShakeFinished()) {
            SSLEngineResult r = handshake();
            log("HandShake done " + getResultString(r));
        }

//        if (!isHandShakeFinished) {
//            doWrap(EMPTY_BUFFER);
//        }
//        this.doWrap(writeBuffer);
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
        if (maxLength > buffer.length - offset || offset < 0 || maxLength <= 0) {
            throw new IllegalArgumentException("Wrong size requested. Buffer length: " + buffer.length
                    + " offset: " + offset + " maxLength " + maxLength);
        }
        if (!isHandShakeFinished()) {
            handshake();
        }
//        int readLength = 0;
//        while (readLength < maxLength) {
//            if (decryptedBuffer.remaining() == 0) {
//                if (doUnwrap() == -1) {
//                    break;
//                }
//            }
//            if (decryptedBuffer.remaining() > 0) {
//                int length = Math.min(maxLength - readLength, decryptedBuffer.remaining());
//                readLength += length;
//                decryptedBuffer.get(buffer, offset, length);
//                offset += length;
//            } else {
//                break;
//            }
//        }
//        log("readDecrypted length: " + readLength);
//        return readLength == 0 ? -1 : readLength;


//        while (decryptedBuffer.remaining() == 0) {
//            try {
//                doUnwrap();
//            } catch (EOFException onClosed) {
//                this.close();
//                return -1;
//            }
//        }
        int limit = Math.min(maxLength, this.decryptedBuffer.remaining());

        decryptedBuffer.get(buffer, offset, limit);

        return limit;
    }

//    private int doUnwrap() throws IOException {
//        //System.err.println("doUnwrap()");
//        if (decryptedBuffer.remaining() == 0) {
//            decryptedBuffer.clear();
//        } else {
//            decryptedBuffer.flip();
//        }
//
//        //limbo.flip();
//        //log("do Unwrap read " + encryptedInput.available());
//        //int count = encryptedInput.read(readEncryptedBuffer, 0, readEncryptedBuffer.length);
//        log("do Unwrap read2 " + count);
//        if (count == -1) {
//            return -1;
//        }
//        SSLEngineResult r = sslEngine.unwrap(ByteBuffer.wrap(readEncryptedBuffer, 0, count), decryptedBuffer);
//        int consumed = r.bytesConsumed();
//        while ((r.getStatus() == Status.BUFFER_UNDERFLOW || consumed < count) && count != -1) {
//            if (r.getStatus() == Status.BUFFER_UNDERFLOW) {
//                log("do Unwrap consumed = " + consumed + " , " + count);
//                byte[] newTmp = new byte[readEncryptedBuffer.length + ENCRYPTED_BUFFER_SIZE];
//                System.arraycopy(readEncryptedBuffer, 0, newTmp, 0, count);
//                readEncryptedBuffer = newTmp;
//                count += encryptedInput.read(readEncryptedBuffer, count, readEncryptedBuffer.length - count);
//            } else if (r.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
//                Runnable runnable = sslEngine.getDelegatedTask();
//                if (runnable != null) runnable.run();
//            }
//            ByteBuffer tempBuf = ByteBuffer.wrap(readEncryptedBuffer, consumed, count - consumed);
//            log("temp " + tempBuf);
//            r = sslEngine.unwrap(tempBuf, decryptedBuffer);
//            consumed += r.bytesConsumed();
//
//            log("do Unwrap consumed = " + consumed + " , " + count + ", r=" + r);
//        }
//        log("Unwrap " + getResultString(r));
////        if (r.getHandshakeStatus() == HandshakeStatus.FINISHED) {
////            isHandShakeFinished = true;
////        }
//
//        decryptedBuffer.flip();
//        //limbo.clear();
//        int length = r.bytesProduced();
//        if (length > 0) {
//            return length;
//        }
//
//        if (r.getStatus() == Status.CLOSED) {
//            throw new EOFException("End Of Stream");
//        } else if (r.getStatus() != Status.OK) {
//            throw new IOException("Unhandled Status: " + r.getStatus());
//        }
//
//        if (r.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
//            continueHandshake(r);
//        }
//        return length;
//    }

    private void doWrap(ByteBuffer serverOut) throws IOException {
        //System.err.println("doWrap()");
        SSLEngineResult r = sslEngine.wrap(serverOut, limbo);
        //log("Wrap " + getResultString(r));
//        if (r.getHandshakeStatus() == HandshakeStatus.FINISHED) {
//            isHandShakeFinished = true;
//        }
        limbo.flip();
        //log("wrap before write " + limbo);
        writableByteChannel.write(limbo);
        //encryptedOutput.write(limbo.array(), limbo.arrayOffset() + limbo.position(), limbo.remaining());
        //log("Wrap after write " + limbo);
        limbo.clear();

        if (r.getStatus() == Status.CLOSED) {
            throw new EOFException("End Of Stream");
        } else if (r.getStatus() != Status.OK) {
            throw new IOException("Unhandled Status: " + r.getStatus());
        }
        if (r.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
            continueHandshake(r);
        }
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