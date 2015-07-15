package com.tainstruments.mercury.common_instrument_interface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;



public class CiiClient {

    /*
     *  Public API -------------------------------------------------------
     */
    public boolean isConnected() {
        return connectionState == ConnectionState_Connected;
    }

    public int getGrantedAccess() {
        return ciiAccessLevel;
    }

    public void registerStatusHandler(int statusMessage, ReceiveStatusHandler handler) {
        synchronized(statusCallbacksLock){
            statusCallbacks.put(statusMessage, handler);
        }
    }

    public void unregisterStatusHandler(int statusMessage) {
        synchronized(statusCallbacksLock){
            statusCallbacks.remove(statusMessage);
        }
    }

    public void registerUnhandledStatusHandler(ReceiveStatusHandler handler) {
        synchronized(statusCallbacksLock){
            unhandledStatusCallback = handler;
        }
    }

    public void unregisterUnhandledStatusHandler() {
        synchronized(statusCallbacksLock){
            unhandledStatusCallback = null;
        }
    }

    public void registerConnectHandler(ConnectHandler handler){
        synchronized(connectCallbackLock){
            connectCallbacks.add(handler);
        }
    }

    public void registerDisconnectHandler(DisconnectHandler handler){
        synchronized(disconnectCallbackLock){
            disconnectCallbacks.add(handler);
        }
    }

    public void registerAsyncErrorHandler(AsyncErrorHandler handler){
        synchronized (asyncErrorsLock){
            asyncErrorCallbacks.add(handler);
        }
    }

    public boolean connect(int requestedAccess) {
        
        if (connectionState != ConnectionState_NotConnected) {
            return false;
        }

        boolean success = backEndManager.connect();

        if (success) {

            connectionState = ConnectionState_WaitingForLogin;
            
            success = login(requestedAccess);

            if (success) {

                connectionState = ConnectionState_Connected;

                synchronized(connectCallbackLock){

                    ListIterator<ConnectHandler>iterator;
                    iterator = connectCallbacks.listIterator();

                    while(iterator.hasNext()){
                        ConnectHandler callback = iterator.next();
                        callback.connected();
                    }
                }

            }
            else
            {
                backEndManager.disconnect();
                connectionState = ConnectionState_NotConnected;
            }
        }

        return success;
    }

    public void disconnect() {

        if (connectionState == ConnectionState_Connected) {

            connectionState = ConnectionState_DisconnectInProgress;

            messagesInFlight.clear();

            backEndManager.disconnect();

            connectionState = ConnectionState_NotConnected;

            synchronized(disconnectCallbackLock){

                ListIterator<DisconnectHandler>iterator;
                iterator = disconnectCallbacks.listIterator();

                while(iterator.hasNext()){
                    DisconnectHandler callback = iterator.next();
                    callback.disconnected();
                }
            }
        }
    }

    public boolean sendAction(   int subcommand, byte[] data, CommandCompletion completion){
        if ((ciiAccessLevel == AccessLevel_Engineering) ||
            (ciiAccessLevel == AccessLevel_Master) ||
            (ciiAccessLevel == AccessLevel_LocalUI)) {
            return sendMessage(MessageTypeAction, subcommand, data, completion);
        }
        else {
            return false;
        }
    }

    public boolean sendGet(  int subcommand, byte[] data, CommandCompletion completion){
        return sendMessage(MessageTypeGet, subcommand, data, completion);
    }

    public void deleteCommandInProgress(int sequenceNumber){
        messagesInFlight.remove(sequenceNumber);
    }

    public CiiClient(String serverAddress) {
        logger = Logger.getInstance();
        ciiAccessLevel = AccessLevel_Invalid;
        connectionState = ConnectionState_NotConnected;

        statusCallbacksLock = new Object();
        statusCallbacks = new HashMap<>();

        asyncErrorsLock = new Object();
        asyncErrorCallbacks = new LinkedList<>();

        connectCallbackLock = new Object();
        connectCallbacks = new LinkedList<>();

        disconnectCallbackLock = new Object();
        disconnectCallbacks = new LinkedList<>();

        messagesInFlight = new MessagesInFlight();

        //
        //  Prebuild the Communications arrays
        //
        MessageTypeGet = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(MtGetCommand).array();
        MessageTypeAction = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(MtActionCommand).array();
        BytesLogin = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(MtLogin).array();

        loginAcceptEvent = new Object();

        backEndManager = new SocketClientBackEndManager(serverAddress, this);


        //
        //  Start this last.
        //
        asyncErrorManager = new AsyncErrorManager();
        asyncErrorManager.start();
    }



    /***********************************************************************
     *                          Private
     **********************************************************************/
    private final Logger logger;
    private int ciiAccessLevel;
    private volatile int connectionState;

    private final HashMap<Integer, ReceiveStatusHandler>statusCallbacks;
    private final Object statusCallbacksLock;
    private ReceiveStatusHandler unhandledStatusCallback;

    private final Object asyncErrorsLock;
    private final LinkedList<AsyncErrorHandler>asyncErrorCallbacks;

    private final Object connectCallbackLock;
    private final LinkedList<ConnectHandler>connectCallbacks;

    private final Object disconnectCallbackLock;
    private final LinkedList<DisconnectHandler>disconnectCallbacks;

    //
    //  We want package access to this.
    //
    AsyncErrorManager asyncErrorManager;

    private final MessagesInFlight messagesInFlight;

    private final byte[] MessageTypeGet;
    private final byte[] MessageTypeAction;
    private final byte[] BytesLogin;


    /*
     *  Various constants
     */
    private final int MtUninitialized = 0x0;
    private final int MtGetCommand = 0x20544547;      /* "GET " */
    private final int MtActionCommand = 0x4E544341;   /* "ACTN" */
    private final int MtLogin = 0x4E474F4C;           /* "LOGN" */
    private final int MtAccept = 0x54504341;          /* "ACPT" */
    private final int MtAck = 0x204B4341;             /* "ACK " */
    private final int MtNak = 0x204B414E;             /* "NAK " */
    private final int MtResponse = 0x20505352;        /* "RSP " */
    private final int MtStatus = 0x54415453;          /* "STAT" */

    public final int AccessLevel_Invalid = 0;
    public final int AccessLevel_ViewOnly = 1;
    public final int AccessLevel_Master = 2;
    public final int AccessLevel_LocalUI = 3;
    public final int AccessLevel_Engineering = 1000;


    private final int ConnectionState_NotConnected = 0;
    private final int ConnectionState_WaitingForLogin = 1;
    private final int ConnectionState_Connected = 2;
    private final int ConnectionState_DisconnectInProgress = 3;

    private final int loginTimeout = 10000; // in ms

    private final Object loginAcceptEvent;
    private boolean loginAcceptReceived;

    private final SocketClientBackEndManager backEndManager;


    private boolean sendMessage(byte[] type,
                                int subcommand,
                                byte[] data,
                                CommandCompletion completion) {

        if (connectionState != ConnectionState_Connected) {
            System.out.println("Failing SendCommand() - not connected!");
            return false;
        }

        int newSequenceNumber = messagesInFlight.getSequenceNumber();
        completion.saveSequenceNumber(newSequenceNumber);

        byte[] sequenceBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(newSequenceNumber).array();
        byte[] subcommandBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(subcommand).array();
        byte[] SendBuffer;

        //
        //  Can't optimize this, we need a single buffer to send.
        //  We have to do the memcpy.
        //
        if (data != null) {
            SendBuffer = new byte[type.length +
                                    sequenceBytes.length +
                                    subcommandBytes.length +
                                    data.length];
        }
        else {
            SendBuffer = new byte[type.length +
                                    sequenceBytes.length +
                                    subcommandBytes.length];
        }

        int destIndex = 0;

        for (int i = 0; i<type.length; i++)
            SendBuffer[destIndex++] = type[i];

        for (int i = 0; i<sequenceBytes.length; i++)
            SendBuffer[destIndex++] = sequenceBytes[i];

        for (int i = 0; i<subcommandBytes.length; i++)
            SendBuffer[destIndex++] = subcommandBytes[i];

        if (data != null) {
            for (int i = 0; i<data.length; i++){
                SendBuffer[destIndex++] = data[i];
            }
        }

        logger.Log("COMMAND", SendBuffer, SendBuffer.length);

        messagesInFlight.add(newSequenceNumber, completion);

        boolean Success = backEndManager.sendMessage(SendBuffer);

        if (!Success) {
            messagesInFlight.remove(newSequenceNumber);
        }

        return Success;
    }




    private boolean login(int requestedAccess) {
        
        byte[] LoginBuffer;
        byte[] MyAddress;
        byte[] Access;
        int CopyLength;

        MyAddress = backEndManager.getLocalAddress();
        Access = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(requestedAccess).array();

        byte[] Username = "Display".getBytes(Charset.forName("UTF-8"));
        byte[] MachineName = "Cortex".getBytes(Charset.forName("UTF-8"));

        LoginBuffer = new byte[BytesLogin.length + Access.length + MyAddress.length + 64 + 64];

        int loginBufferIdx = 0;
        for (int i = 0; i< BytesLogin.length; i++){
            LoginBuffer[loginBufferIdx++] = BytesLogin[i];
        }

        loginBufferIdx = 4;
        for (int i = 0; i< Access.length; i++){
            LoginBuffer[loginBufferIdx++] = Access[i];
        }

        loginBufferIdx = 8;
        for (int i = 0; i< MyAddress.length; i++){
            LoginBuffer[loginBufferIdx++] = MyAddress[i];
        }

        loginBufferIdx = 12;
        CopyLength = Username.length > 64 ? 64 : Username.length;
        for (int i = 0; i< CopyLength; i++){
            LoginBuffer[loginBufferIdx++] = Username[i];
        }

        loginBufferIdx = 76;
        CopyLength = MachineName.length > 64 ? 64 : MachineName.length;
        for (int i = 0; i< CopyLength; i++){
            LoginBuffer[loginBufferIdx++] = MachineName[i];
        }

        logger.Log("LOGIN", LoginBuffer, LoginBuffer.length);

        boolean Success = backEndManager.sendMessage(LoginBuffer);

        if (!Success) {
            asyncErrorManager.sendAsyncError("Failed Login!");
        }
        else {
            synchronized(loginAcceptEvent){
                try {
                    loginAcceptEvent.wait(loginTimeout);
                    if (loginAcceptReceived){
                        Success = true;
                    }
                    else{
                        asyncErrorManager.sendAsyncError("Login Accept failed! " + loginTimeout + " ms");
                        Success = false;
                    }
                } catch (InterruptedException ex) {
                    String err = "loginAcceptEvent.wait failed with " + ex;
                    asyncErrorManager.sendAsyncError(err);
                    System.out.println(err);
                    Success = false;
                }
            }
        }
        return Success;
    }


    /*
    This is running on the existing ReaderThread...
    */
    void handleUnexpectedDisconnect() {

        messagesInFlight.clear();

        if (connectionState != ConnectionState_Connected) {
            //
            //  If we haven't established a good connection, DON'T try
            //  to recover!!!
            //
            return;
        }

        synchronized(disconnectCallbackLock){

            ListIterator<DisconnectHandler>iterator;
            iterator = disconnectCallbacks.listIterator();

            while(iterator.hasNext()){
                DisconnectHandler callback = iterator.next();
                callback.disconnected();
            }
        }

        connectionState = ConnectionState_NotConnected;

        boolean Success;

        do
        {
            System.out.println("CiiClient.AsyncUnexpectedDisconnectHandler waiting ");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                System.out.println("Failed sleeping! " + ex);
            }

            Success = connect(ciiAccessLevel);

        } while (!Success);

        System.out.println("---CiiClient.BackendManagerDisconnectHandler()");
    }




    /**
     *  Helper class to manage sending Async Errors.
     */
    class AsyncErrorManager extends Thread {

        public AsyncErrorManager(){
            asyncErrors = new LinkedBlockingQueue<>();
            this.setDaemon(true);
            this.setPriority(Thread.NORM_PRIORITY + 1);
            this.setName("AsyncErrorManagerThread");
        }

        private final BlockingQueue<String>asyncErrors;

        public void sendAsyncError(String errorDescription) {
            logger.Log(errorDescription, null, 0);

            //
            //  If we aren't connected, filter out superfluous errors.
            //
            if ((connectionState == ConnectionState_Connected) ||
                (connectionState == ConnectionState_WaitingForLogin)) {
                try {
                    asyncErrors.put(errorDescription);
                } catch (InterruptedException ex) {
                    System.out.println("asyncErrors.put() threw " + ex);
                }
            }
        }

        @Override
        public void run() {

            while(true){

                try {
                    //
                    //  Blocking call, will wait here until a message is
                    //  available.
                    //
                    String s = asyncErrors.take();

                    synchronized(asyncErrorsLock){

                        ListIterator<AsyncErrorHandler>iterator;
                        iterator = asyncErrorCallbacks.listIterator();

                        while(iterator.hasNext()){
                            AsyncErrorHandler callback = iterator.next();
                            callback.handleError(s);
                        }
                    }

                } catch (InterruptedException ex) {
                    System.out.println("asyncErrors.take() threw " + ex);
                }
            }

        }
    }


    public void routeReceivedMessage(byte[] buffer, int dataLength)
    {
        int sequenceNumber;
        int statusCode;
        int subcommand;
        MessageTracker messageTracker;
        int substatus;
        CommandCompletion completion;

        ByteBuffer bb = ByteBuffer.wrap(buffer, 0, dataLength).order(ByteOrder.LITTLE_ENDIAN);

        int type = bb.getInt();

        switch (type)
        {
            case MtAccept:

                logger.Log("ACCEPT", buffer, dataLength);
                
                ciiAccessLevel = bb.getInt();

                synchronized(loginAcceptEvent){
                    loginAcceptReceived = true;
                    loginAcceptEvent.notify();
                }

                break;


            case MtAck:
                
                logger.Log("ACK", buffer, dataLength);
                sequenceNumber = bb.getInt();

                messageTracker = messagesInFlight.get(sequenceNumber);
                
                if (messageTracker == null) {
                    asyncErrorManager.sendAsyncError("Protocol Failure - Unexpected ACK");
                    break;
                }

                if (messageTracker.getAckReceived()) {
                    //
                    //  Error!  Double ACK!
                    //
                    messagesInFlight.remove(sequenceNumber);
                    asyncErrorManager.sendAsyncError("Protocol Failure - Double ACK");
                    break;
                }
                else {
                    messageTracker.setAckReceived();
                }


                completion = messageTracker.getCommandCompletion();

                if (completion != null) {
                    completion.receiveAck();
                }
                else {
                    System.out.println("Discarding ACK for Sequence # " + sequenceNumber);
                }

                break;


            case MtNak:
                
                logger.Log("NAK", buffer, dataLength);
                sequenceNumber = bb.getInt();
                statusCode = bb.getInt();

                messageTracker = messagesInFlight.get(sequenceNumber);
                if (messageTracker == null) {
                    asyncErrorManager.sendAsyncError("Protocol Failure - Unexpected NAK");
                    break;
                }

                messagesInFlight.remove(sequenceNumber);

                if (messageTracker.getAckReceived()) {
                    //
                    //  Error!  ACK / NAK!
                    //
                    asyncErrorManager.sendAsyncError("Protocol Failure - ACK - NAK");
                    break;
                }

                completion = messageTracker.getCommandCompletion();

                if (completion != null) {
                    completion.receiveNak(statusCode);
                }
                else {
                    System.out.println("Discarding NAK for Sequence # "
                            + sequenceNumber);
                }

                break;


            case MtResponse:
                
                logger.Log("RSP", buffer, dataLength);
                sequenceNumber = bb.getInt();
                subcommand = bb.getInt();
                statusCode = bb.getInt();

                messageTracker = messagesInFlight.get(sequenceNumber);
                if (messageTracker == null) {
                    asyncErrorManager.sendAsyncError("Protocol Failure - Unexpected RSP");
                    break;
                }

                messagesInFlight.remove(sequenceNumber);

                if (!messageTracker.getAckReceived()) {
                    //
                    //  Error!  No ACK!
                    //
                    asyncErrorManager.sendAsyncError("Protocol Failure - Missing ACK");
                    break;
                }
                
                completion = messageTracker.getCommandCompletion();

                if (completion != null) {

                   completion.receiveResponse(   subcommand,
                                                        statusCode,
                                                        buffer,
                                                        16,
                                                        dataLength - 16);
                }
                else {
                    System.out.println("Discarding RSP for Sequence # " + sequenceNumber);
                }

                break;


            case MtStatus:

                logger.Log("STAT", buffer, dataLength);

                substatus = bb.getInt();

                if (connectionState != ConnectionState_Connected) {
                    System.out.println("Throwing away early status message");
                    break;
                }

                synchronized(statusCallbacksLock) {

                    if (dataLength < 8){
                        break;
                    }

                    ReceiveStatusHandler handler = statusCallbacks.get(substatus);

                    if (handler != null) {
                        handler.receiveStatus(
                                substatus, buffer, 8, dataLength - 8);
                    }
                    else if (unhandledStatusCallback != null) {
                        unhandledStatusCallback.receiveStatus(
                                substatus, buffer, 8, dataLength - 8);
                    }
                }
                break;

            //
            //  We should never see another type of message here.
            //  This is an asymetric protocol between client and server.
            //
            default:
                logger.Log("UNKNOWN", buffer, dataLength);
                asyncErrorManager.sendAsyncError("Unknown MessageType! " + type);
                break;
        }
    }




}


