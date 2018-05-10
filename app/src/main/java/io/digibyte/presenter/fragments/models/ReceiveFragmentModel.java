package io.digibyte.presenter.fragments.models;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import io.digibyte.BR;
import io.digibyte.R;

public class ReceiveFragmentModel extends BaseObservable {

    private boolean shareVisibility;
    private int shareButtonType = 2;
    private boolean copiedButtonLayoutVisibility;
    private boolean separatorVisibility;
    private boolean requestButtonVisibility;
    private String title;
    private String address;
    private boolean showKeyboard;
    private String amount;
    private String isoText;
    private String isoButtonText;

    @Bindable
    public boolean isShareVisibility() {
        return shareVisibility;
    }

    public void setShareVisibility(boolean shareVisibility) {
        this.shareVisibility = shareVisibility;
        notifyPropertyChanged(BR.shareVisibility);
    }

    @Bindable
    public int getShareButtonType() {
        return shareButtonType;
    }

    public void setShareButtonType(int shareButtonType) {
        this.shareButtonType = shareButtonType;
        notifyPropertyChanged(BR.shareButtonType);
    }

    @Bindable
    public boolean isCopiedButtonLayoutVisibility() {
        return copiedButtonLayoutVisibility;
    }

    public void setCopiedButtonLayoutVisibility(boolean copiedButtonLayoutVisibility) {
        this.copiedButtonLayoutVisibility = copiedButtonLayoutVisibility;
        notifyPropertyChanged(BR.copiedButtonLayoutVisibility);
    }

    @Bindable
    public boolean isSeparatorVisibility() {
        return separatorVisibility;
    }

    public void setSeparatorVisibility(boolean separatorVisibility) {
        this.separatorVisibility = separatorVisibility;
        notifyPropertyChanged(BR.separatorVisibility);
    }

    @Bindable
    public boolean isRequestButtonVisibility() {
        return requestButtonVisibility;
    }

    public void setRequestButtonVisibility(boolean requestButtonVisibility) {
        this.requestButtonVisibility = requestButtonVisibility;
        notifyPropertyChanged(BR.requestButtonVisibility);
    }

    @Bindable
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        notifyPropertyChanged(BR.title);
    }

    @Bindable
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        notifyPropertyChanged(BR.address);
    }

    @Bindable
    public boolean isShowKeyboard() {
        return showKeyboard;
    }

    public void setShowKeyboard(boolean showKeyboard) {
        this.showKeyboard = showKeyboard;
        notifyPropertyChanged(BR.showKeyboard);
    }

    @Bindable
    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
        notifyPropertyChanged(BR.amount);
    }

    @Bindable
    public String getIsoText() {
        return isoText;
    }

    public void setIsoText(String isoText) {
        this.isoText = isoText;
        notifyPropertyChanged(BR.isoText);
    }

    @Bindable
    public String getIsoButtonText() {
        return isoButtonText;
    }

    public void setIsoButtonText(String isoButtonText) {
        this.isoButtonText = isoButtonText;
        notifyPropertyChanged(BR.isoButtonText);
    }

    @Bindable
    public int getBRButtonBackgroundRedId() {
        return R.drawable.keyboard_white_button;
    }

    @Bindable
    public int getBRKeyboardColor() {
        return R.color.white;
    }
}