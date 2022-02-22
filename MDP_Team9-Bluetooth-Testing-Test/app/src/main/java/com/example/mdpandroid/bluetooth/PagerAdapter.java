package com.example.mdpandroid.bluetooth;

import android.content.Context;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.mdpandroid.R;
import com.example.mdpandroid.start.MessageBox;


public class PagerAdapter extends FragmentPagerAdapter {

    public enum TabItem {
        MESSAGE(MessageBox.class, R.string.tab_message);

        private final Class<? extends Fragment> fragmentClass;
        private final int titleResId;

        TabItem(Class<? extends Fragment> fragmentClass, @StringRes int titleResId) {
            this.fragmentClass = fragmentClass;
            this.titleResId = titleResId;
        }
    }

    private final TabItem[] tabItems;
    private final Context context;

    public PagerAdapter(FragmentManager fm, Context context, TabItem... tabItems) {
        super(fm);
        this.tabItems = tabItems;
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) { return newInstance(tabItems[position].fragmentClass); }

    private Fragment newInstance(Class<? extends Fragment> fragmentClass) {
        try {
            return fragmentClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("fragment must have public no-arg constructor: " + fragmentClass.getName(), e);
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return context.getString(tabItems[position].titleResId);
    }

    @Override
    public int getCount() { return tabItems.length; }
}
