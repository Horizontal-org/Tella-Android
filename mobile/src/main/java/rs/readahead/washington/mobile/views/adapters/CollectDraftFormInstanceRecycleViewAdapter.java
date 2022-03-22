package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.ShowFormInstanceEntryEvent;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.interfaces.ISavedFormsInterface;


public class CollectDraftFormInstanceRecycleViewAdapter extends RecyclerView.Adapter<CollectDraftFormInstanceRecycleViewAdapter.ViewHolder> {
    private List<CollectFormInstance> instances = Collections.emptyList();
    private final ISavedFormsInterface draftFormsInterface;

    public CollectDraftFormInstanceRecycleViewAdapter(ISavedFormsInterface draftFormsInterface) {
        this.draftFormsInterface = draftFormsInterface;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.draft_collect_form_instance_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final CollectFormInstance instance = instances.get(position);
        final Context context = holder.name.getContext();

        holder.instanceRow.setOnClickListener(v -> MyApplication.bus().post(new ShowFormInstanceEntryEvent(instance.getId())));
        holder.name.setText(instance.getInstanceName());
        holder.organization.setText(instance.getServerName());
        holder.updated.setText(String.format(context.getString(R.string.collect_draft_meta_date_updated),
                Util.getDateTimeString(instance.getUpdated())));
        holder.popupMenu.setOnClickListener(v -> draftFormsInterface.showFormsMenu(instance));
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
        @BindView(R.id.instanceRow)
        ViewGroup instanceRow;
        @BindView(R.id.name)
        TextView name;
        @BindView(R.id.organization)
        TextView organization;
        @BindView(R.id.updated)
        TextView updated;
        @BindView(R.id.popupMenu)
        ImageButton popupMenu;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
