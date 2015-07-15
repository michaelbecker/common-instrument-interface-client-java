package com.tainstruments.mercury.common_messages;

import com.tainstruments.mercury.cii_client_helpers.GetMessage;
import com.tainstruments.mercury.cii_client_helpers.SetMessage;
import com.tainstruments.mercury.common_instrument_interface.CiiClient;
import com.tainstruments.mercury.common_instrument_interface.ConnectHandler;
import com.tainstruments.mercury.common_instrument_interface.DisconnectHandler;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Observable;



public class NetworkSettings extends Observable implements ConnectHandler, DisconnectHandler {

    private final int GET_SUBSTATUS = 0x5;
    private final int SET_SUBSTATUS = 0x10005;
    private final CiiClient ciiClient;
    private final NetworkSettingsGetMessage getMessage;
    private final NetworkSettingsSetMessage setMessage;
    private final Object lock;
    private NetworkSettingsData networkSettingsData;


    
    private void invalidate(){
        synchronized (lock){
            networkSettingsData = null;
        }
    }


    public NetworkSettings(CiiClient ciiClient){
        super();

        lock = new Object();
        this.ciiClient = ciiClient;

        getMessage = new NetworkSettingsGetMessage(ciiClient, GET_SUBSTATUS);
        setMessage = new NetworkSettingsSetMessage(ciiClient, SET_SUBSTATUS);

        //
        //  Do this stuff last.
        //
        ciiClient.registerConnectHandler(this);
        ciiClient.registerDisconnectHandler(this);

        if (ciiClient.isConnected()){
            getMessage.get();
        }
        else{
            invalidate();
        }
    }



    /**
     * Returns the cached NetworkSettingsData.
     * @return NetworkSettingsData or null
     */
    public NetworkSettingsData get(){
        synchronized (lock){
            return networkSettingsData;
        }
    }

    /**
     * Performs a synchronous get() directly to the instrument.
     * It then in-line updates its internal state and returns the latest copy
     * of the NetworkSettingsData.
     * @return NetworkSettingsData or null
     */
    public NetworkSettingsData getSynchronous() {
        boolean success = getMessage.getSynchronous();
        synchronized(lock){
            if (success)
                return networkSettingsData;
            else
                return null;
        }
    }


    public boolean setSynchronous(  boolean isDhcp,
                                    String ip,
                                    String mask,
                                    String gate)
    throws InvalidTaIpAddressException, InvalidNetworkSettingsException {

        NetworkSettingsData ns = new NetworkSettingsData(isDhcp, ip, mask, gate);
        byte[] data = ns.getByteArrayForSet();

        boolean set_success = setMessage.setSynchronous(data);
        boolean get_success = getMessage.getSynchronous();

        return set_success && get_success;
    }

    

    public boolean set( boolean isDhcp,
                        String ip,
                        String mask,
                        String gate)
    throws InvalidTaIpAddressException, InvalidNetworkSettingsException {

        NetworkSettingsData ns = new NetworkSettingsData(isDhcp, ip, mask, gate);
        byte[] data = ns.getByteArrayForSet();

        boolean set_success = setMessage.set(data);
        boolean get_success = getMessage.get();

        return set_success && get_success;
    }



    private class NetworkSettingsGetMessage extends GetMessage {

        public NetworkSettingsGetMessage(CiiClient ciiClient, int subStatus) {
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
        public void receiveResponse( int subcommand,
                                            int statusCode,
                                            byte[] data,
                                            int startingOffset,
                                            int dataLength){
            synchronized (lock){

                if (dataLength < 22 ){
                    invalidate();
                }
                else{
                    ByteBuffer bb = ByteBuffer.wrap(data, startingOffset, dataLength).order(ByteOrder.LITTLE_ENDIAN);
                    
                    int dhcp_raw = bb.getInt();
                    boolean dhcp;
                    dhcp = dhcp_raw != 0;

                    byte [] ip = new byte[4];
                    for (int i = 0; i<4; i++)
                        ip[i] = bb.get();

                    byte [] mask = new byte[4];
                    for (int i = 0; i<4; i++)
                        mask[i] = bb.get();

                    byte [] gate = new byte[4];
                    for (int i = 0; i<4; i++)
                        gate[i] = bb.get();

                    byte [] mac = new byte[6];
                    for (int i = 0; i<6; i++)
                        mac[i] = bb.get();

                    networkSettingsData
                            = new NetworkSettingsData(dhcp, ip, mask, gate, mac);
                }
            }

            super.receiveResponse(subcommand, statusCode, data, startingOffset, dataLength);
            setChanged();
            notifyObservers();
        }
    }


    private class NetworkSettingsSetMessage extends SetMessage {

        public NetworkSettingsSetMessage(CiiClient ciiClient, int subStatus) {
            super(ciiClient, subStatus);
        }
    }


    /**
     * Not for public use. Needed for an internal interface.
     */
    @Override
    public void connected() {
        getMessage.get();
    }

    /**
     * Not for public use. Needed for an internal interface.
     */
    @Override
    public void disconnected() {
        invalidate();
        setChanged();
        notifyObservers();
    }
}

