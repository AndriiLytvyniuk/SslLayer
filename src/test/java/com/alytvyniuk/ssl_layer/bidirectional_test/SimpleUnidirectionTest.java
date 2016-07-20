package com.alytvyniuk.ssl_layer.bidirectional_test;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by andrii on 12.07.16.
 */
public class SimpleUnidirectionTest extends BaseTest {

    private static final String TEST_DIRECTORY = "src/test/res/ssl_converter_test/ordinary";
    private static final String KEY_DIRECTORY = "src/test/res/ssl_converter_test/keys/ssl_proxy.jks";
    private static final String KEY_PASSWORD = "ssl_proxy";
    private static final String REQUEST_FILE_PATH = "src/test/res/ssl_converter_test/requests/request";
    private static final String RESPONSE_FILE_PATH = null;
    private static final boolean IS_BIDIRECTIONAL = false;

    @Test
    public void ordinaryTest() throws IOException {
        System.out.println("ordinaryTest");
        init(TEST_DIRECTORY, IS_BIDIRECTIONAL, KEY_DIRECTORY, KEY_PASSWORD, REQUEST_FILE_PATH, RESPONSE_FILE_PATH);
        runRequest(1000);
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
        mClientSentFile.delete();
        mServerReceivedFile.delete();
    }
}
