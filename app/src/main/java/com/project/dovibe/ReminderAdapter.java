package com.project.dovibe;

import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    public interface OnReminderActionListener {
        void onToggleEnabled(Reminder r, int pos, boolean on);
        void onDelete(Reminder r, int pos);
    }

    private List<Reminder>           list;
    private OnReminderActionListener listener;

    public ReminderAdapter(List<Reminder> list, OnReminderActionListener listener) {
        this.list     = list;
        this.listener = listener;
    }

    public void updateList(List<Reminder> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @Override
    public ReminderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ReminderViewHolder h, int pos) {
        Reminder r = list.get(pos);

        h.tvTitle.setText(r.title);
        h.tvDate.setText(r.date + "  ·  " + formatTime(r.hour, r.minute));
        h.tvType.setText(typeEmoji(r.type) + "  " + r.type);
        h.tvRepeat.setText("yearly".equals(r.repeatMode) ? "🔁 Every year" : "📅 Once");

        h.swEnabled.setOnCheckedChangeListener(null);
        h.swEnabled.setChecked(r.enabled);
        h.swEnabled.setOnCheckedChangeListener((btn, on) -> listener.onToggleEnabled(r, pos, on));

        h.btnDelete.setOnClickListener(v -> listener.onDelete(r, pos));
    }

    @Override public int getItemCount() { return list.size(); }

    private String formatTime(int h, int m) {
        String ap = h >= 12 ? "PM" : "AM";
        int dh    = h % 12 == 0 ? 12 : h % 12;
        return String.format("%d:%02d %s", dh, m, ap);
    }

    private String typeEmoji(String type) {
        if (type == null) return "🔔";
        switch (type) {
            case "Birthday":    return "🎂";
            case "Anniversary": return "💍";
            case "Meeting":     return "📋";
            case "Holiday":     return "🏖️";
            default:            return "🔔";
        }
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        TextView    tvTitle, tvDate, tvType, tvRepeat;
        Switch      swEnabled;
        ImageButton btnDelete;

        ReminderViewHolder(View v) {
            super(v);
            tvTitle   = v.findViewById(R.id.tvReminderTitle);
            tvDate    = v.findViewById(R.id.tvReminderDate);
            tvType    = v.findViewById(R.id.tvReminderType);
            tvRepeat  = v.findViewById(R.id.tvReminderRepeat);
            swEnabled = v.findViewById(R.id.switchReminderEnabled);
            btnDelete = v.findViewById(R.id.btnDeleteReminder);
        }
    }
}