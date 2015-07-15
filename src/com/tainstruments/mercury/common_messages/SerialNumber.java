package com.tainstruments.mercury.common_messages;

import com.tainstruments.mercury.common_instrument_interface.*;
import com.tainstruments.mercury.cii_client_helpers.*;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Observable;



public class SerialNumber extends Observable implements ConnectHandler, DisconnectHandler {

    private final int MaxLength = 48;
    private final int GET_SUBSTATUS = 0x2;
    private final int SET_SUBSTATUS = 0x10002;
    private String serialNumber;
    private final CiiClient ciiClient;
    private final SerialNumberGetMessage getSerialNumber;
    private final SerialNumberSetMessage setSerialNumber;
    private final Object lock;


    private void invalidate(){
        synchronized (lock){
            serialNumber = null;
        }
    }


    public SerialNumber(CiiClient ciiClient){

        super();

        lock = new Object();
        this.ciiClient = ciiClient;

        getSerialNumber = new SerialNumberGetMessage(ciiClient, GET_SUBSTATUS);
        setSerialNumber = new SerialNumberSetMessage(ciiClient, SET_SUBSTATUS);

        //
        //  Do this stuff last.
        //
        ciiClient.registerConnectHandler(this);
        ciiClient.registerDisconnectHandler(this);

        if (ciiClient.isConnected()){
            getSerialNumber.get();
        }
        else{
            invalidate();
        }
    }

    
    /**
     * Returns the cached SN string in this object.
     * @return SN or null
     */
    public String get(){
        synchronized (lock){
            return serialNumber;
        }
    }

    /**
     * Performs a synchronous get() directly to the instrument.
     * It then in-line updates its internal state and returns the latest copy
     * of the SN.
     * @return SN or null
     */
    public String getSynchronous() {
        boolean success = getSerialNumber.getSynchronous();
        synchronized(lock){
            if (success)
                return serialNumber;
            else
                return null;
        }
    }


    public boolean setSynchronous(String sn) {

        sn = sn.trim();

        byte [] data = sn.getBytes(Charset.forName("UTF-8"));
        if (data.length > MaxLength) {
            return false;
        }

        boolean set_success = setSerialNumber.setSynchronous(data);
        boolean get_success = getSerialNumber.getSynchronous();

        return set_success && get_success;
    }


    /**
     * Sends an async set() then a get().
     * @param sn The new serial number string.
     * @return true on success, false on a failure.
     * This depends on the async nature of the CommandCompletion class to
     * update internal state.
     */
    public boolean set(String sn){

        sn = sn.trim();

        byte [] data = sn.getBytes(Charset.forName("UTF-8"));
        if (data.length > MaxLength) {
            return false;
        }

        boolean set_success = setSerialNumber.set(data);
        boolean get_success = getSerialNumber.get();

        return set_success && get_success;
    }



    private class SerialNumberGetMessage extends GetMessage {
        
        public SerialNumberGetMessage(CiiClient ciiClient, int subStatus) {
            super(ciiClient, subStatus);
        }

        @Override
        public void receiveNak(int errorCode) {
            invalidate();
            super.receiveNak(errorCode);
            setChanged();
            notifyObservers();
        }

        @Override
        public void receiveResponse( int subcommand,
                                            int statusCode,
                                            byte[] data,
                                            int startingOffset,
                                            int dataLength){
            synchronized (lock){
                byte [] localData = Arrays.copyOfRange(data, startingOffset, startingOffset + dataLength);

                try {
                    
                    serialNumber = new String(localData, "UTF-8");
                    serialNumber = serialNumber.trim();
                    
                } catch (UnsupportedEncodingException ex) {
                    System.out.println(
                            "SN conversion failed! UnsupportedEncodingException "
                                    + ex);
                    invalidate();
                }
            }

            super.receiveResponse(subcommand, statusCode, data, startingOffset, dataLength);
            setChanged();
            notifyObservers();
        }
    }


    private class SerialNumberSetMessage extends SetMessage {

        public SerialNumberSetMessage(CiiClient ciiClient, int subStatus) {
            super(ciiClient, subStatus);
        }
    }

    /**
     * Not for public use. Needed for an internal interface.
     */
    @Override
    public void connected() {
        getSerialNumber.get();
    }

    /**
     * Not for public use. Needed for an internal interface.
     */
    @Override
    public void disconnected() {
        invalidate();
        setChanged();
        notifyObservers();
    }



}
