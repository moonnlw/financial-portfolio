package handlers;

import org.example.Bot;
import currency.Currency;
import currency.CurrencyConversion;
import currency.CurrencyMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public class CurrencyHandler {

    public Bot bot;

    public CurrencyHandler(Bot bot) {
        this.bot = bot;
    }

    private final CurrencyMode currencyMode = CurrencyMode.getInstance();
    private final CurrencyConversion currencyConversion =
            CurrencyConversion.getInstance();

    public void handleCallback(Update update) throws TelegramApiException {
        Message message = update.getCallbackQuery().getMessage();
        String[] param = update.getCallbackQuery().getData().split(":");
        String action = param[0];
        Long chatID = message.getChatId();
        Currency newCurrency = Currency.valueOf(param[1]);
        switch (action) {
            case "ORIGINAL":
                currencyMode.setOriginalCurrency(message.getChatId(), newCurrency);
                break;
            case "TARGET":
                currencyMode.setTargetCurrency(message.getChatId(), newCurrency);
                break;
        }
        currencyEdit(chatID, message.getMessageId());
    }

    public List<List<InlineKeyboardButton>> createCurrencyButtons (Long chatID) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        for (Currency currency : Currency.values()) {
            buttons.add(Arrays.asList(
                    InlineKeyboardButton.builder().
                            text(getCurrencyButton(currencyMode.getOriginalCurrency(chatID), currency))
                            .callbackData("ORIGINAL:" + currency).build(),
                    InlineKeyboardButton.builder()
                            .text(getCurrencyButton(currencyMode.getTargetCurrency(chatID), currency))
                            .callbackData("TARGET:" + currency).build()));
        }
        return buttons;
    }

    private String getCurrencyButton (Currency saved, Currency current){
        return saved == current ? current + " ✅" : current.name();
    }

    public void currencyResponse (Long chatID) throws TelegramApiException {
        bot.execute(SendMessage.builder().text("Выберете оригинальную и целевую валюту")
                .chatId(chatID.toString())
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(createCurrencyButtons(chatID)).build())
                .build());
    }

    private void currencyEdit (Long chatID, Integer messageID) throws TelegramApiException {
        bot.execute(
                EditMessageReplyMarkup.builder()
                        .chatId(chatID.toString())
                        .messageId(messageID)
                        .replyMarkup(InlineKeyboardMarkup.builder().keyboard(createCurrencyButtons(chatID)).build())
                        .build());
    }

    public void convertion (Long chatID, String message) throws TelegramApiException {
        Optional<Double> value = parseDouble(message);
        Currency originalCurrency = currencyMode.getOriginalCurrency(chatID);
        Currency targetCurrency = currencyMode.getTargetCurrency(chatID);
        double ratio = currencyConversion.getConversionRatio(originalCurrency, targetCurrency);
        if (value.isPresent()) {
            sendMessage(chatID,
                    String.format(
                            "%4.2f %s = %4.2f %s",
                            value.get(), originalCurrency, (value.get() * ratio), targetCurrency));
        }
    }

    private Optional<Double> parseDouble(String messageText) {
        try {
            return Optional.of(Double.parseDouble(messageText));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void sendMessage(Long chatID, String text) throws TelegramApiException {
        bot.execute(SendMessage.builder()
                .chatId(chatID.toString())
                .text(text)
                .build());
    }
}


