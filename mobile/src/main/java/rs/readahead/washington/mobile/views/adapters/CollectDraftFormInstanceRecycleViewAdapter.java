package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.databinding.DraftCollectFormInstanceRowBinding;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.interfaces.ISavedFormsInterface;


public class CollectDraftFormInstanceRecycleViewAdapter extends RecyclerView.Adapter<CollectDraftFormInstanceRecycleViewAdapter.ViewHolder> {
    private List<CollectFormInstance> instances = Collections.emptyList();
    private final ISavedFormsInterface draftFormsInterface;
    private DraftCollectFormInstanceRowBinding itemBinding;

    public CollectDraftFormInstanceRecycleViewAdapter(ISavedFormsInterface draftFormsInterface) {
        this.draftFormsInterface = draftFormsInterface;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        itemBinding = DraftCollectFormInstanceRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final CollectFormInstance instance = instances.get(position);
        final Context context = holder.binding.name.getContext();

        holder.binding.instanceRow.setOnClickListener(v -> draftFormsInterface.showFormInstance(instance));
        holder.binding.name.setText(instance.getInstanceName());
        holder.binding.organization.setText(instance.getServerName());
        holder.binding.updated.setText(String.format(context.getString(R.string.collect_draft_meta_date_updated),
                Util.getDateTimeString(instance.getUpdated())));
        holder.binding.popupMenu.setOnClickListener(v -> draftFormsInterface.showFormsMenu(instance));
    }

    @Override
    public int getItemCount() {
        return instances.size();
    }

    public void setInstances(List<CollectFormInstance> forms) {
        this.instances = forms;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        DraftCollectFormInstanceRowBinding binding;

        ViewHolder(DraftCollectFormInstanceRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
