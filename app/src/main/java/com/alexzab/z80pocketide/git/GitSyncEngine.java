package com.alexzab.z80pocketide.git;

import android.content.Context;
import android.util.AtomicFile;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Small GitHub working-copy manager. No local .git or local commits.
 * An encrypted token is managed separately; only content hashes and Git blob IDs
 * are stored in this app's private snapshot.
 *
 * Concurrency: a branch is verified again before the atomic non-forced push.
 * Conflicts stop the entire sync before any file is overwritten or pushed.
 */
public final class GitSyncEngine {
    public static final int MAX_FILE_BYTES = 2 * 1024 * 1024;
    public static final long MAX_PROJECT_BYTES = 20L * 1024L * 1024L;

    public enum Choice { LOCAL, REMOTE, MANUAL }

    public static final class Resolution {
        public final Choice choice;
        public final String remoteSha;
        public final byte[] merged;
        public Resolution(Choice choice, String remoteSha, byte[] merged) {
            this.choice = choice;
            this.remoteSha = remoteSha;
            this.merged = merged;
        }
    }

    public static final class Conflict {
        public final String path, remoteSha;
        public final byte[] local, remote;
        public Conflict(String path, String remoteSha, byte[] local, byte[] remote) {
            this.path = path;
            this.remoteSha = remoteSha;
            this.local = local;
            this.remote = remote;
        }
    }

    public static final class Result {
        public final List<Conflict> conflicts;
        public final int pulled, pushed;
        public final String commit;
        Result(List<Conflict> conflicts, int pulled, int pushed, String commit) {
            this.conflicts = conflicts;
            this.pulled = pulled;
            this.pushed = pushed;
            this.commit = commit;
        }
        public boolean success() { return conflicts.isEmpty(); }
    }

    private static final class Baseline {
        String blob, hash;
        Baseline(String blob, String hash) { this.blob = blob; this.hash = hash; }
    }

    private final Context context;
    private final GitHubApi api;
    public GitSyncEngine(Context context, GitHubApi api) {
        this.context = context.getApplicationContext();
        this.api = api;
    }

    public Result sync(String repo, String branch, Map<String,Resolution> resolutions)
            throws IOException {
        GitProjectState.validateRepository(repo);
        if (!GitProjectState.safeBranch(branch)) throw new IOException("Invalid Git branch");
        File folder = GitProjectState.folder(context, repo, branch);
        GitHubApi.Remote remote = api.remote(repo, branch);
        Map<String,Baseline> bases = readSnapshot(repo, branch);
        Map<String,byte[]> local = collectLocal(folder);
        Map<String,byte[]> incoming = new LinkedHashMap<>();
        Map<String,byte[]> upload = new LinkedHashMap<>();
        List<Conflict> conflicts = new ArrayList<>();

        Set<String> all = new LinkedHashSet<>();
        all.addAll(bases.keySet());
        all.addAll(remote.blobs.keySet());
        all.addAll(local.keySet());

        for (String path : all) {
            Baseline base = bases.get(path);
            byte[] bytes = local.get(path);
            String localHash = bytes == null ? null : hash(bytes);
            String remoteSha = remote.blobs.get(path);
            GitSyncPlanner.Action action = GitSyncPlanner.decide(
                    base == null ? null : base.hash, localHash,
                    base == null ? null : base.blob, remoteSha);
            if (action == GitSyncPlanner.Action.NONE) continue;
            if (action == GitSyncPlanner.Action.PUSH) {
                upload.put(path, bytes);
                continue;
            }
            if (action == GitSyncPlanner.Action.PULL) {
                incoming.put(path, remoteSha == null ? null : api.blob(repo, remoteSha));
                continue;
            }

            byte[] remoteBytes = remoteSha == null ? null : api.blob(repo, remoteSha);
            // Both sides may have independently arrived at identical contents.
            if (Arrays.equals(bytes, remoteBytes)) continue;
            Resolution choice = resolutions == null ? null : resolutions.get(path);
            if (choice == null || !Objects.equals(choice.remoteSha, remoteSha)) {
                conflicts.add(new Conflict(path, remoteSha, bytes, remoteBytes));
            } else if (choice.choice == Choice.REMOTE) {
                incoming.put(path, remoteBytes);
            } else if (choice.choice == Choice.LOCAL) {
                upload.put(path, bytes);
            } else {
                if (choice.merged == null || choice.merged.length > MAX_FILE_BYTES)
                    throw new IOException("Merged file is missing or too large: " + path);
                upload.put(path, choice.merged);
                incoming.put(path, choice.merged);
            }
        }

        if (!conflicts.isEmpty())
            return new Result(conflicts, 0, 0, remote.commit);

        String head = remote.commit;
        if (!upload.isEmpty()) {
            // Recheck before committing, then use a non-force ref update.
            GitHubApi.Remote latest = api.remote(repo, branch);
            if (!latest.commit.equals(remote.commit))
                throw new IOException("GitHub changed during sync. Tap Sync again to recheck conflicts.");
            head = api.commitChanges(repo, branch, remote, upload);
            remote = api.remote(repo, branch);
            if (!head.equals(remote.commit))
                throw new IOException("Branch moved after push; run Sync again to reconcile.");
        }

        // Only now (after a successful push) update the local working files.
        for (Map.Entry<String,byte[]> entry : incoming.entrySet()) {
            File target = GitProjectState.file(folder, entry.getKey());
            if (entry.getValue() == null) {
                if (target.exists() && !target.delete())
                    throw new IOException("Cannot delete " + entry.getKey());
            } else {
                write(target, entry.getValue());
            }
        }

        // A fresh baseline represents the exact current GitHub tree.
        Map<String,Baseline> updated = new LinkedHashMap<>();
        for (Map.Entry<String,String> item : remote.blobs.entrySet()) {
            File target = GitProjectState.file(folder, item.getKey());
            if (!target.isFile())
                throw new IOException("Missing working file after sync: " + item.getKey());
            updated.put(item.getKey(), new Baseline(item.getValue(), hash(read(target))));
        }
        writeSnapshot(repo, branch, updated);
        return new Result(Collections.emptyList(), incoming.size(), upload.size(), head);
    }

    public List<String> sourceFiles(String repo, String branch) throws IOException {
        File folder = GitProjectState.folder(context, repo, branch);
        List<String> paths = new ArrayList<>();
        for (String path : collectLocal(folder).keySet()) {
            String lower = path.toLowerCase(java.util.Locale.ROOT);
            if (lower.endsWith(".asm") || lower.endsWith(".z80") || lower.endsWith(".s"))
                paths.add(path);
        }
        paths.sort(String.CASE_INSENSITIVE_ORDER);
        return paths;
    }

    private Map<String,Baseline> readSnapshot(String repo, String branch) throws IOException {
        Map<String,Baseline> result = new LinkedHashMap<>();
        File file = GitProjectState.snapshot(context, repo, branch);
        if (!file.isFile()) return result;
        try {
            JSONObject object = new JSONObject(new String(read(file), StandardCharsets.UTF_8));
            if (!repo.equals(object.optString("repo")) || !branch.equals(object.optString("branch")))
                throw new IOException("Git baseline belongs to another project");
            JSONObject entries = object.optJSONObject("files");
            if (entries != null) {
                JSONArray keys = entries.names();
                if (keys != null) for (int i = 0; i < keys.length(); i++) {
                    String path = keys.optString(i);
                    if (!GitProjectState.accepted(path)) throw new IOException("Unsafe snapshot path");
                    JSONObject data = entries.optJSONObject(path);
                    if (data != null)
                        result.put(path, new Baseline(data.optString("blob"), data.optString("hash")));
                }
            }
        } catch (org.json.JSONException e) { throw new IOException("Invalid Git project snapshot", e); }
        return result;
    }

    private void writeSnapshot(String repo, String branch, Map<String,Baseline> data)
            throws IOException {
        try {
            JSONObject object = new JSONObject().put("repo", repo).put("branch", branch);
            JSONObject files = new JSONObject();
            for (Map.Entry<String,Baseline> item : data.entrySet())
                files.put(item.getKey(), new JSONObject()
                        .put("blob", item.getValue().blob).put("hash", item.getValue().hash));
            object.put("files", files);
            File path = GitProjectState.snapshot(context, repo, branch);
            write(path, object.toString().getBytes(StandardCharsets.UTF_8));
        } catch (org.json.JSONException e) { throw new IOException("Cannot save Git project snapshot", e); }
    }

    private static Map<String,byte[]> collectLocal(File root) throws IOException {
        Map<String,byte[]> found = new LinkedHashMap<>();
        if (root.exists()) scan(root, root, found, 0);
        return found;
    }

    private static void scan(File root, File folder, Map<String,byte[]> found, int depth)
            throws IOException {
        if (depth > 12) throw new IOException("Git workspace has too many nested folders");
        File[] files = folder.listFiles();
        if (files == null) throw new IOException("Cannot list Git workspace");
        for (File file : files) {
            if (file.isDirectory()) { scan(root, file, found, depth + 1); continue; }
            if (!file.isFile()) continue;
            String relative = file.getCanonicalPath()
                    .substring(root.getCanonicalPath().length() + 1)
                    .replace(File.separatorChar, '/');
            if (!GitProjectState.accepted(relative)) continue;
            if (found.size() >= 400)
                throw new IOException("Git workspace exceeds 400 files");
            found.put(relative, read(file));
        }
    }

    static byte[] read(File file) throws IOException {
        if (file.length() > MAX_FILE_BYTES)
            throw new IOException("File exceeds 2 MiB: " + file.getName());
        if (file.length() > MAX_PROJECT_BYTES) throw new IOException("File is too large");
        try (FileInputStream stream = new FileInputStream(file);
             ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int size;
            while ((size = stream.read(buffer)) >= 0) {
                if (result.size() + size > MAX_PROJECT_BYTES) throw new IOException("File is too large");
                result.write(buffer, 0, size);
            }
            return result.toByteArray();
        }
    }

    private static void write(File path, byte[] bytes) throws IOException {
        File parent = path.getParentFile();
        if (parent == null || (!parent.exists() && !parent.mkdirs()))
            throw new IOException("Cannot create workspace directory");
        AtomicFile atomic = new AtomicFile(path);
        FileOutputStream stream = null;
        try {
            stream = atomic.startWrite();
            stream.write(bytes);
            atomic.finishWrite(stream);
        } catch (IOException e) {
            if (stream != null) atomic.failWrite(stream);
            throw e;
        }
    }

    private static String hash(byte[] bytes) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder b = new StringBuilder(64);
            for (byte value : digest) b.append(String.format(java.util.Locale.US, "%02x", value & 255));
            return b.toString();
        } catch (Exception error) { throw new IOException("Cannot calculate SHA-256", error); }
    }
}
