package com.toonyoo.xiawenhao.k720;

import com.example.hellojni.HelloJni;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by xiawenhao on 16/7/11.
 */
public abstract class BaseSerialPortDevice {
    private HelloJni mSerialPort;

    private ReentrantLock mSerialPortOperationLock;

    private ByteArrayOutputStream mBufferOutputSteam = new ByteArrayOutputStream();

    /**
     * buffer size for input steam to write to default size is 4
     */
    private int mReadBufferSize = 4;

    /**
     * timeout for read operation, default is 0.5s
     */
    private int mTimeWaitForReading = 1000;

    public int getReadBufferSize() {
        return mReadBufferSize;
    }

    public void setReadBufferSize(int readBufferSize) {
        if (readBufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size for input steam you set(" + readBufferSize + ") must greater than 0");
        }
        this.mReadBufferSize = readBufferSize;
    }

    public int getTimeWaitForReading() {
        return mTimeWaitForReading;
    }

    public void setTimeWaitForReading(int timeWaitForReading) {
        if (timeWaitForReading < 0) {
            throw new IllegalArgumentException("Time wait for reading you put(" + timeWaitForReading + ") can not less than 0.");
        }

        this.mTimeWaitForReading = timeWaitForReading;
    }


    public BaseSerialPortDevice(String portPath, int baudrate) throws IOException {
        this(new File(portPath), baudrate);
    }

    public BaseSerialPortDevice(File portPathFile, int baudrate) throws IOException {
        mSerialPort = new HelloJni(portPathFile, baudrate, 0);

        mSerialPortOperationLock = new ReentrantLock();
    }

    /**
     * this function will get the cmds from device if it has something to read otherwise it will return null;
     *
     * @return cmds from hard device
     */
    public byte[] readCMDs() throws IOException {
        InputStream ips = mSerialPort.getInputStream();

        return readSteam(ips);
    }

    /**
     * this function will send cmds to hard device
     *
     * @param cmds cmds to send
     * @return if this opreation success will return true otherwise false
     */
    public void writeCMDs(byte[] cmds) throws IOException {
        OutputStream ops = mSerialPort.getOutputStream();

        ops.write(cmds);
        ops.flush();
    }


    /**
     * read all bytes in a input steam
     *
     * @param inputStream steam to read
     * @return the data in this steam
     * @throws IOException
     */
    private byte[] readSteam(InputStream inputStream) throws IOException {
        byte[] buf = new byte[getReadBufferSize()];
        int readSize;

        mBufferOutputSteam.reset();

        while ((readSize = inputStream.read(buf)) == getReadBufferSize()) {
            mBufferOutputSteam.write(buf);
        }

        buf = Arrays.copyOf(buf, readSize);
        mBufferOutputSteam.write(buf);

        return mBufferOutputSteam.toByteArray();
    }


    /**
     * execute a command and read response for this
     *
     * @param command command to execute
     * @return response for this command
     */
    public byte[] execute(byte[] command) throws IOException {
        if (!mSerialPortOperationLock.tryLock()) {
            return null;
        } else {
            writeCMDs(command);
            try {
                Thread.sleep(getTimeWaitForReading());
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }
            byte[] response = readCMDs();
            mSerialPortOperationLock.unlock();
            return response;
        }

    }

}
