package com.example.checkinscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;


public class LoginActivity extends AppCompatActivity {
    private static final int REQUEST_INTERNET_PERMISSION = 1;
    private String code;
    private String email;
    private String password;
    private boolean isOCRLoopFinished = false;  // Check if the OCR + scrolling operation is finished and the check in menu is reached
    private volatile boolean isActivityRunning = false;

    @Override
    protected void onResume() {
        super.onResume();
        isActivityRunning = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityRunning = false;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        code = getIntent().getStringExtra("code");
        DatabaseHelper db = new DatabaseHelper(this);
        ArrayList<String> account = db.getAccount();
        email = account.get(0);
        password = account.get(1);

        // Check for internet permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, REQUEST_INTERNET_PERMISSION);
        }

        // Set up the webview
        WebView webView = findViewById(R.id.webview_check_in);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        View rootView = this.getWindow().getDecorView().getRootView();

        webView.setWebViewClient(new WebViewClient() {
            boolean loginInitiated = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Enable cookies
                CookieManager.getInstance().setAcceptCookie(true);
                CookieManager.getInstance().flush();
                // Check if the webview was redirected to the login page
                if (url.startsWith("https://login.microsoftonline.com/") && !loginInitiated) {
                    loginInitiated = true;
                    isOCRLoopFinished = false;

                    new Handler().postDelayed(() -> {
                        // Fill in the email
                        view.evaluateJavascript("document.querySelector(\"input[type='email']\").value = '" + email + "';", null);

                        // Click on an empty space on the form to unselect the input field (as the form locks up with an error message otherwise)
                        MotionEvent motionEventDown = MotionEvent.obtain(
                                SystemClock.uptimeMillis(),
                                SystemClock.uptimeMillis(),
                                MotionEvent.ACTION_DOWN,
                                view.getWidth() - 1,
                                view.getHeight() - 1,
                                0
                        );
                        MotionEvent motionEventUp = MotionEvent.obtain(
                                SystemClock.uptimeMillis(),
                                SystemClock.uptimeMillis(),
                                MotionEvent.ACTION_UP,
                                view.getWidth() - 1,
                                view.getHeight() - 1,
                                0
                        );
                        view.dispatchTouchEvent(motionEventDown);
                        view.dispatchTouchEvent(motionEventUp);
                        motionEventDown.recycle();
                        motionEventUp.recycle();
                    }, 1000);

                    new Handler().postDelayed(() -> {
                        // Submit the email
                        view.evaluateJavascript("document.querySelector(\"input[type='submit']\").click();", null);
                    }, 2000);

                    // Add a delay before entering the password
                    new Handler().postDelayed(() -> {
                        // Fill in the password
                        view.evaluateJavascript("document.querySelector(\"input[type='password']\").value = '" + password + "';", null);
                        // Delay to avoid the form not registering the password before the next click
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            Log.e("LoginActivity", "Error: " + e.getMessage());
                        }

                        // Click on an empty space on the form to unselect the input field (as the form locks up with an error message otherwise)
                        MotionEvent motionEventDown = MotionEvent.obtain(
                                SystemClock.uptimeMillis(),
                                SystemClock.uptimeMillis(),
                                MotionEvent.ACTION_DOWN,
                                view.getWidth() - 1,
                                view.getHeight() - 1,
                                0
                        );
                        MotionEvent motionEventUp = MotionEvent.obtain(
                                SystemClock.uptimeMillis(),
                                SystemClock.uptimeMillis(),
                                MotionEvent.ACTION_UP,
                                view.getWidth() - 1,
                                view.getHeight() - 1,
                                0
                        );
                        view.dispatchTouchEvent(motionEventDown);
                        view.dispatchTouchEvent(motionEventUp);
                        motionEventDown.recycle();
                        motionEventUp.recycle();
                    }, 3000);


                    // Submit the password
                    new Handler().postDelayed(() -> {
                        view.evaluateJavascript("document.querySelector(\"input[type='submit']\").click();", null);
                    }, 4000);

                    // Click the "Yes" button (In the "Reduce subsequent logins" dialog)
                    // This is done as a loop because the user needs to enter 2FA code first, so the code regularly checks for the presence of the "Yes" button
                    new Handler().postDelayed(() -> CompletableFuture.runAsync(() -> {
                        final boolean[] end = {false};
                        while (!end[0]) {
                            Log.d("LoginActivity", "Yes login button iteration");
                            runOnUiThread(() -> {
                                // Check if the 2FA form is not open (the 2FA form also has a 'submit' input element that does not need to be clicked)
                                view.evaluateJavascript("document.querySelector(\"div[class='displaySign']\") == null;", value -> {
                                    if (Boolean.parseBoolean(value)) {
                                        // Check if the "Yes" button is present
                                        view.evaluateJavascript("document.querySelector(\"input[type='submit']\") != null;", value1 -> {
                                            if (Boolean.parseBoolean(value1)) {
                                                // Click the "Yes" button
                                                view.evaluateJavascript("document.querySelector(\"input[type='submit']\").click();", null);
                                                end[0] = true;
                                            }
                                        });
                                    }
                                });
                            });
                            // Delay before the next check
                            try {
                                Thread.sleep(800);
                            } catch (InterruptedException e) {
                                Log.e("LoginActivity", "Error: " + e.getMessage());
                            }
                        }
                    }), 8000);
                }
                // Check in if the user is on the Napier menu page
                else if (url.startsWith("https://i.napier.ac.uk/campusm/home#menu") && view.getProgress() == 100) {
                    new Handler().postDelayed(() -> {
                        if (!isOCRLoopFinished && view.getUrl().startsWith("https://i.napier.ac.uk/")) { // Check to avoid scrolling after the check in button is clicked
                            // Start the OCR + scrolling operation (search and click the "Check in" menu tile)
                            startOCRCheckInMenuLoop(view);
                        }
                    }, 800);

                    // Check-in operation
                    new Handler().postDelayed(() -> {
                        if (isOCRLoopFinished && view.getUrl().startsWith("https://i.napier.ac.uk/")) {
                            new Handler().postDelayed(() -> {
                                CompletableFuture.runAsync(() -> {
                                    final boolean[] end = {false};
                                    while (!end[0] && isActivityRunning) {
                                        Log.d("LoginActivity", "Check in iteration");
                                        runOnUiThread(() -> {
                                            // The check-in form always starts with the keyboard open, so we can use that to check if the user has selected the event to check-in
                                            if (isKeyboardOpen(rootView)) {
                                                // Auto-type in the check-in code (as the check-in form does not support Javascript, we need to simulate keyboard input)
                                                simulateTyping(code);
                                                hideKeyboard();
                                                new Handler().postDelayed(() -> {
                                                    end[0] = true;
                                                    Bitmap screenshot = getScreenShot(view);
                                                    // Find and click on the "Submit" button
                                                    analyzeScreenAndClick(view, screenshot, result -> {
                                                        if (result) {
                                                            Log.d("LoginActivity", "Check-in submitted");
                                                            // Find and click on the "Finish" button
                                                            new Handler().postDelayed(() -> {
                                                                Bitmap screenshot1 = getScreenShot(view);
                                                                analyzeScreenAndClick(view, screenshot1, result1 -> {
                                                                    if (result1) {
                                                                        Log.d("LoginActivity", "Check-in successful");
                                                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                                        Toast toast = Toast.makeText(getApplicationContext(), "Check-in successful", Toast.LENGTH_SHORT);
                                                                        toast.show();
                                                                        startActivity(intent);
                                                                    }
                                                                }, "Finish");
                                                            }, 1000);
                                                        }
                                                    }, "Submit");
                                                }, 1000);
                                            }
                                        });
                                        // Delay before the next check
                                        try {
                                            Thread.sleep(300);
                                        } catch (InterruptedException e) {
                                            Log.e("LoginActivity", "Error: " + e.getMessage());
                                        }
                                    }
                                });
                            }, 400);
                        }
                    }, 4000);
                }
            }
        });
        webView.loadUrl("https://i.napier.ac.uk/campusm/home");
    }

    public void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void simulateTyping(String text) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && imm.isActive()) {
            BaseInputConnection inputConnection = new BaseInputConnection(getCurrentFocus(), true);
            for (char c : text.toCharArray()) {
                inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, getAndroidKeyCode(c)));
                inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, getAndroidKeyCode(c)));
            }
        }
    }

    private int getAndroidKeyCode(char c) {
        return Character.toUpperCase(c) - 36;
    }

    // Check if the keyboard is open by comparing the visible display frame with the root view
    private boolean isKeyboardOpen(View rootView) {
        final int SOFT_KEYBOARD_HEIGHT_DP_THRESHOLD = 128;
        Rect r = new Rect();
        rootView.getWindowVisibleDisplayFrame(r);
        DisplayMetrics dm = rootView.getResources().getDisplayMetrics();
        int heightDiff = rootView.getBottom() - r.bottom;
        return heightDiff > SOFT_KEYBOARD_HEIGHT_DP_THRESHOLD * dm.density;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_INTERNET_PERMISSION) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Internet permission is required for this app to function", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public static Bitmap getScreenShot(View view) {
        view.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }

    // Start the OCR + scrolling operation to find and click the "Check in" menu tile
    public void startOCRCheckInMenuLoop(WebView view) {
        Bitmap screenshot = getScreenShot(view);
        analyzeScreenAndClick(view, screenshot, result -> {
            if (!result) {
                scroll(view);
                startOCRCheckInMenuLoop(view);
            }
            else {
                Log.d("LoginActivity", "Cycle finished");
                isOCRLoopFinished = true;
            }
        }, "CHECK IN");
    }

    // Simulate a scroll operation on the webview
    public void scroll(WebView view) {
        if (isOCRLoopFinished) {
            return;
        }

        float x = view.getWidth() / 2.0f;
        float yStart = view.getHeight() * 0.75f;
        float yEnd = view.getHeight() * 0.25f;

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, yStart, 0);
        view.dispatchTouchEvent(downEvent);

        long moveTime = SystemClock.uptimeMillis();
        MotionEvent moveEvent = MotionEvent.obtain(downTime, moveTime, MotionEvent.ACTION_MOVE, x, yEnd, 0);
        view.dispatchTouchEvent(moveEvent);

        long upTime = SystemClock.uptimeMillis();
        MotionEvent upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x, yEnd, 0);
        view.dispatchTouchEvent(upEvent);

        downEvent.recycle();
        moveEvent.recycle();
        upEvent.recycle();
        Log.d("LoginActivity", "Scroll");
    }

    public interface AnalyzeCallback {
        void onAnalyzeComplete(boolean result);
    }

    // Analyze a screenshot for a specific text and simulate a user click on its coordinates
    // This is needed as the Napier portal does not support Javascript automation
    public void analyzeScreenAndClick(WebView view, Bitmap screenshot, AnalyzeCallback callback, String text) {
        InputImage image = InputImage.fromBitmap(screenshot, 0);
        TextRecognizerOptions options = TextRecognizerOptions.DEFAULT_OPTIONS;
        TextRecognizer recognizer = TextRecognition.getClient(options);
        recognizer.process(image).addOnSuccessListener(visionText -> {
            boolean stringFound = false;
            for (Text.TextBlock block : visionText.getTextBlocks()) {
                String blockText = block.getText();
                Rect blockFrame = block.getBoundingBox();
                if (blockText.equals(text)) {
                    stringFound = true;
                    assert blockFrame != null;
                    float x = blockFrame.exactCenterX();
                    float y = blockFrame.exactCenterY();
                    MotionEvent motionEvent = MotionEvent.obtain(
                            SystemClock.uptimeMillis(),
                            SystemClock.uptimeMillis(),
                            MotionEvent.ACTION_DOWN,
                            x,
                            y,
                            0
                    );
                    view.dispatchTouchEvent(motionEvent);
                    motionEvent.recycle();
                    MotionEvent upEvent = MotionEvent.obtain(
                            SystemClock.uptimeMillis(),
                            SystemClock.uptimeMillis(),
                            MotionEvent.ACTION_UP,
                            x, y,
                            0
                    );
                    view.dispatchTouchEvent(upEvent);
                    upEvent.recycle();
                    Log.d("LoginActivity", "OCR Click");
                }
            }
            callback.onAnalyzeComplete(stringFound); // Call the callback when operation is complete
        });
    }
}