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
            System.out.println(TAG + " Received " + received);
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
        System.out.println(TAG + " Sent " + new String(buffer));
    }
}
