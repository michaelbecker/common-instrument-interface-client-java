package com.tainstruments.mercury.common_messages;

import java.util.UUID;


/**
 * Immutable object describing the current Procedure status on the Instrument.
 * Since this is immutable, we don't bother with accessors.
 */
public final class ProcedureStatusData {

    public final ProcedureRunState runState;
    public final ProcedureEndStatus endStatus;
    public final int curSegmentIndex;
    public final UUID guid;

    public ProcedureStatusData(int procedureState, int procedureEndStatus, int curSegmentIndex, UUID guid){
        this.runState = ProcedureRunState.getInstance(procedureState);
        this.endStatus = ProcedureEndStatus.getInstance(procedureEndStatus);
        this.curSegmentIndex = curSegmentIndex;
        this.guid = new UUID(guid.getMostSignificantBits(), guid.getLeastSignificantBits());
    }

    public ProcedureStatusData(int procedureState, int procedureEndStatus, int curSegmentIndex, long msb, long lsb){
        this.runState = ProcedureRunState.getInstance(procedureState);
        this.endStatus = ProcedureEndStatus.getInstance(procedureEndStatus);
        this.curSegmentIndex = curSegmentIndex;
        this.guid = new UUID(msb, lsb);
    }

}


