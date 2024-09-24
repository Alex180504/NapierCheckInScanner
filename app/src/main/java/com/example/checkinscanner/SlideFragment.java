package com.example.checkinscanner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

// Custom Fragment class that represents a single slide in the ViewPager2.
public class SlideFragment extends Fragment {
    private final CharSequence text;  // Text to be displayed in the slide
    private final int imageResId;   // Resource ID of the image to be displayed in the slide

    public SlideFragment(CharSequence text, int imageResId) {
        this.text = text;
        this.imageResId = imageResId;
    }

    // Method to have the fragment instantiate its user interface view
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_slide, container, false);
        TextView textView = view.findViewById(R.id.slide_text);
        ImageView imageView = view.findViewById(R.id.slide_image);

        textView.setText(text);
        imageView.setImageResource(imageResId);

        return view;
    }
}