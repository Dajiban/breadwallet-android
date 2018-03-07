package io.digibyte.tools.list.items;

import io.digibyte.R;
import io.digibyte.presenter.entities.TxItem;
import io.digibyte.tools.list.ListItemData;

public class ListItemTransactionData extends ListItemData
{
    public final TxItem transactionItem;

    public ListItemTransactionData(TxItem aTransactionItem, OnListItemClickListener aListener)
    {
        super(R.layout.list_item_transaction, ListItemTransactionViewHolder.class, aListener);

        this.transactionItem = aTransactionItem;
    }
}