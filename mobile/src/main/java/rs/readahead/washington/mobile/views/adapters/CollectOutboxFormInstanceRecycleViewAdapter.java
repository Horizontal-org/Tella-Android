package rs.readahead.washington.mobile.views.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.bus.event.ReSubmitFormInstanceEvent;
import rs.readahead.washington.mobile.databinding.OutboxCollectFormRowBinding;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.views.fragment.OutboxFormListListener;

public class CollectOutboxFormInstanceRecycleViewAdapter extends RecyclerView.Adapter<CollectOutboxFormInstanceRecycleViewAdapter.ViewHolder> {
    private List<CollectFormInstance> instances = Collections.emptyList();
    private final OutboxFormListListener listener;

    public CollectOutboxFormInstanceRecycleViewAdapter(OutboxFormListListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        OutboxCollectFormRowBinding binding = OutboxCollectFormRowBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final CollectFormInstance instance = instances.get(position);

//        CollectFormInstanceStatus status = instance.getStatus();

        holder.binding.formName.setText(instance.getFormName());
        holder.binding.serverName.setText(instance.getServerName());
        holder.binding.formRow.setOnClickListener(v -> MyApplication.bus().post(new ReSubmitFormInstanceEvent(instance)));
        holder.binding.menu.setOnClickListener(v -> listener.showOptionsBottomSheet(instance));
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
        public OutboxCollectFormRowBinding binding;

        ViewHolder(OutboxCollectFormRowBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }
    }
}
