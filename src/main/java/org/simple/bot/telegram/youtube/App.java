package org.simple.bot.telegram.youtube;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class App {

    public static void main(String[] args) {
        try {
            if (args.length > 0) {
                TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
                telegramBotsApi.registerBot(new GenerateLinkHandler(args[0]));
            } else {
                System.err.println("Token is not correct or missing.");
            }
        } catch (TelegramApiException ex) {
            ex.printStackTrace();
        }
    }
}
