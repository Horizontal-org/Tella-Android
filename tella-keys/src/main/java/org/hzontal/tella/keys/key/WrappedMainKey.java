package org.hzontal.tella.keys.key;

public class WrappedMainKey {
    public String wrapperName;
    public byte[] iv;
    public byte[] data;
    public byte[] salt;
    public int iterationCount;

    public WrappedMainKey(String wrapperName) {
        this.wrapperName = wrapperName;
        this.iv = new byte[0];
        this.data = new byte[0];
        this.salt = new byte[0];
        this.iterationCount = 0;
    }

    public String getWrapperName() {
        return wrapperName;
    }
}
