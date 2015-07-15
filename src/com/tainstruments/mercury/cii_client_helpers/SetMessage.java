package com.tainstruments.mercury.cii_client_helpers;

import com.tainstruments.mercury.common_instrument_interface.CiiClient;


public class SetMessage extends CommandMessage {

    public final long DEFAULT_TIMEOUT_IN_MS = 5000;

    public SetMessage( CiiClient ciiClient, int subStatus) {

        super(ciiClient, subStatus);
    }

    public boolean setSynchronous(){
        return setSynchronous(null, DEFAULT_TIMEOUT_IN_MS);
    }

    public boolean setSynchronous(byte [] data){
        return setSynchronous(data, DEFAULT_TIMEOUT_IN_MS);
    }

    public boolean setSynchronous(long timeoutInMs){
        return setSynchronous(null, timeoutInMs);
    }

    public boolean setSynchronous(byte[] data, long timeoutInMs){
        
        boolean sendSuccess;

        synchronized(sync){

            resetCompleted();
            
            sendSuccess = ciiClient.sendAction(subStatus, data, this);
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
    public boolean set(){
        return set(null);
    }

    public boolean set(byte[] data){

        synchronized(sync){
            resetCompleted();
            return ciiClient.sendAction(subStatus, data, this);
        }
    }

    
}
