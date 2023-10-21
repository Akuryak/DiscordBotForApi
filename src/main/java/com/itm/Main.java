package com.itm;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpRequest;
import java.util.Properties;

public class Main {
    private static Logger log = LoggerFactory.getLogger(Main.class);
    private static final Properties APP_PROPS = new Properties();

    public static void main(String[] args) {
        try (FileInputStream fis = new FileInputStream("src/main/resources/application.properties")){
            APP_PROPS.load(fis);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        DiscordClient bot = DiscordClient.create(APP_PROPS.getProperty("bot.token"));
        bot.withGateway(client ->
                        client.on(MessageCreateEvent.class, event -> {
                            Message message = event.getMessage();
                            switch (message.getContent().toLowerCase()) {
                                case "!ping" :
                                    log.info("Send message \"Pong!\"");
                                    return message.getChannel()
                                        .flatMap(channel -> channel.createMessage("Pong!"));

                                case "!test":
                                    return test(message);

                                default:
                                    return Mono.empty().cast(Message.class);
                            }
                        }))
                .block();
    }

    private static String getToken(){
        HttpRequest httpRequest = HttpRequest.newBuilder(URI
                        .create(APP_PROPS.getProperty("api.token.url")))
                .setHeader("Content-type", "application/x-www-form-urlencoded")
                .setHeader("grant_type", APP_PROPS.getProperty("api.grant_type"))
                .setHeader("client_id", APP_PROPS.getProperty("api.client_id"))
                .setHeader("username", APP_PROPS.getProperty("api.username"))
                .setHeader("password", APP_PROPS.getProperty("api.password"))
                .setHeader("client_secret", APP_PROPS.getProperty("api.client_secret"))
                .GET()
                .build();
        return httpRequest.headers().firstValue("access_token").orElse("");
    }

    private static String testService(String getUrl, String service){
        try {
            URL url = new URL(getUrl);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", getToken());
            connection.connect();

            int code = connection.getResponseCode();

            if (code == 200)
                return service + "-service - OK ✅ \n";
            else
                return service + "-service - DOWN \uD83D\uDEAB " + code + "\n";
        }
        catch (IOException exception){
            return exception.getMessage();
        }
    }

    private static Mono<Message> test(Message message) {
        String res = "";
        res += testService(APP_PROPS.getProperty("api.user-service"), "user");
        res += testService(APP_PROPS.getProperty("api.task-service"), "task");
        res += testService(APP_PROPS.getProperty("api.direction-service"), "direction");
        res += testService(APP_PROPS.getProperty("api.review-service"), "review");
        res += testService(APP_PROPS.getProperty("api.notification-service"), "notification");
        res += testService(APP_PROPS.getProperty("api.achievement-service"), "achievement");
        res += testService(APP_PROPS.getProperty("api.profile-service"), "profile");
        res += testService(APP_PROPS.getProperty("api.file-service"), "file");
        String finalRes = res;
        log.info(finalRes);
        return message.getChannel()
                .flatMap(channel -> channel.createMessage(finalRes));
    }
}