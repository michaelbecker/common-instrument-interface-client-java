package com.tainstruments.mercury.cii_client_helpers;

import com.tainstruments.mercury.common_instrument_interface.*;


public class GetMessage extends CommandMessage {

    public final long DEFAULT_TIMEOUT_IN_MS = 5000;
    
    public GetMessage(  CiiClient ciiClient,
                        int subStatus) {
        
        super(ciiClient, subStatus);
    }


    public boolean getSynchronous() {
        return getSynchronous(null, DEFAULT_TIMEOUT_IN_MS);
    }

    public boolean getSynchronous(byte [] data) {
        return getSynchronous(data, DEFAULT_TIMEOUT_IN_MS);
    }

    public boolean getSynchronous(long timeoutInMs) {
        return getSynchronous(null, timeoutInMs);
    }

    public boolean getSynchronous(byte[] data, long timeoutInMs) {

        boolean sendSuccess;

        synchronized(sync){

            resetCompleted();

            sendSuccess = ciiClient.sendGet(subStatus, data, this);
            if (!sendSuccess){
                return false;
            }

            try {
                sync.wait(timeoutInMs);
            } catch (InterruptedException ex) {
                System.out.println("Failed waiting - exception " + ex);
                return false;
            }

            if (!isCompleted()){
                return false;
            }
            else{
                return errorCode == 0;
            }
        }
    }

    
    //
    //  Async calls
    //
    public boolean get() {
        return get(null);
    }

    public boolean get(byte [] data){
        synchronized(sync){
            resetCompleted();
            return ciiClient.sendGet(subStatus, data, this);
        }
    }

}

