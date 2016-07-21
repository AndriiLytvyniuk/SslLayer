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
package com.alytvyniuk.ssl_layer.tests;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by andrii on 12.07.16.
 */
public class LongMessageBidirectionalTest extends BaseTest {

    private static final String KEY_DIRECTORY = "src/test/res/ssl_converter_test/keys/ssl_proxy.jks";
    private static final String KEY_PASSWORD = "ssl_proxy";
    private static final String REQUEST_FILE_PATH = "src/test/res/ssl_converter_test/requests/long_message_request";
    private static final String RESPONSE_FILE_PATH = "src/test/res/ssl_converter_test/requests/long_message_request";
    private static final boolean IS_BIDIRECTIONAL = true;

    @Test
    public void test() throws IOException {
        init(IS_BIDIRECTIONAL, KEY_DIRECTORY, KEY_PASSWORD, REQUEST_FILE_PATH, RESPONSE_FILE_PATH);
        runRequest();
    }

    @Override
    protected void onConnectionFinished() {
        boolean isForwardEqual = false;
        boolean isBackwardEqual = false;
        try {
            isForwardEqual = FileUtils.contentEquals(mClientSentFile, mServerReceivedFile);
            isBackwardEqual = FileUtils.contentEquals(mServerSentFile, mClientReceivedFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(isForwardEqual);
        Assert.assertTrue(isBackwardEqual);
    }
}
