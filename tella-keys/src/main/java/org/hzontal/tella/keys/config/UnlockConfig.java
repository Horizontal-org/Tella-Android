package org.hzontal.tella.keys.config;

import org.hzontal.tella.keys.wrapper.IMainKeyWrapper;

public class UnlockConfig {
    public IUnlocker unLocker;
    public IMainKeyWrapper wrapper;

    public UnlockConfig(IUnlocker unLocker, IMainKeyWrapper wrapper) {
        this.unLocker = unLocker;
        this.wrapper = wrapper;
    }
}
