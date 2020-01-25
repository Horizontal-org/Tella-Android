package rs.readahead.washington.mobile.views.adapters;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.DeleteFormInstanceEvent;
import rs.readahead.washington.mobile.bus.event.ShowFormInstanceEntryEvent;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.util.Util;


public class CollectDraftFormInstanceRecycleViewAdapter extends RecyclerView.Adapter<CollectDraftFormInstanceRecycleViewAdapter.ViewHolder> {
    private List<CollectFormInstance> instances = Collections.emptyList();

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.draft_collect_form_instance_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final CollectFormInstance instance = instances.get(position);

        final Context context = holder.name.getContext();

        holder.name.setText(instance.getInstanceName());
        holder.organization.setText(instance.getServerName());
        holder.updated.setText(String.format(context.getString(R.string.ra_updated_with_param),
                Util.getDateTimeString(instance.getUpdated())));
        holder.instanceRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApplication.bus().post(new ShowFormInstanceEntryEvent(instance.getId()));
            }
        });
        holder.popupMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(context, v);

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        MyApplication.bus().post(new DeleteFormInstanceEvent(instance.getId(), instance.getStatus()));
                        return true;
                    }
                });
                popup.inflate(R.menu.draft_forms_list_item_menu);
                popup.show();
            }
        });
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
