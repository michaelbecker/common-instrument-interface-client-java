package com.tainstruments.mercury.common_instrument_interface;

import java.util.HashMap;

class MessagesInFlight {

    private int sequenceNumberGenerator;
    private final Object sequenceNumberLock;
    private final HashMap<Integer, MessageTracker> messagesInFlight;


    public MessagesInFlight() {
        sequenceNumberLock = new Object();
        sequenceNumberGenerator = Integer.MAX_VALUE - 10;
        messagesInFlight = new HashMap<>();
    }

    
    public int getSequenceNumber() {

        int value;

        synchronized(sequenceNumberLock){
            
            do {

                sequenceNumberGenerator++;

                if (sequenceNumberGenerator == Integer.MAX_VALUE){
                    sequenceNumberGenerator = 1;
                }
                else if (sequenceNumberGenerator <= 0){
                    sequenceNumberGenerator = 1;
                }
                
            } while (messagesInFlight.containsKey(sequenceNumberGenerator));

            value = sequenceNumberGenerator;
        }
        
        return value;
    }

    
    public void add(int sequenceNumber, CommandCompletion completion) {
        
        synchronized (sequenceNumberLock) {

            //System.out.println("Adding sequenceNumber - " + sequenceNumber);
            MessageTracker v = messagesInFlight.put(sequenceNumber, new MessageTracker(completion));
            
            //
            //  Sanity check, this should ALWAYS be null.
            //
            if (v != null){
                throw new RuntimeException("Internal Error - duplicate messages in flight");
            }
        }
    }

    
    public void remove(int sequenceNumber) {

        synchronized (sequenceNumberLock) {
            //System.out.println("Removing sequenceNumber - " + sequenceNumber);
            messagesInFlight.remove(sequenceNumber);
        }
    }


    public MessageTracker get(int sequenceNumber) {

        synchronized (sequenceNumberLock) {
            MessageTracker messageTracker = messagesInFlight.get(sequenceNumber);
            return messageTracker;
        }
    }

    public void clear() {

        synchronized (sequenceNumberLock) {
            messagesInFlight.clear();
        }
    }


    

}
