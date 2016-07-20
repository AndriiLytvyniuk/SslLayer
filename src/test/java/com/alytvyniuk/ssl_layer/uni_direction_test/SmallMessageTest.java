package com.alytvyniuk.ssl_layer.uni_direction_test;

import org.junit.Test;

import java.io.IOException;

/**
 * Created by andrii on 12.07.16.
 */
public class SmallMessageTest extends BaseTest {

    @Test
    public void smallMessageTest() throws IOException {
        System.out.println("smallMessageTest");
        runRequest("src/test/res/ssl_converter_test/requests/small_request", 1000);
    }
}
