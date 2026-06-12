package com.ysu.capstone.chat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.ysu.capstone.R;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;
    private static final int VIEW_TYPE_SCHEDULE = 3;

    private List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if ("schedule_card".equals(message.getType())) {
            return VIEW_TYPE_SCHEDULE;
        }
        return message.getSender().equals("user") ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_SCHEDULE) {
            View view = inflater.inflate(R.layout.item_schedule_card, parent, false);
            return new ScheduleViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.inc_chat_item, parent, false);
            return new MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);

        Log.d("MessageSender", "Sender: " + message.getSender() +
                ", Text: " + message.getText() +
                ", Type: " + message.getType());

        if (holder instanceof ScheduleViewHolder) {
            ScheduleViewHolder scheduleHolder = (ScheduleViewHolder) holder;
            View scheduleView = message.getScheduleView();
            if (scheduleView != null && scheduleView.getParent() != null) {
                ((ViewGroup) scheduleView.getParent()).removeView(scheduleView);
            }
            if (scheduleHolder.container != null) {
                scheduleHolder.container.removeAllViews();
                if (scheduleView != null) {
                    scheduleHolder.container.addView(scheduleView);
                }
            }
        } else {
            MessageViewHolder messageHolder = (MessageViewHolder) holder;
            if (message.getSender().equals("user")) {
                messageHolder.rightChatTv.setText(message.getText());
                messageHolder.rightChatTv.setVisibility(View.VISIBLE);
                messageHolder.leftChatTv.setVisibility(View.GONE);
                messageHolder.dotLoadingLayout.setVisibility(View.GONE);
            } else {
                if (message.getText().equals("loading")) {
                    messageHolder.leftChatTv.setVisibility(View.GONE);
                    messageHolder.dotLoadingLayout.setVisibility(View.VISIBLE);
                } else {
                    messageHolder.leftChatTv.setText(message.getText());
                    messageHolder.leftChatTv.setVisibility(View.VISIBLE);
                    messageHolder.dotLoadingLayout.setVisibility(View.GONE);
                }
                messageHolder.rightChatTv.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView leftChatTv, rightChatTv;
        LinearLayout dotLoadingLayout;

        MessageViewHolder(View itemView) {
            super(itemView);
            leftChatTv = itemView.findViewById(R.id.leftChatTv);
            rightChatTv = itemView.findViewById(R.id.rightChatTv);
            dotLoadingLayout = itemView.findViewById(R.id.dotLoadingLayout);
        }
    }

    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;

        ScheduleViewHolder(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.schedule_card_container);
        }
    }
}