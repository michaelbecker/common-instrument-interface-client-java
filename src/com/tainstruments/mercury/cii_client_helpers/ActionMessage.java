package com.tainstruments.mercury.cii_client_helpers;

import com.tainstruments.mercury.common_instrument_interface.*;


public class ActionMessage extends CommandMessage {

    public final long DEFAULT_TIMEOUT_IN_MS = 60000;

    public ActionMessage(   CiiClient ciiClient,
                            int subStatus) {

        super(ciiClient, subStatus);
    }


    public boolean sendSynchronous(){
        return sendSynchronous(null, DEFAULT_TIMEOUT_IN_MS);
    }

    public boolean sendSynchronous(byte [] data){
        return sendSynchronous(data, DEFAULT_TIMEOUT_IN_MS);
    }

    public boolean sendSynchronous(long timeoutInMs){
        return sendSynchronous(null, timeoutInMs);
    }


    public boolean sendSynchronous(byte[] data, long timeoutInMs){

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
    public boolean send() {
        return send(null);
    }

    public boolean send(byte [] data) {
        synchronized(sync){
            resetCompleted();
            return ciiClient.sendAction(subStatus, data, this);
        }
    }

}

