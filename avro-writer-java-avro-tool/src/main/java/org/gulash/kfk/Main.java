package org.gulash.kfk;

import org.gulash.kfk.avro.model.User;

public class Main {
    public static void main(String[] args) {
        var user = User.newBuilder()
                .setName("John Doe")
                .setFavoriteColor("White")
                .setFavoriteNumber(1)
                //.setEmail("john.doe@gmail.com")
                .build();

        System.out.println(user);
    }
}