package com.example.holly_renfrew_weight_tracker_project;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class WeightAdapter extends RecyclerView.Adapter<WeightAdapter.WeightViewHolder> {

    private List<WeightEntry> weightList;
    private final Context context;
    private final DatabaseHelper dbHelper;
    private final OnWeightChangeListener listener;


    // -------------------------------------------------------
    // CALLBACK INTERFACE FOR DASHBOARD
    // -------------------------------------------------------
    public interface OnWeightChangeListener {
        void onWeightUpdated();
        void onWeightDeleted();
    }


    public WeightAdapter(Context context, DatabaseHelper dbHelper,
                         List<WeightEntry> weightList,
                         OnWeightChangeListener listener) {

        this.context = context;
        this.weightList = weightList;
        this.dbHelper = dbHelper;
        this.listener = listener;
    }


    // -------------------------------------------------------
    // UPDATE LIST WHEN CHANGED (THREAD SAFE THROUGH DASHBOARD)
    // -------------------------------------------------------
    public void setWeights(List<WeightEntry> newList) {
        this.weightList = newList;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public WeightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.weight_item, parent, false);
        return new WeightViewHolder(row);
    }


    @Override
    public void onBindViewHolder(@NonNull WeightViewHolder holder, int position) {
        WeightEntry entry = weightList.get(position);

        String formattedText = entry.getDate() + " — " + entry.getWeight() + " lbs";
        holder.textWeightEntry.setText(formattedText);

        // -------------------------------------------------------
        // UPDATE ENTRY: OPEN DIALOG FOR NEW VALUE
        // -------------------------------------------------------
        holder.buttonUpdateEntry.setOnClickListener(v -> showUpdateDialog(entry));

        // -------------------------------------------------------
        // DELETE ENTRY
        // -------------------------------------------------------
        holder.buttonDeleteEntry.setOnClickListener(v -> new AlertDialog.Builder(context)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to remove this entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean success = dbHelper.deleteWeight(entry.getId());
                    if (success && listener != null) listener.onWeightDeleted();
                })
                .setNegativeButton("Cancel", null)
                .show());
    }


    @Override
    public int getItemCount() {
        return weightList.size();
    }


    // -------------------------------------------------------
    // DIALOG FOR UPDATING AN ENTRY'S WEIGHT
    // -------------------------------------------------------
    private void showUpdateDialog(WeightEntry entry) {
        EditText edit = new EditText(context);
        edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edit.setHint("Enter new weight");
        edit.setText(String.valueOf(entry.getWeight()));

        new AlertDialog.Builder(context)
                .setTitle("Update Weight Entry")
                .setMessage("Enter the new weight:")
                .setView(edit)
                .setPositiveButton("Update", (dialog, which) -> {

                    String newWeightStr = edit.getText().toString().trim();
                    if (newWeightStr.isEmpty()) return;

                    try {
                        double newWeight = Double.parseDouble(newWeightStr);
                        boolean updated = dbHelper.updateWeight(entry.getId(), newWeight);

                        if (updated && listener != null) listener.onWeightUpdated();

                    } catch (NumberFormatException ignored) {}

                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    // -------------------------------------------------------
    // VIEW HOLDER
    // -------------------------------------------------------
    public static class WeightViewHolder extends RecyclerView.ViewHolder {

        TextView textWeightEntry;
        ImageButton buttonUpdateEntry;
        ImageButton buttonDeleteEntry;

        public WeightViewHolder(@NonNull View itemView) {
            super(itemView);

            textWeightEntry = itemView.findViewById(R.id.textWeightEntry);
            buttonUpdateEntry = itemView.findViewById(R.id.buttonUpdateEntry);
            buttonDeleteEntry = itemView.findViewById(R.id.buttonDeleteEntry);
        }
    }
}
