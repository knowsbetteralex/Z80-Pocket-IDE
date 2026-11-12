package com.alexzab.z80pocketide.git;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import java.io.File;
import java.io.IOException;

/** Selected GitHub project and its small working directory (no local .git). */
public final class GitProjectState {
    private static final String PREFS = "github_project";
    private GitProjectState() {}

    public static String repo(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("repo", null);
    }

    public static String branch(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("branch", "main");
    }

    public static boolean active(Context context) { return repo(context) != null; }

    public static void select(Context context, String repo, String branch) {
        validateRepository(repo);
        if (!safeBranch(branch)) throw new IllegalArgumentException("Unsafe branch name");
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("repo", repo).putString("branch", branch).apply();
    }

    public static void deactivate(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove("repo").remove("branch").apply();
    }

    public static File root(Context context) {
        String repo = repo(context);
        return repo == null ? null : folder(context, repo, branch(context));
    }

    public static File folder(Context context, String repo, String branch) {
        validateRepository(repo);
        if (!safeBranch(branch)) throw new IllegalArgumentException("Unsafe branch name");
        String key = repo.replace("/", "__") + "___" + branch.replace("/", "__");
        return new File(new File(context.getFilesDir(), "git-workspaces"), key);
    }

    public static File snapshot(Context context, String repo, String branch) {
        File folder = new File(context.getFilesDir(), "git-snapshots");
        String key = repo.replace("/", "__") + "___" + branch.replace("/", "__");
        return new File(folder, key + ".json");
    }

    public static boolean contains(Context context, String uriString) {
        File root = root(context);
        if (root == null || uriString == null) return false;
        Uri uri = Uri.parse(uriString);
        if (!"file".equals(uri.getScheme()) || uri.getPath() == null) return false;
        try {
            String prefix = root.getCanonicalPath() + File.separator;
            return new File(uri.getPath()).getCanonicalPath().startsWith(prefix);
        } catch (IOException e) { return false; }
    }

    public static File file(File root, String path) throws IOException {
        if (!safePath(path)) throw new IOException("Unsafe Git path: " + path);
        File result = new File(root, path);
        if (!result.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator))
            throw new IOException("Git path outside project: " + path);
        return result;
    }

    public static boolean safePath(String path) {
        if (path == null || path.isEmpty() || path.length() > 500
                || path.startsWith("/") || path.indexOf('\\') >= 0
                || path.indexOf('\0') >= 0 || path.indexOf(':') >= 0)
            return false;
        for (String segment : path.split("/", -1))
            if (segment.isEmpty() || segment.equals(".") || segment.equals(".."))
                return false;
        return true;
    }

    public static boolean accepted(String path) {
        if (!safePath(path)) return false;
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith(".github/") || lower.startsWith(".git/"))
            return false;
        // Narrow scope for an ASM IDE; never silently edit unrelated repo files.
        return lower.matches(".*\\.(asm|inc|z80|s|scr|bin|json|txt|md|dat|aym|aym1|csv)$");
    }

    public static boolean safeBranch(String branch) {
        return branch != null && !branch.isEmpty() && branch.length() <= 120
                && branch.matches("[A-Za-z0-9_./-]+")
                && !branch.startsWith("/") && !branch.endsWith("/")
                && !branch.contains("..") && !branch.contains("//");
    }

    public static void validateRepository(String repo) {
        if (repo == null || !repo.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+"))
            throw new IllegalArgumentException("Invalid owner/repository");
    }
}
