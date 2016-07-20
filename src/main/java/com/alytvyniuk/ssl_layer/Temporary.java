package com.alytvyniuk.ssl_layer;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by andrii on 14.07.16.
 */
public class Temporary {

    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.put((byte)2);
        buffer.put((byte)3);
        buffer.put((byte)4);
        buffer.flip();
        System.out.println(buffer + " " + Arrays.toString(buffer.array()));
//        buffer.get();
        buffer.get();
//        System.out.println(buffer);
        buffer.compact();
        System.out.println(buffer + " " + Arrays.toString(buffer.array()));
        System.out.println(buffer.get() + " " + buffer.get());

        String st = "1";
        System.out.println(Arrays.toString(st.getBytes()));
    }
}
