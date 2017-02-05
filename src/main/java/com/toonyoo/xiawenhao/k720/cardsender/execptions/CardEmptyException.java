package com.toonyoo.xiawenhao.k720.cardsender.execptions;

/**
 * Created by xiawenhao on 2016/11/29.
 */

public class CardEmptyException extends CardSenderException {
    public CardEmptyException() {
    }

    public CardEmptyException(String message) {
        super(message);
    }

    public CardEmptyException(String message, Throwable cause) {
        super(message, cause);
    }

    public CardEmptyException(Throwable cause) {
        super(cause);
    }
}
