package com.alytvyniuk.ssl_layer.test_facility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Created by alytvyniuk on 27.01.16.
 */
public class NetworkChannel {

    private final PipedOutputStream mOS;
    private final PipedInputStream mIS;
    private static final int CHANNEL_SIZE = 20000;

    public NetworkChannel() throws IOException {
        mOS = new PipedOutputStream();
        mIS = new PipedInputStream(mOS, CHANNEL_SIZE);
    }

    public OutputStream getOutputStream() {
        return mOS;
    }

    public InputStream getInputStream() {
        return mIS;
    }
}
