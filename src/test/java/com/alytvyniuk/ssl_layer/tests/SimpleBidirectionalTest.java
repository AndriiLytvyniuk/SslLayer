package com.alytvyniuk.ssl_layer.tests;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by andrii on 12.07.16.
 */
public class SimpleBidirectionalTest extends BaseTest {

    private static final String KEY_DIRECTORY = "src/test/res/ssl_converter_test/keys/ssl_proxy.jks";
    private static final String KEY_PASSWORD = "ssl_proxy";
    private static final String REQUEST_FILE_PATH = "src/test/res/ssl_converter_test/requests/request";
    private static final String RESPONSE_FILE_PATH = null;
    private static final boolean IS_BIDIRECTIONAL = true;

    @Test
    public void test() throws IOException {
        init(IS_BIDIRECTIONAL, KEY_DIRECTORY, KEY_PASSWORD, REQUEST_FILE_PATH, RESPONSE_FILE_PATH);
        runRequest();
    }

    @Override
    protected void onConnectionFinished() {
        boolean isEqual = false;
        try {
            isEqual = FileUtils.contentEquals(mClientSentFile, mServerReceivedFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(isEqual);
    }
}
