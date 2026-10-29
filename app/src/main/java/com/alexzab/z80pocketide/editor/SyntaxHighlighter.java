package com.alexzab.z80pocketide.editor;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.CharacterStyle;
import android.text.TextPaint;
import android.widget.EditText;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Only colors the on-screen portion of the source (+ a little margin).
 *
 * Full-document regex and thousands of ForegroundColorSpans used to run on the
 * Android UI thread. Inserting a character near the start then had to update
 * every following span and repeatedly allocate the entire source. The worker
 * now scans immutable snapshots. The UI only applies a small bounded result
 * when the user has paused typing, with stale calculations discarded.
 */
public final class SyntaxHighlighter {
    private static final int[] COLORS={
            0,
            0xFF204EA0, // mnemonics
            0xFF9D268C, // registers
            0xFF007D5C, // directives
            0xFFB55C00, // numbers
            0xFF007680, // labels
            0xFFB42A2A, // strings
            0xFF7D7D7D  // comments
    };
    private static final long TYPING_DELAY_MS=950L;
    private static final long SCROLL_DELAY_MS=160L;
    private static final int MARGIN_LINES=12;
    private static final int MAX_WINDOW_CHARS=14000;
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor(r -> {
        Thread thread=new Thread(r,"z80-syntax-worker");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<EditText,Controller> CONTROLLERS=new WeakHashMap<>();

    private SyntaxHighlighter() {}

    public static void attach(EditText editor) {
        Controller controller=new Controller(editor);
        CONTROLLERS.put(editor,controller);
        editor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) {}
            @Override public void afterTextChanged(Editable text) {
                controller.schedule(TYPING_DELAY_MS);
            }
        });
        controller.schedule(TYPING_DELAY_MS);
    }

    /** Called on viewport movement; no syntax scanning happens during scrolling. */
    public static void refreshViewport(EditText editor) {
        Controller controller=CONTROLLERS.get(editor);
        if(controller!=null)controller.schedule(SCROLL_DELAY_MS);
    }

    private static final class Controller {
        private final WeakReference<EditText> editorRef;
        private final Handler handler=new Handler(Looper.getMainLooper());
        private int generation;
        private final Runnable refresh=this::snapshotAndQueue;

        Controller(EditText editor) {
            editorRef=new WeakReference<>(editor);
        }

        void schedule(long delay) {
            generation++;
            handler.removeCallbacks(refresh);
            handler.postDelayed(refresh,delay);
        }

        void snapshotAndQueue() {
            final EditText editor=editorRef.get();
            if(editor==null || editor.getWindowToken()==null)return;
            final Editable text=editor.getText();
            final int length=text.length();
            final Layout layout=editor.getLayout();
            int from=0,to=length;
            if(layout!=null && layout.getLineCount()>0) {
                int sy=editor.getScrollY();
                int top=layout.getLineForVertical(Math.max(0,sy-editor.getTotalPaddingTop()));
                int bottom=layout.getLineForVertical(Math.max(0,
                        sy+editor.getHeight()-editor.getTotalPaddingTop()));
                from=layout.getLineStart(Math.max(0,top-MARGIN_LINES));
                to=layout.getLineEnd(Math.min(layout.getLineCount()-1,
                        bottom+MARGIN_LINES));
                if(to-from>MAX_WINDOW_CHARS)to=Math.min(length,from+MAX_WINDOW_CHARS);
            }else{
                to=Math.min(length,MAX_WINDOW_CHARS);
            }
            if(to<=from)return;
            // Snapshot is taken only after typing stops, not once per keystroke.
            final String visible=text.subSequence(from,to).toString();
            final String complete=editor instanceof CodeEditorView
                    ? ((CodeEditorView)editor).getHighlightSource():text.toString();
            final int offset=from,expectedLength=length,revision=generation;
            WORKER.execute(() -> {
                List<SyntaxTokenPlanner.Token> tokens=
                        SyntaxTokenPlanner.plan(visible,complete,offset);
                handler.post(() -> {
                    EditText target=editorRef.get();
                    if(target==null || target.getWindowToken()==null
                            || revision!=generation || target.getText().length()!=expectedLength)
                        return;
                    applyTokens(target.getText(),tokens);
                });
            });
        }
    }

    private static final class SyntaxColor extends ForegroundColorSpan {
        SyntaxColor(int color){super(color);}
    }
    private static final class SyntaxBold extends StyleSpan {
        SyntaxBold(){super(Typeface.BOLD);}
    }

    private static void applyTokens(Editable text,List<SyntaxTokenPlanner.Token> tokens) {
        // Never remove search-match backgrounds, diagnostics, or user formatting.
        SyntaxColor[] previous=text.getSpans(0,text.length(),SyntaxColor.class);
        for(SyntaxColor s:previous)text.removeSpan(s);
        SyntaxBold[] bold=text.getSpans(0,text.length(),SyntaxBold.class);
        for(SyntaxBold s:bold)text.removeSpan(s);
        for(SyntaxTokenPlanner.Token token:tokens) {
            if(token.start<0 || token.end>text.length() || token.end<=token.start)continue;
            text.setSpan(new SyntaxColor(COLORS[token.kind]),token.start,token.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if(token.bold)
                text.setSpan(new SyntaxBold(),token.start,token.end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /** Direct recoloring kept for tools/tests; normal typing always uses worker. */
    public static void apply(Editable text) {
        apply(text,text.toString());
    }
    public static void apply(Editable text,String fullSource) {
        // Bounded even for direct callers. Do not recreate thousands of spans.
        int end=Math.min(text.length(),MAX_WINDOW_CHARS);
        applyTokens(text,SyntaxTokenPlanner.plan(text.subSequence(0,end).toString(),
                fullSource,0));
    }

    /** Red underline for an assembler diagnostic, without replacing syntax colours. */
    public static void setErrorLine(EditText editor,int oneBasedLine) {
        Editable text=editor.getText();
        ErrorUnderlineSpan[] existing=text.getSpans(0,text.length(),ErrorUnderlineSpan.class);
        for(ErrorUnderlineSpan span:existing)text.removeSpan(span);
        if(oneBasedLine<1 || text.length()==0)return;

        String source=text.toString();
        int start=0;
        for(int line=1;line<oneBasedLine;line++) {
            int next=source.indexOf('\n',start);
            if(next<0)return;
            start=next+1;
        }
        int end=source.indexOf('\n',start);
        if(end<0)end=source.length();
        String visible=SourceSymbols.maskLiterals(source.substring(start,end));
        int relativeStart=0;
        while(relativeStart<visible.length()
                && Character.isWhitespace(visible.charAt(relativeStart)))relativeStart++;
        int relativeEnd=visible.length();
        while(relativeEnd>relativeStart
                && Character.isWhitespace(visible.charAt(relativeEnd-1)))relativeEnd--;
        if(relativeEnd>relativeStart)text.setSpan(new ErrorUnderlineSpan(),
                start+relativeStart,start+relativeEnd,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static final class ErrorUnderlineSpan extends CharacterStyle {
        @Override public void updateDrawState(TextPaint paint) {
            paint.underlineColor=Color.rgb(219,48,57);
            paint.underlineThickness=2.3f;
            paint.setUnderlineText(true);
        }
    }
}
