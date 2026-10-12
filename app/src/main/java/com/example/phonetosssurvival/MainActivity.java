package com.example.phonetosssurvival;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class MainActivity extends AppCompatActivity {

    private static final int REAL_PAGE_COUNT = 2; // Expandable to 3+ pages seamlessly
    private static final int INFINITE_COUNT = 10000;

    private ViewPager2 viewPager;
    private View dotCenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupSwipeNavigation();
    }

    private void setupSwipeNavigation() {
        viewPager = findViewById(R.id.viewPager);
        dotCenter = findViewById(R.id.dotCenter);

        // ViewPager2 FragmentStateAdapter
        MainPagerAdapter adapter = new MainPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Initial Starting Position centered in infinite loop (Game Page = real index 0)
        int initialPos = (INFINITE_COUNT / 2) - ((INFINITE_COUNT / 2) % REAL_PAGE_COUNT);
        viewPager.setCurrentItem(initialPos, false);

        // Sync ViewPager2 Swipe -> Carousel Center Dot Glow Pulse
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (dotCenter != null) {
                    dotCenter.setScaleX(0.6f);
                    dotCenter.setScaleY(0.6f);
                    dotCenter.animate()
                            .scaleX(1.15f)
                            .scaleY(1.15f)
                            .setDuration(180)
                            .withEndAction(() -> dotCenter.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120))
                            .start();
                }
            }
        });
    }

    public void navigateToLeaderboard() {
        if (viewPager != null) {
            int currentPos = viewPager.getCurrentItem();
            int realPos = Math.abs(currentPos) % REAL_PAGE_COUNT;
            if (realPos == 0) {
                viewPager.setCurrentItem(currentPos + 1, true);
            }
        }
    }

    private static class MainPagerAdapter extends FragmentStateAdapter {

        MainPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            int realPos = Math.abs(position) % REAL_PAGE_COUNT;
            if (realPos == 0) {
                return new GameFragment();
            } else {
                return new LeaderboardFragment();
            }
        }

        @Override
        public int getItemCount() {
            return INFINITE_COUNT;
        }
    }
}
