package com.alexzab.z80pocketide.git;

import java.util.Objects;

/** Pure three-way file decision: local snapshot vs current working file vs remote GitHub. */
public final class GitSyncPlanner {
    public enum Action { NONE, PULL, PUSH, CONFLICT }
    private GitSyncPlanner() {}

    public static Action decide(String baseLocalHash, String localHash,
                                String baseGitSha, String remoteGitSha) {
        boolean localChanged = !Objects.equals(baseLocalHash, localHash);
        boolean remoteChanged = !Objects.equals(baseGitSha, remoteGitSha);
        if (localChanged && remoteChanged) return Action.CONFLICT;
        if (localChanged) return Action.PUSH;
        if (remoteChanged) return Action.PULL;
        return Action.NONE;
    }
}
