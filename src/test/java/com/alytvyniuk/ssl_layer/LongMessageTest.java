package com.alytvyniuk.ssl_layer;

import org.junit.Test;

import java.io.IOException;

/**
 * Created by andrii on 12.07.16.
 */
public class LongMessageTest extends BaseTest {

    @Test
    public void longMessageTest() throws IOException {
        System.out.println("ordinaryTest");
        runRequest("src/test/res/ssl_converter_test/requests/long_message_request", 1000);
    }
}
