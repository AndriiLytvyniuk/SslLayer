package com.alytvyniuk.ssl_layer.bidirectional_test;

import com.alytvyniuk.ssl_layer.SslLayer;
import com.alytvyniuk.ssl_layer.test_facility.NetworkChannel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by alytvyniuk on 28.01.16.
 */
public class ClientThread extends BaseThread {

    private static final String TAG = "ClientThread";
    private final BufferedReader mRequestIS;
    private static final int DEFAULT_SERVER_BUFFER_SIZE = 1024;
    private byte [] mReceiveBuffer = new byte[DEFAULT_SERVER_BUFFER_SIZE];
    private boolean mIsBidirectional;

    public ClientThread(SslLayer sslLayer, File request, File sentFile, File receivedFile, boolean isBidirectional) throws FileNotFoundException {
        super(ClientThread.class.getSimpleName(), sslLayer, sentFile, receivedFile);
        mRequestIS = new BufferedReader(new InputStreamReader(new FileInputStream(request)));
        mIsBidirectional = isBidirectional;
    }

    @Override
    public void run() {
        String line;
        try {
            while ((line = mRequestIS.readLine()) != null) {
                if (line.startsWith("DELAY")) {
                    int delay = Integer.parseInt(line.split(" ")[1]);
                    Thread.sleep(delay);
                    continue;
                }
                line = line.concat(System.lineSeparator());
                byte[] data = line.getBytes();
                write(data);
                if (mIsBidirectional) {
                    read(mReceiveBuffer);
                }
            }
            write(GOODBYE.getBytes());
            if (mIsBidirectional) {
                read(mReceiveBuffer);
            }
            mChannelOS.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
