package com.example.hellojni;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by xiawenhao on 2016/11/30.
 */

public class HelloJni {
    private static final String TAG = "SerialPort";
    static int Bad_CommOpen = -101;
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;
    static File f = new File("/dev/");

    static {
        System.loadLibrary("hello-jni");
    }

    public HelloJni(File device, int baudrate, int flags) throws SecurityException, IOException {
        if(!device.canRead() || !device.canWrite()) {
            try {
                Process e = Runtime.getRuntime().exec("/system/bin/su");
                String cmd = "chmod 666 " + device.getAbsolutePath() + "\n" + "exit\n";
                e.getOutputStream().write(cmd.getBytes());
                if(e.waitFor() != 0 || !device.canRead() || !device.canWrite()) {
                    throw new SecurityException();
                }
            } catch (Exception var6) {
                var6.printStackTrace();
                throw new SecurityException();
            }
        }

        this.mFd = OpenCom(device.getAbsolutePath(), baudrate, flags);
        if(this.mFd == null) {
            System.out.println("SerialPort native open returns null");
            throw new IOException();
        } else {
            this.mFileInputStream = new FileInputStream(this.mFd);
            this.mFileOutputStream = new FileOutputStream(this.mFd);
        }
    }

    public InputStream getInputStream() {
        return this.mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return this.mFileOutputStream;
    }

    public static String getDeviceName() {
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.startsWith("ttyUSB0");
            }
        };
        File[] myFile = f.listFiles(filter);
        return myFile.length == 0?"/dev/ttyUSB0":"/dev/ttyUSB1";
    }

    private static native FileDescriptor OpenCom(String var0, int var1, int var2);

    public native void CloseCom();

    public static native void barcodePowerControl(boolean var0);

    public static native void barcodeScan(int var0);

    public static native void gpsPowerControl(boolean var0);

    public static native void gprsPowerControl(boolean var0);

    public static native void bluetoothPowerControl(boolean var0);

    public static native void uhfPowerControl(boolean var0);

    public static native void hfPowerControl(boolean var0);

    public static native void flashPowerControl(boolean var0);

    public static native void setRgbLed(int var0);

    public static native int buzzSetting(int var0);
}