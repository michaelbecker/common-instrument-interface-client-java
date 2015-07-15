package com.tainstruments.mercury.common_messages;

import com.tainstruments.mercury.common_instrument_interface.*;
import com.tainstruments.mercury.cii_client_helpers.*;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Observable;



public class InstrumentName extends Observable implements ConnectHandler, DisconnectHandler {

    private final int MaxLengthInBytes = 48;
    private final int GetInstrumentNameSubstatus = 3;
    private final int SetInstrumentNameSubstatus = 0x10003;
    private String instrumentName;
    private final CiiClient ciiClient;
    private final InstrumentNameGetMessage getInstrumentName;
    private final InstrumentNameSetMessage setInstrumentName;
    private final Object lock;


    private void invalidateName() {
        synchronized (lock){
            instrumentName = null;
        }
    }


    public InstrumentName(CiiClient ciiClient) {

        super();

        lock = new Object();
        this.ciiClient = ciiClient;

        getInstrumentName = new InstrumentNameGetMessage(ciiClient, GetInstrumentNameSubstatus);
        setInstrumentName = new InstrumentNameSetMessage(ciiClient, SetInstrumentNameSubstatus);

        //
        //  Do this stuff last.
        //
        ciiClient.registerConnectHandler(this);
        ciiClient.registerDisconnectHandler(this);

        if (ciiClient.isConnected()){
            getInstrumentName.get();
        }
        else{
            invalidateName();
        }
    }


    /**
     * Returns the cached name string in this object.
     * @return Name or null
     */
    public String get(){
        synchronized (lock){
            return instrumentName;
        }
    }

    
    /**
     * Performs a synchronous get() directly to the instrument.
     * It then in-line updates its internal state and returns the latest copy
     * of the Name.
     * @return Name or null
     */
    public String getSynchronous() {
        boolean success = getInstrumentName.getSynchronous();
        synchronized(lock){
            if (success)
                return instrumentName;
            else
                return null;
        }
    }


    /**
     * Sends a set() command then a get() command synchronously in-line.
     * @param name  The new Name string.
     * @return true on success, false on a failure.
     */
    public boolean setSynchronous(String name) {

        name = name.trim();
        
        byte [] data = name.getBytes(Charset.forName("UTF-16LE"));

        if (data.length > MaxLengthInBytes) {
            return false;
        }

        boolean set_success = setInstrumentName.setSynchronous(data);
        boolean get_success = getInstrumentName.getSynchronous();

        return set_success && get_success;
    }


    /**
     * Sends an async set() then a get().
     * @param name  The new Location string.
     * @return true on success, false on a failure.
     * This depends on the async nature of the CommandCompletion class to
     * update internal state.
     */
    public boolean set(String name) {

        name = name.trim();

        byte [] data = name.getBytes(Charset.forName("UTF-16LE"));

        if (data.length > MaxLengthInBytes) {
            return false;
        }

        boolean set_success = setInstrumentName.set(data);
        boolean get_success = getInstrumentName.get();

        return set_success && get_success;
    }


    private class InstrumentNameGetMessage extends GetMessage {

        public InstrumentNameGetMessage(CiiClient ciiClient, int subStatus) {
            super(ciiClient, subStatus);
        }

        @Override
        public void receiveNak(int errorCode) {
            invalidateName();
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

                    instrumentName = new String(localData, "UTF-16LE");
                    instrumentName = instrumentName.trim();

                } catch (UnsupportedEncodingException ex) {
                    System.out.println(
                            "Name conversion failed! UnsupportedEncodingException "
                                    + ex);
                    invalidateName();
                }
            }

            super.receiveResponse(subcommand, statusCode, data, startingOffset, dataLength);
            setChanged();
            notifyObservers();
        }
    }


    private class InstrumentNameSetMessage extends SetMessage {

        public InstrumentNameSetMessage(CiiClient ciiClient, int subStatus) {
            super(ciiClient, subStatus);
        }
    }

    /**
     * Not for public use. Needed for an internal interface.
     */
    @Override
    public void connected() {
        getInstrumentName.get();
    }

    /**
     * Not for public use. Needed for an internal interface.
     */
    @Override
    public void disconnected() {
        invalidateName();
        setChanged();
        notifyObservers();
    }
}
