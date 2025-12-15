package com.example.holly_renfrew_weight_tracker_project;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WeightAdapter extends RecyclerView.Adapter<WeightAdapter.WeightViewHolder> {

    private final List<WeightEntry> weightList;
    private final Context context;
    private final DatabaseHelper dbHelper;
    private final OnWeightChangeListener listener;

    public interface OnWeightChangeListener {
        void onWeightUpdated();
        void onWeightDeleted();
    }

    public WeightAdapter(Context context, DatabaseHelper dbHelper, List<WeightEntry> weightList,
                         OnWeightChangeListener listener) {
        this.context = context;
        this.dbHelper = dbHelper;
        this.weightList = weightList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WeightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.weight_item, parent, false);
        return new WeightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeightViewHolder holder, int position) {
        WeightEntry entry = weightList.get(position);
        holder.textView.setText(entry.getDisplayText());

        holder.buttonUpdate.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Update Weight");

            final EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setText(String.valueOf(entry.getWeight()));
            builder.setView(input);

            builder.setPositiveButton("Update", (dialog, which) -> {
                String text = input.getText().toString().trim();
                if (text.isEmpty()) {
                    Toast.makeText(context, "Weight cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    double newWeight = Double.parseDouble(text);
                    if (dbHelper.updateWeight(entry.getId(), newWeight)) {
                        entry.setWeight(newWeight);
                        notifyItemChanged(position);
                        if (listener != null) listener.onWeightUpdated();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Invalid number format", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (dbHelper.deleteWeight(entry.getId())) {
                weightList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, weightList.size());
                if (listener != null) listener.onWeightDeleted();
            }
        });
    }

    @Override
    public int getItemCount() {
        return weightList.size();
    }

    /** Update the adapter with new weights */
    public void setWeights(List<WeightEntry> newList) {
        weightList.clear();
        weightList.addAll(newList);
        notifyDataSetChanged();
    }

    public static class WeightViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;
        final ImageButton buttonUpdate, buttonDelete;

        public WeightViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textWeightEntry);
            buttonUpdate = itemView.findViewById(R.id.buttonUpdateEntry);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteEntry);
        }
    }
}
