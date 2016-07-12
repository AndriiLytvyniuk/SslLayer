package com.alytvyniuk.ssl_layer.test_facility;

import com.mauriciotogneri.trail.Trail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Created by alytvyniuk on 28.01.16.
 */
public class ClientThread extends Thread {

    private static final String TAG = "ClientThread";
    private final BufferedReader mRequestIS;
    private final OutputStream mClientWriteFileOS;
    private final OutputStream mChannelOS;

    public ClientThread(OutputStream channelOS, File request, File clientWriteFile) throws FileNotFoundException {
        super(ClientThread.class.getSimpleName());
        mRequestIS = new BufferedReader(new InputStreamReader(new FileInputStream(request)));
        mClientWriteFileOS = new FileOutputStream(clientWriteFile);
        mChannelOS = channelOS;
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
                mChannelOS.write(data, 0, data.length);
                mClientWriteFileOS.write(data, 0, data.length);
                Trail.verbose(TAG, "Sent " + new String(data));
            }
            mChannelOS.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
