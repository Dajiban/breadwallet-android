package io.digibyte.tools.list;

import android.util.SparseArray;

public class ListItemData
{
    public interface OnListItemClickListener
    {
        void onListItemClick(ListItemData aListItemData);
    }

    private static SparseArray<Class<?>> listItemViewHolders = new SparseArray<>();
    public static Class<?> getViewHolder(int aResourceId) { return listItemViewHolders.get(aResourceId); }

    public final int resourceId;
    public final OnListItemClickListener onClickListener;
    public final OnListItemClickListener onLongClickListener;

    public ListItemData(int aResourceId, Class<?> aViewHolder)
    {
        this(aResourceId, aViewHolder, null);
    }

    public ListItemData(int aResourceId, Class<?> aViewHolder, OnListItemClickListener aClickListener)
    {
        this(aResourceId, aViewHolder, aClickListener, null);
    }

    public ListItemData(int aResourceId, Class<?> aViewHolder, OnListItemClickListener aClickListener, OnListItemClickListener aLongClickListener)
    {
        this.resourceId = aResourceId;
        this.onClickListener = aClickListener;
        this.onLongClickListener = aLongClickListener;
        listItemViewHolders.append(aResourceId, aViewHolder);
    }

    public void onClick()
    {
        if(null != this.onClickListener)
        {
            this.onClickListener.onListItemClick(this);
        }
    }

    public boolean onLongClick()
    {
        if(null != this.onLongClickListener)
        {
            this.onLongClickListener.onListItemClick(this);

            return true;
        }
        return false;
    }
}
