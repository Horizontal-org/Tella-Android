package org.hzontal.tella.keys.wrapper;

public class UnencryptedKeyWrapper extends PBEKeyWrapper {
    @Override
    public String getName() {
        return UnencryptedKeyWrapper.class.getName();
    }
}
