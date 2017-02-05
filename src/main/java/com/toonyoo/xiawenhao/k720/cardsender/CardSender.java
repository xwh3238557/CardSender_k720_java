package com.toonyoo.xiawenhao.k720.cardsender;



import android.support.annotation.Nullable;

import com.toonyoo.xiawenhao.k720.BaseSerialPortDevice;
import com.toonyoo.xiawenhao.k720.cardsender.execptions.CardBlockException;
import com.toonyoo.xiawenhao.k720.cardsender.execptions.CardEmptyException;
import com.toonyoo.xiawenhao.k720.cardsender.execptions.CardSenderException;
import com.toonyoo.xiawenhao.k720.cardsender.execptions.CardSenderIOException;
import com.toonyoo.xiawenhao.k720.cardsender.execptions.NoResponseReciveException;
import com.toonyoo.xiawenhao.k720.cardsender.execptions.WrongStateException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



/**
 * Created by admin on 16/6/7.
 */
public class CardSender extends BaseSerialPortDevice {
    private final String TAG = this.getClass().getSimpleName();

    private final ExecutorService singleThreadPool = Executors.newSingleThreadExecutor();



    /**
     * 发卡器回调
     */
    public interface Callback {
        void onSuccess(byte[] response);

        void onError(CardSenderException e);
    }

    public CardSender(String portPath, int baudrate) throws IOException {
        super(portPath, baudrate);
    }

    public CardSender(File portPathFile, int baudrate) throws IOException {
        super(portPathFile, baudrate);
    }

    private void execCMDs(final byte[] cmd, final Callback callback) {
        this.singleThreadPool.execute(new Runnable() {
            public void run() {
                byte[] result;
                try {
                    if ((result = execute(cmd)) != null) {
                        callback.onSuccess(result);
                    } else {
                        callback.onError(new NoResponseReciveException("没有收到设备的回复"));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    callback.onError(new CardSenderIOException(e.getMessage()));
                }
            }
        });
    }

    public void sendCard(@Nullable Callback callback) {
        byte[] cmd = new byte[]{(byte) 2, (byte) 68, (byte) 67, (byte) 3, (byte) 6, (byte) 5};
        this.execCMDs(cmd, callback);
    }

    public void recycleCard(@Nullable Callback callback) {
        byte[] cmd = new byte[]{(byte) 2, (byte) 67, (byte) 80, (byte) 3, (byte) 18, (byte) 5};
        this.execCMDs(cmd, callback);
    }

    public void reset(@Nullable Callback callback) {
        byte[] cmd = new byte[]{(byte) 2, (byte) 83, (byte) 84, (byte) 3, (byte) 6, (byte) 5};
        this.execCMDs(cmd, callback);
    }

    public void sendToPosition1(@Nullable Callback callback) {
        byte[] cmd = new byte[]{(byte) 2, (byte) 68, (byte) 72, (byte) 3, (byte) 13, (byte) 5};
        this.execCMDs(cmd, callback);
    }

    public void sendToPosition2(@Nullable Callback callback) {
        byte[] cmd = new byte[]{(byte) 2, (byte) 69, (byte) 83, (byte) 3, (byte) 23, (byte) 5};
        this.execCMDs(cmd, callback);
    }

    public void sendCardOut(@Nullable Callback callback) {
        byte[] cmd = new byte[]{(byte) 2, (byte) 70, (byte) 85, (byte) 3, (byte) 18, (byte) 5};
        this.execCMDs(cmd, callback);
    }

    public void searchStateCode(@Nullable Callback callback) {
        byte[] cmd = new byte[]{(byte) 2, (byte) 82, (byte) 70, (byte) 3, (byte) 21, (byte) 5};
        this.execCMDs(cmd, callback);
    }

    public void searchMark(@Nullable Callback callback) {
        byte[] cmd = new byte[]{(byte) 2, (byte) 80, (byte) 66, (byte) 3, (byte) 19, (byte) 5};
        this.execCMDs(cmd, callback);
    }

    public void cleanMark(@Nullable Callback callback) {
        byte[] cmd = new byte[]{(byte) 2, (byte) 80, (byte) 67, (byte) 3, (byte) 18, (byte) 5};
        this.execCMDs(cmd, callback);
    }

    public void getVersion(@Nullable Callback callback) {
        byte[] cmd = new byte[]{(byte) 2, (byte) 71, (byte) 86, (byte) 3, (byte) 16, (byte) 5};
        this.execCMDs(cmd, callback);
    }

    public void sendAck(@Nullable Callback callback) {
        byte[] cmd = new byte[]{(byte) 5};
        this.execCMDs(cmd, callback);
    }

    private List<byte[]> getValidCMD(byte[] input) {
        ArrayList cmds = new ArrayList();

        for (int n = 0; n < input.length - 7; ++n) {
            byte[] cmdLine = new byte[]{input[n], input[n + 1], input[n + 2], input[n + 3], input[n + 4], input[n + 5], input[n + 6], input[n + 7]};
            if (this.checkIfIsValid(cmdLine)) {
                byte[] resultCMD = new byte[]{cmdLine[3], cmdLine[4], cmdLine[5]};
                cmds.add(resultCMD);
            }
        }

        return cmds;
    }

    private boolean checkIfIsValid(byte[] input) {
        if (input[0] != 2) {
            return false;
        } else if (input[1] != 83) {
            return false;
        } else if (input[2] != 70) {
            return false;
        } else if (input[6] != 3) {
            return false;
        } else {
            byte[] inputWithoutLast = new byte[]{input[0], input[1], input[2], input[3], input[4], input[5], input[6]};
            byte BCC = 0;
            byte[] var4 = inputWithoutLast;
            int var5 = inputWithoutLast.length;

            for (int var6 = 0; var6 < var5; ++var6) {
                byte cmd = var4[var6];
                BCC ^= cmd;
            }

            return BCC == input[7];
        }
    }

    public static enum State {
        SendCardError(new byte[]{(byte) 50, (byte) 48, (byte) 48}, "发卡错误", -1),
        CardStocked(new byte[]{(byte) 48, (byte) 50, (byte) 48}, "发卡堵塞", -2),
        CardsCountIsFull(new byte[]{(byte) 49, (byte) 48, (byte) 48}, "发卡器已满", -3),
        SendingCard(new byte[]{(byte) 56, (byte) 48, (byte) 48}, "正在发卡", 1),
        RecyclingCard(new byte[]{(byte) 52, (byte) 48, (byte) 48}, "正在回收", 2),
        CardIsOut(new byte[]{(byte) 48, (byte) 48, (byte) 52}, "卡片已发出", 3),
        CardIsInWriteArea(new byte[]{(byte) 48, (byte) 48, (byte) 50}, "卡片在读卡区", 4),
        CardsCountIsZero(new byte[]{(byte) 48, (byte) 48, (byte) 56}, "发卡器已空", -4),
        CardsCountIsLow(new byte[]{(byte) 48, (byte) 49, (byte) 48}, "发卡器卡片数量低", -5),
        CardSenderIsReady(new byte[]{(byte) 48, (byte) 48, (byte) 48}, "发卡器准备好发卡", 0),
        getCardSenderIsNotReady(new byte[]{(byte) 48, (byte) 48, (byte) 49}, "发卡器未准备好发卡", -6);

        private byte[] cmd;
        private String description;
        private int code;

        private State(byte[] cmd, String description, int code) {
            this.cmd = cmd;
            this.description = description;
            this.code = code;
        }

        @Nullable
        public static CardSender.State getState(byte[] cmd) {
            CardSender.State[] var1 = values();
            int var2 = var1.length;

            for (int var3 = 0; var3 < var2; ++var3) {
                CardSender.State state = var1[var3];
                if (Arrays.equals(cmd, state.cmd)) {
                    return state;
                }
            }

            return null;
        }

        public String getDescription() {
            return description;
        }

        public byte[] getCMD() {
            return cmd;
        }

        public int getCode() {
            return code;
        }
    }

    public void isReadyForSendCard(final Callback callback) {
        searchStateCode(new Callback() {
            @Override
            public void onSuccess(byte[] bytes) {
                if (bytes.length < 8) {
                    if (bytes.length == 1 && bytes[0] == 0x06) {
                        sendAck(new Callback() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                byte[] statusCode = new byte[]{bytes[4], bytes[5], bytes[6]};

                                CardSender.State state = CardSender.State.getState(statusCode);

                                if (state == null) {
                                    callback.onError(new CardSenderException("未查询到状态"));
                                    return;
                                }

                                switch (state) {
                                    case CardsCountIsFull:
                                    case CardSenderIsReady:
                                    case CardsCountIsLow:
                                        callback.onSuccess(bytes);
                                        break;
                                    case CardsCountIsZero:
                                        callback.onError(new CardEmptyException("发卡器已空"));
                                        break;
                                    case CardIsInWriteArea:
                                        callback.onError(new CardBlockException("卡片在读卡区"));
                                        break;
                                    case CardIsOut:
                                        callback.onError(new WrongStateException("卡片已发出"));
                                        break;
                                    case SendingCard:
                                        callback.onError(new WrongStateException("正在发卡"));
                                        break;
                                    default:
                                        callback.onError(new CardSenderException("未知异常,statusCode==" + Arrays.toString(statusCode)));
                                        break;
                                }
                            }

                            @Override
                            public void onError(CardSenderException e) {
                                callback.onError(e);
                            }
                        });
                    } else {
                        callback.onError(new CardSenderException("发卡器异常"));
                        return;
                    }

                }
                byte[] statusCode = new byte[]{bytes[4], bytes[5], bytes[6]};

                CardSender.State state = CardSender.State.getState(statusCode);

                if (state == null) {
                    callback.onError(new CardSenderException("未查询到状态"));
                    return;
                }

                switch (state) {
                    case CardsCountIsFull:
                    case CardSenderIsReady:
                    case CardsCountIsLow:
                        callback.onSuccess(bytes);
                        break;
                    case CardsCountIsZero:
                        callback.onError(new CardEmptyException("发卡器已空"));
                        break;
                    case CardIsInWriteArea:
                        callback.onError(new WrongStateException("卡片在读卡区"));
                        break;
                    case CardIsOut:
                        callback.onError(new WrongStateException("卡片已发出"));
                        break;
                    default:
                        callback.onError(new CardSenderException("未知异常,statusCode==" + Arrays.toString(statusCode)));
                        break;
                }
            }

            @Override
            public void onError(CardSenderException e) {
                callback.onError(e);
            }
        });
    }
}
