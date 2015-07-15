package com.tainstruments.mercury.common_messages;


/**
 * Immutable object describing a software version for a node on the instrument.
 * Since this is immutable, we don't bother with accessors. This format
 * is used for both the Cortex and Arm7s.
 */
public final class SoftwareVersion {

    public final int major;
    public final int minor;
    public final int release;
    public final int build;

    public SoftwareVersion(int major, int minor, int release, int build) {
        this.major = major;
        this.minor = minor;
        this.release = release;
        this.build = build;
    }

    @Override
    public String toString(){
        StringBuilder s = new StringBuilder();
        s.append(major);
        s.append(".");
        s.append(minor);
        s.append(".");
        s.append(release);
        s.append(".");
        s.append(build);
        return s.toString();
    }
}

