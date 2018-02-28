package io.digibyte.tools.list.items;

import io.digibyte.R;
import io.digibyte.presenter.entities.TxItem;
import io.digibyte.tools.list.ListItemData;

public class ListItemTransactionData extends ListItemData
{
    public final TxItem transactionItem;

    public ListItemTransactionData(TxItem aTransactionItem)
    {
        super(R.layout.list_item_transaction, ListItemTransactionViewHolder.class);

        this.transactionItem = aTransactionItem;
    }
}
