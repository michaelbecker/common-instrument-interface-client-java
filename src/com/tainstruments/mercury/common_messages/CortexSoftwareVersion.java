package com.tainstruments.mercury.common_messages;

import com.tainstruments.mercury.cii_client_helpers.GetMessage;
import com.tainstruments.mercury.common_instrument_interface.CiiClient;
import com.tainstruments.mercury.common_instrument_interface.ConnectHandler;
import com.tainstruments.mercury.common_instrument_interface.DisconnectHandler;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Observable;


public class CortexSoftwareVersion extends Observable implements ConnectHandler, DisconnectHandler {

    private SoftwareVersion version;
    private final Object lock;
    private final GetCortexSoftwareVersion getCortexSoftwareVersion;
    private final int GetSoftwareVersionSubstatus = 0x0;


    public CortexSoftwareVersion(CiiClient ciiClient) {
        lock = new Object();
        getCortexSoftwareVersion = new GetCortexSoftwareVersion(ciiClient, GetSoftwareVersionSubstatus);

        ciiClient.registerConnectHandler(this);
        ciiClient.registerDisconnectHandler(this);

        if (ciiClient.isConnected()){
            getCortexSoftwareVersion.get();
        }
        else{
            invalidateVersion();
        }
    }

    
    /**
     * Returns the cached SoftwareVersion string in this object.
     * @return SoftwareVersion or null
     */
    public SoftwareVersion get(){
        synchronized(lock){
            return version;
        }
    }

    /**
     * Performs a synchronous get() directly to the instrument.
     * It then in-line updates its internal state and returns the latest copy
     * of the Location.
     * @return SoftwareVersion or null
     */
    public SoftwareVersion getSynchronous() {
        boolean success = getCortexSoftwareVersion.getSynchronous();
        synchronized(lock){
            if (success)
                return version;
            else
                return null;
        }
    }

    private void invalidateVersion(){
        synchronized(lock){
            version = null;
        }
    }


    private class GetCortexSoftwareVersion extends GetMessage {

        public GetCortexSoftwareVersion(CiiClient ciiClient, int subStatus) {
            super(ciiClient, subStatus);
        }

        @Override
        public void receiveNak(int errorCode) {
            invalidateVersion();
            super.receiveNak(errorCode);
            setChanged();
            notifyObservers();
        }

        @Override
        public void receiveResponse(    int subcommand,
                                        int statusCode,
                                        byte[] data,
                                        int startingOffset,
                                        int dataLength){

            int count = dataLength / 4;

            ByteBuffer bb = ByteBuffer.wrap(data, startingOffset, dataLength).order(ByteOrder.LITTLE_ENDIAN);

            if (count >= 4){
                int major = bb.getInt();
                int minor = bb.getInt();
                int release = bb.getInt();
                int build = bb.getInt();
                synchronized (lock){
                    version = new SoftwareVersion(major, minor, release, build);
                }
            }
            else{
                invalidateVersion();
            }

            super.receiveResponse(subcommand, statusCode, data, startingOffset, dataLength);
            setChanged();
            notifyObservers();
        }
    }

    /**
     * Not for public use. Needed for an internal interface.
     */
    @Override
    public void connected() {
        getCortexSoftwareVersion.get();
    }

    /**
     * Not for public use. Needed for an internal interface.
     */
    @Override
    public void disconnected() {
        invalidateVersion();
        setChanged();
        notifyObservers();
    }

}
