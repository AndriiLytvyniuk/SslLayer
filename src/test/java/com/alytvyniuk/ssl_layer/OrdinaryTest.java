package com.alytvyniuk.ssl_layer;

import org.junit.Test;

import java.io.IOException;

/**
 * Created by andrii on 12.07.16.
 */
public class OrdinaryTest extends BaseTest {

    @Test
    public void ordinaryTest() throws IOException {
        System.out.println("ordinaryTest");
        runRequest("src/test/res/ssl_converter_test/requests/request", 1000);
    }
}
