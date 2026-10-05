package com.example.samplestickerapp;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class CreativeStudioActivity extends BaseActivity {
    private static final int PICK_IMAGE = 1001;
    private ImageView preview;
    private TextView status;
    private Bitmap currentBitmap;
    private Bitmap originalBitmap;
    private Uri lastSavedUri;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creative_studio);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        preview = findViewById(R.id.studio_preview);
        status = findViewById(R.id.studio_status);
        findViewById(R.id.action_create).setOnClickListener(v -> pickImage());
        findViewById(R.id.action_remove_bg).setOnClickListener(v -> removeBackground());
        findViewById(R.id.action_border).setOnClickListener(v -> addWhiteBorder());
        findViewById(R.id.action_animate).setOnClickListener(v -> animatePreview());
        findViewById(R.id.action_save).setOnClickListener(v -> saveSticker());
        findViewById(R.id.action_share).setOnClickListener(v -> shareSticker());
        findViewById(R.id.action_clear).setOnClickListener(v -> clearStudio());
        findViewById(R.id.action_packs).setOnClickListener(v -> openStickerPacks());
        findViewById(R.id.action_ai).setOnClickListener(v -> showAiInfo());
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_IMAGE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) { }
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
            if (bitmap == null) throw new IllegalStateException("No se pudo leer la imagen");
            bitmap = resizeForSticker(bitmap, 1600);
            originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            preview.setImageBitmap(currentBitmap);
            status.setText("Imagen lista. Ahora puedes quitar fondo, poner borde o animarla.");
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private static Bitmap resizeForSticker(Bitmap source, int maxSide) {
        int w = source.getWidth(), h = source.getHeight(), longest = Math.max(w, h);
        if (longest <= maxSide) return source;
        float scale = maxSide / (float) longest;
        return Bitmap.createScaledBitmap(source, Math.max(1, Math.round(w * scale)),
                Math.max(1, Math.round(h * scale)), true);
    }

    private boolean requireImage() {
        if (currentBitmap != null) return true;
        Toast.makeText(this, "Primero pulsa Crear Sticker y elige una imagen", Toast.LENGTH_SHORT).show();
        return false;
    }

    private void removeBackground() {
        if (!requireImage()) return;
        status.setText("Eliminando fondo local…");
        new RemoveBackgroundTask(this).execute(currentBitmap.copy(Bitmap.Config.ARGB_8888, true));
    }

    private void addWhiteBorder() {
        if (!requireImage()) return;
        int radius = Math.max(3, Math.min(18, Math.round(Math.min(currentBitmap.getWidth(), currentBitmap.getHeight()) / 45f)));
        Bitmap src = currentBitmap;
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint white = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        white.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        for (int y = -radius; y <= radius; y++) for (int x = -radius; x <= radius; x++)
            if (x * x + y * y <= radius * radius) canvas.drawBitmap(src, x, y, white);
        canvas.drawBitmap(src, 0, 0, new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        currentBitmap = out;
        preview.setImageBitmap(currentBitmap);
        status.setText("Borde blanco aplicado.");
    }

    private void animatePreview() {
        if (!requireImage()) return;
        AlphaAnimation pulse = new AlphaAnimation(0.72f, 1f);
        pulse.setDuration(420);
        pulse.setRepeatMode(AlphaAnimation.REVERSE);
        pulse.setRepeatCount(5);
        preview.startAnimation(pulse);
        status.setText("Vista previa animada activada. La exportación GIF/WebP será la siguiente fase.");
    }

    private void saveSticker() {
        if (!requireImage()) return;
        try {
            String name = "ashley_sticker_" + System.currentTimeMillis() + ".png";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Ashley Stickers");
                ContentResolver resolver = getContentResolver();
                Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IllegalStateException("No se pudo crear el archivo");
                try (OutputStream out = resolver.openOutputStream(uri)) { currentBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); }
                lastSavedUri = uri;
            } else {
                File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Ashley Stickers");
                if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("No se pudo crear la carpeta");
                File file = new File(dir, name);
                try (FileOutputStream out = new FileOutputStream(file)) { currentBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); }
                lastSavedUri = Uri.fromFile(file);
            }
            status.setText("Sticker guardado en Ashley Stickers.");
            Toast.makeText(this, "Sticker guardado", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo guardar el sticker", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareSticker() {
        if (!requireImage()) return;
        if (lastSavedUri == null) saveSticker();
        if (lastSavedUri == null) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/png");
        share.putExtra(Intent.EXTRA_STREAM, lastSavedUri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "Compartir sticker"));
    }

    private void clearStudio() {
        preview.clearAnimation();
        currentBitmap = null;
        originalBitmap = null;
        lastSavedUri = null;
        preview.setImageResource(R.drawable.studio_empty);
        status.setText("Listo para crear otro sticker.");
    }

    private void showAiInfo() {
        new AlertDialog.Builder(this).setTitle("Generar con IA")
                .setMessage("La interfaz ya está preparada para conectar un modelo de IA. En esta primera versión no inventamos una API ni una clave: el editor local funciona sin conexión.")
                .setPositiveButton("Entendido", null).show();
    }

    private void openStickerPacks() { new LoadPacksTask(this).execute(); }

    private static class RemoveBackgroundTask extends AsyncTask<Bitmap, Void, Bitmap> {
        private final WeakReference<CreativeStudioActivity> activityRef;
        RemoveBackgroundTask(CreativeStudioActivity activity) { activityRef = new WeakReference<>(activity); }
        @Override protected Bitmap doInBackground(Bitmap... input) { return floodFillCorners(input[0]); }
        @Override protected void onPostExecute(Bitmap result) {
            CreativeStudioActivity activity = activityRef.get();
            if (activity == null) return;
            activity.currentBitmap = result;
            activity.preview.setImageBitmap(result);
            activity.status.setText("Fondo eliminado en modo local. Para fondos complejos usaremos IA en la siguiente fase.");
        }
    }

    private static Bitmap floodFillCorners(Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        Bitmap out = src.copy(Bitmap.Config.ARGB_8888, true);
        int[] pixels = new int[w * h];
        out.getPixels(pixels, 0, w, 0, 0, w, h);
        int[] seeds = {0, w - 1, (h - 1) * w, h * w - 1};
        boolean[] visited = new boolean[pixels.length];
        int tolerance = 42;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int seed : seeds) {
            if (visited[seed]) continue;
            int base = pixels[seed];
            queue.add(seed); visited[seed] = true;
            while (!queue.isEmpty()) {
                int p = queue.removeFirst();
                if (!similar(pixels[p], base, tolerance)) continue;
                pixels[p] &= 0x00FFFFFF;
                int x = p % w, y = p / w;
                if (x > 0) enqueue(queue, visited, p - 1);
                if (x + 1 < w) enqueue(queue, visited, p + 1);
                if (y > 0) enqueue(queue, visited, p - w);
                if (y + 1 < h) enqueue(queue, visited, p + w);
            }
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h);
        return out;
    }

    private static void enqueue(ArrayDeque<Integer> queue, boolean[] visited, int p) {
        if (!visited[p]) { visited[p] = true; queue.add(p); }
    }

    private static boolean similar(int a, int b, int tolerance) {
        return Math.abs(Color.red(a) - Color.red(b)) <= tolerance
                && Math.abs(Color.green(a) - Color.green(b)) <= tolerance
                && Math.abs(Color.blue(a) - Color.blue(b)) <= tolerance;
    }

    private static class LoadPacksTask extends AsyncTask<Void, Void, ArrayList<StickerPack>> {
        private final WeakReference<CreativeStudioActivity> ref;
        LoadPacksTask(CreativeStudioActivity activity) { ref = new WeakReference<>(activity); }
        @Override protected ArrayList<StickerPack> doInBackground(Void... ignored) {
            try { return StickerPackLoader.fetchStickerPacks(ref.get()); } catch (Exception e) { return null; }
        }
        @Override protected void onPostExecute(ArrayList<StickerPack> packs) {
            CreativeStudioActivity a = ref.get();
            if (a == null) return;
            if (packs == null || packs.isEmpty()) {
                Toast.makeText(a, "No se encontraron packs", Toast.LENGTH_SHORT).show(); return;
            }
            Intent intent = new Intent(a, packs.size() > 1 ? StickerPackListActivity.class : StickerPackDetailsActivity.class);
            if (packs.size() > 1) intent.putParcelableArrayListExtra(StickerPackListActivity.EXTRA_STICKER_PACK_LIST_DATA, packs);
            else {
                intent.putExtra(StickerPackDetailsActivity.EXTRA_SHOW_UP_BUTTON, true);
                intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_DATA, packs.get(0));
            }
            a.startActivity(intent);
        }
    }
}