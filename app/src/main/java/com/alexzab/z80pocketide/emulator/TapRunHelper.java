package com.alexzab.z80pocketide.emulator;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;

import com.alexzab.z80pocketide.assembler.Assembler;
import com.alexzab.z80pocketide.assembler.AssemblyResult;
import com.alexzab.z80pocketide.i18n.AppLanguage;
import com.alexzab.z80pocketide.i18n.Texts;
import com.alexzab.z80pocketide.zx.TapWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

/** Builds an in-memory source into an autorun TAP and launches it without opening an editor tab. */
public final class TapRunHelper {
    private TapRunHelper() {}

    public static String buildAndLaunch(Activity activity, String source, String title,
                                        AppLanguage language) throws Exception {
        AssemblyResult result = new Assembler().assemble(source);
        byte[] tap = new TapWriter().programTap(
                programName(title), result.getOrigin(), result.getBytes());

        File dir = new File(activity.getCacheDir(), "shared");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException(Texts.pick(language,
                    "cannot create cache directory", "не удалось создать временную папку"));
        }
        File file = new File(dir, "program.tap");
        try (FileOutputStream stream = new FileOutputStream(file)) {
            stream.write(tap);
        }

        Uri uri = Uri.parse("content://" + activity.getPackageName() + ".tap/program.tap");
        Intent view = createViewIntent(uri);
        String preferredPackage = EmulatorSettings.getPackage(activity);
        String preferredLabel = EmulatorSettings.getLabel(activity);

        if (preferredPackage != null) {
            view.setPackage(preferredPackage);
            if (view.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(view);
                return Texts.pick(language, "Opening in ", "Открытие в ")
                        + (preferredLabel == null ? preferredPackage : preferredLabel);
            }
            EmulatorSettings.clear(activity);
            view.setPackage(null);
        }

        activity.startActivity(Intent.createChooser(view,
                Texts.pick(language, "Open TAP with", "Открыть TAP в")));
        return Texts.pick(language,
                "TAP built · choose your ZX Spectrum emulator",
                "TAP собран · выберите эмулятор ZX Spectrum");
    }

    public static String buildSummary(String source, AppLanguage language) {
        AssemblyResult result = new Assembler().assemble(source);
        return language == AppLanguage.RU
                ? String.format(Locale.US, "%d байт · ORG $%04X",
                    result.getBytes().length, result.getOrigin() & 0xFFFF)
                : String.format(Locale.US, "%d bytes · ORG $%04X",
                    result.getBytes().length, result.getOrigin() & 0xFFFF);
    }

    private static Intent createViewIntent(Uri uri) {
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(uri, "application/octet-stream");
        view.setClipData(ClipData.newRawUri("ZX Spectrum TAP", uri));
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return view;
    }

    private static String programName(String title) {
        String name = title == null ? "PROGRAM" : title;
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        name = name.replaceAll("[^A-Za-z0-9_-]", "_");
        if (name.isEmpty()) name = "PROGRAM";
        return name.length() > 10 ? name.substring(0, 10) : name;
    }
}
