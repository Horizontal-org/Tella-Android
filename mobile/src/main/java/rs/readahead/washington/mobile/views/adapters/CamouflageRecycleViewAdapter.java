package rs.readahead.washington.mobile.views.adapters;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.presentation.entity.CamouflageOption;


public class CamouflageRecycleViewAdapter extends RecyclerView.Adapter<CamouflageRecycleViewAdapter.ViewHolder> {
    private List<CamouflageOption> icons = Collections.emptyList();
    private int selectedPosition;


    public CamouflageRecycleViewAdapter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_camouflage_icon, parent,false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final CamouflageOption icon = icons.get(position);

        holder.iconView.setImageResource(icon.drawableResId);
        holder.nameView.setText(icon.stringResId);

        holder.rootView.setSelected(position == selectedPosition);

        holder.rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedPosition(holder.getAdapterPosition());
            }
        });
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
        @BindView(R.id.root)
        View rootView;
        @BindView(R.id.icon)
        ImageView iconView;
        @BindView(R.id.name)
        TextView nameView;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
