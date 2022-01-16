package org.nwolfhub;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.GetUpdatesResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class UpdateListener {
    private static TelegramBot bot;
    public static boolean working = true;
    public static void initialize(TelegramBot bot) {
        UpdateListener.bot = bot;
        working = true;
        new Thread(UpdateListener::work).start();
        Logger.getLogger(UpdateListener.class.getName()).info("Up and running!");
    }

    private static void work() {
        int offsetId = 0;
        List<Update> updateList = new ArrayList<>();
        while (working) {
            GetUpdates request = new GetUpdates().limit(100).offset(offsetId).timeout(10);
            try {
                GetUpdatesResponse response = bot.execute(request);
                updateList.clear();
                updateList.addAll(response.updates());
                offsetId = updateList.get(updateList.size()-1).updateId() + 1;
                for(Update update:updateList) {
                    UpdateHandler.handleUpdate(update);
                }
            } catch (Exception ignored) {}
        }
    }
}
