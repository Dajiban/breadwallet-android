package io.digibyte.tools.list;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public class ListItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
{
    protected ListItemData theItemData;

    public ListItemViewHolder(View itemView)
    {
        super(itemView);

        this.itemView.setOnClickListener(this);
    }

    public void process(ListItemData aListItemData)
    {
        theItemData = aListItemData;
    }

    @Override
    public void onClick(View view)
    {
        if(null != theItemData)
        {
            theItemData.onClick();
        }
    }
}
