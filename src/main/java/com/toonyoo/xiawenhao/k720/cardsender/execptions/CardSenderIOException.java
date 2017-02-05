package com.toonyoo.xiawenhao.k720.cardsender.execptions;

/**
 * Created by xiawenhao on 2016/11/29.
 */

public class CardSenderIOException extends CardSenderException {

    public CardSenderIOException() {
    }

    public CardSenderIOException(String message) {
        super(message);
    }

    public CardSenderIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public CardSenderIOException(Throwable cause) {
        super(cause);
    }
}
