package org.nwolfhub;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.SendMessage;
import org.nwolfhub.pins.Pin;
import org.nwolfhub.pins.PinboardUser;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class UpdateHandler {
    private static TelegramBot bot;
    private static Logger logger = Logger.getLogger(UpdateHandler.class.getName());
    public static HashMap<Long, PinboardUser> users;
    public static HashMap<Long, PrePin> prePins;

    public static void initialize(TelegramBot bot) {
        if(!bot.execute(new GetMe()).isOk()) {
            logger.warning("Bot token is wrong or telegram servers are unavailable");
            System.exit(1);
        }
        try(ObjectInputStream stream = new ObjectInputStream(new FileInputStream("pinboardUsers.sopi"))) {
            users = (HashMap<Long, PinboardUser>) stream.readObject();
            logger.info("Loaded users");
        } catch (IOException | ClassNotFoundException e) {
            logger.warning("Could not load users. Bitchass errors!");
            users = new HashMap<>();
        }
        UpdateHandler.bot = bot;
        prePins = new HashMap<>(); // TODO: 14.12.2021 store em in file aswell
        startThreads();
    }

    public static void exit() {
        backupUsers();
    }

    private static InlineKeyboardMarkup buildKb(PrePin pin) {
        boolean preText = pin.getText()!=null;
        boolean preWhen = pin.getUnixWhen()!=null;
        return new InlineKeyboardMarkup(new InlineKeyboardButton("Текст " + (preText?"✅" + "(" + pin.getText() + ")":"❌")).callbackData("pinText"), new InlineKeyboardButton("Дата " + (preWhen?"✅":"❌")).callbackData("pinData"), new InlineKeyboardButton("Создать пин").callbackData("createPin"));
    }

    private static Long date2Unix(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        //DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try {
            //Date tD = Date.from(LocalDate.parse(date, format));
            Date tD = formatter.parse(date);
            return tD.getTime() / 1000L;
        } catch (DateTimeParseException | ParseException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    private static void startThreads() {
        new Thread(UpdateHandler::checkPins).start();
        new Thread(UpdateHandler::usersBackuper).start();
    }

    private static void usersBackuper() {
        Long nextTimestamp = System.currentTimeMillis() + 300000L;
        while (true) {
            if(nextTimestamp<System.currentTimeMillis()) {
                backupUsers();
                nextTimestamp = System.currentTimeMillis() + 300000L;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void backupUsers() {
        if(!new File("pinboardUsers.sopi").exists()) {
            try {
                new File("pinboardUsers.sopi").createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try(ObjectOutputStream str = new ObjectOutputStream(new FileOutputStream("pinboardUsers.sopi"))) {
            str.writeObject(users);
            str.flush();
        } catch (IOException e) {
            e.printStackTrace();
            logger.warning("Failed to upload users!");
        }
    }

    private static void checkPins() {
        while (true) {
            for(PinboardUser user:users.values()) {
                List<Pin> rPins = user.getReadyPins();
                if(rPins.size()>0) {
                    for(Pin pin:rPins) {
                        bot.execute(new SendMessage(user.id, "Пришёл твой пин!\n\n\n" + pin.text));
                    }
                    user.removeExpiredPins();
                }
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void handleUpdate(Update update) {
        if(update.message()!=null) {
            Long from_id = update.message().from().id();
            if(update.message().text()!=null) {
                String text = update.message().text();
                String command = text.toLowerCase(Locale.ROOT);
                switch (command) {
                    case "/start":
                        if(users.get(from_id)==null) {users.put(from_id, new PinboardUser(from_id));bot.execute(new SendMessage(from_id, "Регистрация в боте завершена"));}
                        bot.execute(new SendMessage(from_id, "Главное меню\nДанный бот был создан специально для @Holkoil").replyMarkup(new InlineKeyboardMarkup(new InlineKeyboardButton("Новый пин").callbackData("newPin"), new InlineKeyboardButton("Мои пины").callbackData("myPins"))));
                        break;
                    default:
                        PinboardUser u;
                        if((u = users.get(from_id))!=null) {
                            switch (u.state) {
                                case "pinText":
                                    PrePin pp = prePins.get(from_id);
                                    if(pp!=null) {
                                        pp.setText(text);
                                        prePins.replace(from_id, pp);
                                        u.setState("none");
                                        users.replace(from_id, u);
                                        bot.execute(new SendMessage(from_id, "Задано").replyMarkup(buildKb(pp)));
                                    }
                                    break;
                                case "pinData":
                                    pp = prePins.get(from_id);
                                    if(pp!=null) {
                                        Long unix = date2Unix(text);
                                        if(unix.equals(0L)) {
                                            bot.execute(new SendMessage(from_id, "Неправильно написан формат даты"));
                                        }
                                        else {
                                            pp.setUnixWhen(unix);
                                            u.setState("none");
                                            users.replace(from_id, u);
                                            prePins.replace(from_id, pp);
                                            bot.execute(new SendMessage(from_id, "Задано").replyMarkup(buildKb(pp)));
                                        }
                                    }
                                    break;
                            }
                        }
                        break;
                }
            }
        } else if(update.callbackQuery()!=null) {
            Long from_id = update.callbackQuery().from().id();
            Integer message_id = update.callbackQuery().message().messageId();
            switch (update.callbackQuery().data()) {
                case "myPins":
                    PinboardUser u = users.get(from_id);
                    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                    if(u!=null) {
                        List<Pin> uPins = u.getPins();
                        for(int now = 0; now<uPins.size(); now++) {
                            Pin p = uPins.get(now);
                            markup.addRow(new InlineKeyboardButton(p.getText()).callbackData("viewPin" + now));
                        }
                        markup.addRow(new InlineKeyboardButton("Назад").callbackData("menu"));
                        bot.execute(new EditMessageText(from_id, message_id, "Твои пины: ").replyMarkup(markup));
                    }
                    break;
                case "menu":
                    if(users.get(from_id)==null) {users.put(from_id, new PinboardUser(from_id));bot.execute(new SendMessage(from_id, "Регистрация в боте завершена"));}
                    bot.execute(new EditMessageText(from_id, message_id, "Главное меню\nДанный бот был создан специально для @Holkoil").replyMarkup(new InlineKeyboardMarkup(new InlineKeyboardButton("Новый пин").callbackData("newPin"), new InlineKeyboardButton("Мои пины").callbackData("myPins"))));
                    break;
                case "newPin":
                    u = users.get(from_id);
                    PrePin pp = new PrePin(from_id);
                    if(prePins.get(from_id)!=null) {
                        pp = prePins.get(from_id);
                    }
                    if(u.getPins().size()<50) {
                        prePins.put(from_id, pp);
                        bot.execute(new EditMessageText(from_id, message_id, "Окей, новый пин. Заполни табличку").replyMarkup(buildKb(pp)));
                    } else {
                        bot.execute(new EditMessageText(from_id, message_id, "Ты упёрся в лимит пинов! Почисти их там, что-ли"));
                    }
                    break;
                case "pinText":
                    pp = prePins.get(from_id);
                    if(pp!=null) {
                        u = users.get(from_id);
                        u.setState("pinText");
                        bot.execute(new EditMessageText(from_id, message_id, "Введи текст пина!"));
                    }
                    break;
                case "pinData":
                    pp = prePins.get(from_id);
                    if(pp!=null) {
                        u = users.get(from_id);
                        u.setState("pinData");
                        bot.execute(new EditMessageText(from_id, message_id, "Введи дату пина! \nФормат:\n24.12.2021 23:59"));
                    }
                    break;
                case "createPin":
                    pp = prePins.get(from_id);
                    if(pp!=null) {
                        u = users.get(from_id);
                        Pin p = pp.convertToPin();
                        if(p!=null) {
                            u.addPin(p);
                            prePins.remove(from_id);
                            bot.execute(new EditMessageText(from_id, message_id, "Создал пин. Дата: " + p.unixWhen + "\nСейчас: " + System.currentTimeMillis()/1000L).replyMarkup(new InlineKeyboardMarkup(new InlineKeyboardButton("Новый пин").callbackData("newPin"), new InlineKeyboardButton("Мои пины").callbackData("myPins"))));
                        } else {
                            bot.execute(new EditMessageText(from_id, message_id, "Твой пин ещё не готов!"));
                        }
                    }
                    break;
                default:
                    if(update.callbackQuery().data().contains("viewPin")) {
                        Pin p = users.get(from_id).getPin(Integer.valueOf(update.callbackQuery().data().split("viewPin")[1]));
                        bot.execute(new EditMessageText(from_id, message_id, "Текст: " + p.getText() + "\nДата: " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(p.unixWhen*1000))).replyMarkup(new InlineKeyboardMarkup(new InlineKeyboardButton("Назад").callbackData("myPins"), new InlineKeyboardButton("Удалить\uD83D\uDDD1️").callbackData("deletePin" + update.callbackQuery().data().split("viewPin")[1]))));
                    }
                    else if(update.callbackQuery().data().contains("deletePin")) {
                        Pin p = users.get(from_id).getPin(Integer.valueOf(update.callbackQuery().data().split("deletePin")[1]));
                        if(p!=null) {
                            users.get(from_id).removePin(p);
                            bot.execute(new EditMessageText(from_id, message_id, "Пин удалён").replyMarkup(new InlineKeyboardMarkup(new InlineKeyboardButton("Новый пин").callbackData("newPin"), new InlineKeyboardButton("Мои пины").callbackData("myPins"))));
                            u = users.get(from_id);
                            markup = new InlineKeyboardMarkup();
                            if(u!=null) {
                                List<Pin> uPins = u.getPins();
                                for(int now = 0; now<uPins.size(); now++) {
                                    p = uPins.get(now);
                                    markup.addRow(new InlineKeyboardButton(p.getText()).callbackData("viewPin" + now));
                                }
                                bot.execute(new EditMessageText(from_id, message_id, "Твои пины: ").replyMarkup(markup));
                            }
                        } else {
                            bot.execute(new EditMessageText(from_id, message_id, "Пин не найден!"));
                        }
                    }
                    break;
            }
            bot.execute(new AnswerCallbackQuery(update.callbackQuery().id()));
        }
    }
    private static class PrePin {
        public final Long from;

        public String getText() {
            return text;
        }

        public PrePin setText(String text) {
            this.text = text;
            return this;
        }

        public Long getUnixWhen() {
            return unixWhen;
        }

        public PrePin setUnixWhen(Long unixWhen) {
            this.unixWhen = unixWhen;
            return this;
        }

        public String text;
        public Long unixWhen;

        public PrePin(Long from) {
            this.from = from;
            this.text = null;
            this.unixWhen = null;
        }

        public Pin convertToPin() {
            if(getText()!=null && getUnixWhen() != null) {
                return new Pin(from, unixWhen, text);
            } else return null;
        }

    }
}
