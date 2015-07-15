package com.tainstruments.mercury.common_messages;

public enum ProcedureEndStatus {

    RUNNING(0),
    COMPLETE(1),
    USER_STOPPED(2),
    ERROR(3),
    NOT_RUN(4);

    private ProcedureEndStatus(int value){
        this.value = value;
    }

    public int getValue(){
        return value;
    }

    private final int value;

    public static ProcedureEndStatus getInstance(int id){
        switch(id){
            case 0: return RUNNING;
            case 1: return COMPLETE;
            case 2: return USER_STOPPED;
            case 3: return ERROR;
            case 4: return NOT_RUN;
            default: return null;
        }
    }

    @Override
    public String toString(){
        switch (this){
            case RUNNING: return "Running";
            case COMPLETE: return "Complete";
            case USER_STOPPED: return "User stopped";
            case ERROR: return "Error";
            case NOT_RUN: return "Not run";
            default: return "UNKNOWN";
        }
    }
}
