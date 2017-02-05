package com.toonyoo.xiawenhao.k720.cardsender.execptions;

/**
 * Created by xiawenhao on 2016/11/29.
 */

public class CardBlockException extends CardSenderException {
    public CardBlockException() {
    }

    public CardBlockException(String message) {
        super(message);
    }

    public CardBlockException(String message, Throwable cause) {
        super(message, cause);
    }

    public CardBlockException(Throwable cause) {
        super(cause);
    }
}
