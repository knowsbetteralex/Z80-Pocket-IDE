package com.alexzab.z80pocketide.git;

import org.junit.Test;
import static org.junit.Assert.*;

public final class GitSyncPlannerTest {
    @Test public void nonoverlappingChangesSyncWithoutConflicts() {
        assertEquals(GitSyncPlanner.Action.PULL, GitSyncPlanner.decide("a", "a", "old", "new"));
        assertEquals(GitSyncPlanner.Action.PUSH, GitSyncPlanner.decide("a", "b", "old", "old"));
        assertEquals(GitSyncPlanner.Action.NONE, GitSyncPlanner.decide("a", "a", "old", "old"));
    }
    @Test public void simultaneousEditNeedsChoice() {
        assertEquals(GitSyncPlanner.Action.CONFLICT, GitSyncPlanner.decide("a", "b", "old", "new"));
        assertEquals(GitSyncPlanner.Action.CONFLICT, GitSyncPlanner.decide("a", null, "old", "new"));
        assertEquals(GitSyncPlanner.Action.CONFLICT, GitSyncPlanner.decide(null, "b", null, "new"));
    }
    @Test public void deletionsAreAccountedFor() {
        assertEquals(GitSyncPlanner.Action.PULL, GitSyncPlanner.decide("a", "a", "old", null));
        assertEquals(GitSyncPlanner.Action.PUSH, GitSyncPlanner.decide("a", null, "old", "old"));
    }
}
