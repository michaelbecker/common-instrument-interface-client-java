package com.tainstruments.mercury.common_messages;

import com.tainstruments.mercury.common_instrument_interface.*;
import com.tainstruments.mercury.cii_client_helpers.*;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Observable;



public class InstrumentLocation extends Observable implements ConnectHandler, DisconnectHandler {

    private final int MaxLengthInBytes = 48;
    private final int GetInstrumentLocationSubstatus = 4;
    private final int SetInstrumentLocationSubstatus = 0x10004;
    private String instrumentLocation;
    private final CiiClient ciiClient;
    private final InstrumentLocationGetMessage getInstrumentLocation;
    private final InstrumentLocationSetMessage setInstrumentLocation;
    private final Object lock;


    private void invalidateLocation(){
        synchronized (lock){
            instrumentLocation = null;
        }
    }


    public InstrumentLocation(CiiClient ciiClient){

        super();

        lock = new Object();
        this.ciiClient = ciiClient;

        getInstrumentLocation = new InstrumentLocationGetMessage(ciiClient, GetInstrumentLocationSubstatus);
        setInstrumentLocation = new InstrumentLocationSetMessage(ciiClient, SetInstrumentLocationSubstatus);

        //
        //  Do this stuff last.
        //
        ciiClient.registerConnectHandler(this);
        ciiClient.registerDisconnectHandler(this);

        if (ciiClient.isConnected()){
            getInstrumentLocation.get();
        }
        else{
            invalidateLocation();
        }
    }

    /**
     * Returns the cached Location string in this object.
     * @return Location or null
     */
    public String get(){
        synchronized (lock){
            return instrumentLocation;
        }
    }

    /**
     * Performs a synchronous get() directly to the instrument.
     * It then in-line updates its internal state and returns the latest copy
     * of the Location.
     * @return Location or null
     */
    public String getSynchronous() {
        boolean success = getInstrumentLocation.getSynchronous();
        synchronized(lock){
            if (success)
                return instrumentLocation;
            else
                return null;
        }
    }


    /**
     * Sends a set() command then a get() command synchronously in-line.
     * @param location  The new Location string.
     * @return true on success, false on a failure.
     */
    public boolean setSynchronous(String location) {

        location = location.trim();

        byte [] data = location.getBytes(Charset.forName("UTF-16LE"));

        if (data.length > MaxLengthInBytes) {
            return false;
        }

        boolean set_success = setInstrumentLocation.setSynchronous(data);
        boolean get_success = getInstrumentLocation.getSynchronous();

        return set_success && get_success;
    }


    /**
     * Sends an async set() then a get().
     * @param location  The new Location string.
     * @return true on success, false on a failure.
     * This depends on the async nature of the CommandCompletion class to
     * update internal state.
     */
    public boolean set(String location) {

        location = location.trim();

        byte [] data = location.getBytes(Charset.forName("UTF-16LE"));

        if (data.length > MaxLengthInBytes) {
            return false;
        }

        boolean set_success = setInstrumentLocation.set(data);
        boolean get_success = getInstrumentLocation.get();

        return set_success && get_success;
    }


    private class InstrumentLocationGetMessage extends GetMessage {

        public InstrumentLocationGetMessage(CiiClient ciiClient, int subStatus) {
            super(ciiClient, subStatus);
        }

        @Override
        public void receiveNak(int errorCode) {
            invalidateLocation();
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

                    instrumentLocation = new String(localData, "UTF-16LE");
                    instrumentLocation = instrumentLocation.trim();

                } catch (UnsupportedEncodingException ex) {
                    System.out.println(
                            "Location conversion failed! UnsupportedEncodingException "
                                    + ex);
                    invalidateLocation();
                }
            }

            super.receiveResponse(subcommand, statusCode, data, startingOffset, dataLength);
            setChanged();
            notifyObservers();
        }
    }


    private class InstrumentLocationSetMessage extends SetMessage {

        public InstrumentLocationSetMessage(CiiClient ciiClient, int subStatus) {
            super(ciiClient, subStatus);
        }
    }


    /**
     * Not for public use. Needed for an internal interface.
     */
    @Override
    public void connected() {
        getInstrumentLocation.get();
    }

    /**
     * Not for public use. Needed for an internal interface.
     */
    @Override
    public void disconnected() {
        invalidateLocation();
        setChanged();
        notifyObservers();
    }
}
