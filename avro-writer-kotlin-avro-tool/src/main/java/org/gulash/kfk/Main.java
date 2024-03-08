package org.gulash.kfk;

public class Main {
    public static void main(String[] args) {
        // Используем класс
        var user = User.newBuilder().setName("aa").setFavoriteColor("wwww").setFavoriteNumber(1).setEmail("aaas@ddd.ru").build();

        System.out.println(user);
    }
}

