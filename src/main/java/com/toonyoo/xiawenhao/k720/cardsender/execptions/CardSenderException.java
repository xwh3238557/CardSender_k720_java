package com.toonyoo.xiawenhao.k720.cardsender.execptions;

/**
 * Created by xiawenhao on 2016/11/29.
 */

public class CardSenderException extends Exception {
    public CardSenderException() {
    }

    public CardSenderException(String message) {
        super(message);
    }

    public CardSenderException(String message, Throwable cause) {
        super(message, cause);
    }

    public CardSenderException(Throwable cause) {
        super(cause);
    }
}
