package com.toonyoo.xiawenhao.k720.cardreader;

import android.util.Log;

import com.hardware.xiawenhao.rfm.BaseSerialPortClass;
import com.hardware.xiawenhao.rfm.BaseSerialPortException;
import com.hardware.xiawenhao.rfm.CardReader.CardReaderException;
import com.hardware.xiawenhao.rfm.Utils.HexUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by wuchao on 16/5/24.
 *
 *
 */
public class CardReader extends BaseSerialPortClass {
    private static final String TAG="CardReader";
    private ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    private int verifyCount=0;
    private static byte[] DEFAULT_PASS=new byte[6];
    private static byte[] LOGICAL_NUMBER_PASS=new byte[6];
    private int verifyTypeA=96;
    private int verifyTypeB=97;
    private String physicsNumber;
    public CardReader() {
    }
    public CardReader(int cardReaderBaudRate, String cardReaderPortPath) {
        File cardReaderPortFile = new File(cardReaderPortPath);
        baudRate=cardReaderBaudRate;
        bindFile=cardReaderPortFile;
    }
    public void setDefaultPass(byte[] bytes){
        DEFAULT_PASS=bytes;
    }
    public void initCardReader(final CardReader.InitCardReaderListener listener){
        setModelStatus((byte)00,new SetModelStatusListener(){
            @Override
            public void onSuccess() {
                setWorkType((byte) 65, new SetWorkTypeListener() {
                    @Override
                    public void onSuccess() {
                        setModelStatus((byte)01,new SetModelStatusListener(){
                            @Override
                            public void onSuccess() {
                                listener.onSuccess();
                            }

                            @Override
                            public void onFailed(BaseSerialPortException var1) {
                                listener.onFailed(var1);
                            }
                        });
                    }
                    @Override
                    public void onFailed(BaseSerialPortException var1) {
                        listener.onFailed(var1);
                    }
                });
            }
            @Override
            public void onFailed(BaseSerialPortException var1) {
                listener.onFailed(var1);
            }
        });
    }
    private void checkPassword(byte[] password) throws CardReaderException {
        if(password.length != 6) {
            throw new CardReaderException(CardReaderException.PasswordIsNotTheRightLength, "密码长度不符合标准");
        }
    }
    /**
     *设置模块非接触工作方式
     *指令0x3A
     */
    public void setWorkType(byte type,SetWorkTypeListener listener){
        byte[] cmd = new byte[]{(byte)58, type};

        try {
            byte[] e = CardReader.this.execCMD(cmd);
            listener.onSuccess();
        } catch (Exception var7) {
            BaseSerialPortException exception = CardReader.this.dealWithExceptions(var7);
            listener.onFailed(exception);
        }
    }
    /**
     *设置模块天线状态
     *指令0x05
     */
    public void setModelStatus(byte model,SetModelStatusListener listener){
        byte[] cmd = new byte[]{(byte)05, model};

        try {
            byte[] e = CardReader.this.execCMD(cmd);
            listener.onSuccess();
        } catch (Exception var7) {
            BaseSerialPortException exception = CardReader.this.dealWithExceptions(var7);
            listener.onFailed(exception);
        }
    }
    /**
     *寻卡
     *指令0x46
     */
    public void searchCard(final CardReader.SearchCardListener listener) {
        this.cachedThreadPool.execute(new Runnable() {
            public void run() {
                byte[] cmd = new byte[]{(byte)70, (byte)82};

                try {
                    byte[] e = CardReader.this.execCMD(cmd);
                    System.out.println(HexUtil.GetHexString(e));
                    CardReader.CardType[] var8 = CardReader.CardType.values();
                    int var4 = var8.length;

                    for(int var5 = 0; var5 < var4; ++var5) {
                        CardReader.CardType type = var8[var5];
                        if(Arrays.equals(type.rawBytes, e)) {
                            listener.onSuccess(type);
                            return;
                        }
                    }

                    listener.onFailed(new CardReaderException(CardReaderException.WrongCardType, "卡类型有误"));
                } catch (Exception var7) {
                    BaseSerialPortException exception = CardReader.this.dealWithExceptions(var7);
                    listener.onFailed(exception);
                }

            }
        });
    }

    private BaseSerialPortException dealWithExceptions(Exception e) {
        e.printStackTrace();
        if(e instanceof IOException) {
            return new CardReaderException(CardReaderException.IOOpreationFailed, "IO操作错误");
        } else if(e instanceof BaseSerialPortException) {
            BaseSerialPortException exception = (BaseSerialPortException)e;
            return exception;
        } else {
            throw new RuntimeException("不是可预知的Exception类型");
        }
    }
    public void readCardWithPhysicsNumber(final int sec, final int block, final byte[] password, final CardReader.ReadCardWithPhsicsNumberListener callback) {
        final int absoluteBlock=sec*4+block;


        final CardReader.ReadBlockListener readBlockListener = new CardReader.ReadBlockListener() {
            @Override
            public void onSuccess(byte[] var1) {
                callback.onSuccess(physicsNumber,var1);
            }
            @Override
            public void onFailed(BaseSerialPortException e) {
                callback.onFailed(e);
            }
        };
        final CardReader.Listener listener = new CardReader.Listener() {
            public void onSuccess(byte[] res) {
                CardReader.this.readBlock(absoluteBlock, readBlockListener);
            }
            public void onFailed(BaseSerialPortException e) {
                callback.onFailed(e);
            }
        };

        final CardReader.SelectCardListener selectCardListener = new CardReader.SelectCardListener(){

            @Override
            public void onSuccess() {
                CardReader.this.checkPassword(absoluteBlock, password, verifyTypeA, listener);

            }

            @Override
            public void onFailed(BaseSerialPortException var1) {
                callback.onFailed(var1);
            }
        };
        final CardReader.ConflictreventionListener conflictreventionListener = new CardReader.ConflictreventionListener() {
            public void onSuccess(byte[] cardAddress) {
                physicsNumber=getPhysicsNumber(cardAddress);
                CardReader.this.selectCard(cardAddress,selectCardListener);
            }
            public void onFailed(BaseSerialPortException e) {
                callback.onFailed(e);
            }
        };
        CardReader.SearchCardListener searchCardListener = new CardReader.SearchCardListener() {
            public void onSuccess(CardReader.CardType type) {
                CardReader.this.setConflictrevention(conflictreventionListener);
            }

            public void onFailed(BaseSerialPortException e) {
                callback.onFailed(e);
            }
        };
        this.searchCard(searchCardListener);
    }

    public void readCard(final int sec, final int block, final byte[] password, final CardReader.Listener callback, final int verifyCount) {
        final int absoluteBlock=sec*4+block;
        final CardReader.ReadBlockListener readBlockListener = new CardReader.ReadBlockListener() {
            @Override
            public void onSuccess(byte[] var1) {
                callback.onSuccess(var1);
            }
            @Override
            public void onFailed(BaseSerialPortException e) {
                callback.onFailed(e);
            }
        };
        final CardReader.Listener listener = new CardReader.Listener() {
            public void onSuccess(byte[] res) {
                CardReader.this.readBlock(absoluteBlock, readBlockListener);
            }
            public void onFailed(BaseSerialPortException e) {
//                if(verifyCount<=2){
//                    if(verifyCount==1){
//                        CardReader.this.readCard(sec, block, DEFAULT_PASS, callback,2);
//                    }else{
//                        callback.onFailed(e);
//                    }
//                }else{
                    callback.onFailed(e);
//                }
            }
        };

        final CardReader.SelectCardListener selectCardListener = new CardReader.SelectCardListener(){

            @Override
            public void onSuccess() {
                CardReader.this.checkPassword(absoluteBlock, password, verifyTypeA, listener);

            }

            @Override
            public void onFailed(BaseSerialPortException var1) {
                callback.onFailed(var1);
            }
        };
        final CardReader.ConflictreventionListener conflictreventionListener = new CardReader.ConflictreventionListener() {
            public void onSuccess(byte[] cardAddress) {
                CardReader.this.selectCard(cardAddress,selectCardListener);
            }
            public void onFailed(BaseSerialPortException e) {
                callback.onFailed(e);
            }
        };
        CardReader.SearchCardListener searchCardListener = new CardReader.SearchCardListener() {
            public void onSuccess(CardReader.CardType type) {
                CardReader.this.setConflictrevention(conflictreventionListener);
            }

            public void onFailed(BaseSerialPortException e) {
                callback.onFailed(e);
            }
        };
        this.searchCard(searchCardListener);
    }

    public void writeCard(final int sec, final int block, final byte[] password, final byte[] data, final CardReader.WriteCardCallback callback, final int verifyCount) {
        final int absoluteBlock=sec*4+block;
        final CardReader.WriteDataListener writeDataListener = new CardReader.WriteDataListener() {
            public void onSuccess() {
                callback.onSuccess();
            }

            public void onFailed(BaseSerialPortException e) {
                callback.onFailed(e);
            }
        };
        final CardReader.Listener listener = new CardReader.Listener() {
            public void onSuccess(byte[] res) {
                CardReader.this.writeData(absoluteBlock, data, writeDataListener);
            }

            public void onFailed(BaseSerialPortException e) {
//                if(verifyCount<=2){
//                    if(verifyCount==1){
//                        CardReader.this.writeCard(sec, block, DEFAULT_PASS, data, callback,2);
//                    }else{
//                        callback.onFailed(e);
//                    }
//                }else{
                    callback.onFailed(e);
//                }
            }
        };
        final CardReader.SelectCardListener selectCardListener = new CardReader.SelectCardListener(){

            @Override
            public void onSuccess() {
                CardReader.this.checkPassword(absoluteBlock, password, verifyTypeA, listener);
            }

            @Override
            public void onFailed(BaseSerialPortException var1) {
                callback.onFailed(var1);
            }
        };
        final CardReader.ConflictreventionListener conflictreventionListener = new CardReader.ConflictreventionListener() {
            public void onSuccess(byte[] cardAddress) {
                CardReader.this.selectCard(cardAddress,selectCardListener);
            }

            public void onFailed(BaseSerialPortException e) {
                callback.onFailed(e);
            }
        };
        CardReader.SearchCardListener searchCardListener = new CardReader.SearchCardListener() {
            public void onSuccess(CardReader.CardType type) {
                CardReader.this.setConflictrevention(conflictreventionListener);
            }

            public void onFailed(BaseSerialPortException e) {
                callback.onFailed(e);
            }
        };
        this.searchCard(searchCardListener);
    }

    public void readMoney(final int sec, final int block, final byte[] password, final CardReader.Listener callback) {
        final int absoluteBlock=sec*4+block;
        final CardReader.ReadWalletListener readWallListener = new CardReader.ReadWalletListener() {
            @Override
            public void onSuccess(byte[] var1) {
                callback.onSuccess(var1);
            }
            @Override
            public void onFailed(BaseSerialPortException e) {
                callback.onFailed(e);
            }
        };
        final CardReader.Listener listener = new CardReader.Listener() {
            public void onSuccess(byte[] res) {
                CardReader.this.readWallet(absoluteBlock, readWallListener);
            }
            public void onFailed(BaseSerialPortException e) {
                callback.onFailed(e);
            }
        };
        final CardReader.SelectCardListener selectCardListener = new CardReader.SelectCardListener(){

            @Override
            public void onSuccess() {
                CardReader.this.checkPassword(absoluteBlock, password, verifyTypeA, listener);
            }

            @Override
            public void onFailed(BaseSerialPortException var1) {
                callback.onFailed(var1);
            }
        };
        final CardReader.ConflictreventionListener conflictreventionListener = new CardReader.ConflictreventionListener() {
            public void onSuccess(byte[] cardAddress) {
                CardReader.this.selectCard(cardAddress,selectCardListener);
            }
            public void onFailed(BaseSerialPortException e) {
                callback.onFailed(e);
            }
        };
        CardReader.SearchCardListener searchCardListener = new CardReader.SearchCardListener() {
            public void onSuccess(CardReader.CardType type) {
                CardReader.this.setConflictrevention(conflictreventionListener);
            }

            public void onFailed(BaseSerialPortException e) {
                callback.onFailed(e);
            }
        };
        this.searchCard(searchCardListener);
    }

    private void writeData(final int area, final byte[] data, final CardReader.WriteDataListener listener) {
        this.cachedThreadPool.execute(new Runnable() {
            public void run() {
                byte[] _cmd = new byte[]{(byte)76, (byte)area};
                _cmd = Arrays.copyOf(_cmd, data.length + 2);
                System.arraycopy(data, 0, _cmd, 2, data.length);

                try {
                    CardReader.this.execCMD(_cmd);
                    listener.onSuccess();
                } catch (Exception var4) {
                    BaseSerialPortException exception = CardReader.this.dealWithExceptions(var4);
                    listener.onFailed(exception);
                }

            }
        });
    }
    /**
     *选卡
     *指令0x48
     */
    public void selectCard(final byte[] address, final CardReader.SelectCardListener listener) {
        this.cachedThreadPool.execute(new Runnable() {
            public void run() {
                byte[] _cmd = new byte[]{(byte)72};
                byte[] cmd = Arrays.copyOf(_cmd, _cmd.length + address.length);
                System.arraycopy(address, 0, cmd, 1, address.length);

                try {
                    CardReader.this.execCMD(cmd);
                    listener.onSuccess();
                } catch (Exception var5) {
                    BaseSerialPortException exception = CardReader.this.dealWithExceptions(var5);
                    listener.onFailed(exception);
                }

            }
        });
    }

    public byte[] getDataBytes(byte[] bytes) {
        int length = bytes.length;
        ByteBuffer bb = ByteBuffer.allocate(length - 8);

        for(int data = 6; data < length - 2; ++data) {
            bb.put(bytes[data]);
        }

        byte[] var5 = bb.array();
        var5 = this.getBytesWithOutAddition(var5);
        return var5;
    }

    public byte[] getBytesWithOutAddition(byte[] bytes) {
        Byte[] byteArr = HexUtil.GetOriginalByteArray(bytes);
        ArrayList byteList = new ArrayList();
        Byte[] specialBytes = byteArr;
        int specialByteList = byteArr.length;

        int resultArray;
        byte currentByte;
        for(resultArray = 0; resultArray < specialByteList; ++resultArray) {
            currentByte = specialBytes[resultArray].byteValue();
            byteList.add(Byte.valueOf(currentByte));
        }

        specialBytes = new Byte[]{Byte.valueOf((byte)2), Byte.valueOf((byte)3), Byte.valueOf((byte)16)};
        ArrayList var10 = new ArrayList();
        Byte[] var11 = specialBytes;
        int var12 = specialBytes.length;

        for(int nextByte = 0; nextByte < var12; ++nextByte) {
            Byte b = var11[nextByte];
            var10.add(b);
        }
        Log.i(TAG,"byteList.size()==="+byteList.size());
        for(resultArray = 0; resultArray < byteList.size()-1; ++resultArray) {
            currentByte = ((Byte)byteList.get(resultArray)).byteValue();
            if(currentByte == 16) {
                byte var13 = ((Byte)byteList.get(resultArray + 1)).byteValue();
                if(var10.contains(Byte.valueOf(var13))) {
                    byteList.remove(resultArray);
                }
            }
        }

        var11 = new Byte[byteList.size()];
        return HexUtil.GetPackagedByteArray((Byte[])byteList.toArray(var11));
    }

    /**
     *防冲突
     *指令0x47
     */
    public void setConflictrevention(final CardReader.ConflictreventionListener listener) {
        this.cachedThreadPool.execute(new Runnable() {
            public void run() {
                byte[] cmd = new byte[]{(byte)71, (byte)4};

                try {
                    byte[] e = CardReader.this.execCMD(cmd);
                    listener.onSuccess(CardReader.this.getDataBytes(e));
                    Log.i(TAG,"conflictrevention after e==="+HexUtil.GetHexString(CardReader.this.getDataBytes(e)));
                } catch (Exception var4) {
                    BaseSerialPortException exception = CardReader.this.dealWithExceptions(var4);
                    listener.onFailed(exception);
                }

            }
        });
    }
    /**
     *验证密钥
     *指令0x4A
     */
    public void checkPassword(int forBlock, byte[] password, int verifyType,CardReader.Listener listener) {
        try {
            this.checkPassword(password);
        } catch (CardReaderException var10) {
            BaseSerialPortException e = this.dealWithExceptions(var10);
            listener.onFailed(e);
        }

        ArrayList byteList = new ArrayList();
        byteList.add(Byte.valueOf((byte)74));
        byteList.add(Byte.valueOf((byte)verifyType));
        byteList.add(Byte.valueOf((byte)forBlock));
        byte[] var11 = password;
        int exception = password.length;

        for(int var7 = 0; var7 < exception; ++var7) {
            byte b = var11[var7];
            byteList.add(Byte.valueOf(b));
        }

        try {
            var11 = this.execCMD(HexUtil.GetByteArrayWithList(byteList));
            listener.onSuccess(var11);
        } catch (Exception var9) {
            BaseSerialPortException var12 = this.dealWithExceptions(var9);
            listener.onFailed(var12);
        }

    }

    /**
     *读块
     *指令0x4B
     */
    public void readBlock(final int blockNumber, final CardReader.ReadBlockListener listener) {
        this.cachedThreadPool.execute(new Runnable() {
            public void run() {
                byte[] cmd = new byte[]{(byte)75, (byte)blockNumber};

                try {
                    byte[] e = CardReader.this.execCMD(cmd);
                    Log.i(TAG,"readBlock: " + HexUtil.GetHexString(e));
                    e = CardReader.this.getDataBytes(e);
                    Log.i(TAG,"after deal readBlock: " + HexUtil.GetHexString(e));
                    listener.onSuccess(e);
                } catch (Exception var4) {
                    BaseSerialPortException exception = CardReader.this.dealWithExceptions(var4);
                    listener.onFailed(exception);
                }

            }
        });
    }

    /**
     * 读扇区
     * 指令0x4B
     *
     * */
    public void readArea(final int areaNumber, final byte[] password, final CardReader.Listener callback, final int verifyCount) {
        final CardReader.Listener block2Listener = new CardReader.Listener(){

            @Override
            public void onSuccess(byte[] var1) {
                int block3=areaNumber*4+3;
                byte[] cmd = new byte[]{(byte)75, (byte)block3};
                try {
                    byte[] e = execCMD(cmd);
                    callback.onSuccess(e);
                } catch (Exception var7) {
                    BaseSerialPortException exception = dealWithExceptions(var7);
                    callback.onFailed(exception);
                }
            }

            @Override
            public void onFailed(BaseSerialPortException var1) {
                callback.onFailed(var1);
            }
        };
        final CardReader.Listener block1Listener = new CardReader.Listener(){

            @Override
            public void onSuccess(byte[] var1) {
                int block2=areaNumber*4+2;
                byte[] cmd = new byte[]{(byte)75, (byte)block2};
                try {
                    byte[] e = execCMD(cmd);
                    block2Listener.onSuccess(e);
                } catch (Exception var7) {
                    BaseSerialPortException exception = dealWithExceptions(var7);
                    block2Listener.onFailed(exception);
                }
            }

            @Override
            public void onFailed(BaseSerialPortException var1) {
                callback.onFailed(var1);
            }
        };

        final CardReader.Listener block0Listener = new CardReader.Listener(){

            @Override
            public void onSuccess(byte[] var1) {
                int block1=areaNumber*4+1;
                byte[] cmd = new byte[]{(byte)75, (byte)block1};
                try {
                    byte[] e = execCMD(cmd);
                    block1Listener.onSuccess(e);
                } catch (Exception var7) {
                    BaseSerialPortException exception = dealWithExceptions(var7);
                    block1Listener.onFailed(exception);
                }
            }
            @Override
            public void onFailed(BaseSerialPortException var1) {
                callback.onFailed(var1);
            }
        };
        final CardReader.Listener listener = new CardReader.Listener() {
            public void onSuccess(byte[] res) {
                int block0=areaNumber*4;
                byte[] cmd = new byte[]{(byte)75, (byte)block0};
                try {
                    byte[] e = execCMD(cmd);
                    block0Listener.onSuccess(e);
                } catch (Exception var7) {
                    BaseSerialPortException exception = dealWithExceptions(var7);
                    block0Listener.onFailed(exception);
                }
            }
            public void onFailed(BaseSerialPortException e) {

                if(verifyCount<=2){
                    if(verifyCount==1){
                        CardReader.this.readArea(areaNumber, DEFAULT_PASS, callback, 2);
                    }else{
                        callback.onFailed(e);
                    }
                }else{
                    callback.onFailed(e);
                }
            }
        };
        final CardReader.SelectCardListener selectCardListener = new CardReader.SelectCardListener(){

            @Override
            public void onSuccess() {
                int block0=areaNumber*4;
                CardReader.this.checkPassword(block0, password, verifyTypeA, listener);
            }

            @Override
            public void onFailed(BaseSerialPortException var1) {
                callback.onFailed(var1);
            }
        };
        final CardReader.ConflictreventionListener conflictreventionListener = new CardReader.ConflictreventionListener() {
            public void onSuccess(byte[] cardAddress) {
                CardReader.this.selectCard(cardAddress,selectCardListener);
            }
            public void onFailed(BaseSerialPortException e) {
                callback.onFailed(e);
            }
        };
        CardReader.SearchCardListener searchCardListener = new CardReader.SearchCardListener() {
            public void onSuccess(CardReader.CardType type) {
                CardReader.this.setConflictrevention(conflictreventionListener);
            }

            public void onFailed(BaseSerialPortException e) {
                callback.onFailed(e);
            }
        };
        this.searchCard(searchCardListener);

    }
    /**
     *初始化钱包
     *指令0x4D
     */
    public void initWallet(final int blockNumber, final int initNumber, final InitWalletListener listener){
        byte[] cmd = new byte[]{(byte)77,(byte)blockNumber};
        byte[] money = getBytesWirteData(4,initNumber,4);
        cmd = Arrays.copyOf(cmd, cmd.length + money.length);
        System.arraycopy(money, 0, cmd, 2, money.length);
        try {
            byte[] e = CardReader.this.execCMD(cmd);
            listener.onSuccess(e);
        } catch (Exception var4) {
            BaseSerialPortException exception = CardReader.this.dealWithExceptions(var4);
            listener.onFailed(exception);
        }
//        checkPassword(blockNumber,password,new CardReader.Listener(){
//            @Override
//            public void onSuccess(byte[] var1) {
//
//            }
//
//            @Override
//            public void onFailed(BaseSerialPortException var1) {
//                listener.onFailed(var1);
//            }
//        });
    }
    /**
     *读钱包
     *指令0x4E
     */
    public void readWallet(final int blockNumber, final ReadWalletListener listener){
        byte[] cmd = new byte[]{(byte)78,(byte)blockNumber};
        try {
            byte[] e = CardReader.this.execCMD(cmd);
            Log.i(TAG,"readWallet: " + HexUtil.GetHexString(e));
//            e = CardReader.this.getDataBytes(e);
            Log.i(TAG,"after deal readWallet: " + HexUtil.GetHexString(e));
            listener.onSuccess(e);
        } catch (Exception var4) {
            BaseSerialPortException exception = CardReader.this.dealWithExceptions(var4);
            listener.onFailed(exception);
        }
    }
    /**
     *充值
     *指令0x50
     */
    public void rechargeMoney(final int blockNumber, final int addMoney, byte[] password, final ReadWalletListener listener){
        checkPassword(blockNumber,password, verifyTypeA, new CardReader.Listener(){
            @Override
            public void onSuccess(byte[] var1) {
                byte[] cmd = new byte[]{(byte)80,(byte)blockNumber};
                byte[] money = getBytesWirteData(4,addMoney,4);
                cmd = Arrays.copyOf(cmd, cmd.length + money.length);
                System.arraycopy(money, 0, cmd, 2, money.length);
                try {
                    byte[] e = CardReader.this.execCMD(cmd);
                    listener.onSuccess(e);
                } catch (Exception var4) {
                    BaseSerialPortException exception = CardReader.this.dealWithExceptions(var4);
                    listener.onFailed(exception);
                }
            }

            @Override
            public void onFailed(BaseSerialPortException var1) {
                listener.onFailed(var1);
            }
        });
    }

    /**
     *扣款
     *指令0x4F
     */
    public void changeMoney(final int blockNumber, final int lessMoney, byte[] password, final ReadWalletListener listener){
        checkPassword(blockNumber,password,verifyTypeA,new CardReader.Listener(){
            @Override
            public void onSuccess(byte[] var1) {
                byte[] cmd = new byte[]{(byte)79,(byte)blockNumber};
                byte[] money = getBytesWirteData(4,lessMoney,4);
                cmd = Arrays.copyOf(cmd, cmd.length + money.length);
                System.arraycopy(money, 0, cmd, 2, money.length);
                try {
                    byte[] e = CardReader.this.execCMD(cmd);
                    listener.onSuccess(e);
                } catch (Exception var4) {
                    BaseSerialPortException exception = CardReader.this.dealWithExceptions(var4);
                    listener.onFailed(exception);
                }
            }

            @Override
            public void onFailed(BaseSerialPortException var1) {
                listener.onFailed(var1);
            }
        });
    }
    /**
    * 修改卡密码
    *
    * */
    public void changeCardPassword(int secNumber,byte[] oldPassWord,byte[] newPassWord,CardReader.WriteCardCallback listener){
        byte[] oldPassBytes=oldPassWord;
        byte[] newPassBytes=newPassWord;
        byte[] defaultPassB=HexUtil.GetBytesFormHexString("FFFFFFFFFFFF");
        if(oldPassBytes.length==6&&newPassBytes.length==6){
            byte[] accessControlBytes=HexUtil.GetBytesFormHexString("FF078069");
            byte[] data = new byte[16];
            System.arraycopy(newPassBytes, 0, data, 0, newPassBytes.length);
            System.arraycopy(accessControlBytes, 0, data, newPassBytes.length, accessControlBytes.length);
            System.arraycopy(defaultPassB, 0, data, newPassBytes.length+accessControlBytes.length, defaultPassB.length);
            writeCard(secNumber,3,oldPassBytes,data,listener,1);
        }

    }

    public byte[] execCMD(byte[] cmd) throws IOException, BaseSerialPortException {
        byte[] modelAddress = new byte[]{(byte)0, (byte)0};
        byte length = Byte.valueOf(Integer.toString(cmd.length + 2)).byteValue();
        byte startMark = 2;
        byte endMark = 3;
        ArrayList cmdList = new ArrayList();
        cmdList.add(Byte.valueOf(startMark));
        HexUtil.AddArrayToList(cmdList, modelAddress);
        cmdList.add(Byte.valueOf(length));
        HexUtil.AddArrayToList(cmdList, this.getRawCmd(cmd));
        ArrayList bccList = new ArrayList();
        HexUtil.AddArrayToList(bccList, modelAddress);
        bccList.add(Byte.valueOf(length));
        HexUtil.AddArrayToList(bccList, cmd);
        byte[] bccBytes = HexUtil.GetByteArrayWithList(bccList);
        byte bcc = GetBCC(bccBytes);
        HexUtil.AddArrayToList(cmdList, this.getRawCmd(bcc));
        cmdList.add(Byte.valueOf(endMark));
        byte[] finalCMD = HexUtil.GetByteArrayWithList(cmdList);
//        Log.i(TAG,"Write: " + HexUtil.GetHexString(finalCMD));

        byte[] response = super.execCMD(finalCMD);
//        Log.i(TAG,"Read: " + HexUtil.GetHexString(response));

        this.checkResult(cmd[0], response);
        return response;
    }

    public void checkResult(byte commandByte, byte[] receiveBytes) throws BaseSerialPortException {
        receiveBytes = this.getBytesWithOutAddition(receiveBytes);
//        System.out.println("receiveBytes: " + HexUtil.GetHexString(receiveBytes));
        if(receiveBytes[0] != 2) {
            throw new CardReaderException(CardReaderException.WrongReturnFormat, "开始符不匹配");
        } else if(receiveBytes[receiveBytes.length - 1] != 3) {
            throw new CardReaderException(CardReaderException.WrongReturnFormat, "结束符不匹配");
        } else if(receiveBytes[1] == 0 && receiveBytes[2] == 0) {
            if(commandByte != receiveBytes[4]) {
                if(!isSpecialByte(receiveBytes[4])){
                    throw new CardReaderException(CardReaderException.WrongReturnFormat, "命令位不匹配");
                }else{
                    if(commandByte != receiveBytes[5]){
                        throw new CardReaderException(CardReaderException.WrongReturnFormat, "命令位不匹配");
                    }else if(receiveBytes[6] != 0){
                        throw new CardReaderException(CardReaderException.ExecuteFailed, "命令执行失败");
                    }
                }
            }else if(receiveBytes[5] != 0){
                throw new CardReaderException(CardReaderException.ExecuteFailed, "命令执行失败");
            }
        } else {
            throw new CardReaderException(CardReaderException.WrongReturnFormat, "模块地址不匹配");
        }
    }

    public byte[] getReturnDatas(byte[] receiveBytes) {
        int length = receiveBytes.length;
        ByteBuffer resultBuffer = ByteBuffer.allocate(length - 8);

        for(int index = 6; index < length - 2; ++index) {
            resultBuffer.put(receiveBytes[index]);
        }

        return resultBuffer.array();
    }

    public static byte GetBCC(byte[] bytes) {
        int bcc = 0;
        byte[] var2 = bytes;
        int var3 = bytes.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            byte _byte = var2[var4];
            bcc += _byte;
        }

        bcc &= -1;
        return (byte)bcc;
    }

    private byte[] getRawCmd(byte bit) {
        return bit != 2 && bit != 3 && bit != 16?new byte[]{bit}:new byte[]{(byte)16, bit};
    }

    private boolean isSpecialByte(byte bit){
        return bit == 2 || bit == 3 || bit == 16?true:false;
    }

    private byte[] getRawCmd(byte[] bytes) {
        ArrayList res = new ArrayList();
        byte[] var3 = bytes;
        int var4 = bytes.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            byte b = var3[var5];
            byte[] byteArr = this.getRawCmd(b);
            byte[] var8 = byteArr;
            int var9 = byteArr.length;

            for(int var10 = 0; var10 < var9; ++var10) {
                byte rawByte = var8[var10];
                res.add(Byte.valueOf(rawByte));
            }
        }

        return HexUtil.GetByteArrayWithList(res);
    }


    public interface ReadCardWithPhsicsNumberListener {
        void onSuccess(String physicsNumber, byte[] data);

        void onFailed(BaseSerialPortException var1);
    }

    public interface Listener {
        void onSuccess(byte[] var1);

        void onFailed(BaseSerialPortException var1);
    }

    public interface ReadBlockListener {
        void onSuccess(byte[] var1);

        void onFailed(BaseSerialPortException var1);
    }

    public interface ConflictreventionListener {
        void onSuccess(byte[] var1);

        void onFailed(BaseSerialPortException var1);
    }

    public interface SelectCardListener {
        void onSuccess();

        void onFailed(BaseSerialPortException var1);
    }

    public interface WriteDataListener {
        void onSuccess();

        void onFailed(BaseSerialPortException var1);
    }

    public interface WriteCardCallback {
        void onSuccess();

        void onFailed(BaseSerialPortException var1);
    }

    public static enum CardType {
        S50(new byte[]{(byte)2, (byte)0, (byte)0, (byte)5, (byte)70, (byte)0, (byte)4, (byte)0, (byte)79, (byte)3}),
        S70(new byte[]{(byte)2, (byte)0, (byte)0, (byte)5, (byte)70, (byte)0, (byte)2, (byte)0, (byte)79, (byte)3});

        byte[] rawBytes;

        private CardType(byte[] rawBytes) {
            this.rawBytes = rawBytes;
        }
    }

    public interface SearchCardListener {
        void onSuccess(CardReader.CardType var1);

        void onFailed(BaseSerialPortException var1);
    }

    public interface SetModelStatusListener{
        void onSuccess();

        void onFailed(BaseSerialPortException var1);
    }

    public interface SetWorkTypeListener{
        void onSuccess();

        void onFailed(BaseSerialPortException var1);
    }

    public interface InitWalletListener{
        void onSuccess(byte[] var1);

        void onFailed(BaseSerialPortException var1);
    }
    public interface ReadWalletListener{
        void onSuccess(byte[] var1);

        void onFailed(BaseSerialPortException var1);
    }

    public interface InitCardReaderListener {
        void onSuccess();

        void onFailed(BaseSerialPortException var1);
    }

    public byte[] getBytesWirteData(int byteCount,int writeData,int byteLimit){

        byte[] zeroData = new byte[byteCount];
        for(int i=0;i<zeroData.length;i++){
            zeroData[i]=0x00;
        }
        byte[] data = new byte[byteCount];
        byte[] num = parseIntToByteArray(writeData, byteLimit);

        System.arraycopy(zeroData, 0, data, 0, byteCount-byteLimit);
        System.arraycopy(num, 0, data, byteCount-byteLimit, byteLimit);
        return data;
    }

    public byte[] get16byteWirteData(int writeData,int byteLimit){
        byte[] zeroData = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        byte[] data = new byte[16];
        byte[] num = parseIntToByteArray(writeData, byteLimit);

        System.arraycopy(zeroData, 0, data, 0, 16-byteLimit);
        System.arraycopy(num, 0, data, 16-byteLimit, byteLimit);
        return data;
    }

    private byte[] parseIntToByteArray(int i ,int length){
        byte[] bLocalArr = new byte[length];
        for (int index = 0; (index < 4) && (index < length); index++) {
            bLocalArr[index] = (byte) (i >> 8 * index & 0xFF);
        }
        return bLocalArr;
    }

    private String getPhysicsNumber(byte[] data){
        String physicsNumber="";
        for(int i=data.length-1;i>=0;i--){
            physicsNumber=physicsNumber+HexUtil.GetHexString(data[i]).replace("0x","");
        }
        return physicsNumber;
    }
}
