package com.alexzab.z80pocketide.update;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.alexzab.z80pocketide.BuildConfig;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Checks GitHub Releases without blocking or disrupting the editor. */
public final class ReleaseUpdates {
    private static final String API_URL =
            "https://api.github.com/repos/knowsbetteralex/Z80-Pocket-IDE/releases/latest";
    public static final String RELEASES_URL =
            "https://github.com/knowsbetteralex/Z80-Pocket-IDE/releases/latest";
    private static final String PREFS = "github_release_updates";
    private static final String LAST_READ_TAG = "last_read_tag";
    private static final long CHECK_INTERVAL_MS = 15 * 60 * 1000L;

    public static final class ReleaseInfo {
        public final String tag, name, body, commit, url;
        private ReleaseInfo(JSONObject json) {
            tag = json.optString("tag_name", "");
            name = json.optString("name", "Z80 Pocket IDE");
            body = json.optString("body", "");
            commit = json.optString("target_commitish", "");
            url = json.optString("html_url", RELEASES_URL);
        }
        public String safeUrl() {
            return url.startsWith("https://github.com/knowsbetteralex/Z80-Pocket-IDE/releases/")
                    ? url : RELEASES_URL;
        }
    }

    private final SharedPreferences preferences;
    private final Runnable onChange;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService reader = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "z80-github-release-check");
        thread.setDaemon(true);
        return thread;
    });
    private ReleaseInfo latest;
    private long lastAttemptMs;
    private boolean checking;
    private volatile boolean closed;

    public ReleaseUpdates(Context context, Runnable onChange) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.onChange = onChange;
    }

    public ReleaseInfo getLatestRelease() { return latest; }

    public boolean hasUnreadRelease() {
        return latest != null && ReleaseNotes.isUnread(latest.tag, latest.commit,
                BuildConfig.BUILD_COMMIT, preferences.getString(LAST_READ_TAG, ""));
    }

    public void markRead() {
        if (latest == null || latest.tag.isEmpty()) return;
        preferences.edit().putString(LAST_READ_TAG, latest.tag).apply();
        onChange.run();
    }

    public void checkForUpdates() {
        long now = System.currentTimeMillis();
        if (closed || checking || (lastAttemptMs != 0 && now - lastAttemptMs < CHECK_INTERVAL_MS))
            return;
        lastAttemptMs = now;
        checking = true;
        reader.execute(() -> {
            ReleaseInfo fetched = null;
            try {
                fetched = downloadLatest();
            } catch (Exception e) {
                Log.d("Z80ReleaseUpdates", "Could not check GitHub Releases", e);
            }
            final ReleaseInfo result = fetched;
            mainHandler.post(() -> {
                if (closed) return;
                checking = false;
                if (result != null && !result.tag.isEmpty()) {
                    latest = result;
                    onChange.run();
                }
            });
        });
    }

    private static ReleaseInfo downloadLatest() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
        try {
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "Z80-Pocket-IDE");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            try (InputStream stream = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int size, total = 0;
                while ((size = stream.read(buffer)) != -1) {
                    total += size;
                    if (total > 128 * 1024) return null;
                    output.write(buffer, 0, size);
                }
                return new ReleaseInfo(new JSONObject(
                        new String(output.toByteArray(), StandardCharsets.UTF_8)));
            }
        } finally {
            connection.disconnect();
        }
    }

    public void close() {
        closed = true;
        reader.shutdownNow();
    }
}
