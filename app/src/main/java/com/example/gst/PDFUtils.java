package com.example.gst;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class PDFUtils {

    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private final PdfDocument document;
    private int pageNumber;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public PDFUtils() {
        document = new PdfDocument();
        pageNumber = 1;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void createPdf(Context context, String fileName) {
        if (hasWritePermission(context)) {
            // Write the document content
            File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "BillingPDFs");
            boolean success = folder.mkdirs();
            if (!success) {
                // Handle failure to create directories
                Toast.makeText(context, "Failed to create directories for PDF", Toast.LENGTH_LONG).show();
                return;
            }
            String targetPdf = fileName + ".pdf";
            File filePath = new File(folder, targetPdf);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    document.writeTo(Files.newOutputStream(filePath.toPath()));
                }
                Toast.makeText(context, "File saved: " + filePath, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, "Something went wrong: " + e, Toast.LENGTH_LONG).show();
            }

            // Close the document
            document.close();

            // Open the saved PDF file
            Uri uri = Uri.fromFile(filePath);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setDataAndType(uri, "application/pdf");
            context.startActivity(intent);
        } else {
            requestWritePermission(context);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void addPageToPDF(View printView) {
        Bitmap bitmap = getBitmapFromView(printView);

        int PAGE_WIDTH = 210 * 8;
        int PAGE_HEIGHT = 297 * 8;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        canvas.drawPaint(paint);

        bitmap = Bitmap.createScaledBitmap(bitmap, PAGE_WIDTH, PAGE_HEIGHT, true);

        paint.setColor(Color.BLUE);
        canvas.drawBitmap(bitmap, 0, 0, null);
        document.finishPage(page);

        pageNumber++;
    }

    public static Bitmap getBitmapFromView(View view) {
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null)
            bgDrawable.draw(canvas);
        else
            canvas.drawColor(Color.WHITE);
        view.draw(canvas);
        return returnedBitmap;
    }

    private boolean hasWritePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestWritePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }
}
