package com.document.hospicecalllog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SummaryAdapter extends RecyclerView.Adapter<SummaryAdapter.SummaryViewHolder> {
    private List<ActionCount> actionCounts;
    
    public SummaryAdapter(List<ActionCount> actionCounts) {
        this.actionCounts = actionCounts;
    }
    
    @NonNull
    @Override
    public SummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_summary, parent, false);
        return new SummaryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SummaryViewHolder holder, int position) {
        ActionCount actionCount = actionCounts.get(position);
        holder.bind(actionCount);
    }
    
    @Override
    public int getItemCount() {
        return actionCounts.size();
    }
    
    public void updateActionCounts(List<ActionCount> newActionCounts) {
        this.actionCounts = newActionCounts;
        notifyDataSetChanged();
    }
    
    static class SummaryViewHolder extends RecyclerView.ViewHolder {
        private TextView actionText;
        private TextView countText;
        
        public SummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            actionText = itemView.findViewById(R.id.actionText);
            countText = itemView.findViewById(R.id.countText);
        }
        
        public void bind(ActionCount actionCount) {
            actionText.setText(actionCount.getAction() != null ? actionCount.getAction() : "No Action");
            countText.setText(String.valueOf(actionCount.getCount()));
        }
    }
}
