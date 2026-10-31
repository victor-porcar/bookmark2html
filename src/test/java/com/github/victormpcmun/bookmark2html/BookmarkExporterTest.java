package com.github.victormpcmun.bookmark2html;

import com.github.victormpcmun.bookmark2html.backup.BackupRotation;
import com.github.victormpcmun.bookmark2html.backup.ZipBookmarkBackup;
import com.github.victormpcmun.bookmark2html.finder.FolderFinder;
import com.github.victormpcmun.bookmark2html.reader.ChromeBookmarkReader;
import com.github.victormpcmun.bookmark2html.render.PageIcons;
import com.github.victormpcmun.bookmark2html.render.PrettyHtmlRenderer;
import com.github.victormpcmun.bookmark2html.writer.FileOutputWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookmarkExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesThePrettyHtmlOfTheRequestedFolderAndBacksUpTheBookmarks() throws IOException {
        Path output = tempDir.resolve("nested/bookmarks.html");
        Path backupDirectory = tempDir.resolve("backup");

        ExportResult result = exporter().export(
                new Arguments(TestBookmarks.file(), output, "TECHNICAL", Optional.of(backupDirectory), 5, PageIcons.none()));

        String html = Files.readString(output);
        assertTrue(html.contains("<h1 class=\"page-title\">TECHNICAL</h1>"));
        assertTrue(html.contains("https://docs.oracle.com/en/java/"));
        assertTrue(html.contains("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "\n<link rel=\"icon\" href=\"/assets/favicon-32.png\">"
                + "\n<link rel=\"apple-touch-icon\" href=\"/assets/touch.png\">"));
        Path backupFile = result.backupFile().orElseThrow();
        assertEquals(backupDirectory, backupFile.getParent());
        assertTrue(Files.exists(backupFile));
        assertEquals(List.of(), result.deletedBackups());
    }

    @Test
    void deletesTheBackupsBeyondTheNumberToKeep() throws IOException {
        Path backupDirectory = Files.createDirectories(tempDir.resolve("backup"));
        Path oldest = Files.createFile(backupDirectory.resolve("original_bookmarks_20200101_000000.zip"));
        Path older = Files.createFile(backupDirectory.resolve("original_bookmarks_20210101_000000.zip"));
        Arguments arguments = new Arguments(
                TestBookmarks.file(), tempDir.resolve("out.html"), "TECHNICAL", Optional.of(backupDirectory), 2,
                PageIcons.none());

        ExportResult result = exporter().export(arguments);

        assertEquals(List.of(oldest), result.deletedBackups());
        assertFalse(Files.exists(oldest));
        assertTrue(Files.exists(older));
        assertTrue(Files.exists(result.backupFile().orElseThrow()));
    }

    @Test
    void makesNoBackupWhenZeroBackupsAreKept() throws IOException {
        Path output = tempDir.resolve("bookmarks.html");

        ExportResult result = exporter().export(
                new Arguments(TestBookmarks.file(), output, "TECHNICAL", Optional.empty(), 0, PageIcons.none()));

        assertEquals(Optional.empty(), result.backupFile());
        assertEquals(List.of(), result.deletedBackups());
        try (Stream<Path> files = Files.list(tempDir)) {
            assertEquals(List.of(output), files.toList());
        }
    }

    private BookmarkExporter exporter() {
        return new BookmarkExporter(
                new ChromeBookmarkReader(),
                new FolderFinder(),
                PrettyHtmlRenderer.withDefaultTemplate(domains -> Map.of(),
                        new PageIcons(Optional.of("/assets/favicon-32.png"), Optional.of("/assets/touch.png"))),
                new FileOutputWriter(),
                new ZipBookmarkBackup(Clock.systemDefaultZone()),
                new BackupRotation());
    }
}
