package io.digibyte.tools.list.items;

import android.view.View;
import android.widget.ProgressBar;

import io.digibyte.R;
import io.digibyte.presenter.customviews.BRText;
import io.digibyte.tools.list.ListItemData;
import io.digibyte.tools.list.ListItemViewHolder;
import io.digibyte.tools.manager.SyncManager;
import io.digibyte.tools.util.Utils;

public class ListItemSyncingViewHolder extends ListItemViewHolder
{
    private final BRText date;
    private final ProgressBar progress;

    public ListItemSyncingViewHolder(View anItemView)
    {
        super(anItemView);

        date = anItemView.findViewById(R.id.sync_date);
        progress = anItemView.findViewById(R.id.sync_progress);
    }

    @Override
    public void process(ListItemData aListItemData)
    {
        super.process(aListItemData);

        this.itemView.setBackgroundResource(R.drawable.tx_rounded);
        this.progress.setProgress((int) (SyncManager.getInstance().getProgress() * 100));
        this.date.setText(Utils.formatTimeStamp(SyncManager.getInstance().getLastBlockTimestamp() * 1000, "MMM. dd, yyyy  ha"));
    }
}