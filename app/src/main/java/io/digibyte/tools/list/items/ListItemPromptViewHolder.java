package io.digibyte.tools.list.items;

import android.view.View;
import android.widget.ImageButton;

import io.digibyte.R;
import io.digibyte.presenter.customviews.BRText;
import io.digibyte.tools.list.ListItemData;
import io.digibyte.tools.list.ListItemViewHolder;

public class ListItemPromptViewHolder extends ListItemViewHolder
{
    private final BRText title;
    private final BRText description;
    private final ImageButton close;

    public ListItemPromptViewHolder(View anItemView)
    {
        super(anItemView);

        title = anItemView.findViewById(R.id.info_title);
        description = anItemView.findViewById(R.id.info_description);
        close = anItemView.findViewById(R.id.info_close_button);
    }

    @Override
    public void process(ListItemData aListItemData)
    {
        super.process(aListItemData);

        ListItemPromptData data = (ListItemPromptData) aListItemData;

        this.title.setText(data.title);
        this.description.setText(data.description);
        this.itemView.setBackgroundResource(R.drawable.tx_rounded);
    }
}
