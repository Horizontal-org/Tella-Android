package org.hzontal.shared_ui.breadcrumb;

import static org.hzontal.shared_ui.BuildConfig.LIBRARY_PACKAGE_NAME;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.hzontal.shared_ui.R;
import org.hzontal.shared_ui.breadcrumb.model.IBreadcrumbItem;

import java.util.ArrayList;
import java.util.List;

import static org.hzontal.shared_ui.BuildConfig.LIBRARY_PACKAGE_NAME;

public class BreadcrumbsView extends FrameLayout {

	/**
	 * Internal implement of BreadcrumbsView
	 */
	private RecyclerView mRecyclerView;
	private BreadcrumbsAdapter mAdapter;

	/**
	 * Popup Menu Theme Id
	 */
	private int mPopupThemeId = -1;

	private ColorStateList mTextColor;
	private ColorStateList mSelectedTextColor;
	private int mTextSize;
	private int mTextPadding;
	private int mHomeIcon;

    private static final String KEY_SUPER_STATES = LIBRARY_PACKAGE_NAME + ".superStates";
    private static final String KEY_BREADCRUMBS = LIBRARY_PACKAGE_NAME + ".breadcrumbs";

	public BreadcrumbsView(Context context) {
		this(context, null);
	}

	public BreadcrumbsView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public BreadcrumbsView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BreadcrumbsView, defStyleAttr, R.style.BreadcrumbsView);
			mPopupThemeId = a.getResourceId(R.styleable.BreadcrumbsView_popupTheme, -1);
			mTextColor = a.getColorStateList(R.styleable.BreadcrumbsView_crumbsTextColor);
			mSelectedTextColor = a.getColorStateList(R.styleable.BreadcrumbsView_crumbsSelectedTextColor);
            mTextSize = a.getDimensionPixelSize(R.styleable.BreadcrumbsView_crumbsTextSize, -1);
            mTextPadding = a.getDimensionPixelSize(R.styleable.BreadcrumbsView_crumbsPadding, -1);
			mHomeIcon = a.getResourceId(R.styleable.BreadcrumbsView_homeIcon, -1);
			a.recycle();
		}

		init();
	}

	/**
	 * Init BreadcrumbsView
	 */
	private void init() {
		// Init RecyclerView
		if (mRecyclerView == null) {
			ViewGroup.LayoutParams rvLayoutParams = new ViewGroup.LayoutParams(-1, -1);
			rvLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
			mRecyclerView = new RecyclerView(getContext());

			// Create Horizontal LinearLayoutManager
			BreadcrumbsLayoutManager layoutManager = new BreadcrumbsLayoutManager(getContext(), RecyclerView.HORIZONTAL, ViewUtils.isRtlLayout(getContext()));
			mRecyclerView.setLayoutManager(layoutManager);
			mRecyclerView.setOverScrollMode(OVER_SCROLL_NEVER);

			// Add RecyclerView
			addView(mRecyclerView, rvLayoutParams);
		}
		// Init Adapter
		if (mAdapter == null) {
			mAdapter = new BreadcrumbsAdapter(this);
			if (mPopupThemeId != -1) {
				mAdapter.setPopupThemeId(mPopupThemeId);
			}
		}
		mRecyclerView.setAdapter(mAdapter);
	}

	/**
	 * Get breadcrumb items list
	 *
	 * @return Breadcrumb Items
	 */
	public @NonNull List<IBreadcrumbItem> getItems() {
		return mAdapter.getItems();
	}

	/**
	 * Get current breadcrumb item
	 *
	 * @return Current item
	 */
	public @NonNull <E extends IBreadcrumbItem> E getCurrentItem() {
		return mAdapter.<E>getItems().get(mAdapter.getItems().size() - 1);
	}

	/**
	 * Set breadcrumb items list
	 *
	 * @param items Target list
	 */
	public <E extends IBreadcrumbItem> void setItems(@NonNull List<E> items) {
		mAdapter.setItems(items);
		mAdapter.notifyDataSetChanged();
		postDelayed(() -> performSafeSmoothScrollToPosition(mAdapter.getItemCount() - 1), 500);
	}

	private void performSafeSmoothScrollToPosition(int position) {
		if (position >= 0 && position < mAdapter.getItemCount()) {
			mRecyclerView.smoothScrollToPosition(position);
		}
	}

	/**
	 * Set breadcrumb items list and animates them correctly with recyclerview diff
	 *
	 * @param items Target list
	 */
	public <E extends IBreadcrumbItem> void updateItems(@NonNull List<E> items) {
		DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new BreadcrumbsDiffCallback((List<IBreadcrumbItem>)items, mAdapter.getItems()));
		mAdapter.setItems(items);
		diffResult.dispatchUpdatesTo(mAdapter);
		postScroll(-1, 0);
	}

	/**
	 * Notify which item has been changed to update text view
	 *
	 * @param index The item position
	 */
	public void notifyItemChanged(int index) {
		mAdapter.notifyItemChanged(index * 2);
	}

	/**
	 * Add a new item
	 *
	 * @param item New item
	 */
	public <E extends IBreadcrumbItem> void addItem(@NonNull E item) {
		int oldSize = mAdapter.getItemCount();
		mAdapter.getItems().add(item);
		mAdapter.notifyItemRangeInserted(oldSize, 2);
		mAdapter.notifyItemChanged(oldSize - 1);
		postDelayed(new Runnable() {
			@Override
			public void run() {
				performSafeSmoothScrollToPosition(mAdapter.getItemCount() - 1);
			}
		}, 500);
	}

	/**
	 * Remove items after a position
	 *
	 * @param afterPos The first position of the removing range
	 */
	public void removeItemAfter(final int afterPos) {
		if (afterPos <= mAdapter.getItems().size() - 1) {
			int oldSize = mAdapter.getItemCount();
			while (mAdapter.getItems().size() > afterPos) {
				mAdapter.getItems().remove(mAdapter.getItems().size() - 1);
			}
			mAdapter.notifyItemRangeRemoved(afterPos * 2 - 1, oldSize - afterPos);
			/* Add delay time to fix animation */
			postDelayed(() -> {
				int currentPos = afterPos * 2 - 1 - 1;
				mAdapter.notifyItemChanged(currentPos);
				performSafeSmoothScrollToPosition(currentPos);
			}, 100);
		}
	}

	/**
	 * Remove last item
	 */
	public void removeLastItem() {
		removeItemAfter(mAdapter.getItems().size() - 1);
	}

	/**
	 * Set BreadcrumbsView callback (Recommend to use DefaultBreadcrumbsCallback)
	 *
	 * @param callback Callback should be set
	 * @see BreadcrumbsCallback
	 * @see DefaultBreadcrumbsCallback
	 */
	public <T> void setCallback(@Nullable BreadcrumbsCallback<T> callback) {
		mAdapter.setCallback(callback);
	}

	/**
	 * Get callback
	 *
	 * @return Callback
	 * @see BreadcrumbsCallback
	 */
	public @Nullable <T> BreadcrumbsCallback<T> getCallback() {
		return mAdapter.getCallback();
	}

	// Save/Restore View Instance State
	@Override
	public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_SUPER_STATES, super.onSaveInstanceState());
        bundle.putParcelableArrayList(KEY_BREADCRUMBS, new ArrayList<>(mAdapter.getItems()));
        return bundle;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            try {
				super.onRestoreInstanceState(bundle.getParcelable(KEY_SUPER_STATES));
				setItems(bundle.getParcelableArrayList(KEY_BREADCRUMBS));
				return;
			} catch (RuntimeException exception){
				super.onRestoreInstanceState(BaseSavedState.EMPTY_STATE);
			}
		}
		super.onRestoreInstanceState(BaseSavedState.EMPTY_STATE);
	}

	private void postScroll(final int index, final int delay) {
		mRecyclerView.postDelayed(() -> {
			int max = mAdapter.getItemCount() - 1;
			int i = index == -1 ? max : Math.max(index, max);
			performSafeSmoothScrollToPosition(i);
		}, delay);
	}

	public ColorStateList getTextColor() {
		return mTextColor;
	}

	public ColorStateList getSelectedTextColor() {
		return mSelectedTextColor;
	}

	public int getTextSize() {
	    return mTextSize;
    }

    public int getTextPadding() {
	    return mTextPadding;
    }
	public int getHomeIcon() {
		return mHomeIcon;
	}
}
