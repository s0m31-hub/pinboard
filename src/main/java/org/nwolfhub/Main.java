package org.nwolfhub;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.GetMe;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        String token;
        try(FileInputStream fis = new FileInputStream("token.sopi")) {
            token = new String(fis.readAllBytes());
        } catch (IOException e) {
            e.printStackTrace();
            token = null;
            System.exit(1);
        }
        TelegramBot bot = new TelegramBot(token);
        System.out.println(bot.execute(new GetMe()).user());
        UpdateHandler.initialize(bot);
        UpdateListener.initialize(bot);
        Scanner sc = new Scanner(System.in);
        sc.nextLine();
        UpdateHandler.exit();
        System.exit(0);
    }
}
