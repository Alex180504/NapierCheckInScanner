package com.example.checkinscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.core.Preview;
import androidx.camera.core.CameraSelector;
import android.util.Log;
import androidx.camera.core.ImageAnalysis;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import androidx.appcompat.app.AppCompatActivity;

public class ScanActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private ExecutorService cameraExecutor;
    private androidx.camera.view.PreviewView previewView;
    private TextView scanResult;
    private final List<String> discardedCodes = new ArrayList<>();  // List of codes that have been discarded by the user

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        scanResult = findViewById(R.id.textViewCode);
        previewView = findViewById(R.id.viewFinder);

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();

        ImageButton BackBtn = findViewById(R.id.btn_back2);
        BackBtn.setOnClickListener(v -> finish());

        Button ScanBtn = findViewById(R.id.btn_confirm_scan);
        ScanBtn.setOnClickListener(v -> {
            if (scanResult.getText().length() > 0) {
                Intent intent = new Intent(ScanActivity.this, LoginActivity.class);
                intent.putExtra("code", scanResult.getText().toString());   // Pass the scanned code to the LoginActivity
                startActivity(intent);
            } else {
                Toast.makeText(ScanActivity.this, R.string.toast_no_code, Toast.LENGTH_SHORT).show();
            }
        });

        ImageButton DiscardBtn = findViewById(R.id.btn_discard);
        DiscardBtn.setOnClickListener(v -> {
            String currentCode = scanResult.getText().toString();
            if (!currentCode.isEmpty()) {
                discardedCodes.add(currentCode);
                scanResult.setText(""); // Clear the current code
            }
        });
    }

    private void startCamera() {
        // Get a future that completes when the camera provider is available
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        // Add a listener that runs when the future completes
        cameraProviderFuture.addListener(() -> {
            try {
                // Get the camera provider
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // Bind the camera provider to the preview
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(ScanActivity.this, R.string.toast_camera_launch_error, Toast.LENGTH_SHORT).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this)); // Run the listener on the main thread
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        // Create a Preview use case
        Preview preview = new Preview.Builder().build();

        // Create a CameraSelector object and select the back camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Create an ImageAnalysis use case and set the backpressure strategy
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        // Set an analyzer to process the images
        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            // Get the rotation degree of the image
            int rotationDegrees = image.getImageInfo().getRotationDegrees();
            // Pass image to the ML Kit Vision API
            TextRecognizerOptions options = TextRecognizerOptions.DEFAULT_OPTIONS;
            InputImage inputImage = InputImage.fromMediaImage(Objects.requireNonNull(image.getImage()), rotationDegrees);
            // Process the image and recognize the text
            TextRecognition.getClient(options).process(inputImage)
                    .addOnSuccessListener(this::processTextRecognitionResult)
                    .addOnFailureListener(
                            e -> {
                                Toast.makeText(ScanActivity.this, R.string.toast_ocr_error, Toast.LENGTH_SHORT).show();
                                finish();
                            })
                    .addOnCompleteListener(task -> image.close());
        });

        // Set the surface provider for the preview
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Unbind all use cases before rebinding
        cameraProvider.unbindAll();

        try {
            // Bind the preview and imageAnalysis use cases to the camera lifecycle
            cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis);
        } catch (Exception exc) {
            Log.e("ScanActivity", "Use case binding failed", exc);
            Toast.makeText(this, R.string.toast_error, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // Search the OCR output for a match with the check-in code regex pattern
    private void processTextRecognitionResult(Text texts) {
        String pattern = "^[A-Z]{3} [A-Z]{3}$|^[A-Z]{6}$|^[A-Z]{3}-[A-Z]{3}$|^[A-Z]{2}-[A-Z]{2}-[A-Z]{2}$|^[A-Z]{2} [A-Z]{2} [A-Z]{2}$";
        Pattern r = Pattern.compile(pattern);
        for (Text.TextBlock block : texts.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                Matcher m = r.matcher(line.getText());
                if (m.find()) {
                    Log.d("ScanActivity", "Match found: " + m.group());
                    String c = m.group().replaceAll(" ", "");
                    c = c.replaceAll("-", "");
                    c = c.replaceAll("–", "");
                    if (!discardedCodes.contains(c) && scanResult.getText().length() == 0) {
                        scanResult.setText(c);
                    }
                }
            }
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Error: permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
