package com.alytvyniuk.ssl_layer;

import java.nio.ByteBuffer;

/**
 * Created by andrii on 14.07.16.
 */
public class Temporary {

    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(40);
        buffer.putLong(80L);
        buffer.putLong(80L);
        buffer.putLong(80L);
        buffer.flip();
        System.out.println(buffer);
//        buffer.get();
//        buffer.get();
//        System.out.println(buffer);
        buffer.compact();
        System.out.println(buffer);
        Integer a = 5;
        try {
            a.byteValue();
        } catch (NullPointerException e) {
            System.out.println("NullPointerException");
        } finally {
            System.out.println("finally");
        }

    }
}
