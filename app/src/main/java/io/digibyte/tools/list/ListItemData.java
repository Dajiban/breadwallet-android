package io.digibyte.tools.list;

import android.util.SparseArray;

public class ListItemData
{
    private static SparseArray<Class<?>> listItemViewHolders = new SparseArray<>();
    public static Class<?> getViewHolder(int aResourceId) { return listItemViewHolders.get(aResourceId); }

    public final int resourceId;

    public ListItemData(int aResourceId, Class<?> aViewHolder)
    {
        this.resourceId = aResourceId;
        listItemViewHolders.append(aResourceId, aViewHolder);
    }

    public void onClick()
    {
        // Override this
    }
}
