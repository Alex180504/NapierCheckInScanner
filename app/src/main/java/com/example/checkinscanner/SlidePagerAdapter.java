package com.example.checkinscanner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

// Custom adapter class for ViewPager2 that returns a SlideFragment corresponding to the position in the data set
public class SlidePagerAdapter extends FragmentStateAdapter {
    // Represents each pages to be displayed in the ViewPager2
    private final SlideFragment[] slides;

    public SlidePagerAdapter(@NonNull FragmentActivity fragmentActivity, SlideFragment[] slides) {
        super(fragmentActivity);
        this.slides = slides;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return slides[position];
    }

    @Override
    public int getItemCount() {
        return slides.length;
    }
}