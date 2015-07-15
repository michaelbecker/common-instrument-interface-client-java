package com.tainstruments.mercury.common_instrument_interface;

public interface CommandCompletion {

    void receiveAck();
    
    void receiveNak(int errorCode);

    void receiveResponse(    int subcommand,
                                    int statusCode,
                                    byte[] data,
                                    int startingOffset,
                                    int dataLength);

    /*
     *  This will be called inside of your call to SendXxxCommand(), to
     *  allow you to save the sequence number for this command.
     */
    void saveSequenceNumber(int sequenceNumber);
}
