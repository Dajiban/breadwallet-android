package io.digibyte.presenter.activities;

import android.animation.LayoutTransition;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.transition.TransitionManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.platform.APIClient;

import java.math.BigDecimal;
import java.util.ArrayList;

import io.digibyte.R;
import io.digibyte.presenter.activities.util.BRActivity;
import io.digibyte.presenter.customviews.BRSearchBar;
import io.digibyte.presenter.entities.TxItem;
import io.digibyte.presenter.fragments.FragmentManage;
import io.digibyte.tools.adapter.TransactionListAdapter;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.list.ListItemData;
import io.digibyte.tools.list.items.ListItemPromptData;
import io.digibyte.tools.list.items.ListItemSyncingData;
import io.digibyte.tools.list.items.ListItemTransactionData;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.manager.InternetManager;
import io.digibyte.tools.manager.SyncManager;
import io.digibyte.tools.manager.SyncService;
import io.digibyte.tools.manager.TxManager;
import io.digibyte.tools.manager.TxManager.onStatusListener;
import io.digibyte.tools.security.BitcoinUrlHandler;
import io.digibyte.tools.sqlite.TransactionDataSource;
import io.digibyte.tools.threads.BRExecutor;
import io.digibyte.tools.util.BRConstants;
import io.digibyte.tools.util.BRCurrency;
import io.digibyte.tools.util.BRExchange;
import io.digibyte.tools.util.Utils;
import io.digibyte.wallet.BRPeerManager;
import io.digibyte.wallet.BRWalletManager;

import static io.digibyte.presenter.activities.ReEnterPinActivity.reEnterPinActivity;
import static io.digibyte.presenter.activities.SetPinActivity.introSetPitActivity;
import static io.digibyte.presenter.activities.intro.IntroActivity.introActivity;
import static io.digibyte.tools.animation.BRAnimator.t1Size;
import static io.digibyte.tools.animation.BRAnimator.t2Size;
import static io.digibyte.tools.util.BRConstants.PLATFORM_ON;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 8/4/15.
 * Copyright (c) 2016 breadwallet LLC
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

public class BreadActivity extends BRActivity implements BRWalletManager.OnBalanceChanged, BRPeerManager.OnTxStatusUpdate, BRSharedPrefs.OnIsoChangedListener, TransactionDataSource.OnTxAddedListener, InternetManager.ConnectionReceiverListener, SyncManager.onStatusListener, onStatusListener
{
    private static final String TAG = BreadActivity.class.getName();

    private final int LIST_SECTION_PROMPT = 0;
    private final int LIST_SECTION_TRANSACTIONS = 1;

    private InternetManager mConnectionReceiver;

    private LinearLayout sendButton;
    private LinearLayout receiveButton;
    private LinearLayout menuButton;
    private TextView primaryPrice;
    private TextView secondaryPrice;
    private TextView equals;
    private TextView manageText;
    private ConstraintLayout walletProgressLayout;
    private LinearLayout toolbarLayout;
    private ImageButton searchIcon;
    private BRSearchBar searchBar;
    public ViewFlipper barFlipper;

    private RecyclerView theListView;
    private ListItemPromptData theListItemPromptData;
    private ListItemSyncingData theListItemSyncingData;
    private TransactionListAdapter theTransactionListAdapter;

    private ConstraintLayout toolBarConstraintLayout;
    private String savedFragmentTag;
    private boolean uiIsDone;

    // TODO: Remove/cleanup this block
    public static boolean appVisible = false;
    private static BreadActivity app;
    public static BreadActivity getApp() { return app; }
    public static final Point screenParametersPoint = new Point();


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bread);

        // TODO: Remove/cleanup this block
        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);
        app = this;

        this.initializeViews();

        BRWalletManager.getInstance().addBalanceChangedListener(this);
        BRPeerManager.getInstance().addStatusUpdateListener(this);
        BRSharedPrefs.addIsoChangedListener(this);

        TxManager.getInstance().addListener(this);
        SyncManager.getInstance().addListener(this);

        this.onConnectionChanged(InternetManager.getInstance().isConnected(this));

        // TODO: Remove/cleanup this block
        if (introSetPitActivity != null)
        {
            introSetPitActivity.finish();
        }
        if (introActivity != null)
        {
            introActivity.finish();
        }
        if (reEnterPinActivity != null)
        {
            reEnterPinActivity.finish();
        }

        if (!BRSharedPrefs.getGreetingsShown(BreadActivity.this))
        {
            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    BRAnimator.showGreetingsMessage(BreadActivity.this);
                    BRSharedPrefs.putGreetingsShown(BreadActivity.this, true);
                }
            }, 1000);
        }

        this.updateUI();
        this.updateList();
    }

    private void initializeViews()
    {
        sendButton = findViewById(R.id.send_layout);
        receiveButton = findViewById(R.id.receive_layout);
        manageText = findViewById(R.id.manage_text);
        menuButton = findViewById(R.id.menu_layout);
        primaryPrice = findViewById(R.id.primary_price);
        secondaryPrice = findViewById(R.id.secondary_price);
        equals = findViewById(R.id.equals);
        toolBarConstraintLayout = findViewById(R.id.bread_toolbar);
        walletProgressLayout = findViewById(R.id.loading_wallet_layout);
        toolbarLayout = findViewById(R.id.toolbar_layout);
        searchIcon = findViewById(R.id.search_icon);
        barFlipper = findViewById(R.id.tool_bar_flipper);
        searchBar = findViewById(R.id.search_bar);

        // Set listeners
        sendButton.setOnClickListener(this.onButtonSend);
        receiveButton.setOnClickListener(this.onButtonReceive);
        menuButton.setOnClickListener(this.onButtonMenu);
        manageText.setOnClickListener(this.onManageText);
        primaryPrice.setOnClickListener(this.onButtonPrice);
        secondaryPrice.setOnClickListener(this.onButtonPrice);
        searchIcon.setOnClickListener(this.onButtonSearch);

        // Setup list view
        theListView = findViewById(R.id.tx_list);
        theListView.setItemAnimator(null);
        theListView.setLayoutManager(new LinearLayoutManager(this));
        theTransactionListAdapter = new TransactionListAdapter();
        theTransactionListAdapter.addSection(LIST_SECTION_PROMPT);
        theTransactionListAdapter.addSection(LIST_SECTION_TRANSACTIONS);
        theListView.setAdapter(theTransactionListAdapter);

        // Setup flipper bar
        barFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_enter));
        barFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_exit));

        // TODO: Remove/cleanup this block
        toolbarLayout.removeView(walletProgressLayout);
        primaryPrice.setTextSize(TypedValue.COMPLEX_UNIT_PX, t1Size);//make it the size it should be after animation to get the X
        secondaryPrice.setTextSize(TypedValue.COMPLEX_UNIT_PX, t2Size);//make it the size it should be after animation to get the X

        final ViewTreeObserver observer = primaryPrice.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                if (observer.isAlive())
                {
                    observer.removeOnGlobalLayoutListener(this);
                }
                if (uiIsDone)
                {
                    return;
                }
                uiIsDone = true;
                setPriceTags(BRSharedPrefs.getPreferredBTC(BreadActivity.this), false);
            }
        });
    }

    private void updateUI()
    {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable()
        {
            @Override
            public void run()
            {
                Thread.currentThread().setName(Thread.currentThread().getName() + ":updateUI");
                //sleep a little in order to make sure all the commits are finished (like SharePreferences commits)
                String iso = BRSharedPrefs.getIso(BreadActivity.this);

                //current amount in satoshis
                final BigDecimal amount = new BigDecimal(BRSharedPrefs.getCatchedBalance(BreadActivity.this));

                //amount in BTC units
                BigDecimal btcAmount = BRExchange.getBitcoinForSatoshis(BreadActivity.this, amount);
                final String formattedBTCAmount = BRCurrency.getFormattedCurrencyString(BreadActivity.this, "DGB", btcAmount);

                //amount in currency units
                BigDecimal curAmount = BRExchange.getAmountFromSatoshis(BreadActivity.this, iso, amount);
                final String formattedCurAmount = BRCurrency.getFormattedCurrencyString(BreadActivity.this, iso, curAmount);
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        primaryPrice.setText(formattedBTCAmount);
                        secondaryPrice.setText(String.format("%s", formattedCurAmount));
                    }
                });

                TxManager.getInstance().updateTxList();
            }
        });
    }

    private void updateList()
    {
        if(SyncManager.getInstance().isRunning())
        {
            if(null == theListItemSyncingData)
            {
                theListItemSyncingData = new ListItemSyncingData();
                theTransactionListAdapter.addItemInSection(LIST_SECTION_PROMPT, theListItemSyncingData);
            }
        }
        else
        {

            // TODO: Show propmt
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////// List item click listeners ////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private ListItemData.OnListItemClickListener onTransactionListItemClick = new ListItemData.OnListItemClickListener()
    {
        @Override
        public void onListItemClick(ListItemData aListItemData)
        {
            int position = 0;
            ArrayList<TxItem> transactionItems = new ArrayList<>();
            ArrayList<ListItemData> transactionList = theTransactionListAdapter.getItemsInSection(LIST_SECTION_TRANSACTIONS);
            for(int index = 0; index < transactionList.size(); index++)
            {
                ListItemTransactionData listItem = (ListItemTransactionData) transactionList.get(index);
                if(listItem.equals(aListItemData))
                {
                    position = index;
                }
                transactionItems.add(listItem.transactionItem);
            }
            BRAnimator.showTransactionPager(BreadActivity.this, transactionItems, position);
        }
    };

    private ListItemData.OnListItemClickListener onPromptListItemClick = new ListItemData.OnListItemClickListener()
    {
        @Override
        public void onListItemClick(ListItemData aListItemData)
        {
            // TODO: Do prompt stuff?
        }
    };

    private ListItemData.OnListItemClickListener onPromptListItemCloseClick = new ListItemData.OnListItemClickListener()
    {
        @Override
        public void onListItemClick(ListItemData aListItemData)
        {
            // TODO: Hide prompt stuff?
        }
    };



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////// Manager Listeners ////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onSyncManagerStart()
    {
        this.updateList();
    }

    public void onSyncManagerUpdate()
    {
        if(null != theListItemSyncingData)
        {
            theTransactionListAdapter.updateSection(LIST_SECTION_PROMPT);
        }
    }

    @Override
    public void onSyncManagerFinished()
    {
        if(!SyncManager.getInstance().isRunning())
        {
            if(null != theListItemSyncingData)
            {
                theTransactionListAdapter.removeItemInSection(LIST_SECTION_PROMPT, theListItemSyncingData);
                theListItemSyncingData = null;
            }
        }
    }

    @Override
    public void onTxManagerUpdate(TxItem[] aTransactionList)
    {
        ArrayList<ListItemData> transactionList = new ArrayList<>();

        int transactionsCount = aTransactionList.length;
        for(int index = 0; index < transactionsCount; index++)
        {
            transactionList.add(new ListItemTransactionData(index, transactionsCount, aTransactionList[index], onTransactionListItemClick));
        }
        theTransactionListAdapter.addItemsInSection(LIST_SECTION_TRANSACTIONS, transactionList);
    }

    @Override
    public void onStatusUpdate()
    {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable()
        {
            @Override
            public void run()
            {
                TxManager.getInstance().updateTxList();
            }
        });
    }

    @Override
    public void onIsoChanged(String iso)
    {
        updateUI();
    }

    @Override
    public void onTxAdded()
    {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable()
        {
            @Override
            public void run()
            {
                TxManager.getInstance().updateTxList();
            }
        });
        BRWalletManager.getInstance().refreshBalance(BreadActivity.this);
    }

    @Override
    public void onConnectionChanged(boolean isConnected)
    {
        if (isConnected)
        {
            if (barFlipper != null)
            {
                if (barFlipper.getDisplayedChild() == 2)
                {
                    barFlipper.setDisplayedChild(0);
                }
            }

            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable()
            {
                @Override
                public void run()
                {
                    final double progress = BRPeerManager.syncProgress(BRSharedPrefs.getStartHeight(BreadActivity.this));
                    //                    Log.e(TAG, "run: " + progress);
                    if (progress < 1 && progress > 0)
                    {
                        SyncManager.getInstance().startSyncingProgressThread();
                    }
                }
            });
        }
        else
        {
            if (barFlipper != null)
            {
                barFlipper.setDisplayedChild(2);
            }

            SyncManager.getInstance().stopSyncingProgressThread();
        }
    }

    @Override
    public void onBalanceChanged(final long balance)
    {
        updateUI();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////// UI OnClick Listeners /////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private View.OnClickListener onButtonSend = new View.OnClickListener()
    {
        @Override
        public void onClick(View aView)
        {
            if (BRAnimator.isClickAllowed())
            {
                BRAnimator.showSendFragment(BreadActivity.this, null);
            }
        }
    };

    private View.OnClickListener onButtonReceive = new View.OnClickListener()
    {
        @Override
        public void onClick(View aView)
        {
            if (BRAnimator.isClickAllowed())
            {
                BRAnimator.showReceiveFragment(BreadActivity.this, true);
            }
        }
    };

    private View.OnClickListener onButtonMenu = new View.OnClickListener()
    {
        @Override
        public void onClick(View aView)
        {
            if (BRAnimator.isClickAllowed())
            {
                BRAnimator.showMenuFragment(BreadActivity.this);
            }
        }
    };

    private View.OnClickListener onManageText = new View.OnClickListener()
    {
        @Override
        public void onClick(View aView)
        {
            if (BRAnimator.isClickAllowed())
            {
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.setCustomAnimations(0, 0, 0, R.animator.plain_300);
                FragmentManage fragmentManage = new FragmentManage();
                transaction.add(android.R.id.content, fragmentManage, FragmentManage.class.getName());
                transaction.addToBackStack(null);
                transaction.commit();
            }
        }
    };

    private View.OnClickListener onButtonPrice = new View.OnClickListener()
    {
        @Override
        public void onClick(View aView)
        {
            if (BRAnimator.isClickAllowed())
            {
                boolean b = !BRSharedPrefs.getPreferredBTC(BreadActivity.this);
                setPriceTags(b, true);
                BRSharedPrefs.putPreferredBTC(BreadActivity.this, b);
            }
        }
    };

    private View.OnClickListener onButtonSearch = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if (BRAnimator.isClickAllowed())
            {
                barFlipper.setDisplayedChild(1); //search bar
                searchBar.onShow(true);
            }
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////// LEFT OVERS CLEANUP? //////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void setPriceTags(boolean btcPreferred, boolean animate)
    {
        secondaryPrice.setTextSize(!btcPreferred ? t1Size : t2Size);
        primaryPrice.setTextSize(!btcPreferred ? t2Size : t1Size);
        ConstraintSet set = new ConstraintSet();
        set.clone(toolBarConstraintLayout);
        if (animate)
        {
            TransitionManager.beginDelayedTransition(toolBarConstraintLayout);
        }
        int px4 = Utils.getPixelsFromDps(this, 4);
        int px16 = Utils.getPixelsFromDps(this, 16);
        //align to parent left
        set.connect(!btcPreferred ? R.id.secondary_price : R.id.primary_price, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.END, px16);
        //align equals after the first item
        set.connect(R.id.equals, ConstraintSet.START, !btcPreferred ? secondaryPrice.getId() : primaryPrice.getId(), ConstraintSet.END, px4);
        //align second item after equals
        set.connect(!btcPreferred ? R.id.primary_price : R.id.secondary_price, ConstraintSet.START, equals.getId(), ConstraintSet.END, px4);
        //        align the second item to the baseline of the first
        //        set.connect(!btcPreferred ? R.id.primary_price : R.id.secondary_price, ConstraintSet.BASELINE, btcPreferred ? R.id.primary_price : R.id.secondary_price, ConstraintSet.BASELINE, 0);
        // Apply the changes
        set.applyTo(toolBarConstraintLayout);

        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                updateUI();
            }
        }, toolBarConstraintLayout.getLayoutTransition().getDuration(LayoutTransition.CHANGING));
    }

    private void setupNetworking()
    {
        if (mConnectionReceiver == null)
        {
            mConnectionReceiver = InternetManager.getInstance();
        }
        IntentFilter mNetworkStateFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectionReceiver, mNetworkStateFilter);
        InternetManager.addConnectionListener(this);
    }

    private void saveVisibleFragment()
    {
        if (getFragmentManager().getBackStackEntryCount() == 0)
        {
            return;
        }
        savedFragmentTag = getFragmentManager().getBackStackEntryAt(0).getName();
    }

    //returns x-pos relative to root layout
    private float getRelativeX(View myView)
    {
        if (myView.getParent() == myView.getRootView())
        {
            return myView.getX();
        }
        else
        {
            return myView.getX() + getRelativeX((View) myView.getParent());
        }
    }

    //returns y-pos relative to root layout
    private float getRelativeY(View myView)
    {
        if (myView.getParent() == myView.getRootView())
        {
            return myView.getY();
        }
        else
        {
            return myView.getY() + getRelativeY((View) myView.getParent());
        }
    }

    //0 crypto is left, 1 crypto is right
    private int getSwapPosition()
    {
        if (primaryPrice == null || secondaryPrice == null)
        {
            return 0;
        }
        return getRelativeX(primaryPrice) < getRelativeX(secondaryPrice) ? 0 : 1;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////// Activity overrides ///////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        //leave it empty, avoiding the os bug
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        Uri data = intent.getData();
        if (data != null)
        {
            String scheme = data.getScheme();
            if (scheme != null && (scheme.startsWith("digibyte") || scheme.startsWith("digiid")))
            {
                BitcoinUrlHandler.processRequest(this, intent.getDataString());
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        appVisible = true;
        app = this;
        if (PLATFORM_ON)
        {
            APIClient.getInstance(this).updatePlatform();
        }

        setupNetworking();

        if (!BRWalletManager.getInstance().isCreated())
        {
            BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable()
            {
                @Override
                public void run()
                {
                    BRWalletManager.getInstance().initWallet(BreadActivity.this);
                }
            });
        }
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                updateUI();
            }
        }, 1000);

        BRWalletManager.getInstance().refreshBalance(this);
        SyncService.scheduleBackgroundSync(this);

        BRAnimator.showFragmentByTag(this, savedFragmentTag);
        savedFragmentTag = null;
        TxManager.getInstance().onResume(BreadActivity.this);
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        app = this;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        appVisible = false;
        saveVisibleFragment();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        unregisterReceiver(mConnectionReceiver);
        InternetManager.removeConnectionListener(this);

        BRWalletManager.getInstance().removeListener(this);
        BRPeerManager.getInstance().removeListener(this);
        BRSharedPrefs.removeListener(this);

        TxManager.getInstance().removeListener(this);
        SyncManager.getInstance().removeListener(this);

        //sync the kv stores
        if (PLATFORM_ON)
        {
            BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable()
            {
                @Override
                public void run()
                {
                    APIClient.getInstance(BreadActivity.this).syncKvStore();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case BRConstants.CAMERA_REQUEST_ID:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    BRAnimator.openScanner(this, BRConstants.SCANNER_REQUEST);
                }
                break;
            }
        }
    }
}