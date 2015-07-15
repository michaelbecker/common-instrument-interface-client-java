package com.tainstruments.mercury.common_messages;


public class TaMacAddress {

    private final int [] mac;

    /**
     * @return A 6 byte array suitable for communications with a Mercury Instrument.
     * You cannot use this for printing, etc. The instrument treats these as
     * unsigned characters, while Java always treats them as signed.
     */
    public byte[] getByteArray() {
        byte[] a = new byte[6];
        for(int i=0; i<4; i++)
            a[i] = (byte)mac[i];
        return a;
    }


    private String nibbleToString(int n){
        n &= 0xF;
        switch(n){
            case 0x0: return "0";
            case 0x1: return "1";
            case 0x2: return "2";
            case 0x3: return "3";
            case 0x4: return "4";
            case 0x5: return "5";
            case 0x6: return "6";
            case 0x7: return "7";
            case 0x8: return "8";
            case 0x9: return "9";
            case 0xA: return "a";
            case 0xB: return "b";
            case 0xC: return "c";
            case 0xD: return "d";
            case 0xE: return "e";
            case 0xF: return "f";
            default:  return "?";
        }
    }

    private String byteToHex(int b){

        StringBuilder sb = new StringBuilder();

        int nibble = (b & 0xF0) >>> 4;
        sb.append(nibbleToString(nibble));

        nibble = b & 0xF;
        sb.append(nibbleToString(nibble));

        return sb.toString();
    }


    @Override
    public String toString(){

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i< 6; i++){
            sb.append(byteToHex(mac[i]));
            if (i < 5)
                sb.append(":");
        }
        
        return sb.toString();
    }

    public TaMacAddress(byte[] raw){
        mac = new int[6];

        for (int i=0; i<6; i++){

            mac[i] = raw[i];
            if (raw[i] < 0){
                mac[i] += 256;
            }
        }
    }


}
