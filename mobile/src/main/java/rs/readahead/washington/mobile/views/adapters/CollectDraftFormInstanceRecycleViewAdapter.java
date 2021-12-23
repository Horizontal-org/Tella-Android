package rs.readahead.washington.mobile.views.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.bus.event.ShowFormInstanceEntryEvent;
import rs.readahead.washington.mobile.databinding.DraftCollectFormRowBinding;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.views.fragment.DraftFormListListener;

public class CollectDraftFormInstanceRecycleViewAdapter extends RecyclerView.Adapter<CollectDraftFormInstanceRecycleViewAdapter.ViewHolder> {
    private List<CollectFormInstance> instances = Collections.emptyList();
    private final DraftFormListListener listener;

    public CollectDraftFormInstanceRecycleViewAdapter(DraftFormListListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        DraftCollectFormRowBinding binding = DraftCollectFormRowBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final CollectFormInstance instance = instances.get(position);

        holder.binding.formName.setText(instance.getInstanceName());
        holder.binding.serverName.setText(instance.getServerName());
        holder.binding.formRow.setOnClickListener(v -> MyApplication.bus().post(new ShowFormInstanceEntryEvent(instance.getId())));
        holder.binding.menu.setOnClickListener(v -> listener.showDeleteBottomSheet(instance));
    }

    @Override
    public int getItemCount() {
        return instances.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setInstances(List<CollectFormInstance> forms) {
        this.instances = forms;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public DraftCollectFormRowBinding binding;

        ViewHolder(DraftCollectFormRowBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }
    }
}
