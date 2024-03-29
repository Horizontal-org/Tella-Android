package org.hzontal.shared_ui.breadcrumb;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.hzontal.shared_ui.R;
import org.hzontal.shared_ui.breadcrumb.model.IBreadcrumbItem;
import org.hzontal.shared_ui.breadcrumb.model.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BreadcrumbsAdapter extends RecyclerView.Adapter<BreadcrumbsAdapter.ItemHolder> {

	private final int DROPDOWN_OFFSET_Y_FIX;

	private List<IBreadcrumbItem> items;
	private BreadcrumbsCallback callback;

	private BreadcrumbsView parent;

	private int mPopupThemeId = -1;

	public BreadcrumbsAdapter(BreadcrumbsView parent) {
		this(parent, new ArrayList<>());
	}

	public BreadcrumbsAdapter(BreadcrumbsView parent, ArrayList<IBreadcrumbItem> items) {
		this.parent = parent;
		this.items = items;
		DROPDOWN_OFFSET_Y_FIX = parent.getResources().getDimensionPixelOffset(R.dimen.dropdown_offset_y_fix_value);
	}

	public @NonNull <E extends IBreadcrumbItem> List<E> getItems() {
		return (List<E>) this.items;
	}

	public <E extends IBreadcrumbItem> void setItems(@NonNull List<E> items) {
		this.items = (List<IBreadcrumbItem>) items;
	}

	public void setCallback(@Nullable BreadcrumbsCallback callback) {
		this.callback = callback;
	}

	public @Nullable BreadcrumbsCallback getCallback() {
		return this.callback;
	}

	public void setPopupThemeId(@IdRes int popupThemeId) {
		this.mPopupThemeId = popupThemeId;
	}

	@NonNull
    @Override
	public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		if (viewType == R.layout.breadcrumbs_view_item_arrow) {
			return new ArrowIconHolder(inflater.inflate(viewType, parent, false));
		} else if (viewType == R.layout.breadcrumbs_view_item_text) {
			return new BreadcrumbItemHolder(inflater.inflate(viewType, parent, false));
		} else {
			throw new IllegalArgumentException("Unknown view type:" + viewType);
		}
	}

	@Override
	public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
		onBindViewHolder(holder, position, null);
	}

	@Override
	public void onBindViewHolder(@NonNull ItemHolder holder, int position, List<Object> payloads) {
		int viewType = getItemViewType(position);
		int truePos = BreadcrumbsUtil.getTruePosition(viewType, position);
		holder.setItem(items.get(truePos));
	}

	@Override
	public int getItemCount() {
		return BreadcrumbsUtil.getAdapterCount(items);
	}

	@Override
	public int getItemViewType(int position) {
		return BreadcrumbsUtil.getItemViewType(position);
	}

	class BreadcrumbItemHolder extends ItemHolder<IBreadcrumbItem> {

		TextView button;

		BreadcrumbItemHolder(View itemView) {
			super(itemView);
			button = (TextView) itemView;
			// enable touch feedback only for items that have a callback
			if (callback != null) {
				button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						callback.onItemClick(parent, getAdapterPosition() / 2);
					}
				});
			} else {
				button.setClickable(false);
			}
			button.setTextSize(TypedValue.COMPLEX_UNIT_PX, parent.getTextSize());
			button.setPadding(parent.getTextPadding(), parent.getTextPadding(), parent.getTextPadding(), parent.getTextPadding());
		}

		@Override
		public void setItem(@NonNull IBreadcrumbItem item) {
			super.setItem(item);
			button.setText(((Item) item.getSelectedItem()).getName());
			button.setTextColor(getAdapterPosition() == getItemCount() - 1 ? parent.getSelectedTextColor()
					: parent.getTextColor());
			Log.d("Text position", button.getText() + " position : "+ getAdapterPosition());

			if (getAdapterPosition() == 0){
				Drawable img =ResourcesCompat.getDrawable(getContext().getResources(), parent.getHomeIcon(), null);
				assert img != null;
				img.setBounds(0, 0, 60, 60);
				button.setCompoundDrawables(img,null,null,null);
			}
			else {
				button.setCompoundDrawables(null,null,null,null);
			}
		}
	}

	class ArrowIconHolder extends ItemHolder<IBreadcrumbItem> {

		ImageButton imageButton;
		ListPopupWindow popupWindow;

		ArrowIconHolder(View itemView) {
			super(itemView);
			Drawable normalDrawable = getContext().getResources().getDrawable(R.drawable.ic_chevron_right_black_24dp);
			Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
			// DrawableCompat.setTint(wrapDrawable, ViewUtils.getColorFromAttr(getContext(), android.R.attr.textColorSecondary));
			DrawableCompat.setTintList(wrapDrawable, parent.getTextColor());
			imageButton = (ImageButton) itemView;
			imageButton.setImageDrawable(wrapDrawable);
			imageButton.setOnClickListener(view -> {
				if (item.hasMoreSelect()) {
					try {
						popupWindow.show();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			createPopupWindow();
		}

		@RequiresApi(api = Build.VERSION_CODES.KITKAT)
		@Override
		public void setItem(@NonNull IBreadcrumbItem item) {
			super.setItem(item);
			imageButton.setClickable(item.hasMoreSelect());
			if (item.hasMoreSelect()) {
				List<Map<String, String>> list = new ArrayList<>();
				for (Object obj : item.getItems()) {
					Map<String, String> map = new HashMap<>();
					map.put("text", obj.toString());
					list.add(map);
				}
				ListAdapter adapter = new SimpleAdapter(getPopupThemedContext(), list, R.layout.breadcrumbs_view_dropdown_item, new String[] {"text"}, new int[] {android.R.id.text1});
				popupWindow.setAdapter(adapter);
				popupWindow.setWidth(ViewUtils.measureContentWidth(getPopupThemedContext(), adapter));
				imageButton.setOnTouchListener(popupWindow.createDragToOpenListener(imageButton));
			} else {
				imageButton.setOnTouchListener(null);
			}
		}

		private void createPopupWindow() {
			popupWindow = new ListPopupWindow(getPopupThemedContext());
			popupWindow.setAnchorView(imageButton);
			popupWindow.setOnItemClickListener((adapterView, view, i, l) -> {
				if (callback != null) {
					callback.onItemChange(parent, getAdapterPosition() / 2, getItems().get(getAdapterPosition() / 2 + 1).getItems().get(i));
					popupWindow.dismiss();
				}
			});
			imageButton.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					popupWindow.setVerticalOffset(-imageButton.getMeasuredHeight() + DROPDOWN_OFFSET_Y_FIX);
					imageButton.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				}
			});
		}

	}

	class ItemHolder<T> extends RecyclerView.ViewHolder {

		T item;

		ItemHolder(View itemView) {
			super(itemView);
		}

		public void setItem(@NonNull T item) {
			this.item = item;
		}

		Context getContext() {
			return itemView.getContext();
		}

		Context getPopupThemedContext() {
			return mPopupThemeId != -1 ? new ContextThemeWrapper(getContext(), mPopupThemeId) : getContext();
		}

	}

}
