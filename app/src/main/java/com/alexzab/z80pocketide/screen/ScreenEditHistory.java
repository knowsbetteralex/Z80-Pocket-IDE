package com.alexzab.z80pocketide.screen;

import com.alexzab.z80pocketide.project.SpectrumScreenDocument;
import java.util.*;

/** One stroke = one byte-level undo step; record changed bytes only. */
public final class ScreenEditHistory {
    private static final int LIMIT=100;
    private static final class Step {
        final int[] offsets;
        final byte[] before,after;
        Step(int[] offsets,byte[] before,byte[] after) {
            this.offsets=offsets;this.before=before;this.after=after;
        }
    }
    private final Deque<Step> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private byte[] before;
    private byte[] saved;
    public ScreenEditHistory(byte[] initial){saved=initial.clone();}
    public void begin(SpectrumScreenDocument document) {
        if(before==null)before=document.toRaw();
    }
    public boolean end(SpectrumScreenDocument document) {
        if(before==null)return false;
        byte[] after=document.toRaw();
        int count=0;
        for(int i=0;i<after.length;i++)if(before[i]!=after[i])count++;
        if(count==0){before=null;return false;}
        int[] positions=new int[count];
        byte[] old=new byte[count],now=new byte[count];
        for(int i=0,j=0;i<after.length;i++)if(before[i]!=after[i]){
            positions[j]=i;old[j]=before[i];now[j]=after[i];j++;
        }
        undo.push(new Step(positions,old,now));
        if(undo.size()>LIMIT)undo.removeLast();
        redo.clear();
        before=null;
        return true;
    }
    public boolean undo(SpectrumScreenDocument doc) {
        if(undo.isEmpty())return false;
        Step step=undo.pop();
        apply(doc,step,false);
        redo.push(step);
        return true;
    }
    public boolean redo(SpectrumScreenDocument doc) {
        if(redo.isEmpty())return false;
        Step step=redo.pop();
        apply(doc,step,true);
        undo.push(step);
        return true;
    }
    private static void apply(SpectrumScreenDocument document,Step step,boolean forward){
        byte[] bytes=document.toRaw();
        byte[] values=forward?step.after:step.before;
        for(int i=0;i<step.offsets.length;i++)bytes[step.offsets[i]]=values[i];
        document.replaceRaw(bytes);
    }
    public boolean canUndo(){return !undo.isEmpty();}
    public boolean canRedo(){return !redo.isEmpty();}
    public boolean isDirty(SpectrumScreenDocument doc) {
        return !Arrays.equals(saved,doc.toRaw());
    }
    public void markSaved(SpectrumScreenDocument doc){saved=doc.toRaw();}
    public void reset(SpectrumScreenDocument doc) {
        before=null;undo.clear();redo.clear();saved=doc.toRaw();
    }
}
