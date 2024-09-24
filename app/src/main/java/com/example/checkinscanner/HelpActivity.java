package com.example.checkinscanner;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class HelpActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private SlidePagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        SlideFragment[] slides = new SlideFragment[] {
                new SlideFragment(Html.fromHtml("<h2>" + getString(R.string.part_1_title) + "</h2>" + "<br>" + getString(R.string.slide_1_text), Html.FROM_HTML_MODE_COMPACT), R.drawable.screenshot_main_menu_manage_account),
                new SlideFragment(Html.fromHtml("<h2>" + getString(R.string.part_1_title) + "</h2>" + "<br>" + getString(R.string.slide_2_text), Html.FROM_HTML_MODE_COMPACT), R.drawable.screenshot_account_btn),
                new SlideFragment(Html.fromHtml("<h2>" + getString(R.string.part_1_title) + "</h2>" + "<br>" + getString(R.string.slide_3_text), Html.FROM_HTML_MODE_COMPACT), R.drawable.screenshot_new_account_light),
                new SlideFragment(Html.fromHtml("<h2>" + getString(R.string.part_2_title) + "</h2>" + "<br>" + getString(R.string.slide_4_text), Html.FROM_HTML_MODE_COMPACT), R.drawable.screenshot_main_menu_scan),
                new SlideFragment(Html.fromHtml("<h2>" + getString(R.string.part_2_title) + "</h2>" + "<br>" + getString(R.string.slide_5_text), Html.FROM_HTML_MODE_COMPACT), R.drawable.screenshot_camera_start),
                new SlideFragment(Html.fromHtml("<h2>" + getString(R.string.part_2_title) + "</h2>" + "<br>" + getString(R.string.slide_6_text), Html.FROM_HTML_MODE_COMPACT), R.drawable.screenshot_scan),
                new SlideFragment(Html.fromHtml("<h2>" + getString(R.string.part_2_title) + "</h2>" + "<br>" + getString(R.string.slide_7_text), Html.FROM_HTML_MODE_COMPACT), R.drawable.screenshot_scan_reset),
                new SlideFragment(Html.fromHtml("<h2>" + getString(R.string.part_2_title) + "</h2>" + "<br>" + getString(R.string.slide_8_text), Html.FROM_HTML_MODE_COMPACT), R.drawable.screenshot_scan_submit),
                new SlideFragment(Html.fromHtml("<h2>" + getString(R.string.part_3_title) + "</h2>" + "<br>" + getString(R.string.slide_9_text), Html.FROM_HTML_MODE_COMPACT), R.drawable.screenshot_2fa),
                new SlideFragment(Html.fromHtml("<h2>" + getString(R.string.part_3_title) + "</h2>" + "<br>" + getString(R.string.slide_10_text), Html.FROM_HTML_MODE_COMPACT), R.drawable.screenshot_event_selection),
                new SlideFragment(Html.fromHtml("<h2>" + getString(R.string.part_3_title) + "</h2>" + "<br>" + getString(R.string.slide_11_text), Html.FROM_HTML_MODE_COMPACT), R.drawable.screenshit_check_in_final)
        };

        viewPager = findViewById(R.id.view_pager);
        pagerAdapter = new SlidePagerAdapter(this, slides);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setPageTransformer(new ViewPager2.PageTransformer() {
            @Override
            public void transformPage(@NonNull View page, float position) {
                // Fade animation
                page.setTranslationX(-position * page.getWidth());
                page.setAlpha(Math.max(0f, 1 - Math.abs(position) * 4));
                // Horizontal slide animation
                float offset = position * (page.getWidth());
                page.setTranslationX(offset);
            }
        });

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {}).attach();

        ImageButton prevButton = findViewById(R.id.button_prev);
        prevButton.setOnClickListener(v -> viewPager.setCurrentItem(viewPager.getCurrentItem() - 1));

        ImageButton nextButton = findViewById(R.id.button_next);
        nextButton.setOnClickListener(v -> viewPager.setCurrentItem(viewPager.getCurrentItem() + 1));

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                prevButton.setEnabled(position > 0);
                nextButton.setEnabled(position < slides.length - 1);
                if (position == 0) {
                    prevButton.setVisibility(View.INVISIBLE);
                    nextButton.setVisibility(View.VISIBLE);
                } else if (position == slides.length - 1) {
                    prevButton.setVisibility(View.VISIBLE);
                    nextButton.setVisibility(View.INVISIBLE);
                } else {
                    prevButton.setVisibility(View.VISIBLE);
                    nextButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}