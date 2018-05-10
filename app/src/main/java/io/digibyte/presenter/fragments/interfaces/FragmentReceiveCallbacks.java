package io.digibyte.presenter.fragments.interfaces;

import io.digibyte.presenter.customviews.BRKeyboard;

public interface FragmentReceiveCallbacks extends BRKeyboard.OnInsertListener {

    void shareEmailClick();
    void shareTextClick();
    void shareButtonClick();
    void addressClick();
    void requestButtonClick();
    void backgroundClick();
    void qrImageClick();
    void closeClick();

    void onAmountEditClick();
    void onIsoButtonClick();
}
