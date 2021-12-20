package rs.readahead.washington.mobile.views.adapters;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;


public class ViewPagerAdapter extends FragmentPagerAdapter {
    private final List<FragmentHolder> fragments = new ArrayList<>();


    public ViewPagerAdapter(FragmentManager manager) {
        super(manager);
    }

    public void addFragment(Fragment fragment, String title) {
        fragments.add(new FragmentHolder(title, fragment));
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position).getFragment();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return fragments.get(position).getTitle();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }


    private static class FragmentHolder {
        private String title;
        private Fragment fragment;

        FragmentHolder(String title, Fragment fragment) {
            this.title = title;
            this.fragment = fragment;
        }

        public String getTitle() {
            return title;
        }

        public Fragment getFragment() {
            return fragment;
        }
    }
}
