package io.digibyte.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.WorkerThread;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import io.digibyte.presenter.activities.BreadActivity;
import io.digibyte.presenter.entities.TxItem;
import io.digibyte.tools.adapter.TransactionListAdapter;
import io.digibyte.tools.threads.BRExecutor;
import io.digibyte.wallet.BRPeerManager;
import io.digibyte.wallet.BRWalletManager;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 7/19/17.
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
public class TxManager
{
    public interface onStatusListener
    {
        void onTxManagerUpdate(TxItem[] aTransactionList);
    }

    private static final String TAG = TxManager.class.getName();
    private static TxManager instance;

    private ArrayList<onStatusListener> theListeners;
    public void addListener(onStatusListener aListener) { theListeners.add(aListener); }
    public void removeListener(onStatusListener aListener) { theListeners.remove(aListener); }

    public static TxManager getInstance()
    {
        if (instance == null)
        {
            instance = new TxManager();
        }
        return instance;
    }

    private TxManager()
    {
        theListeners = new ArrayList<>();
    }

    public void onResume(final Activity app)
    {
        crashIfNotMain();
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable()
        {
            @Override
            public void run()
            {
                final double progress = BRPeerManager.syncProgress(BRSharedPrefs.getStartHeight(app));
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (progress > 0 && progress < 1)
                        {
                            updateCard();
                        }
                        else
                        {
                            showNextPrompt(app);
                        }
                    }
                });
            }
        });
    }

    @WorkerThread
    public synchronized void updateTxList()
    {
        final TxItem[] transactions = BRWalletManager.getInstance().getTransactions();
        new Handler(Looper.getMainLooper()).post(new Runnable()
        {
            @Override
            public void run()
            {
                for (onStatusListener listener : theListeners)
                {
                    listener.onTxManagerUpdate(transactions);
                }
            }
        });
    }


















    public void showPrompt(Activity app, PromptManager.PromptItem item)
    {
        crashIfNotMain();
        if (item == null)
        {
            throw new RuntimeException("can't be null");
        }
        BREventManager.getInstance().pushEvent("prompt." + PromptManager.getInstance().getPromptName(item) + ".displayed");
        updateCard();
    }

    public void hidePrompt(final Activity app, final PromptManager.PromptItem item)
    {
        crashIfNotMain();
        if (item == PromptManager.PromptItem.SYNCING)
        {
            showNextPrompt(app);
            updateCard();
        }
        else
        {
            if (item != null)
            {
                BREventManager.getInstance().pushEvent("prompt." + PromptManager.getInstance().getPromptName(item) + ".dismissed");
            }

        }
    }

    private void showNextPrompt(Activity app)
    {
        crashIfNotMain();
        PromptManager.PromptItem toShow = PromptManager.getInstance().nextPrompt(app);
        if (toShow != null)
        {
            updateCard();
        }
        else
        {
            Log.i(TAG, "showNextPrompt: nothing to show");
        }
    }

    public void updateCard()
    {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable()
        {
            @Override
            public void run()
            {
                updateTxList();
            }
        });
    }

    private void setupSwipe(final Activity app)
    {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT)
        {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target)
            {
                //                Toast.makeText(BreadActivity.this, "on Move ", Toast.LENGTH_SHORT).show();
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir)
            {
                hidePrompt(app, null);
                //Remove swiped item from list and notify the RecyclerView
            }

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder)
            {
                if (!(viewHolder instanceof TransactionListAdapter.PromptHolder))
                {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        // TODO: FIx this
        //itemTouchHelper.attachToRecyclerView(txList);
    }


    private class CustomLinearLayoutManager extends LinearLayoutManager
    {

        public CustomLinearLayoutManager(Context context)
        {
            super(context);
        }

        /**
         * Disable predictive animations. There is a bug in RecyclerView which causes views that
         * are being reloaded to pull invalid ViewHolders from the internal recycler stack if the
         * adapter size has decreased since the ViewHolder was recycled.
         */
        @Override
        public boolean supportsPredictiveItemAnimations()
        {
            return false;
        }

        public CustomLinearLayoutManager(Context context, int orientation, boolean reverseLayout)
        {
            super(context, orientation, reverseLayout);
        }

        public CustomLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
        {
            super(context, attrs, defStyleAttr, defStyleRes);
        }
    }

    private void crashIfNotMain()
    {
        if (Looper.myLooper() != Looper.getMainLooper())
        {
            throw new IllegalAccessError("Can only call from main thread");
        }
    }

}
