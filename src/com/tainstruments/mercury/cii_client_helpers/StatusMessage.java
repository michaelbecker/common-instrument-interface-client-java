package com.tainstruments.mercury.cii_client_helpers;

import com.tainstruments.mercury.common_instrument_interface.*;


public abstract class StatusMessage implements ReceiveStatusHandler {

    
    public StatusMessage(CiiClient ciiClient, int subStatus) {

        this.ciiClient = ciiClient;
        this.subStatus = subStatus;

        //
        //  Call this last always.
        //
        ciiClient.registerStatusHandler(subStatus, this);
    }

    /**
     *  Do this to clean up. This unhooks the StstusMessage from the CII.
     */
    public void close() {
        ciiClient.unregisterStatusHandler(subStatus);
    }

    @Override
    public abstract void receiveStatus(int substatus, byte[] buffer, int startingOffset, int dataLength);


    protected final CiiClient ciiClient;
    protected final int subStatus;

}

