package io.digibyte.presenter.fragments.models;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;

import java.math.BigDecimal;

import io.digibyte.BR;
import io.digibyte.DigiByte;
import io.digibyte.R;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.util.BRCurrency;
import io.digibyte.tools.util.BRExchange;
import io.digibyte.wallet.BRWalletManager;

public class SendFragmentModel extends BaseObservable {

    private StringBuilder amountBuilder = new StringBuilder("");
    private String selectedIso = BRSharedPrefs.getPreferredBTC(DigiByte.getContext()) ? "DGB"
            : BRSharedPrefs.getIso(DigiByte.getContext());
    private String enteredAddress = "";
    private FeeType feeType = FeeType.REGULAR;
    private String memo = "";
    private boolean showSendWaiting = false;

    enum FeeType {
        REGULAR, ECONOMY
    }

    @Bindable
    public int getBRButtonBackgroundRedId() {
        return R.drawable.keyboard_white_button;
    }

    @Bindable
    public int getBRKeyboardColor() {
        return R.color.white;
    }

    @Bindable
    public String getIsoText() {
        return BRCurrency.getSymbolByIso(DigiByte.getContext(), getSelectedIso());
    }

    @Bindable
    public String getIsoButtonText() {
        return String.format("%s(%s)",
                BRCurrency.getCurrencyName(DigiByte.getContext(), getSelectedIso()),
                BRCurrency.getSymbolByIso(DigiByte.getContext(), getSelectedIso()));
    }

    @Bindable
    public String getAddress() {
        return enteredAddress;
    }

    public void setAddress(String address) {
        this.enteredAddress = address;
        notifyPropertyChanged(BR.address);
        updateFeeButtons();
    }

    @Bindable
    public int getBalanceTextColor() {
        if (validAmount() && new BigDecimal(getAmount()).doubleValue()
                > getBalanceForISO().doubleValue()) {
            return DigiByte.getContext().getColor(R.color.warning_color);
        } else {
            return DigiByte.getContext().getColor(R.color.light_gray);
        }
    }

    @Bindable
    public int getFeeTextColor() {
        if (validAmount() && new BigDecimal(getAmount()).doubleValue()
                > getBalanceForISO().doubleValue()) {
            return DigiByte.getContext().getColor(R.color.warning_color);
        } else {
            return DigiByte.getContext().getColor(R.color.light_gray);
        }
    }

    @Bindable
    public int getAmountEditTextColor() {
        if (validAmount() && new BigDecimal(getAmount()).doubleValue()
                > getBalanceForISO().doubleValue()) {
            return DigiByte.getContext().getColor(R.color.warning_color);
        } else {
            return DigiByte.getContext().getColor(R.color.light_gray);
        }
    }

    @Bindable
    public int getISOTextColor() {
        if (validAmount() && new BigDecimal(getAmount()).doubleValue()
                > getBalanceForISO().doubleValue()) {
            return DigiByte.getContext().getColor(R.color.warning_color);
        } else {
            return DigiByte.getContext().getColor(R.color.almost_black);
        }
    }

    @Bindable
    public String getBalanceText() {
        return String.format("%s", getFormattedBalance());
    }

    @Bindable
    public String getFeeText() {
        return String.format(DigiByte.getContext().getString(R.string.Send_fee),
                getApproximateFee());
    }

    @Bindable
    public String getAmount() {
        return amountBuilder.toString();
    }

    @Bindable
    public int getRegularFeeTextColor() {
        switch (feeType) {
            default:
            case REGULAR:
                return DigiByte.getContext().getColor(R.color.white);
            case ECONOMY:
                return DigiByte.getContext().getColor(R.color.dark_blue);
        }
    }

    @Bindable
    public Drawable getRegualarFeeBackground() {
        switch (feeType) {
            default:
            case REGULAR:
                return DigiByte.getContext().getDrawable(R.drawable.b_half_left_blue);
            case ECONOMY:
                return DigiByte.getContext().getDrawable(R.drawable.b_half_left_blue_stroke);
        }
    }

    @Bindable
    public int getEconomyTextColor() {
        switch (feeType) {
            default:
            case REGULAR:
                return DigiByte.getContext().getColor(R.color.dark_blue);
            case ECONOMY:
                return DigiByte.getContext().getColor(R.color.white);
        }
    }

    @Bindable
    public Drawable getEconomyBackground() {
        switch (feeType) {
            default:
            case REGULAR:
                return DigiByte.getContext().getDrawable(R.drawable.b_half_right_blue_stroke);
            case ECONOMY:
                return DigiByte.getContext().getDrawable(R.drawable.b_half_right_blue);
        }
    }

    @Bindable
    public String getFeeDescription() {
        switch (feeType) {
            default:
            case REGULAR:
                return String.format(
                        DigiByte.getContext().getString(R.string.FeeSelector_estimatedDeliver),
                        DigiByte.getContext().getString(R.string.FeeSelector_regularTime));
            case ECONOMY:
                return String.format(
                        DigiByte.getContext().getString(R.string.FeeSelector_estimatedDeliver),
                        DigiByte.getContext().getString(R.string.FeeSelector_economyTime));
        }
    }

    @Bindable
    public int getWarningVisibility() {
        return feeType == FeeType.ECONOMY ? View.VISIBLE : View.GONE;
    }

    @Bindable
    public String getMemo() {
        return memo;
    }

    @Bindable
    public int getShowSendWaiting() {
        return showSendWaiting ? 1 : 0;
    }

    public void showSendWaiting(boolean show) {
        showSendWaiting = show;
        notifyPropertyChanged(BR.showSendWaiting);
    }

    public void setMemo(String memo) {
        this.memo = memo;
        notifyPropertyChanged(BR.memo);
    }

    public void setSelectedIso(String iso) {
        selectedIso = iso;
    }

    public String getSelectedIso() {
        return selectedIso;
    }

    private boolean validAmount() {
        try {
            new BigDecimal(getAmount());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void appendAmount(Integer append) {
        amountBuilder.append(append);
    }

    public void appendAmount(String append) {
        amountBuilder.append(append);
    }

    public void setAmount(StringBuilder amount) {
        amountBuilder = amount;
    }

    public void handleDeleteClick() {
        if (!validAmount()) {
            return;
        }
        amountBuilder.deleteCharAt(getAmount().length() - 1);
    }

    private long getFee() {
        if (getSatoshis() == 0 || TextUtils.isEmpty(getAddress())) {
            return 0;
        }
        if (TextUtils.isEmpty(getAddress())) {
            return BRWalletManager.getInstance().feeForTransactionAmount(getSatoshis());
        } else {
            return BRWalletManager.getInstance().feeForTransaction(getAddress(), getSatoshis());
        }
    }

    private BigDecimal getFeeForISO() {
        return BRExchange.getAmountFromSatoshis(DigiByte.getContext(), getSelectedIso(),
                new BigDecimal(getFee()));
    }

    private String getApproximateFee() {
        return BRCurrency.getFormattedCurrencyString(DigiByte.getContext(), getSelectedIso(),
                getFeeForISO());
    }

    private long getSatoshis() {
        if (!validAmount()) {
            return 0;
        }
        long satoshis = selectedIso.equalsIgnoreCase("dgb") ? BRExchange.getSatoshisForBitcoin(
                DigiByte.getContext(), new BigDecimal(getAmount())).longValue()
                : BRExchange.getSatoshisFromAmount(DigiByte.getContext(), getSelectedIso(),
                        new BigDecimal(getAmount())).longValue();
        return satoshis;
    }

    private BigDecimal getBalanceForISO() {
        return BRExchange.getAmountFromSatoshis(DigiByte.getContext(), getSelectedIso(),
                new BigDecimal(BRWalletManager.getInstance().getBalance(DigiByte.getContext())));
    }

    private String getFormattedBalance() {
        return BRCurrency.getFormattedCurrencyString(DigiByte.getContext(), getSelectedIso(),
                getBalanceForISO());
    }

    public void updateText() {
        notifyPropertyChanged(BR.isoButtonText);
        notifyPropertyChanged(BR.amount);
        notifyPropertyChanged(BR.isoText);
        notifyPropertyChanged(BR.balanceText);
        notifyPropertyChanged(BR.feeText);
        notifyPropertyChanged(BR.balanceTextColor);
        notifyPropertyChanged(BR.feeTextColor);
        notifyPropertyChanged(BR.amountEditTextColor);
        notifyPropertyChanged(BR.iSOTextColor);
    }

    public void updateFeeButtons() {
        updateFeeButtons(feeType == FeeType.REGULAR);
    }

    public void updateFeeButtons(boolean isRegular) {
        feeType = isRegular ? FeeType.REGULAR : FeeType.ECONOMY;
        switch (feeType) {
            case REGULAR:
                BRWalletManager.getInstance().setFeePerKb(
                        BRSharedPrefs.getFeePerKb(DigiByte.getContext()), false);
                break;
            case ECONOMY:
                BRWalletManager.getInstance().setFeePerKb(
                        BRSharedPrefs.getEconomyFeePerKb(DigiByte.getContext()), false);
                break;
        }
        notifyPropertyChanged(BR.feeDescription);
        notifyPropertyChanged(BR.economyBackground);
        notifyPropertyChanged(BR.economyTextColor);
        notifyPropertyChanged(BR.regualarFeeBackground);
        notifyPropertyChanged(BR.regularFeeTextColor);
        notifyPropertyChanged(BR.warningVisibility);
        updateText();
    }
}