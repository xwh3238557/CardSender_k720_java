package com.toonyoo.xiawenhao.k720.cardsender.execptions;

/**
 * Created by xiawenhao on 2016/11/30.
 */

public class NoResponseReciveException extends CardSenderException {
    public NoResponseReciveException() {
    }

    public NoResponseReciveException(String message) {
        super(message);
    }

    public NoResponseReciveException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoResponseReciveException(Throwable cause) {
        super(cause);
    }
}
