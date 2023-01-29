package dz.tbot.reminder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class Bot extends TelegramLongPollingBot {

    private static final Logger log = LogManager.getLogger();

    private final String userName;
    private final String token;
    private final Long statusChatId;

    private final Set<Long> knownChat = new HashSet<>();

    @Override
    public void onUpdateReceived(Update update) {
        try {
            log.info("Get update {}", update);

            Message msg = update.getMessage();
            if (msg == null) {
                log.info("Ignoring update without message");
                return;
            }

            String text = msg.getText();
            if (text == null) {
                log.info("No text in the message");
                return;
            }
            Chat chat = msg.getChat();
            if (chat == null) {
                log.info("No chat in the message");
                return;
            }

            Long chatId = chat.getId();
            if (! knownChat.contains(chatId)) {
                knownChat.add(chatId);
                String fname = chat.getFirstName();
                String lname = chat.getLastName();
                log.info("Got new chat {} from {} {}", chatId, fname, lname);
                sendMessage(chatId, String.format("Hello, %s %s!",fname, lname));
            }

            sendMessage(chatId, "echo: " + text);

            if (text.equals("/shutdown")) {
                sendMessage(statusChatId, "Got shutdown command");
                log.info("Got shutdown command");
                System.exit(0);
            }

        } catch (Exception e) {
            log.error("Error in message processing", e);
        }
    }

    @Override
    public String getBotUsername() {
        return userName;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    public void sendMessage(Long chatId, String text) {
        try {
            SendMessage msg = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build();
            execute(msg);
        } catch (TelegramApiException e) {
            log.error("Exception in sending message to {}", chatId, e);
        }
    }

    public Bot() throws IOException {
        try (Reader reader = new FileReader("config.properties")) {
            Properties config = new Properties();
            config.load(reader);
            token = config.getProperty("telegram.token");
            userName = config.getProperty("telegram.username");
            statusChatId = new Long(config.getProperty("statusChatId"));

            if (token == null) throw new RuntimeException("Telegram token is not found in the config");
            if (userName == null) throw new RuntimeException("Username is not found in the config");
        }
    }

    public static void main(String[] args) {
        try {
            Bot bot = new Bot();
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(bot);

            bot.sendMessage(bot.statusChatId, "Bot started !!!");
        } catch (Exception e) {
            log.error("Error during start up: ",e);
        }
    }
}
