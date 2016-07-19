package com.alytvyniuk.ssl_layer.test_facility;

import com.mauriciotogneri.trail.Trail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ServerThread extends Thread {

    private static final String TAG = "ServerThread";
    private final InputStream mRequestIS;
    private final OutputStream mServerWriteFileOS;
    private final byte [] mServerBuffer;

    public ServerThread(InputStream requestIS, File serverWriteFile, int serverBufferSize) throws FileNotFoundException {
        super(ServerThread.class.getSimpleName());
        mServerWriteFileOS = new FileOutputStream(serverWriteFile);
        mRequestIS = requestIS;
        mServerBuffer = new byte[serverBufferSize];
    }

    @Override
    public void run() {
        try {
            while (true) {
                int count = mRequestIS.read(mServerBuffer, 0, mServerBuffer.length);
                if (count == -1) {
                    return;
                }
                String received = new String(mServerBuffer, 0, count);
                Trail.verbose(TAG, "Received " + received);
                mServerWriteFileOS.write(mServerBuffer, 0, count);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}