/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tainstruments.mercury.common_messages;

/**
 * Immutable object describing the network configuration on the instrument.
 * Since this is immutable, we don't bother with accessors. 
 */
public class NetworkSettingsData {
    
    public final boolean isDhcp;
    public final TaIpAddress ipAddress;
    public final TaIpAddress netmask;
    public final TaIpAddress gateway;
    public final TaMacAddress MAC;


    /**
     * @return A byte array suitable for communications with a Mercury Instrument.
     * This array should be suitable to set network settings on the Instrument.
     */
    public byte[] getByteArrayForSet() {

        byte[] send_buf;
        
        
        if (isDhcp){
            send_buf = new byte[4];
            send_buf[0] = 1;
        }
        else{
            byte[] dhcp = new byte[4];
            byte[] ip = ipAddress.getByteArray();
            byte[] mask = netmask.getByteArray();
            byte[] gate = gateway.getByteArray();

            send_buf = new byte[dhcp.length + ip.length + mask.length + gate.length];
            int j = 0;

            for (int i = 0; i < dhcp.length; i++)
                send_buf[j++] = dhcp[i];

            for (int i = 0; i < ip.length; i++)
                send_buf[j++] = ip[i];

            for (int i = 0; i < mask.length; i++)
                send_buf[j++] = mask[i];

            for (int i = 0; i < gate.length; i++)
                send_buf[j++] = gate[i];
        }

        return send_buf;
    }


    @Override
    public String toString(){

        StringBuilder sb = new StringBuilder();
        if (isDhcp){
            sb.append("DHCP ");
        }
        else{
            sb.append("STATIC ");
        }

        sb.append("IP: ").append(ipAddress.toString()).append(" ");
        sb.append("MASK: ").append(netmask.toString()).append(" ");

        if (gateway != null){
            sb.append("GW: ").append(gateway.toString()).append(" ");
        }
        else{
            sb.append("Gateway not set.");
        }

        if (MAC != null){
            sb.append("MAC: ").append(MAC.toString()).append(" ");
        }
        else{
            sb.append("MAC not set.");
        }
        
        sb.append("\n");

        return sb.toString();
    }


    /**
     * Constructor to create a message to send to the instrument.
     * @param ip
     * @param mask
     * @param gate
     * @param isDhcp
     * @throws InvalidTaIpAddressException
     * @throws InvalidNetworkSettingsException
     */
    public NetworkSettingsData( boolean isDhcp,
                                String ip,
                                String mask,
                                String gate
                                )
                                throws InvalidTaIpAddressException,
                                InvalidNetworkSettingsException
                                {
                                    
        this.isDhcp = isDhcp;

        if (isDhcp){
            ipAddress = null;
            netmask = null;
            gateway = null;
        }
        else{
            ipAddress = new TaIpAddress (ip);

            netmask = new TaIpAddress (mask);
            if (!netmask.IsValidNetmask()){
                throw new InvalidNetworkSettingsException (
                        "Invalid netmask " + netmask.toString());
            }

            
            if (gate == null){
                gateway = null;
            }
            else{
                gateway = new TaIpAddress (gate);
                if (!gateway.IsValidGateway(ipAddress, netmask))
                    throw new InvalidNetworkSettingsException (
                            "Invalid Gateway "
                            + gateway.toString() + " for "
                            + ipAddress.toString() + " / "
                            + netmask.toString());
            }
        }

        MAC = null;
    }


    
    /**
     * Constructor that functions on data sent from the instrument.
     * @param isDhcp
     * @param ip
     * @param mask
     * @param gate
     * @param mac
     */
    public NetworkSettingsData( boolean isDhcp,
                                byte[] ip,
                                byte[] mask,
                                byte[] gate,
                                byte[] mac){
                                
        this.isDhcp = isDhcp;
        ipAddress = new TaIpAddress (ip);
        netmask = new TaIpAddress (mask);

        if (gate == null){
            gateway = null;
        }
        else{
            gateway = new TaIpAddress (gate);
        }

        if (mac == null){
            MAC = null;
        }
        else {
            MAC = new TaMacAddress(mac);
        }
    }


    //
    //  Unit test.
    //
    public static void main(String []argv) {
        
        System.out.println("Unit Testing NetworkSettingsData");

        try{
            NetworkSettingsData dio = new NetworkSettingsData(  true,
                                                                "10.52.58.139",
                                                                "255.255.0.0",
                                                                "10.52.200.1");
            System.out.println(dio.toString());
        }
        catch (InvalidTaIpAddressException | InvalidNetworkSettingsException ex){
            System.out.println(ex.getMessage());
        }

        try{
            NetworkSettingsData dio = new NetworkSettingsData(  true,
                                                                "10.52.58.139",
                                                                "255.255.12.0",
                                                                "10.52.200.1");
            System.out.println(dio.toString());
        }
        catch (InvalidTaIpAddressException | InvalidNetworkSettingsException ex){
            System.out.println(ex.getMessage());
        }

        
        try{
            NetworkSettingsData dio = new NetworkSettingsData(  true,
                                                                "10.52.58.139",
                                                                "255.255.0.0",
                                                                "192.168.200.1");
            System.out.println(dio.toString());
        }
        catch (InvalidTaIpAddressException | InvalidNetworkSettingsException ex){
            System.out.println(ex.getMessage());
        }


    }
}

