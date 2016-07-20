package com.alytvyniuk.ssl_layer.bidirectional_test;

import com.alytvyniuk.ssl_layer.SslLayer;
import com.alytvyniuk.ssl_layer.test_facility.NetworkChannel;
import com.mauriciotogneri.trail.Trail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by andrii on 19.07.16.
 */
public abstract class BaseThread extends Thread {

    private static final String TAG = "BaseThread";
    protected static final String GOODBYE = "GOODBYE";
    protected final OutputStream mSentWriteFileOS;
    protected final OutputStream mReceiveWriteFileOS;
    protected final OutputStream mChannelOS;
    protected final InputStream mChannelIS;

    public BaseThread(String name, SslLayer channel, File sentFile, File receivedFile) throws FileNotFoundException {
        super(name);
        mSentWriteFileOS = sentFile != null ? new FileOutputStream(sentFile) : null;
        mReceiveWriteFileOS = receivedFile != null ? new FileOutputStream(receivedFile) : null;
        mChannelOS = channel.getDecryptedOutputStream();
        mChannelIS = channel.getDecryptedInputStream();
    }

    protected boolean read(byte [] buffer) throws IOException {
        while (true) {
            int count = mChannelIS.read(buffer, 0, buffer.length);
            if (count == -1) {
                return false;
            }
            String received = new String(buffer, 0, count);
            Trail.verbose(TAG, "Received " + received);
            if (mReceiveWriteFileOS != null) {
                mReceiveWriteFileOS.write(buffer, 0, count);
            }
            if (received.equals(GOODBYE)) {
                return true;
            }
        }
    }

    protected void write(byte [] buffer) throws IOException {
        mChannelOS.write(buffer, 0, buffer.length);
        if (mSentWriteFileOS != null) {
            mSentWriteFileOS.write(buffer, 0, buffer.length);
        }
        Trail.verbose(TAG, "Sent " + new String(buffer));
    }
}
