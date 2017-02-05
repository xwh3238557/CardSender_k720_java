package com.toonyoo.xiawenhao.k720.cardsender.execptions;

/**
 * Created by xiawenhao on 2016/11/29.
 */

public class DeviceTimeOutException extends CardSenderException {
    public DeviceTimeOutException() {
    }

    public DeviceTimeOutException(String message) {
        super(message);
    }

    public DeviceTimeOutException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeviceTimeOutException(Throwable cause) {
        super(cause);
    }
}
