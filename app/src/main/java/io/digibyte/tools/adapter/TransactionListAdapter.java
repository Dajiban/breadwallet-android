package io.digibyte.tools.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import io.digibyte.tools.list.ListItemData;
import io.digibyte.tools.list.ListItemViewHolder;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/27/15.
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

public class TransactionListAdapter extends RecyclerView.Adapter<ListItemViewHolder>
{
    public static final String TAG = TransactionListAdapter.class.getName();

    private SparseArray<ArrayList<ListItemData>> theItemDataList;

    public TransactionListAdapter()
    {
        theItemDataList = new SparseArray<>();
    }

    public void addSection(int aSectionId)
    {
        theItemDataList.put(aSectionId, new ArrayList<ListItemData>());
    }

    public void removeSection(int aSectionId)
    {
        theItemDataList.remove(aSectionId);
        this.notifyDataSetChanged(); // TODO: Notify correct range position
    }

    public ArrayList<ListItemData> getItemsInSection(int aSectionId)
    {
        return theItemDataList.get(aSectionId, new ArrayList<ListItemData>());
    }

    public void updateSection(int aSectionId)
    {
        this.notifyDataSetChanged(); // TODO: Notify correct range position
    }

    public void addItemsInSection(int aSectionId, ArrayList<ListItemData> anItemDataList)
    {
        theItemDataList.put(aSectionId, anItemDataList);
        this.notifyDataSetChanged(); // TODO: Notify correct range position
    }

    public void removeItemInSection(int aSectionId, ListItemData aListItem)
    {
        if(null != theItemDataList.get(aSectionId))
        {
            theItemDataList.get(aSectionId).remove(aListItem);
            this.notifyDataSetChanged(); // TODO: Notify correct item position
        }
    }

    @Override
    public ListItemViewHolder onCreateViewHolder(ViewGroup aParent, int aResourceId)
    {
        ListItemViewHolder holder;
        LayoutInflater layoutInflater = LayoutInflater.from(aParent.getContext());
        View view = layoutInflater.inflate(aResourceId, aParent, false);

        try
        {
            Class<?> viewHolder = ListItemData.getViewHolder(aResourceId);
            Constructor<?> constructors = viewHolder.getConstructor(View.class);
            holder = (ListItemViewHolder) constructors.newInstance(view);
        }
        catch (Exception ignore)
        {
            holder = new ListItemViewHolder(view);
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(ListItemViewHolder holder, int aPosition)
    {
        holder.process(this.getListItemDataForPosition(aPosition));
    }

    @Override
    public int getItemViewType(int aPosition)
    {
        return this.getListItemDataForPosition(aPosition).resourceId;
    }

    @Override
    public int getItemCount()
    {
        int itemCount = 0;
        for(int sectionIndex = 0; sectionIndex < theItemDataList.size(); sectionIndex++)
        {
            itemCount += theItemDataList.valueAt(sectionIndex).size();
        }
        return itemCount;
    }

    private ListItemData getListItemDataForPosition(int aPosition)
    {
        int currentPosition = -1;
        for(int sectionIndex = 0; sectionIndex < theItemDataList.size(); sectionIndex++)
        {
            for(int index = 0; index < theItemDataList.get(sectionIndex).size(); index++)
            {
                if(aPosition == ++currentPosition)
                {
                    return theItemDataList.get(sectionIndex).get(index);
                }
            }
        }
        return null;
    }
































    public void filterBy(String query, boolean[] switches)
    {
        /*
        filter(query, switches);
        */
    }

    public void resetFilter()
    {
        /*
        itemFeed = backUpFeed;
        notifyDataSetChanged();
        */
    }

    private void filter(final String query, final boolean[] switches)
    {
        /*
        long start = System.currentTimeMillis();
        String lowerQuery = query.toLowerCase().trim();
        if (Utils.isNullOrEmpty(lowerQuery) && !switches[0] && !switches[1] && !switches[2] && !switches[3])
        {
            return;
        }
        int switchesON = 0;
        for (boolean i : switches)
        {
            if (i)
            {
                switchesON++;
            }
        }

        final List<TxItem> filteredList = new ArrayList<>();
        TxItem item;
        for (int i = 0; i < backUpFeed.size(); i++)
        {
            item = backUpFeed.get(i);
            boolean matchesHash = item.getTxHashHexReversed() != null && item.getTxHashHexReversed().contains(lowerQuery);
            boolean matchesAddress = item.getFrom()[0].contains(lowerQuery) || item.getTo()[0].contains(lowerQuery);
            boolean matchesMemo = item.metaData != null && item.metaData.comment != null && item.metaData.comment.toLowerCase().contains(lowerQuery);
            if (matchesHash || matchesAddress || matchesMemo)
            {
                if (switchesON == 0)
                {
                    filteredList.add(item);
                }
                else
                {
                    boolean willAdd = true;
                    //filter by sent and this is received
                    if (switches[0] && (item.getSent() - item.getReceived() <= 0))
                    {
                        willAdd = false;
                    }
                    //filter by received and this is sent
                    if (switches[1] && (item.getSent() - item.getReceived() > 0))
                    {
                        willAdd = false;
                    }

                    int confirms = item.getBlockHeight() == Integer.MAX_VALUE ? 0 : BRSharedPrefs.getLastBlockHeight(mContext) - item.getBlockHeight() + 1;
                    //complete
                    if (switches[2] && confirms >= 6)
                    {
                        willAdd = false;
                    }

                    //pending
                    if (switches[3] && confirms < 6)
                    {
                        willAdd = false;
                    }

                    if (willAdd)
                    {
                        filteredList.add(item);
                    }
                }

            }

        }
        itemFeed = filteredList;
        notifyDataSetChanged();

        Log.e(TAG, "filter: " + query + " took: " + (System.currentTimeMillis() - start));
        */
    }

    public void updateData()
    {
        /*
        if (updatingData)
        {
            return;
        }
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable()
        {
            @Override
            public void run()
            {
                long s = System.currentTimeMillis();
                List<TxItem> newItems = new ArrayList<>(itemFeed);
                TxItem item;
                for (int i = 0; i < newItems.size(); i++)
                {
                    item = newItems.get(i);
                    item.metaData = KVStoreManager.getInstance().getTxMetaData(mContext, item.getTxHash());
                    item.txReversed = Utils.reverseHex(Utils.bytesToHex(item.getTxHash()));

                }
                backUpFeed = newItems;
                String log = String.format("newItems: %d, took: %d", newItems.size(), (System.currentTimeMillis() - s));
                Log.e(TAG, "updateData: " + log);
                updatingData = false;
            }
        });
        */
    }

  }