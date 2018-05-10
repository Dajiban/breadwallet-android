package io.digibyte.presenter.fragments;

import static io.digibyte.tools.security.BitcoinUrlHandler.getRequestFromString;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import java.math.BigDecimal;

import io.digibyte.R;
import io.digibyte.databinding.FragmentSendBinding;
import io.digibyte.presenter.customviews.BRDialogView;
import io.digibyte.presenter.entities.PaymentItem;
import io.digibyte.presenter.entities.RequestObject;
import io.digibyte.presenter.fragments.interfaces.FragmentSendCallbacks;
import io.digibyte.presenter.fragments.interfaces.OnBackPressListener;
import io.digibyte.presenter.fragments.models.SendFragmentModel;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.animation.BRDialog;
import io.digibyte.tools.animation.SpringAnimator;
import io.digibyte.tools.manager.BRClipboardManager;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.security.BRSender;
import io.digibyte.tools.security.BitcoinUrlHandler;
import io.digibyte.tools.threads.BRExecutor;
import io.digibyte.tools.util.BRConstants;
import io.digibyte.tools.util.BRCurrency;
import io.digibyte.tools.util.BRExchange;
import io.digibyte.tools.util.Utils;
import io.digibyte.wallet.BRWalletManager;


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

public class FragmentSend extends Fragment implements OnBackPressListener {
    private static final String TAG = FragmentSend.class.getName();
    private FragmentSendBinding binding;
    private SendFragmentModel sendFragmentModel;

    private FragmentSendCallbacks fragmentSendCallbacks = new FragmentSendCallbacks() {

        @Override
        public void onAmountClickListener() {
            showKeyboard(true);
            binding.balanceContainer.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPasteClickListener() {
            if (!BRAnimator.isClickAllowed()) return;
            String bitcoinUrl = BRClipboardManager.getClipboard(getActivity());
            if (Utils.isNullOrEmpty(bitcoinUrl) || !isInputValid(bitcoinUrl)) {
                showClipboardError();
                return;
            }
            String address = null;

            RequestObject obj = getRequestFromString(bitcoinUrl);

            if (obj == null || obj.address == null) {
                showClipboardError();
                return;
            }
            address = obj.address;
            final BRWalletManager wm = BRWalletManager.getInstance();

            if (BRWalletManager.validateAddress(address)) {
                final String finalAddress = address;
                final Activity app = getActivity();
                if (app == null) {
                    Log.e(TAG, "paste onClick: app is null");
                    return;
                }
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(
                        () -> {
                            if (wm.addressContainedInWallet(finalAddress)) {
                                app.runOnUiThread(() -> {
                                    BRDialog.showCustomDialog(getActivity(), "",
                                            getResources().getString(R.string.Send_containsAddress),
                                            getResources().getString(
                                                    R.string.AccessibilityLabels_close),
                                            null, brDialogView -> brDialogView.dismiss(), null,
                                            null, 0);
                                    BRClipboardManager.putClipboard(getActivity(), "");
                                });

                            } else if (wm.addressIsUsed(finalAddress)) {
                                app.runOnUiThread(() -> BRDialog.showCustomDialog(getActivity(),
                                        getString(R.string.Send_UsedAddress_firstLine),
                                        getString(R.string.Send_UsedAddress_secondLIne), "Ignore",
                                        "Cancel",
                                        brDialogView -> {
                                            brDialogView.dismiss();
                                            sendFragmentModel.setAddress(finalAddress);
                                        }, brDialogView -> brDialogView.dismiss(), null, 0));

                            } else {
                                app.runOnUiThread(() -> sendFragmentModel.setAddress(finalAddress));
                            }
                        });

            } else {
                showClipboardError();
            }
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode
                    == KeyEvent.KEYCODE_ENTER)) {
                binding.sendButton.performClick();
            }
            return false;
        }

        @Override
        public void onIsoButtonClickListener() {
            if (sendFragmentModel.getSelectedIso().equalsIgnoreCase(
                    BRSharedPrefs.getIso(getContext()))) {
                sendFragmentModel.setSelectedIso("DGB");
            } else {
                sendFragmentModel.setSelectedIso(BRSharedPrefs.getIso(getContext()));
            }
            sendFragmentModel.updateText();
        }

        @Override
        public void onScanClickListener() {
            BRAnimator.openScanner(getActivity(), BRConstants.SCANNER_REQUEST);
        }

        @Override
        public void onSendClickListener() {
            sendFragmentModel.showSendWaiting(true);
            boolean allFilled = true;

            String address = sendFragmentModel.getAddress();
            String amountStr = sendFragmentModel.getAmount();
            String iso = sendFragmentModel.getSelectedIso();
            String comment = sendFragmentModel.getMemo();

            //get amount in satoshis from any isos
            BigDecimal bigAmount = new BigDecimal(
                    Utils.isNullOrEmpty(sendFragmentModel.getAmount()) ? "0"
                            : sendFragmentModel.getAmount());
            BigDecimal satoshiAmount = BRExchange.getSatoshisFromAmount(getActivity(), iso,
                    bigAmount);

            if (address.isEmpty() || !BRWalletManager.validateAddress(address)) {
                allFilled = false;
                Activity app = getActivity();
                BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error),
                        app.getString(R.string.Send_noAddress),
                        app.getString(R.string.AccessibilityLabels_close), null,
                        new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismissWithAnimation();
                            }
                        }, null, null, 0);
            }
            if (satoshiAmount.doubleValue() < 1) {
                allFilled = false;
                SpringAnimator.failShakeAnimation(getActivity(), binding.amountEdit);
            }
            if (satoshiAmount.longValue() > BRWalletManager.getInstance().getBalance(
                    getActivity())) {
                allFilled = false;
                SpringAnimator.failShakeAnimation(getActivity(), binding.balanceText);
                SpringAnimator.failShakeAnimation(getActivity(), binding.feeText);
            }

            if (allFilled) {
                BRSender.getInstance().sendTransaction(getContext(),
                        new PaymentItem(new String[]{address}, null, satoshiAmount.longValue(),
                                null, false, comment),
                        () -> sendFragmentModel.showSendWaiting(false));
            } else {
                sendFragmentModel.showSendWaiting(false);
            }
        }

        @Override
        public void onCloseClickListener() {
            fadeOutRemove();
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId
                    == EditorInfo.IME_ACTION_DONE) || (actionId
                    == EditorInfo.IME_ACTION_NEXT)) {
                Utils.hideKeyboard(getActivity());
                new Handler().postDelayed(() -> showKeyboard(true), 500);
                return true;
            }
            return false;
        }

        @Override
        public void onRegularClickListener() {
            sendFragmentModel.updateFeeButtons(true);
        }

        @Override
        public void onClick(String key) {
            handleClick(key);
        }

        @Override
        public void onEconomyClickListener() {
            sendFragmentModel.updateFeeButtons(false);
        }

        @Override
        public void onEditClickListener() {
            toggleFeeSelectionButtons();
        }
    };

    public static void show(Activity app, String bitcoinUrl) {
        FragmentSend fragmentSend = new FragmentSend();
        if (bitcoinUrl != null && !bitcoinUrl.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString("url", bitcoinUrl);
            fragmentSend.setArguments(bundle);
        }
        FragmentTransaction transaction = app.getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_bottom,
                R.animator.from_bottom, R.animator.to_bottom);
        transaction.add(android.R.id.content, fragmentSend, FragmentSend.class.getName());
        transaction.addToBackStack(FragmentSend.class.getName());
        transaction.commitAllowingStateLoss();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentSendBinding.inflate(inflater);
        binding.setCallback(fragmentSendCallbacks);
        binding.feeButtonsLayout.getLayoutTransition().enableTransitionType(
                LayoutTransition.CHANGING);
        binding.signalLayout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        sendFragmentModel = new SendFragmentModel();
        binding.setData(sendFragmentModel);
        sendFragmentModel.updateFeeButtons(true);
        if (getArguments() != null && getArguments()
                .getString("url") != null) {
            setUrl(getArguments().getString("url"));
        }
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(binding.background, false, null);
        colorFade.setStartDelay(350);
        colorFade.setDuration(500);
        colorFade.start();
    }

    private void showKeyboard(boolean show) {
        if (!show) {
            binding.keyboardLayout.setVisibility(View.GONE);
        } else {
            Utils.hideKeyboard(getActivity());
            binding.feeButtonsLayout.setVisibility(View.GONE);
            binding.keyboardLayout.setVisibility(View.VISIBLE);
        }
    }

    private void showClipboardError() {
        BRDialog.showCustomDialog(getActivity(), getString(R.string.Send_emptyPasteboard),
                getResources().getString(R.string.Send_invalidAddressTitle),
                getString(R.string.AccessibilityLabels_close), null,
                brDialogView -> brDialogView.dismiss(), null, null, 0);
        BRClipboardManager.putClipboard(getActivity(), "");
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
    }

    private void handleDigitClick(Integer dig) {
        String currAmount = sendFragmentModel.getAmount();
        String iso = sendFragmentModel.getSelectedIso();
        if (new BigDecimal(currAmount.concat(String.valueOf(dig))).doubleValue()
                <= BRExchange.getMaxAmount(getActivity(), iso).doubleValue()) {
            //do not insert 0 if the balance is 0 now
            if ((currAmount.contains(".") && (currAmount.length() - currAmount.indexOf(".")
                    > BRCurrency.getMaxDecimalPlaces(iso)))) {
                return;
            }
            sendFragmentModel.appendAmount(dig);
            sendFragmentModel.updateText();
        }
    }

    private void handleSeparatorClick() {
        if (sendFragmentModel.getAmount().contains(".") || BRCurrency.getMaxDecimalPlaces(
                sendFragmentModel.getSelectedIso()) == 0) {
            return;
        }
        sendFragmentModel.appendAmount(".");
        sendFragmentModel.updateText();
    }

    private void handleDeleteClick() {
        if (sendFragmentModel.getAmount().length() > 0) {
            sendFragmentModel.handleDeleteClick();
            sendFragmentModel.updateText();
        }
    }

    private void setUrl(String url) {
        RequestObject obj = BitcoinUrlHandler.getRequestFromString(url);
        if (obj == null) return;
        if (obj.address != null) {
            sendFragmentModel.setAddress(obj.address.trim());
        }
        if (obj.message != null) {
            sendFragmentModel.setMemo(obj.message);
        }
        if (obj.amount != null) {
            BigDecimal satoshiAmount = new BigDecimal(obj.amount);
            sendFragmentModel.setAmount(new StringBuilder(
                    BRExchange.getAmountFromSatoshis(getActivity(),
                            sendFragmentModel.getSelectedIso(), satoshiAmount).toPlainString()));
            sendFragmentModel.updateText();
        }
    }

    private void toggleFeeSelectionButtons() {
        binding.keyboardLayout.setVisibility(View.GONE);
        binding.feeButtonsLayout.setVisibility(
                binding.feeButtonsLayout.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
    }

    private boolean isInputValid(String input) {
        return input.matches("[a-zA-Z0-9]*");
    }

    private void fadeOutRemove() {
        ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(binding.background, true, () -> {
            remove();
        });
        colorFade.start();
    }

    private void remove() {
        if (getFragmentManager() == null) {
            return;
        }
        getFragmentManager().popBackStack();
    }

    @Override
    public void onBackPressed() {
        if (binding.keyboardLayout.getVisibility() == View.VISIBLE) {
            showKeyboard(false);
        } else if (binding.feeButtonsLayout.getVisibility() == View.VISIBLE) {
            toggleFeeSelectionButtons();
        } else {
            fadeOutRemove();
        }
    }
}