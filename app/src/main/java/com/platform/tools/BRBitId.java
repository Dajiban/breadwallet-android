package com.platform.tools;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.jniwrappers.BRBIP32Sequence;
import com.jniwrappers.BRKey;
import com.platform.APIClient;

import junit.framework.Assert;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.digibyte.R;
import io.digibyte.presenter.interfaces.BRAuthCompletion;
import io.digibyte.tools.security.AuthManager;
import io.digibyte.tools.security.BRKeyStore;
import io.digibyte.tools.threads.BRExecutor;
import io.digibyte.tools.util.BRConstants;
import io.digibyte.tools.util.TypesConverter;
import io.digibyte.wallet.BRWalletManager;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 1/25/17.
 * Copyright (c) 2017 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class BRBitId {
    public static final String TAG = BRBitId.class.getName();
    public static final String BITCOIN_SIGNED_MESSAGE_HEADER = "DigiByte Signed Message:\n";

    public static boolean isBitId(String uri) {
        try {
            URI bitIdUri = new URI(uri);
            if ("digiid".equalsIgnoreCase(bitIdUri.getScheme())) {
                return true;
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void signAndRespond(@NonNull final Activity app, @NonNull final String bitID,
            boolean isDeepLink) {
        Uri bitUri = Uri.parse(bitID);
        String u = bitUri.getQueryParameter("u");
        String scheme = u != null && u.equalsIgnoreCase("1") ? "http://" : "https://";
        AuthManager.getInstance().authPrompt(app, null,
                app.getString(R.string.VerifyPin_continueBody) + "\n\n" + scheme + bitUri.getHost(), new BRAuthCompletion() {
                    @Override
                    public void onComplete() {
                        internalSignAndRespond(app, bitID, isDeepLink, scheme + bitUri.getHost() + bitUri.getPath());
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    private static void internalSignAndRespond(@NonNull final Activity app, @NonNull final String bitID,
            boolean isDeepLink, String callbackUrl) {
        try {
            byte[] phrase = BRKeyStore.getPhrase(app, BRConstants.REQUEST_PHRASE_BITID);
            byte[] nulTermPhrase = TypesConverter.getNullTerminatedPhrase(phrase);
            byte[] seed = BRWalletManager.getSeedFromPhrase(nulTermPhrase);
            final byte[] key = BRBIP32Sequence.getInstance().bip32BitIDKey(seed, 0, callbackUrl);
            final String sig = signMessage(bitID, new BRKey(key));
            final String address = new BRKey(key).address();
            final JSONObject postJson = new JSONObject();
            postJson.put("uri", bitID);
            postJson.put("address", address);
            postJson.put("signature", sig);
            final RequestBody requestBody = RequestBody.create(null, postJson.toString());
            final Request request = new Request.Builder()
                    .url(callbackUrl)
                    .post(requestBody)
                    .header("Content-Type", "application/json")
                    .build();
            Handler handler = new Handler(Looper.getMainLooper());
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
                final Response res = APIClient.getInstance(app).sendRequest(request);
                Log.d(BRBitId.class.getSimpleName(),
                        "Response: " + res.code() + ", Message: " + res.message());
                if (res.code() == 200) {
                    handler.post(
                            () -> Toast.makeText(app, "DigiID Success", Toast.LENGTH_SHORT).show());
                } else {
                    handler.post(() -> Toast.makeText(app,
                            app.getString(R.string.Import_Error_signing) + ": " + res.message(),
                            Toast.LENGTH_SHORT).show());
                }
                if (isDeepLink) {
                    app.finishAffinity();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> Toast.makeText(app, app.getString(R.string.Import_Error_signing), Toast.LENGTH_SHORT).show());
            if (isDeepLink) {
                app.finishAffinity();
            }
        }
    }

    public static String signMessage(String message, BRKey key) {
        byte[] signingData = formatMessageForBitcoinSigning(message);

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        byte[] sha256First = digest.digest(signingData);
        byte[] sha256Second = digest.digest(sha256First);
        byte[] signature = key.compactSign(sha256Second);

        return Base64.encodeToString(signature, Base64.NO_WRAP);
    }

    private static byte[] formatMessageForBitcoinSigning(String message) {
        byte[] headerBytes = null;
        byte[] messageBytes = null;

        try {
            headerBytes = BITCOIN_SIGNED_MESSAGE_HEADER.getBytes("UTF-8");
            messageBytes = message.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        assert (headerBytes != null);
        assert (messageBytes != null);
        if (headerBytes == null || messageBytes == null) return new byte[0];

        int cap = 1 + headerBytes.length + varIntSize(messageBytes.length) + messageBytes.length;

        ByteBuffer dataBuffer = ByteBuffer.allocate(cap).order(ByteOrder.LITTLE_ENDIAN);
        dataBuffer.put((byte) headerBytes.length);          //put header count
        dataBuffer.put(headerBytes);                        //put the header
        putVarInt(message.length(), dataBuffer);            //put message count
        dataBuffer.put(messageBytes);                       //put the message
        byte[] result = dataBuffer.array();

        Assert.assertEquals(cap, result.length);

        return result;
    }

    /**
     * Returns the encoding size in bytes of its input value.
     *
     * @param i the integer to be measured
     * @return the encoding size in bytes of its input value
     */
    private static int varIntSize(int i) {
        int result = 0;
        do {
            result++;
            i >>>= 7;
        } while (i != 0);
        return result;
    }

    /**
     * Encodes an integer in a variable-length encoding, 7 bits per byte, to a
     * ByteBuffer sink.
     *
     * @param v    the value to encode
     * @param sink the ByteBuffer to add the encoded value
     */
    private static void putVarInt(int v, ByteBuffer sink) {
        while (true) {
            int bits = v & 0x7f;
            v >>>= 7;
            if (v == 0) {
                sink.put((byte) bits);
                return;
            }
            sink.put((byte) (bits | 0x80));
        }
    }
}
