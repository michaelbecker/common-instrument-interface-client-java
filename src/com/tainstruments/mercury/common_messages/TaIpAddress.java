package com.tainstruments.mercury.common_messages;


/**
 * Immutable object describing an IP address compatible with TA Instruments comms.
 */
public class TaIpAddress {

    private final int[] address;

    /**
     * @return An array of 4 ints suitable for printing, converting to a string, etc.
     */
    public int[] getIntArray() {
        int[] a = new int[4];
        for(int i=0; i<4; i++)
            a[i] = address[i];
        return a;
    }

    
    /**
     * @return A 4 byte array suitable for communications with a Mercury Instrument.
     * You cannot use this for printing, etc. The instrument treats these as
     * unsigned characters, while Java always treats them as signed.
     */
    public byte[] getByteArray() {
        byte[] a = new byte[4];
        for(int i=0; i<4; i++)
            a[i] = (byte)address[i];
        return a;
    }


    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(address[0]).append(".").append(address[1]).append(".");
        sb.append(address[2]).append(".").append(address[3]);
        return sb.toString();
    }


    /**
     * String based constructor.
     * The string needs to contain 4 dotted octets.
     * @param address
     * @throws InvalidTaIpAddressException
     */
    public TaIpAddress(String address) throws InvalidTaIpAddressException {

        if (address == null)
            throw new InvalidTaIpAddressException ("Null address");

        String [] octetStr = address.split("\\.");
        if (octetStr.length != 4){
            throw new InvalidTaIpAddressException("Invalid IP Address " + address);
        }

        this.address = new int[4];
        
        for (int i=0; i<4; i++){
            this.address[i] = Integer.parseInt(octetStr[i]);
            if ((this.address[i] > 255) || (this.address[i] < 0)){
                throw new InvalidTaIpAddressException("Invalid IP Address " + address);
            }
        }
    }


    /**
     * Byte array based constructor.
     * The array needs to have a length of exactly 4.
     * @param raw
     */
    public TaIpAddress(byte[] raw) {
        /*  How Java plays with signed to unsigned byte / int conversions.
            a = 128 b = -128
            a = 129 b = -127
            a = 130 b = -126
            a = 254 b = -2
            a = 255 b = -1
            a = 256 b = 0
        */
        this.address = new int[4];

        for (int i=0; i<4; i++){

            this.address[i] = raw[i];
            if (raw[i] < 0){
                this.address[i] += 256;
            }
        }
    }


    /**
     *  Is this a Valid Netmask?
     *  @return True for a valid Netmask, false otherwise.
     */
    public boolean IsValidNetmask(){

        boolean zero_found = false;
        int bit;

        for (int i=0; i<4; i++){
            for (int j=8; j>0; j--){

                bit = (address[i] >>> (j-1)) & 0x01;

                if ((zero_found) && (bit != 0)){
                    return false;
                }
                else if (bit == 0){
                    zero_found = true;
                }
            }
        }

        return true;
    }


    /**
     * Could this TaIpAddress represent a valid gateway?
     * @param ipAddress The IP address of the machine.
     * @param netMask   The netmask of the machine.
     * @return  true if this object could be a gateway, false otherwise.
     */
    public boolean IsValidGateway(TaIpAddress ipAddress, TaIpAddress netMask){

        return IsValidGateway(ipAddress, netMask, this);
    }


    /**
     * Could this TaIpAddress represent a valid gateway?
     * @param ipAddress The IP address of the machine.
     * @param netMask   The netmask of the machine.
     * @param gateway   The gateway address in question.
     * @return  true if this object could be a gateway, false otherwise.
     */
    public static boolean IsValidGateway(TaIpAddress ipAddress, TaIpAddress netMask, TaIpAddress gateway){

        if (!netMask.IsValidNetmask())
            return false;

        for (int i = 0; i < 4; i++){

            if ((ipAddress.address[i] & netMask.address[i])
                    != (gateway.address[i] & netMask.address[i])){
                return false;
            }
        }

        return true;
    }


    
    private static void TestNetMask(String s) throws Exception {

        TaIpAddress m1 = new TaIpAddress(s);
        if (m1.IsValidNetmask())
            System.out.println(m1.toString() + " VALID MASK");
        else
            System.out.println(m1.toString() + " INVALID MASK");
    }
  
    /**
     * Unit test code.
     * @param argv
     * @throws Exception
     */
    public static void main (String [] argv) throws Exception{

        System.out.println("Unit Testing TaIpAddress");

        TaIpAddress t1 = new TaIpAddress("10.52.58.139");
        System.out.println(t1.toString());

        TaIpAddress t2 = new TaIpAddress("255.255.0.0");
        System.out.println(t2.toString());
        
        TaIpAddress t3 = new TaIpAddress("255.255.255.255");
        System.out.println(t3.toString());

        TaIpAddress t4 = new TaIpAddress("0.0.0.0");
        System.out.println(t4.toString());

        byte [] byteIp = new byte[4];
        byteIp[0] = (byte)192;
        byteIp[1] = (byte)168;
        byteIp[2] = (byte)0;
        byteIp[3] = (byte)127;
        TaIpAddress b1 = new TaIpAddress(byteIp);
        System.out.println(b1.toString());

        byteIp[0] = (byte)255;
        byteIp[1] = (byte)224;
        byteIp[2] = (byte)0;
        byteIp[3] = (byte)0;
        TaIpAddress b2 = new TaIpAddress(byteIp);
        System.out.println(b2.toString());


        TestNetMask("128.0.0.0");
        TestNetMask("192.0.0.0");
        TestNetMask("224.0.0.0");
        TestNetMask("240.0.0.0");
        TestNetMask("248.0.0.0");
        TestNetMask("252.0.0.0");
        TestNetMask("254.0.0.0");
        TestNetMask("255.0.0.0");
        TestNetMask("255.255.0.0");
        TestNetMask("255.255.255.0");
        TestNetMask("255.255.255.255");
        TestNetMask("255.255.248.0");


        TestNetMask("128.128.0.0");
        TestNetMask("192.255.0.0");
        TestNetMask("224.0.1.0");
        TestNetMask("240.0.0.255");
        TestNetMask("248.0.45.0");
        TestNetMask("252.252.0.0");
        TestNetMask("255.255.120.0");


        //  Testing Signed / unsigned math...
        int a;
        byte b;

        a = 128;
        b = (byte)a;
        System.out.println("a = " + a + " b = " + b);

        a = 129;
        b = (byte)a;
        System.out.println("a = " + a + " b = " + b);

        a = 130;
        b = (byte)a;
        System.out.println("a = " + a + " b = " + b);

        a = 254;
        b = (byte)a;
        System.out.println("a = " + a + " b = " + b);

        a = 255;
        b = (byte)a;
        System.out.println("a = " + a + " b = " + b);

        a = 256;
        b = (byte)a;
        System.out.println("a = " + a + " b = " + b);


        //TaIpAddress t3 = new TaIpAddress("-1");
        //System.out.println(t3.toString());

    }


}
