package com.project.dovibe;

import android.graphics.Paint;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskActionListener {
        void onToggleDone(Task task, int position);
        void onCycleNotifyMode(Task task, int position);  // Off → Notify → Alarm
        void onEditTask(Task task, int position);
        void onDeleteTask(Task task, int position);
    }

    // Keep old interface working — bridge for CalenderActivity which still uses onToggleAlarm
    public interface OnTaskActionListenerLegacy extends OnTaskActionListener {
        void onToggleAlarm(Task task, int position, boolean alarmOn);
        default void onCycleNotifyMode(Task task, int position) {}
    }

    private List<Task>           list;
    private OnTaskActionListener listener;

    public TaskAdapter(List<Task> list, OnTaskActionListener listener) {
        this.list     = list;
        this.listener = listener;
    }

    public void updateList(List<Task> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @Override
    public TaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(v);
    }

    @Override
    public void onBindViewHolder(TaskViewHolder h, int pos) {
        Task t = list.get(pos);

        // Title + strikethrough when done
        h.tvTitle.setText(t.title);
        if (t.isDone) {
            h.tvTitle.setPaintFlags(h.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            h.tvTitle.setAlpha(0.45f);
        } else {
            h.tvTitle.setPaintFlags(h.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            h.tvTitle.setAlpha(1f);
        }

        // Time + repeat label
        String repeat = "";
        if (t.repeatMode != null) {
            switch (t.repeatMode) {
                case "daily":  repeat = " · Daily";  break;
                case "weekly": repeat = " · Weekly"; break;
                case "custom": repeat = " · Custom"; break;
                default:       repeat = " · Once";   break;
            }
        }
        h.tvDueTime.setText(
                (t.dueTime == null || t.dueTime.isEmpty() ? "No time set" : t.dueTime) + repeat);

        // Category badge colour
        h.tvCategory.setText(t.category);
        switch (t.category == null ? "" : t.category) {
            case "Work":
                h.tvCategory.setBackgroundResource(R.drawable.badge_work);
                h.tvCategory.setTextColor(0xFF5B21B6); break;
            case "Personal":
                h.tvCategory.setBackgroundResource(R.drawable.badge_personal);
                h.tvCategory.setTextColor(0xFF065F46); break;
            default:
                h.tvCategory.setBackgroundResource(R.drawable.badge_urgent);
                h.tvCategory.setTextColor(0xFF991B1B); break;
        }

        // Urgent flag
        h.ivFlag.setVisibility(t.isUrgent ? View.VISIBLE : View.GONE);

        // Card alpha when done
        h.itemView.setAlpha(t.isDone ? 0.6f : 1f);

        // Notify mode label — cycles Off → Notify only → Alarm+Notify
        int mode = t.getNotifyMode();
        switch (mode) {
            case 0:
                h.tvAlarmMode.setText("🔕 Off");
                h.tvAlarmMode.setTextColor(0xFF888780);
                break;
            case 1:
                h.tvAlarmMode.setText("🔔 Notify only");
                h.tvAlarmMode.setTextColor(0xFF6C63FF);
                break;
            case 2:
            default:
                h.tvAlarmMode.setText("🚨 Alarm + Notify");
                h.tvAlarmMode.setTextColor(0xFFE24B4A);
                break;
        }

        // Tap label to cycle mode
        h.tvAlarmMode.setOnClickListener(v -> listener.onCycleNotifyMode(t, pos));

        // Done checkbox
        h.cbDone.setOnClickListener(v -> listener.onToggleDone(t, pos));
        h.cbDone.setChecked(t.isDone);

        // Edit / Delete
        h.btnEdit.setOnClickListener(v   -> listener.onEditTask(t, pos));
        h.btnDelete.setOnClickListener(v -> listener.onDeleteTask(t, pos));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView    tvTitle, tvDueTime, tvCategory, tvAlarmMode;
        CheckBox    cbDone;
        ImageView   ivFlag;
        ImageButton btnEdit, btnDelete;

        TaskViewHolder(View v) {
            super(v);
            tvTitle     = v.findViewById(R.id.tvTaskTitle);
            tvDueTime   = v.findViewById(R.id.tvDueTime);
            tvCategory  = v.findViewById(R.id.tvCategory);
            tvAlarmMode = v.findViewById(R.id.tvAlarmMode);
            cbDone      = v.findViewById(R.id.cbDone);
            ivFlag      = v.findViewById(R.id.ivFlag);
            btnEdit     = v.findViewById(R.id.btnEdit);
            btnDelete   = v.findViewById(R.id.btnDelete);
        }
    }
}