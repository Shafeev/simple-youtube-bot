package org.simple.bot.telegram.youtube;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.AudioFormat;
import com.github.kiulian.downloader.model.videos.formats.VideoWithAudioFormat;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GenerateLinkHandler extends TelegramLongPollingBot {
    private static final String CHOOSE_VIDEO_OR_AUDIO = "Выберите тип ссылок";
    private static final String ADD_LINK_VIDEO = "Укажите ссылку на видео";
    private static final String AUDIO = "audio";
    private static final String VIDEO = "video";
    private static final String BOT_NAME = "GenerateLinkBot";
    private static final String START = "start";
    private static final String HTTPS = "https";
    private static final String YOUTU = "youtu";
    private HashMap<Long, String> urlMap = new HashMap<>();
    private String token;

    GenerateLinkHandler(String token) {
        this.token = token;
    }

    public String getBotUsername() {
        return BOT_NAME;
    }

    public String getBotToken() {
        return this.token;
    }

    @Override
    public void onUpdateReceived(Update update) {

        try {
            if (update.hasMessage()) {
                //Извлекаем объект входящего сообщения
                Message inMessage = update.getMessage();
                Long chatId = inMessage.getChatId();

                if (inMessage.hasText()) {
                    String userMessageText = inMessage.getText();
                    if (userMessageText.contains(START)) {
                        sendMessage(chatId, ADD_LINK_VIDEO);
                    } else if (userMessageText.contains(HTTPS) && userMessageText.contains(YOUTU)) {
                        urlMap.put(chatId, getVideoId(userMessageText));
                        execute(sendInlineKeyBoardMessage(chatId));
                    } else {
                        sendMessage(chatId, ADD_LINK_VIDEO);
                    }
                } else if (inMessage.hasReplyMarkup()) {
                    sendMessage(chatId, ADD_LINK_VIDEO);
                }
            } else {
                if (update.hasCallbackQuery()) {
                    CallbackQuery callbackQuery = update.getCallbackQuery();
                    Message inMessage = callbackQuery.getMessage();
                    String userMessageText = callbackQuery.getData();
                    Long chatId = inMessage.getChatId();

                    if (userMessageText.contains(VIDEO)) {
                        if (urlMap.containsKey(chatId)) {
                            String videoId = urlMap.get(chatId);
                            String videoUrls = getVideoUrl(videoId);
                            sendMessage(chatId, videoUrls);
                        } else {
                            sendMessage(chatId, ADD_LINK_VIDEO);
                        }
                    } else if (userMessageText.contains(AUDIO)) {
                        if (urlMap.containsKey(chatId)) {
                            String videoId = urlMap.get(chatId);
                            String audioUrl = getAudioUrl(videoId);
                            sendMessage(chatId, audioUrl);
                        } else {
                            sendMessage(chatId, ADD_LINK_VIDEO);
                        }
                    }
                }
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private synchronized void sendMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private synchronized SendMessage sendInlineKeyBoardMessage(long chatId) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();

        InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();
        inlineKeyboardButton1.setText(VIDEO);
        inlineKeyboardButton1.setCallbackData(VIDEO);

        keyboardButtonsRow1.add(inlineKeyboardButton1);

        InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
        inlineKeyboardButton2.setText(AUDIO);
        inlineKeyboardButton2.setCallbackData(AUDIO);

        keyboardButtonsRow1.add(inlineKeyboardButton2);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(keyboardButtonsRow1);
        inlineKeyboardMarkup.setKeyboard(keyboard);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(CHOOSE_VIDEO_OR_AUDIO);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        return sendMessage;
    }

    private String getVideoId(String url) {
        String videoId = url;

        String pattern = "(?<=watch\\?v=|/videos/|embed/|(youtu.be/|(v/)|(/u/\\w/)))[^#&?]*";

        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);

        if (matcher.find()) {
            videoId = matcher.group();
        }

        return videoId;
    }


    private VideoInfo getVideoInfo(String videoId) {
        // init downloader
        YoutubeDownloader downloader = new YoutubeDownloader();

        // sync parsing
        RequestVideoInfo request = new RequestVideoInfo(videoId);
        Response<VideoInfo> response = downloader.getVideoInfo(request);
        return response.data();
    }

    private String getAudioUrl(String videoId) {

        VideoInfo videoInfo = getVideoInfo(videoId);

        StringBuilder builder = new StringBuilder();

        // get audio formats
        List<AudioFormat> audioFormats = videoInfo.audioFormats();
        audioFormats.forEach(it -> builder.append(it.type()).append(" : ").append(it.url()).append("\n"));

        return builder.toString();
    }

    private String getVideoUrl(String videoId) {

        VideoInfo videoInfo = getVideoInfo(videoId);

        StringBuilder builder = new StringBuilder();

        // get videos formats only with audio
        List<VideoWithAudioFormat> videoWithAudioFormats = videoInfo.videoWithAudioFormats();
        videoWithAudioFormats.forEach(it -> {
//            System.out.println(it.audioQuality() + ", " + it.videoQuality() + " : " + it.url());
            builder.append(it.type()).append(" : ").append(it.videoQuality()).append(" : ").append(it.url()).append("\n");
        });

        return builder.toString();
    }

}


