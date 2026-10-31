package com.github.victormpcmun.bookmark2html;

import com.github.victormpcmun.bookmark2html.backup.BackupRotation;
import com.github.victormpcmun.bookmark2html.backup.ZipBookmarkBackup;
import com.github.victormpcmun.bookmark2html.favicon.GoogleFaviconSource;
import com.github.victormpcmun.bookmark2html.finder.FolderFinder;
import com.github.victormpcmun.bookmark2html.reader.ChromeBookmarkReader;
import com.github.victormpcmun.bookmark2html.render.PrettyHtmlRenderer;
import com.github.victormpcmun.bookmark2html.writer.FileOutputWriter;

import java.time.Clock;

public final class Bookmark2html {

    private Bookmark2html() {
    }

    public static void main(String[] args) {
        try {
            run(Arguments.parse(args));
        } catch (ExportException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void run(Arguments arguments) {
        System.out.printf("Reading Chrome bookmarks from %s%n", arguments.bookmarksFile());
        ExportResult result = createExporter(arguments).export(arguments);
        System.out.printf("Exported %d bookmarks from folder '%s' into %s%n",
                result.exportedFolder().countLinks(), result.exportedFolder().name(), arguments.outputFile());
        result.backupFile().ifPresentOrElse(
                backupFile -> System.out.printf("Chrome bookmarks file backed up into %s%n", backupFile),
                () -> System.out.println("No backup made (0 backups to keep)"));
        result.deletedBackups().forEach(deleted ->
                System.out.printf("Old backup deleted: %s%n", deleted.getFileName()));
    }

    private static BookmarkExporter createExporter(Arguments arguments) {
        return new BookmarkExporter(
                new ChromeBookmarkReader(),
                new FolderFinder(),
                PrettyHtmlRenderer.withDefaultTemplate(new GoogleFaviconSource(), arguments.pageIcons()),
                new FileOutputWriter(),
                new ZipBookmarkBackup(Clock.systemDefaultZone()),
                new BackupRotation());
    }
}
