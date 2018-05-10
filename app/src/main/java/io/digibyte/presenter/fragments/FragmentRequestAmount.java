package io.digibyte.presenter.fragments;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.math.BigDecimal;

import io.digibyte.R;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.qrcode.QRUtils;
import io.digibyte.tools.util.BRConstants;
import io.digibyte.tools.util.BRCurrency;
import io.digibyte.tools.util.BRExchange;
import io.digibyte.tools.util.Utils;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class FragmentRequestAmount extends FragmentReceive {
    private static final String TAG = FragmentRequestAmount.class.getName();
    private StringBuilder amountBuilder = new StringBuilder(0);
    private String receiveAddress;
    private String selectedIso;

    public static void show(Activity activity) {
        FragmentRequestAmount fragmentRequestAmount = new FragmentRequestAmount();
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_RECEIVE, true);
        fragmentRequestAmount.setArguments(bundle);
        FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_bottom,
                R.animator.from_bottom, R.animator.to_bottom);
        transaction.add(android.R.id.content, fragmentRequestAmount,
                FragmentRequestAmount.class.getName());
        transaction.addToBackStack(FragmentRequestAmount.class.getName());
        transaction.commitAllowingStateLoss();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentReceiveRootView = super.onCreateView(inflater, container, savedInstanceState);
        selectedIso = BRSharedPrefs.getPreferredBTC(getContext()) ? "DGB" : BRSharedPrefs.getIso(getContext());
        receiveAddress = BRSharedPrefs.getReceiveAddress(getActivity());
        updateText();
        fragmentReceiveBinding.keyboardLayout.setVisibility(View.VISIBLE);
        fragmentReceiveBinding.amountLayout.setVisibility(View.VISIBLE);
        return fragmentReceiveRootView;
    }

    @Override
    protected void onAmountEditClick() {
        showKeyboard(true);

    }

    @Override
    protected void onKeyboardClick(String key) {
        handleClick(key);
    }

    @Override
    protected void onIsoButtonClick() {
        if (selectedIso.equalsIgnoreCase(BRSharedPrefs.getIso(getContext()))) {
            selectedIso = "DGB";
        } else {
            selectedIso = BRSharedPrefs.getIso(getContext());
        }
        boolean generated = generateQrImage(receiveAddress, amountBuilder.toString(),
                selectedIso);
        if (!generated) {
            throw new RuntimeException("failed to generate qr image for address");
        }
        updateText();
    }

    @Override
    protected boolean onShareEmail() {
        showKeyboard(false);
        BigDecimal bigAmount = new BigDecimal(
                (Utils.isNullOrEmpty(amountBuilder.toString()) || amountBuilder.toString().equalsIgnoreCase(".")) ? "0"
                        : amountBuilder.toString());
        long amount = BRExchange.getSatoshisFromAmount(getActivity(), selectedIso,
                bigAmount).longValue();
        String bitcoinUri = Utils.createBitcoinUrl(receiveAddress, amount, null, null,
                null);
        Uri qrImageUri = QRUtils.getQRImageUri(getContext(), bitcoinUri);
        QRUtils.share("mailto:", getActivity(), qrImageUri, null, null);
        return true;
    }

    @Override
    protected boolean onShareSMS() {
        showKeyboard(false);
        QRUtils.share("sms:", getActivity(), null, receiveAddress, amountBuilder.toString());
        return true;
    }

    @Override
    protected void onAddressClick() {
        super.onAddressClick();
        showKeyboard(false);
        showShareButtons(false);
    }

    @Override
    protected void onShareButtonClick() {
        super.onShareButtonClick();
        showKeyboard(false);
    }

    @Override
    protected void onQRClick() {
        super.onQRClick();
        showKeyboard(false);
        showShareButtons(false);
    }

    private void handleClick(String key) {
        if (key == null) {
            Log.e(TAG, "handleClick: key is null! ");
            return;
        }
        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key.substring(0, 1)));
        } else if (key.charAt(0) == '.') {
            handleSeparatorClick();
        }

        generateQrImage(receiveAddress, amountBuilder.toString(), selectedIso);
    }

    private void handleDigitClick(Integer dig) {
        String currAmount = amountBuilder.toString();
        String iso = selectedIso;
        if (new BigDecimal(currAmount.concat(String.valueOf(dig))).doubleValue()
                <= BRExchange.getMaxAmount(getActivity(), iso).doubleValue()) {
            //do not insert 0 if the balance is 0 now
            if (currAmount.equalsIgnoreCase("0")) amountBuilder = new StringBuilder("");
            if ((currAmount.contains(".") && (currAmount.length() - currAmount.indexOf(".")
                    > BRCurrency.getMaxDecimalPlaces(iso)))) {
                return;
            }
            amountBuilder.append(dig);
            updateText();
        }
    }

    private void handleSeparatorClick() {
        String currAmount = amountBuilder.toString();
        if (currAmount.contains(".") || BRCurrency.getMaxDecimalPlaces(selectedIso) == 0) {
            return;
        }
        amountBuilder.append(".");
        updateText();
    }

    private void handleDeleteClick() {
        String currAmount = amountBuilder.toString();
        if (currAmount.length() > 0) {
            amountBuilder.deleteCharAt(currAmount.length() - 1);
            updateText();
        }
    }

    private void updateText() {
        receiveFragmentModel.setAmount(amountBuilder.toString());
        receiveFragmentModel.setIsoText(BRCurrency.getSymbolByIso(getActivity(), selectedIso));
        receiveFragmentModel.setIsoButtonText(String.format("%s(%s)", BRCurrency.getCurrencyName(getActivity(), selectedIso), BRCurrency.getSymbolByIso(getActivity(), selectedIso)));
    }

    private void showKeyboard(boolean show) {
        if (!show) {
            receiveFragmentModel.setShowKeyboard(false);
        } else {
            if (!receiveFragmentModel.isShowKeyboard()) {
                receiveFragmentModel.setShowKeyboard(true);
                showShareButtons(false);
            }
        }
    }

    private boolean generateQrImage(String address, String strAmount, String iso) {
        String amountArg = "";
        if (strAmount != null && !strAmount.isEmpty()) {
            BigDecimal bigAmount = new BigDecimal(
                    (Utils.isNullOrEmpty(strAmount) || strAmount.equalsIgnoreCase(".")) ? "0"
                            : strAmount);
            long amount = BRExchange.getSatoshisFromAmount(getActivity(), iso,
                    bigAmount).longValue();
            String am = new BigDecimal(amount).divide(new BigDecimal(100000000), 8,
                    BRConstants.ROUNDING_MODE).toPlainString();
            amountArg = "?amount=" + am;
        }
        return QRUtils.generateQR(getActivity(), "digibyte:" + address + amountArg, fragmentReceiveBinding.qrImage);
    }

    @Override
    protected boolean allowRequestAmountButtonShow() {
        return false;
    }
}