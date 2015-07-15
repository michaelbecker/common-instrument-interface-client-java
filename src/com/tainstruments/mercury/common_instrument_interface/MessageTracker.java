package com.tainstruments.mercury.common_instrument_interface;

class MessageTracker {

    private final CommandCompletion commandCompletion;
    private boolean ackReceived;

    public MessageTracker(CommandCompletion c) {
        commandCompletion = c;
    }

    public CommandCompletion getCommandCompletion() {
        return commandCompletion;
    }

    public boolean getAckReceived() {
        return ackReceived;
    }

    public void setAckReceived() {
        ackReceived = true;
    }

}
