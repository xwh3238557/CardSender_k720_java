package com.toonyoo.xiawenhao.k720.cardsender.execptions;

/**
 * Created by xiawenhao on 2016/11/29.
 */

public class WrongStateException extends CardSenderException {
    public WrongStateException() {
    }

    public WrongStateException(String message) {
        super(message);
    }

    public WrongStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongStateException(Throwable cause) {
        super(cause);
    }
}
