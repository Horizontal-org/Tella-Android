package org.horizontal.tella.mobile.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.List;
import org.horizontal.tella.mobile.databinding.CardCamouflageIconBinding;
import org.horizontal.tella.mobile.presentation.entity.CamouflageOption;


public class CamouflageRecycleViewAdapter extends RecyclerView.Adapter<CamouflageRecycleViewAdapter.ViewHolder> {
    private List<CamouflageOption> icons = Collections.emptyList();
    private int selectedPosition;


    public CamouflageRecycleViewAdapter() {
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        org.horizontal.tella.mobile.databinding.CardCamouflageIconBinding itemBinding = CardCamouflageIconBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final CamouflageOption icon = icons.get(position);

        holder.iconView.setImageResource(icon.drawableResId);
        holder.nameView.setText(icon.stringResId);

        holder.rootView.setSelected(position == selectedPosition);

        holder.rootView.setOnClickListener(v -> setSelectedPosition(holder.getAdapterPosition()));

    }

    @Override
    public int getItemCount() {
        return icons.size();
    }

    public void setIcons(List<CamouflageOption> icons, int selectedPosition) {
        this.icons = icons;
        this.selectedPosition = selectedPosition;
        notifyDataSetChanged();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    private void setSelectedPosition(int position) {
        if (selectedPosition == position) {
            return;
        }

        notifyItemChanged(selectedPosition);
        selectedPosition = position;
        notifyItemChanged(selectedPosition);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardCamouflageIconBinding binding;
        View rootView;
        ImageView iconView;
        TextView nameView;

        public ViewHolder(CardCamouflageIconBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            rootView = binding.getRoot();
            iconView = binding.icon;
            nameView = binding.name;
        }
    }
}
