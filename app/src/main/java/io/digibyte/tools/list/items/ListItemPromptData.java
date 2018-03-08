package io.digibyte.tools.list.items;

import io.digibyte.R;
import io.digibyte.tools.list.ListItemData;
import io.digibyte.tools.manager.PromptManager;

public class ListItemPromptData extends ListItemData
{
    private final OnListItemClickListener onCloseClickListener;

    public final PromptManager.PromptItem promptItem;

    public ListItemPromptData(PromptManager.PromptItem aPromptItem, OnListItemClickListener aClickListener, OnListItemClickListener aCloseClickListener)
    {
        super(R.layout.list_item_prompt, ListItemPromptViewHolder.class, aClickListener);
        this.onCloseClickListener = aCloseClickListener;
        this.promptItem = aPromptItem;
    }

    public boolean onCloseClick()
    {
        if(null != this.onCloseClickListener)
        {
            this.onCloseClickListener.onListItemClick(this);

            return true;
        }
        return false;
    }
}
