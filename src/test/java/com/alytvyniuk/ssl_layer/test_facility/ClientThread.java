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
package com.alytvyniuk.ssl_layer.test_facility;

import com.alytvyniuk.ssl_layer.SslLayer;

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
