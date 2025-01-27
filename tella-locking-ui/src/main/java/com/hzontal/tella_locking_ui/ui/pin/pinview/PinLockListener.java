package com.hzontal.tella_locking_ui.ui.pin.pinview;


public interface PinLockListener {

    /**
     * Triggers when the complete pin is entered,
     * depends on the pin length set by the user
     *
     * @param pin the complete pin
     */
    void onPinConfirmation(String pin);


    /**
     * Triggers when the pin is empty after manual deletion
     */
    void onEmpty();

    /**
     * Triggers on a key press on the {@link PinLockView}
     *
     * @param pinLength       the current pin length
     * @param intermediatePin the intermediate pin
     */
    void onPinChange(int pinLength, String intermediatePin);
}
