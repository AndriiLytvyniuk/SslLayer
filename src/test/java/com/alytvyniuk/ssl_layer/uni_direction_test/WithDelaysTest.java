package com.alytvyniuk.ssl_layer.uni_direction_test;

import org.junit.Test;

import java.io.IOException;

/**
 * Created by andrii on 12.07.16.
 */
public class WithDelaysTest extends BaseTest {

    @Test
    public void withDelaysTest() throws IOException {
        System.out.println("withDelaysTest");
        runRequest("src/test/res/ssl_converter_test/requests/request_with_delays", 1000);
    }
}
