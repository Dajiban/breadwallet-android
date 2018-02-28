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
import android.util.Log;
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
import io.digibyte.tools.list.items.ListItemSyncingData;
import io.digibyte.tools.list.items.ListItemTransactionData;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.manager.InternetManager;
import io.digibyte.tools.manager.SyncManager;
import io.digibyte.tools.manager.SyncService;
import io.digibyte.tools.manager.TxManager;
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

public class BreadActivity extends BRActivity implements BRWalletManager.OnBalanceChanged, BRPeerManager.OnTxStatusUpdate, BRSharedPrefs.OnIsoChangedListener, TransactionDataSource.OnTxAddedListener, FragmentManage.OnNameChanged, InternetManager.ConnectionReceiverListener, SyncManager.onStatusListener
{
    private static final String TAG = BreadActivity.class.getName();

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
    private TransactionListAdapter theTransactionListAdapter;
    private ListItemSyncingData theListItemSyncData;

    private ConstraintLayout toolBarConstraintLayout;
    private String savedFragmentTag;
    private boolean uiIsDone;

    public static boolean appVisible = false;
    private static BreadActivity app;
    public static BreadActivity getApp()
    {
        return app;
    }
    public static final Point screenParametersPoint = new Point();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bread);
        BRWalletManager.getInstance().addBalanceChangedListener(this);
        BRPeerManager.getInstance().addStatusUpdateListener(this);
        BRPeerManager.setOnSyncFinished(new BRPeerManager.OnSyncSucceeded()
        {
            @Override
            public void onFinished()
            {
                //put some here
            }
        });
        BRSharedPrefs.addIsoChangedListener(this);

        app = this;
        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);

        initializeViews();

        setListeners();

        toolbarLayout.removeView(walletProgressLayout);

        setUpBarFlipper();

        BRAnimator.init(this);
        primaryPrice.setTextSize(TypedValue.COMPLEX_UNIT_PX, t1Size);//make it the size it should be after animation to get the X
        secondaryPrice.setTextSize(TypedValue.COMPLEX_UNIT_PX, t2Size);//make it the size it should be after animation to get the X

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

        TxManager.getInstance().init(this);
        SyncManager.getInstance().addListener(this);

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


        onConnectionChanged(InternetManager.getInstance().isConnected(this));

        updateUI();
        buildList();
    }


    private void buildList()
    {
        ArrayList<ListItemData> itemDataList = new ArrayList<>();
        TransactionListAdapter adapter = (TransactionListAdapter) theListView.getAdapter();

        if (SyncManager.getInstance().isRunning())
        {
            itemDataList.add(theListItemSyncData);
        }

        final TxItem[] transactions = BRWalletManager.getInstance().getTransactions();
        for (TxItem transaction : transactions)
        {
            itemDataList.add(new ListItemTransactionData(transaction));
        }
        adapter.addItems(itemDataList);
    }

    public void onTransactionListItemClick()
    {
        // Todo: BRAnimator.showTransactionPager(app, adapter.getItems(), currentPrompt == null ? position : position - 1);
    }

    public void onPromptListItemClick()
    {
        // TODO: Do prompt stuff
    }

    public void onPromptListItemCloseClick()
    {
        // TODO: Hide prompt
    }

    @Override
    public void onSyncManagerStart()
    {
        Log.d("[test]", "onSyncManagerStart");
        theTransactionListAdapter.insertItem(0, theListItemSyncData);
    }

    public void onSyncManagerUpdate()
    {
        Log.d("[test]", "onSyncManagerUpdate");
        theTransactionListAdapter.updateItem(theListItemSyncData);
    }

    @Override
    public void onSyncManagerFinished()
    {
        Log.d("[test]", "onSyncManagerFinished");
        theTransactionListAdapter.removeItem(theListItemSyncData);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        //leave it empty, avoiding the os bug
    }

    private void setUrlHandler(Intent intent)
    {
        Uri data = intent.getData();
        if (data == null)
        {
            return;
        }
        String scheme = data.getScheme();
        if (scheme != null && (scheme.startsWith("digibyte") || scheme.startsWith("digiid")))
        {
            String str = intent.getDataString();
            BitcoinUrlHandler.processRequest(this, str);
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        setUrlHandler(intent);
    }

    private void setListeners()
    {
        sendButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!BRAnimator.isClickAllowed())
                {
                    return;
                }
                BRAnimator.showSendFragment(BreadActivity.this, null);

            }
        });

        receiveButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!BRAnimator.isClickAllowed())
                {
                    return;
                }
                BRAnimator.showReceiveFragment(BreadActivity.this, true);
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!BRAnimator.isClickAllowed())
                {
                    return;
                }
                //start the server for Buy Bitcoin
                BRAnimator.showMenuFragment(BreadActivity.this);

            }
        });
        manageText.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!BRAnimator.isClickAllowed())
                {
                    return;
                }
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.setCustomAnimations(0, 0, 0, R.animator.plain_300);
                FragmentManage fragmentManage = new FragmentManage();
                fragmentManage.setOnNameChanged(BreadActivity.this);
                transaction.add(android.R.id.content, fragmentManage, FragmentManage.class.getName());
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
        primaryPrice.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                swap();
            }
        });
        secondaryPrice.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                swap();
            }
        });

        searchIcon.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!BRAnimator.isClickAllowed())
                {
                    return;
                }
                barFlipper.setDisplayedChild(1); //search bar
                searchBar.onShow(true);
            }
        });

    }

    private void swap()
    {
        if (!BRAnimator.isClickAllowed())
        {
            return;
        }
        boolean b = !BRSharedPrefs.getPreferredBTC(this);
        setPriceTags(b, true);
        BRSharedPrefs.putPreferredBTC(this, b);
    }

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

    private void setUpBarFlipper()
    {
        barFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_enter));
        barFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_exit));
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        app = this;
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
                buildList();
            }
        }, 1000);

        BRWalletManager.getInstance().refreshBalance(this);
        SyncService.scheduleBackgroundSync(this);

        BRAnimator.showFragmentByTag(this, savedFragmentTag);
        savedFragmentTag = null;
        TxManager.getInstance().onResume(BreadActivity.this);

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

    private void initializeViews()
    {
        // Always cast your custom Toolbar here, and set it as the ActionBar.
        sendButton = (LinearLayout) findViewById(R.id.send_layout);
        receiveButton = (LinearLayout) findViewById(R.id.receive_layout);
        manageText = (TextView) findViewById(R.id.manage_text);
        menuButton = (LinearLayout) findViewById(R.id.menu_layout);
        primaryPrice = (TextView) findViewById(R.id.primary_price);
        secondaryPrice = (TextView) findViewById(R.id.secondary_price);
        equals = (TextView) findViewById(R.id.equals);
        toolBarConstraintLayout = (ConstraintLayout) findViewById(R.id.bread_toolbar);
        walletProgressLayout = (ConstraintLayout) findViewById(R.id.loading_wallet_layout);
        toolbarLayout = (LinearLayout) findViewById(R.id.toolbar_layout);
        searchIcon = (ImageButton) findViewById(R.id.search_icon);
        barFlipper = (ViewFlipper) findViewById(R.id.tool_bar_flipper);
        searchBar = (BRSearchBar) findViewById(R.id.search_bar);

        // Setup RecyclerView with TransactionListAdapter
        theListView = findViewById(R.id.tx_list);
        theListItemSyncData = new ListItemSyncingData();
        theTransactionListAdapter = new TransactionListAdapter();
        theListView.setItemAnimator(null);
        theListView.setAdapter(theTransactionListAdapter);
        theListView.setLayoutManager(new LinearLayoutManager(this));

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

    @Override
    public void onBalanceChanged(final long balance)
    {
        updateUI();
    }

    public void updateUI()
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
                TxManager.getInstance().updateTxList(BreadActivity.this);
            }
        });
    }

    @Override
    public void onStatusUpdate()
    {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable()
        {
            @Override
            public void run()
            {
                TxManager.getInstance().updateTxList(BreadActivity.this);
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
                TxManager.getInstance().updateTxList(BreadActivity.this);
            }
        });
        BRWalletManager.getInstance().refreshBalance(BreadActivity.this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case BRConstants.CAMERA_REQUEST_ID:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    BRAnimator.openScanner(this, BRConstants.SCANNER_REQUEST);
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                }
                else
                {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onNameChanged(String name)
    {
        //        walletName.setText(name);
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
}