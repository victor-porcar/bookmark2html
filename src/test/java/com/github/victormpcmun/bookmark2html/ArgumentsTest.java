package com.github.victormpcmun.bookmark2html;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArgumentsTest {

    @TempDir
    Path tempDir;

    @Test
    void readsTheFiveArguments() {
        Path file = TestBookmarks.file();

        Arguments arguments = parse(file.toString(), output(), "TECHNICAL", backup(), "5");

        assertEquals(file, arguments.bookmarksFile());
        assertEquals(Path.of(output()), arguments.outputFile());
        assertEquals("TECHNICAL", arguments.folderName());
        assertEquals(Optional.of(Path.of(backup())), arguments.backupDirectory());
        assertEquals(5, arguments.backupsToKeep());
        assertEquals(Optional.empty(), arguments.pageFavicon());
    }

    @Test
    void readsTheOptionalPageFavicon() {
        Arguments arguments = Arguments.parse(
                new String[]{bookmarks(), output(), "TECHNICAL", backup(), "5", " /assets/favicon-32.png "});

        assertEquals(Optional.of("/assets/favicon-32.png"), arguments.pageFavicon());
    }

    @Test
    void blankPageFaviconMeansNone() {
        Arguments arguments = Arguments.parse(new String[]{bookmarks(), output(), "TECHNICAL", backup(), "5", " "});

        assertEquals(Optional.empty(), arguments.pageFavicon());
    }

    @ParameterizedTest
    @ValueSource(strings = {"relative/backup", "-", "whatever"})
    void zeroBackupsMeansNoBackupAndTheDirectoryIsIgnored(String backupDirectory) {
        Arguments arguments = parse(bookmarks(), output(), "TECHNICAL", backupDirectory, "0");

        assertEquals(Optional.empty(), arguments.backupDirectory());
        assertEquals(0, arguments.backupsToKeep());
    }

    @Test
    void keywordMeansEveryBookmark() {
        assertEquals("", parse(bookmarks(), output(), "ALL_EXISTING_BOOKMARKS", backup(), "5").folderName());
    }

    @Test
    void emptyFolderNameIsRejected() {
        ExportException error = assertThrows(ExportException.class,
                () -> parse(bookmarks(), output(), " ", backup(), "5"));

        assertEquals("folder name is empty; use ALL_EXISTING_BOOKMARKS to export every bookmark", error.getMessage());
    }

    @Test
    void outputFileMustBeAFullPath() {
        ExportException error = assertThrows(ExportException.class,
                () -> parse(bookmarks(), "out.html", "TECHNICAL", backup(), "5"));

        assertEquals("output html file must be a full path: 'out.html'", error.getMessage());
    }

    @Test
    void backupDirectoryMustBeAFullPath() {
        assertThrows(ExportException.class, () -> parse(bookmarks(), output(), "TECHNICAL", "backup", "5"));
    }

    @Test
    void oneBackupToKeepStillNeedsAFullPathDirectory() {
        assertEquals(1, parse(bookmarks(), output(), "TECHNICAL", backup(), "1").backupsToKeep());
        assertThrows(ExportException.class, () -> parse(bookmarks(), output(), "TECHNICAL", "backup", "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "five", "2.5", ""})
    void backupsToKeepMustBeAWholeNumberOfAtLeastZero(String value) {
        ExportException error = assertThrows(ExportException.class,
                () -> parse(bookmarks(), output(), "TECHNICAL", backup(), value));

        assertEquals("number of backups to keep must be a whole number of at least 0 (0 makes no backup): '"
                + value + "'", error.getMessage());
    }

    @Test
    void theFirstFiveArgumentsAreMandatoryAndAtMostSixAreAccepted() {
        assertThrows(ExportException.class,
                () -> Arguments.parse(new String[]{bookmarks(), output(), "TECHNICAL", backup()}));
        assertThrows(ExportException.class,
                () -> Arguments.parse(new String[]{bookmarks(), output(), "TECHNICAL", backup(), "5", "icon.png", "7"}));
    }

    private Arguments parse(String bookmarks, String output, String folder, String backup, String backupsToKeep) {
        return Arguments.parse(new String[]{bookmarks, output, folder, backup, backupsToKeep});
    }

    private String bookmarks() {
        return TestBookmarks.file().toString();
    }

    private String output() {
        return tempDir.resolve("out.html").toString();
    }

    private String backup() {
        return tempDir.resolve("backup").toString();
    }
}
