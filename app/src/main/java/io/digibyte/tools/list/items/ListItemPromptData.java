package io.digibyte.tools.list.items;

import io.digibyte.R;
import io.digibyte.tools.list.ListItemData;

public class ListItemPromptData extends ListItemData
{
    public final String title;
    public final String description;

    public ListItemPromptData(String aTitle, String aDescription)
    {
        super(R.layout.list_item_prompt, ListItemPromptViewHolder.class);

        this.title = aTitle;
        this.description = aDescription;
    }
}
