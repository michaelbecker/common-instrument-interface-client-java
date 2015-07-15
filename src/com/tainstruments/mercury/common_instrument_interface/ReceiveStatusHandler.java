package com.tainstruments.mercury.common_instrument_interface;

public interface ReceiveStatusHandler {

    void receiveStatus(int substatus, byte[] buffer, int startingOffset, int dataLength);

}
