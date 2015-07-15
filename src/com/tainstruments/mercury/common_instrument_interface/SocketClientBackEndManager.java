package com.tainstruments.mercury.common_instrument_interface;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.*;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


class SocketClientBackEndManager implements Runnable {
    
    /*
     *  Public API -------------------------------------------------------
     */
    public boolean connect() {

        disconnectRequested = false;

        try {
            socket = new Socket(serverAddress, serverConnectionPort);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
        } catch (IOException ex) {
            ciiClient.asyncErrorManager.sendAsyncError(
                    "Connect failed with IOException " + ex);
            return false;
        }

        readerThread = new Thread(this, "Reader-Thread");
        readerThread.setDaemon(true);
        readerThread.setPriority(Thread.MAX_PRIORITY);
        readerThread.start();

        return true;
    }

    public void disconnect() {

        disconnectRequested = true;

        shutdownNetwork();

        //
        //  Wait for the reader to clean up.
        //
        if (readerThread != null) {

            try {
                readerThread.join(500);
            } catch (InterruptedException ex) {
                ciiClient.asyncErrorManager.sendAsyncError(
                        "Disconnect failed with InterruptedException " + ex);
            }
        }

        socket = null;
        outputStream = null;
        inputStream = null;
        readerThread = null;
    }
        
    byte[] getLocalAddress(){

        InetAddress localAddress = socket.getLocalAddress();
        return localAddress.getAddress();
    }
        
    public boolean sendMessage(byte[] buffer) {

        byte[] length = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(buffer.length).array();
        boolean success = false;

        try
        {
            synchronized (sendMessageLock)
            {
                outputStream.write(sync);
                outputStream.write(length);
                outputStream.write(buffer);
                outputStream.write(end);
            }

            success = true;
        }
        catch (IOException ex) {
            ciiClient.asyncErrorManager.sendAsyncError(
                    "SendMessage failed with IOException " + ex);
            shutdownNetwork();
        }

        return success;
    }

    public SocketClientBackEndManager(String serverIpString, CiiClient client) {

        try {
            //
            //  Don't build the object if we don't have a valid Address.
            //  This code is strictly to allow the framework to check the
            //  parsing of the string.
            //
            serverAddress = InetAddress.getByName(serverIpString);
            if (serverAddress == null){
                throw new RuntimeException("Invalid IP Address");
            }
        } catch (UnknownHostException ex) {
            throw new RuntimeException("Invalid IP Address " + ex);
        }

        readBuffer = new byte[maxReadBuffer];
        ciiClient = client;

        sync = new byte[4];
        sync[0] = (byte)'S';
        sync[1] = (byte)'Y';
        sync[2] = (byte)'N';
        sync[3] = (byte)'C';

        end = new byte[4];
        end[0] = (byte)'E';
        end[1] = (byte)'N';
        end[2] = (byte)'D';
        end[3] = (byte)' ';

        sendMessageLock = new Object();
        disconnectRequested = false;
    }


        

    /***********************************************************************
     *                          Private
     **********************************************************************/
    private InetAddress serverAddress;
    private final int serverConnectionPort = 8080;
    private final int maxReadBuffer = 10 * 1024 * 1024;
    private byte[] readBuffer;
    private CiiClient ciiClient;
    private final byte[] sync;
    private final byte[] end;
    private final Object sendMessageLock;
    private volatile boolean disconnectRequested;

    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Thread readerThread;



    private void shutdownNetwork() {
        if (socket != null) {
            try {
                socket.shutdownInput();
                socket.shutdownOutput();
            } catch(IOException ex){
            }
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch(IOException ex){
            }
        }
        if (inputStream != null){
            try {
                inputStream.close();
            } catch(IOException ex){
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch(IOException ex){
            }
        }
    }



    private boolean receiveUntilComplete(int LengthToRead) {
        
        int TotalBytesRead = 0;
        int CurBytesRead;

        do
        {
            try
            {
                CurBytesRead = inputStream.read(readBuffer, TotalBytesRead, LengthToRead);
                
                if (CurBytesRead == -1) {
                    ciiClient.asyncErrorManager.sendAsyncError(
                            "ReaderThread " + readerThread.getId() +
                                    " Read shutting down");
                    return false;
                }
                
                TotalBytesRead += CurBytesRead;
                LengthToRead -= CurBytesRead;
            }
            catch (IOException ex) {
                ciiClient.asyncErrorManager.sendAsyncError(
                        "ReaderThread " + readerThread.getId() +
                                " Read failed with IOException " + ex);
                return false;
            }

        } while (LengthToRead > 0);

        return true;
    }


    private void threadTeardown() {
        if (!disconnectRequested) {
            shutdownNetwork();
            ciiClient.handleUnexpectedDisconnect();
        }
    }

    
    @Override
    public void run(){

        while (true)
        {
            boolean success = receiveUntilComplete(8);
            
            if (!success) {
                //
                //  If this fails, we already sent an AsyncError.
                //
                threadTeardown();
                break;
            }

            if ((readBuffer[0] != (byte)'S') ||
                (readBuffer[1] != (byte)'Y') ||
                (readBuffer[2] != (byte)'N') ||
                (readBuffer[3] != (byte)'C')) {
                ciiClient.asyncErrorManager.sendAsyncError(
                        "ReaderThread " + readerThread.getId()
                                + " - Bad SYNC "
                                + readBuffer[0]
                                + readBuffer[1]
                                + readBuffer[2]
                                + readBuffer[3]);
                threadTeardown();
                break;
            }

            ByteBuffer bb = ByteBuffer.wrap(readBuffer, 4, 4).order(ByteOrder.LITTLE_ENDIAN);
            int length = bb.getInt();
            
            if ((length < 4) || (length > maxReadBuffer))
            {
                ciiClient.asyncErrorManager.sendAsyncError("ReaderThread " + readerThread.getId()
                                + "- Bad Length " + length);
                threadTeardown();
                break;
            }

            success = receiveUntilComplete(length + 4);
            if (!success) {
                //
                //  If this fails, we already sent an AsyncError.
                //
                threadTeardown();
                break;
            }

            if ((readBuffer[length + 0] != (byte)'E') ||
                (readBuffer[length + 1] != (byte)'N') ||
                (readBuffer[length + 2] != (byte)'D') ||
                (readBuffer[length + 3] != (byte)' ')) {

                ciiClient.asyncErrorManager.sendAsyncError(
                        "ReaderThread " + readerThread.getId()
                                + "- Bad END "
                                + readBuffer[length + 0]
                                + readBuffer[length + 1]
                                + readBuffer[length + 2]
                                + readBuffer[length + 3]);

                threadTeardown();
                break;
            }

            //
            //  Bounce to CII interface now.
            //
            ciiClient.routeReceivedMessage(readBuffer, length);
        }

    }

}
