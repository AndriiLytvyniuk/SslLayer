package com.alytvyniuk.ssl_layer.bidirectional_test;

import com.alytvyniuk.ssl_layer.SslLayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class ServerThread extends BaseThread {

    private static final String TAG = "ServerThread";
    private final BufferedReader mResponseIS;
    private static final int DEFAULT_SERVER_BUFFER_SIZE = 1024;
    private byte [] mReceiveBuffer = new byte[DEFAULT_SERVER_BUFFER_SIZE];
    private boolean mIsBidirectional;

    public ServerThread(SslLayer sslLayer, File response, File sentFile, File receivedFile, boolean isBidirectional) throws FileNotFoundException {
        super(ClientThread.class.getSimpleName(), sslLayer, sentFile, receivedFile);
        mResponseIS = response != null
                ? new BufferedReader(new InputStreamReader(new FileInputStream(response)))
                : null;
        mIsBidirectional = isBidirectional;
    }

    @Override
    public void run() {
        try {
            boolean isGoodbyeReceived = false;
            while (!isGoodbyeReceived) {
                isGoodbyeReceived = read(mReceiveBuffer);
                if (mIsBidirectional) {
                    respond(isGoodbyeReceived);
                }
            }
            mChannelOS.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void respond(boolean isGoodbyeReceived) throws IOException, InterruptedException {
        if (mResponseIS != null) {
            while (true) {
                String line = mResponseIS.readLine();
                if (line != null) {
                    if (line.startsWith("DELAY")) {
                        int delay = Integer.parseInt(line.split(" ")[1]);
                        Thread.sleep(delay);
                        continue;
                    }
                }
                if (line == null) {
                    line = "DEFAULT RESPONSE";
                }
                if (isGoodbyeReceived) {
                    line = GOODBYE;
                }
                line = line.concat(System.lineSeparator());
                byte[] data = line.getBytes();
                write(data);
                break;
            }
        }
    }
}