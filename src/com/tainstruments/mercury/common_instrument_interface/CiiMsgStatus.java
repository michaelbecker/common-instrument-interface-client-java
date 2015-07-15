package com.tainstruments.mercury.common_instrument_interface;

/**
 * Mercury Trios comm specified message statuses.
 */
public enum CiiMsgStatus
{
    MsSuccess(0),              /**  It worked. */
    MsFailed(1),               /**  It didn't work. */
    MsUnknownCommand(2),       /**  Unknown command message. */
    MsMalformedMessage(3),     /**  Trouble parsing the message at the protocol level */
    MsBusy(4),                 /**  Try again later... */
    MsNotLoggedIn(5),          /**  Try again later... */
    MsAccessDenied(6),         /**  Try again later... */
    MsOperationTimedOut(7),    /**  Internal Timeout */
    MsUserSpecific(256);       /**  Everything past this is custom */

    private final int value;

    private CiiMsgStatus(int value) {
        this.value = value;
    }

    public int getValue(){
        return value;
    }
}

