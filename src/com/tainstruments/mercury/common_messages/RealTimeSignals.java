package com.tainstruments.mercury.common_messages;

import com.tainstruments.mercury.common_instrument_interface.*;
import com.tainstruments.mercury.cii_client_helpers.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Observable;


public class RealTimeSignals extends Observable implements DisconnectHandler {

    private final int RealTimeSignalsSubstatus = 0x20002;
    private int signalCount;
    private final Object lock;
    private float [] signalValues;
    

    public float[] get(){
        synchronized(lock){
            if ((signalValues == null) || (signalCount == 0)){
                return null;
            }
            else{
                float[] sc = new float[signalCount];
                System.arraycopy(signalValues, 0, sc, 0, signalCount);
                return sc;
            }
        }
    }


    public RealTimeSignals(CiiClient ciiClient){

        lock = new Object();
        realTimeSignalStatus = new RealTimeSignalStatusMessage(ciiClient, RealTimeSignalsSubstatus);

        ciiClient.registerDisconnectHandler(this);
    }


    private final RealTimeSignalStatusMessage realTimeSignalStatus;

    
    private class RealTimeSignalStatusMessage extends StatusMessage {

        public RealTimeSignalStatusMessage(CiiClient ciiClient, int subStatus) {
            super(ciiClient, subStatus);
        }

        @Override
        public void receiveStatus(int substatus, byte[] buffer, int startingOffset, int dataLength){
            synchronized(lock){

                int count = dataLength / 4;
                
                if ((signalValues == null) || (signalCount != count)){
                    signalValues = new float[count];
                    signalCount = count;
                }

                ByteBuffer bb = ByteBuffer.wrap(buffer, startingOffset, dataLength).order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i<count; i++){
                    signalValues[i] = bb.getFloat();
                }
            }

            setChanged();
            notifyObservers();
        }
    }

    
    /**
     * Not for public use. Needed for an internal interface.
     */
    @Override
    public void disconnected(){
        synchronized(lock){
            signalCount = 0;
            signalValues = null;
        }

        setChanged();
        notifyObservers();
    }
}
