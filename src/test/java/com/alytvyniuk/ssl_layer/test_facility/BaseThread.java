package com.alytvyniuk.ssl_layer.test_facility;

import com.alytvyniuk.ssl_layer.SslLayer;
import com.mauriciotogneri.trail.Trail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by andrii on 19.07.16.
 */
public abstract class BaseThread extends Thread {

    private String TAG = "BaseThread";
    protected static final String GOODBYE = "GOODBYE";
    protected final OutputStream mSentWriteFileOS;
    protected final OutputStream mReceiveWriteFileOS;
    protected final OutputStream mChannelOS;
    protected final InputStream mChannelIS;
    private final ByteBuffer mLengthValueByteBuffer = ByteBuffer.allocate(4);
    private final byte [] mLengthValueBuffer = new byte[4];

    public BaseThread(String name, SslLayer channel, File sentFile, File receivedFile) throws FileNotFoundException {
        super(name);
        TAG = name;
        mSentWriteFileOS = sentFile != null ? new FileOutputStream(sentFile) : null;
        mReceiveWriteFileOS = receivedFile != null ? new FileOutputStream(receivedFile) : null;
        mChannelOS = channel.getDecryptedOutputStream();
        mChannelIS = channel.getDecryptedInputStream();
    }

    protected boolean read(byte [] buffer) throws IOException {
        mChannelIS.read(mLengthValueBuffer);
        mLengthValueByteBuffer.clear();
        mLengthValueByteBuffer.put(mLengthValueBuffer);
        mLengthValueByteBuffer.flip();
        int length = mLengthValueByteBuffer.getInt();
        int readLength = 0;
        while (length > readLength) {
            int count = mChannelIS.read(buffer, 0, buffer.length);
            if (count == -1) {
                return false;
            }
            readLength += count;
            String received = new String(buffer, 0, count);
            Trail.verbose(TAG, "Received " + received);
            if (mReceiveWriteFileOS != null) {
                mReceiveWriteFileOS.write(buffer, 0, count);
            }
            if (received.equals(GOODBYE)) {
                return true;
            }
        }
        return false;
    }

    protected void write(byte [] buffer) throws IOException {
        mLengthValueByteBuffer.clear();
        mLengthValueByteBuffer.putInt(buffer.length);
        mChannelOS.write(mLengthValueByteBuffer.array());
        mChannelOS.write(buffer, 0, buffer.length);
        if (mSentWriteFileOS != null) {
            mSentWriteFileOS.write(buffer, 0, buffer.length);
        }
        Trail.verbose(TAG, "Sent " + new String(buffer));
    }
}
