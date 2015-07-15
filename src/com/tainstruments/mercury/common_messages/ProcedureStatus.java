package com.tainstruments.mercury.common_messages;

import com.tainstruments.mercury.cii_client_helpers.GetMessage;
import com.tainstruments.mercury.cii_client_helpers.StatusMessage;
import com.tainstruments.mercury.common_instrument_interface.CiiClient;
import com.tainstruments.mercury.common_instrument_interface.ConnectHandler;
import com.tainstruments.mercury.common_instrument_interface.DisconnectHandler;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Observable;




public class ProcedureStatus extends Observable implements ConnectHandler, DisconnectHandler {

    private ProcedureStatusData procedureStatus;
    private final Object lock;
    private final int ProcedureStatusSubstatus = 0x20003;
    private final int GetProcedureStatusSubstatus = 0x9;
    private final ProcedureStatusStatusMessage procedureStatusStatus;
    private final ProcedureStatusGetMessage getProcedureStatus;



    /**
     * Returns the cached ProcedureStatusData in this object.
     * @return ProcedureStatusData or null
     */
    public ProcedureStatusData get(){
        synchronized(lock){
            return procedureStatus;
        }
    }

    /**
     * Performs a synchronous get() directly to the instrument.
     * It then in-line updates its internal state and returns the latest copy
     * of the ProcedureStatusData.
     * @return ProcedureStatusData or null
     */
    public ProcedureStatusData getSynchronous() {
        boolean success = getProcedureStatus.getSynchronous();
        synchronized(lock){
            if (success)
                return procedureStatus;
            else
                return null;
        }
    }


    public ProcedureStatus(CiiClient ciiClient){
        
        super();
        
        lock = new Object();

        procedureStatusStatus = new ProcedureStatusStatusMessage(ciiClient, ProcedureStatusSubstatus);
        getProcedureStatus = new ProcedureStatusGetMessage(ciiClient, GetProcedureStatusSubstatus);
        
        //
        //  Do this stuff last.
        //
        ciiClient.registerConnectHandler(this);
        ciiClient.registerDisconnectHandler(this);

        if (ciiClient.isConnected()){
            getProcedureStatus.get();
        }
        else{
            invalidate();
        }

    }


    private void invalidate() {

        synchronized(lock){
            procedureStatus = null;
        }
    }

    
    private void decodeProcedureStatusBits ( byte[] data,
                                                int startingOffset,
                                                int dataLength) {
        
        if (dataLength < 28){
            invalidate();
        }
        else{
            ByteBuffer bb = ByteBuffer.wrap(data, startingOffset, dataLength).order(ByteOrder.LITTLE_ENDIAN);

            int state = bb.getInt();
            int status = bb.getInt();
            int index = bb.getInt();

            long high = bb.getLong();
            long low = bb.getLong();

            synchronized(lock){
                procedureStatus = new ProcedureStatusData(state, status, index, high, low);
            }
        }
    }


    
    private class ProcedureStatusStatusMessage extends StatusMessage {
        
        public ProcedureStatusStatusMessage(CiiClient ciiClient, int subStatus){
            super(ciiClient, subStatus);
        }

        @Override
        public void receiveStatus(int substatus, byte[] data, int startingOffset, int dataLength){
            decodeProcedureStatusBits(data, startingOffset, dataLength);
            setChanged();
            notifyObservers();
        }
    }


    private class ProcedureStatusGetMessage extends GetMessage {

        public ProcedureStatusGetMessage(CiiClient ciiClient, int subStatus) {
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
        public void receiveResponse(    int subcommand,
                                        int statusCode,
                                        byte[] data,
                                        int startingOffset,
                                        int dataLength){
            decodeProcedureStatusBits(data, startingOffset, dataLength);
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
        getProcedureStatus.get();
    }

    /**
     * Not for public use. Needed for an internal interface.
     */
    @Override
    public void disconnected(){
        invalidate();
        setChanged();
        notifyObservers();
    }


}
