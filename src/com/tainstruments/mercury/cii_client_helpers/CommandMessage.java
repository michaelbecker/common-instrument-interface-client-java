package com.tainstruments.mercury.cii_client_helpers;

import com.tainstruments.mercury.common_instrument_interface.*;


public class CommandMessage implements CommandCompletion {

    protected final CiiClient ciiClient;
    protected final int subStatus;
    protected volatile int sequenceNumber;

    protected final Object sync;
    protected int errorCode;
    protected boolean completed;

    
    protected void resetCompleted() {
        completed = false;
        errorCode = -1;
    }

    public boolean isCompleted() {
        return completed;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public CommandMessage(  CiiClient ciiClient,
                            int subStatus){
        
        this.ciiClient = ciiClient;
        this.subStatus = subStatus;
        sync = new Object();
        completed = false;
        errorCode = -1;
    }

    @Override
    public void receiveAck() {
    }

    @Override
    public void receiveNak(int errorCode) {
        synchronized(sync){
            this.errorCode = errorCode;
            completed = true;
            sync.notify();
        }
    }

    @Override
    public void receiveResponse(    int subcommand,
                                    int statusCode,
                                    byte[] data,
                                    int startingOffset,
                                    int dataLength){
        synchronized(sync){
            this.errorCode = statusCode;
            completed = true;
            sync.notify();
        }
    }

    @Override
    public void saveSequenceNumber(int sequenceNumber){
        this.sequenceNumber = sequenceNumber;
    }
    
}
