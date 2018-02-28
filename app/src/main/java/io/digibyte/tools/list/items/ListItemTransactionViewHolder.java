package io.digibyte.tools.list.items;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.platform.tools.KVStoreManager;

import java.math.BigDecimal;
import java.util.Date;

import io.digibyte.R;
import io.digibyte.presenter.entities.TxItem;
import io.digibyte.tools.list.ListItemData;
import io.digibyte.tools.list.ListItemViewHolder;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.util.BRCurrency;
import io.digibyte.tools.util.BRDateUtil;
import io.digibyte.tools.util.BRExchange;
import io.digibyte.wallet.BRPeerManager;

public class ListItemTransactionViewHolder extends ListItemViewHolder
{
    private final TextView amount;
    private final TextView toFrom;
    private final TextView account;
    private final TextView status;
    private final TextView status_2;
    private final TextView timestamp;
    private final TextView comment;
    private final ImageView arrowIcon;
    private final TextView sentReceived;
    private final ConstraintLayout constraintLayout;

    public ListItemTransactionViewHolder(View anItemView)
    {
        super(anItemView);

        amount = anItemView.findViewById(R.id.amount);
        toFrom = anItemView.findViewById(R.id.to_from);
        account = anItemView.findViewById(R.id.account);
        status = anItemView.findViewById(R.id.status);
        status_2 = anItemView.findViewById(R.id.status_2);
        timestamp = anItemView.findViewById(R.id.timestamp);
        comment = anItemView.findViewById(R.id.comment);
        arrowIcon = anItemView.findViewById(R.id.arrow_icon);
        sentReceived = anItemView.findViewById(R.id.sent_received);
        constraintLayout = anItemView.findViewById(R.id.constraintLayout);
    }

    @Override
    public void process(ListItemData aListItemData)
    {
        super.process(aListItemData);

        Context context = this.itemView.getContext();
        ListItemTransactionData data = (ListItemTransactionData) aListItemData;
        TxItem item = data.transactionItem;

        // TODO: Check if this returns the right value
        int position = this.getAdapterPosition();

        item.metaData = KVStoreManager.getInstance().getTxMetaData(context, item.getTxHash());
        String commentString = (item.metaData == null || item.metaData.comment == null) ? "" : item.metaData.comment;

        this.comment.setText(commentString);

        if (commentString.isEmpty())
        {
            this.comment.setVisibility(View.GONE);

            ConstraintSet set = new ConstraintSet();
            set.clone(this.constraintLayout);
            int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
            set.connect(R.id.status, ConstraintSet.TOP, this.toFrom.getId(), ConstraintSet.BOTTOM, px);
            // Apply the changes
            set.applyTo(this.constraintLayout);
        }
        else
        {
            this.comment.setVisibility(View.VISIBLE);

            ConstraintSet set = new ConstraintSet();
            set.clone(this.constraintLayout);
            int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
            set.connect(R.id.status, ConstraintSet.TOP, this.comment.getId(), ConstraintSet.BOTTOM, px);
            // Apply the changes
            set.applyTo(this.constraintLayout);
            this.comment.requestLayout();
        }

        boolean received = item.getSent() == 0;

        this.account.setText(item.getTo()[0]);
        this.itemView.setBackgroundResource(this.getResourceByPos(position));
        this.arrowIcon.setImageResource(received ? R.drawable.arrow_down_bold_circle : R.drawable.arrow_up_bold_circle);
        this.sentReceived.setText(received ? context.getString(R.string.TransactionDetails_received, "") : context.getString(R.string.TransactionDetails_sent, ""));
        this.toFrom.setText(received ? String.format(context.getString(R.string.TransactionDetails_from), "") : String.format(context.getString(R.string.TransactionDetails_to), ""));

        int blockHeight = item.getBlockHeight();
        int confirms = blockHeight == Integer.MAX_VALUE ? 0 : BRSharedPrefs.getLastBlockHeight(context) - blockHeight + 1;

        int level;
        if (confirms <= 0)
        {
            int relayCount = BRPeerManager.getRelayCount(item.getTxHash());
            if (relayCount <= 0)
            {
                level = 0;
            }
            else if (relayCount == 1)
            {
                level = 1;
            }
            else
            {
                level = 2;
            }
        }
        else
        {
            if (confirms == 1)
            {
                level = 3;
            }
            else if (confirms == 2)
            {
                level = 4;
            }
            else if (confirms == 3)
            {
                level = 5;
            }
            else
            {
                level = 6;
            }
        }

        boolean availableForSpend = false;
        String sentReceived = received ? "Receiving" : "Sending";
        String percentage = "";
        switch (level)
        {
            case 0:
                percentage = "0%";
                break;
            case 1:
                percentage = "20%";
                break;
            case 2:
                percentage = "40%";
                availableForSpend = true;
                break;
            case 3:
                percentage = "60%";
                availableForSpend = true;
                break;
            case 4:
                percentage = "80%";
                availableForSpend = true;
                break;
            case 5:
                percentage = "100%";
                availableForSpend = true;
                break;
        }

        if (availableForSpend && received)
        {
            this.status_2.setText(context.getString(R.string.Transaction_available));
        }
        else
        {
            this.status_2.setVisibility(View.GONE);

            ConstraintSet set = new ConstraintSet();
            set.clone(this.constraintLayout);
            int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());

            set.connect(R.id.status, ConstraintSet.BOTTOM, this.constraintLayout.getId(), ConstraintSet.BOTTOM, px);
            // Apply the changes
            set.applyTo(this.constraintLayout);
        }

        if (level == 6)
        {
            this.status.setText(context.getString(R.string.Transaction_complete));
        }
        else
        {
            this.status.setText(String.format("%s - %s", sentReceived, percentage));
        }

        if (!item.isValid())
        {
            this.status.setText(context.getString(R.string.Transaction_invalid));
        }


        boolean isBTCPreferred = BRSharedPrefs.getPreferredBTC(context);
        String iso = isBTCPreferred ? "DGB" : BRSharedPrefs.getIso(context);
        long satoshisAmount = received ? item.getReceived() : (item.getSent() - item.getReceived());
        this.amount.setText(BRCurrency.getFormattedCurrencyString(context, iso, BRExchange.getAmountFromSatoshis(context, iso, new BigDecimal(satoshisAmount))));

        //if it's 0 we use the current time.
        long timeStamp = item.getTimeStamp() == 0 ? System.currentTimeMillis() : item.getTimeStamp() * 1000;
        CharSequence timeSpan = BRDateUtil.getCustomSpan(new Date(timeStamp));

        this.timestamp.setText(timeSpan);
    }

    private int getResourceByPos(int aPosition)
    {
        /*
        // TODO: FIX THIS
        if (itemFeed != null && itemFeed.size() == 1)
        {
            return R.drawable.tx_rounded;
        }
        else if (aPosition == 0)
        {
            return R.drawable.tx_rounded_up;
        }
        else if (itemFeed != null && aPosition == itemFeed.size() - 1)
        {
            return R.drawable.tx_rounded_down;
        }
        else
        {
            return R.drawable.tx_not_rounded;
        }
        */

        return R.drawable.tx_not_rounded;
    }
}
