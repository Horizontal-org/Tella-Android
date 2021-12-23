package rs.readahead.washington.mobile.views.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.bus.event.ShowBlankFormEntryEvent;
import rs.readahead.washington.mobile.databinding.BlankCollectFormRowBinding;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.views.fragment.BlankFormListListener;


public class CollectDownloadedFormInstanceRecycleViewAdapter extends RecyclerView.Adapter<CollectDownloadedFormInstanceRecycleViewAdapter.ViewHolder> {
    private List<CollectForm> forms = Collections.emptyList();
    private final BlankFormListListener listener;

    public CollectDownloadedFormInstanceRecycleViewAdapter(BlankFormListListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BlankCollectFormRowBinding binding = BlankCollectFormRowBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final CollectForm form = forms.get(position);

        holder.binding.formName.setText(form.getForm().getName());
        holder.binding.serverName.setText(form.getServerName());
        holder.binding.formRow.setOnClickListener(v -> MyApplication.bus().post(new ShowBlankFormEntryEvent(form)));
        holder.binding.menu.setOnClickListener(v -> listener.showDeleteBottomSheet(form));
        // todo: handle star click
    }

    @Override
    public int getItemCount() {
        return forms.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setForms(List<CollectForm> forms) {
        this.forms = forms;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public BlankCollectFormRowBinding binding;

        ViewHolder(BlankCollectFormRowBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }
    }
}
