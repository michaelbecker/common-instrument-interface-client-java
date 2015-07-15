package com.tainstruments.mercury.common_messages;


public enum ProcedureRunState {

    IDLE(0),
    PRETEST(1),
    TEST(2),
    POST_TEST(3);
    
    private ProcedureRunState(int value){
        this.value = value;
    }

    public int getValue(){
        return value;
    }

    private final int value;

    public static ProcedureRunState getInstance(int id){
        switch(id){
            case 0: return IDLE;
            case 1: return PRETEST;
            case 2: return TEST;
            case 3: return POST_TEST;
            default: return null;
        }
    }

    @Override
    public String toString(){
        switch (this){
            case IDLE: return "Idle";
            case PRETEST: return "Pretest";
            case TEST: return "Test";
            case POST_TEST: return "Post Test";
            default: return null;
        }
    }
}
