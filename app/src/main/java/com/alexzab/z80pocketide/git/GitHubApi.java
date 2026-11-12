package com.alexzab.z80pocketide.git;

import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Minimal GitHub Git Data REST API, with one atomic commit for the whole sync. */
public final class GitHubApi {
    private static final String BASE = "https://api.github.com";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS).build();
    private final String token;

    public static final class Repo {
        public final String fullName, branch;
        public final boolean privateRepo;
        Repo(JSONObject entry) {
            fullName = entry.optString("full_name");
            branch = entry.optString("default_branch", "main");
            privateRepo = entry.optBoolean("private", false);
        }
    }

    public static final class Remote {
        public final String commit, treeSha;
        public final Map<String, String> blobs;
        Remote(String commit, String treeSha, Map<String,String> blobs) {
            this.commit = commit;
            this.treeSha = treeSha;
            this.blobs = blobs;
        }
    }

    public GitHubApi(String token) {
        if (token == null || token.trim().isEmpty()) throw new IllegalArgumentException("Log in to GitHub");
        this.token = token.trim();
    }

    private static String repositoryPath(String name) {
        GitProjectState.validateRepository(name);
        return "/repos/" + name;
    }

    private JSONObject request(String method, String path, JSONObject payload) throws IOException {
        Request.Builder request = new Request.Builder().url(BASE + path)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Z80-Pocket-IDE")
                .header("Authorization", "Bearer " + token);
        if ("GET".equals(method)) request.get();
        else {
            RequestBody body = RequestBody.create(JSON, payload == null ? "{}" : payload.toString());
            if ("POST".equals(method)) request.post(body);
            else if ("PATCH".equals(method)) request.patch(body);
            else throw new IOException("Unsupported HTTP method");
        }
        try (Response response = client.newCall(request.build()).execute()) {
            String data = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                String message = "";
                try { message = new JSONObject(data).optString("message", ""); }
                catch (Exception ignored) {}
                throw new IOException("GitHub HTTP " + response.code()
                        + (message.isEmpty() ? "" : ": " + message));
            }
            try { return new JSONObject(data); }
            catch (Exception e) { throw new IOException("Unexpected GitHub response", e); }
        }
    }

    private JSONArray requestArray(String path) throws IOException {
        Request.Builder builder = new Request.Builder().url(BASE + path)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Z80-Pocket-IDE")
                .header("Authorization", "Bearer " + token);
        try (Response response = client.newCall(builder.build()).execute()) {
            String body = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) throw new IOException("GitHub HTTP " + response.code());
            try { return new JSONArray(body); }
            catch (Exception e) { throw new IOException("Unexpected repository list", e); }
        }
    }

    public String username() throws IOException {
        return request("GET", "/user", null).optString("login", "");
    }

    public List<Repo> repositories() throws IOException {
        List<Repo> repos = new ArrayList<>();
        // First 100 most recently updated accessible repositories; no write access implied.
        JSONArray response = requestArray("/user/repos?per_page=100&sort=updated");
        for (int i = 0; i < response.length(); i++) {
            JSONObject item = response.optJSONObject(i);
            if (item == null || item.optBoolean("archived", false)) continue;
            Repo repo = new Repo(item);
            if (repo.fullName.contains("/") && GitProjectState.safeBranch(repo.branch))
                repos.add(repo);
        }
        return repos;
    }

    public Remote remote(String name, String branch) throws IOException {
        String prefix = repositoryPath(name);
        if (!GitProjectState.safeBranch(branch)) throw new IOException("Unsafe branch");
        JSONObject ref = request("GET", prefix + "/git/ref/heads/" + branch, null);
        String head = ref.optJSONObject("object") == null ? "" :
                ref.optJSONObject("object").optString("sha");
        if (head.isEmpty()) throw new IOException("Branch has no head commit");
        JSONObject commit = request("GET", prefix + "/git/commits/" + head, null);
        JSONObject commitTree = commit.optJSONObject("tree");
        if (commitTree == null) throw new IOException("Commit is missing a tree");
        String treeSha = commitTree.optString("sha");
        JSONObject tree = request("GET", prefix + "/git/trees/" + treeSha + "?recursive=1", null);
        if (tree.optBoolean("truncated", false))
            throw new IOException("GitHub tree is too large; incomplete trees cannot be synced");
        Map<String,String> blobs = new LinkedHashMap<>();
        JSONArray items = tree.optJSONArray("tree");
        if (items == null) throw new IOException("Missing Git tree");
        long bytes = 0;
        for (int i = 0; i < items.length(); i++) {
            JSONObject entry = items.optJSONObject(i);
            if (entry == null || !"blob".equals(entry.optString("type"))
                    || !"100644".equals(entry.optString("mode"))) continue;
            String path = entry.optString("path");
            if (!GitProjectState.accepted(path)) continue;
            int size = entry.optInt("size", 0);
            if (size < 0 || size > GitSyncEngine.MAX_FILE_BYTES)
                throw new IOException("File exceeds sync limit (2 MiB): " + path);
            bytes += size;
            if (bytes > GitSyncEngine.MAX_PROJECT_BYTES || blobs.size() >= 400)
                throw new IOException("Project exceeds sync limit (20 MiB / 400 files)");
            blobs.put(path, entry.optString("sha"));
        }
        return new Remote(head, treeSha, blobs);
    }

    public byte[] blob(String repo, String sha) throws IOException {
        if (sha == null || !sha.matches("[0-9a-fA-F]{40,64}")) throw new IOException("Invalid Git blob SHA");
        JSONObject blob = request("GET", repositoryPath(repo) + "/git/blobs/" + sha, null);
        if (!"base64".equals(blob.optString("encoding")))
            throw new IOException("Unknown blob encoding");
        byte[] data = Base64.decode(blob.optString("content"), Base64.DEFAULT);
        if (data.length > GitSyncEngine.MAX_FILE_BYTES)
            throw new IOException("Git blob exceeds 2 MiB");
        return data;
    }

    public String commitChanges(String repo, String branch, Remote remote,
                                Map<String,byte[]> changes) throws IOException {
        if (changes.isEmpty()) return remote.commit;
        String prefix = repositoryPath(repo);
        JSONArray entries = new JSONArray();
        try {
            for (Map.Entry<String,byte[]> entry : changes.entrySet()) {
                String path = entry.getKey();
                if (!GitProjectState.accepted(path)) throw new IOException("Unsafe Git path: " + path);
                JSONObject node = new JSONObject().put("path", path)
                        .put("mode", "100644").put("type", "blob");
                if (entry.getValue() == null) {
                    node.put("sha", JSONObject.NULL);
                } else {
                    byte[] bytes = entry.getValue();
                    if (bytes.length > GitSyncEngine.MAX_FILE_BYTES)
                        throw new IOException("Git file too large: " + path);
                    JSONObject blob = new JSONObject().put("encoding", "base64")
                            .put("content", Base64.encodeToString(bytes, Base64.NO_WRAP));
                    String sha = request("POST", prefix + "/git/blobs", blob).optString("sha");
                    node.put("sha", sha);
                }
                entries.put(node);
            }
            JSONObject tree = new JSONObject().put("base_tree", remote.treeSha)
                    .put("tree", entries);
            String treeSha = request("POST", prefix + "/git/trees", tree).optString("sha");
            JSONObject commit = new JSONObject().put("message", "Sync from Z80 Pocket IDE")
                    .put("tree", treeSha).put("parents", new JSONArray().put(remote.commit));
            String commitSha = request("POST", prefix + "/git/commits", commit).optString("sha");
            JSONObject ref = new JSONObject().put("sha", commitSha).put("force", false);
            // GitHub rejects non-fast-forward branch updates; never force-push.
            request("PATCH", prefix + "/git/refs/heads/" + branch, ref);
            return commitSha;
        } catch (org.json.JSONException e) {
            throw new IOException("Cannot create Git commit", e);
        }
    }
}
