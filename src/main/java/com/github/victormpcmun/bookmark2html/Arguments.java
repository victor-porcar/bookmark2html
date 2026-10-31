package com.github.victormpcmun.bookmark2html;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Command line arguments: bookmarks file (the first existing one among several candidates), output
 * html file, folder to export, the directory where the bookmarks file is backed up and how many
 * backups are kept there, plus optionally the url of the favicon of the generated page.
 * The keyword ALL_EXISTING_BOOKMARKS is turned into an empty folder name, which means every bookmark.
 * Keeping 0 backups means no backup at all: the backup directory is then ignored and left empty here.
 * A blank page favicon is the same as none.
 */
public record Arguments(Path bookmarksFile, Path outputFile, String folderName,
                        Optional<Path> backupDirectory, int backupsToKeep, Optional<String> pageFavicon) {

    static final int NO_BACKUP = 0;

    static final String ALL_BOOKMARKS_KEYWORD = "ALL_EXISTING_BOOKMARKS";
    static final String ALL_BOOKMARKS = "";

    private static final int MANDATORY_ARGUMENTS = 5;
    private static final int ALL_ARGUMENTS = 6;

    private static final String USAGE = """
            expected 5 arguments, or 6 with the optional last one
              1. Chrome bookmarks file, or several candidates separated by ';' (the first existing one
                 is used); a profile directory stands for the Bookmarks file inside it
              2. Output html file, as a full path
              3. Name of the bookmark folder to export (ALL_EXISTING_BOOKMARKS exports every bookmark)
              4. Directory where the Chrome bookmarks file is backed up as a zip, as a full path
                 (ignored when 5 is 0)
              5. Number of backups to keep in that directory, counting the new one; older ones are deleted.
                 0 makes no backup at all
              6. Optional: url of the favicon of the generated page, as written in its html,
                 for example /assets/favicon-32.png; without it the page has no favicon
            example:
              java -jar bookmark2html.jar \
            "C:\\Chrome\\User Data\\Default\\AccountBookmarks;C:\\Chrome\\User Data\\Default\\Bookmarks" \
            "D:\\bookmarks\\bookmarks.html" TECHNICAL "D:\\bookmarks\\backup" 5 favicon.png""";

    public static Arguments parse(String[] args) {
        if (args.length != MANDATORY_ARGUMENTS && args.length != ALL_ARGUMENTS) {
            throw new ExportException(USAGE);
        }
        int backupsToKeep = backupsToKeepOf(args[4]);
        return new Arguments(
                BookmarksFileLocator.firstExisting(args[0]),
                fullPathOf(args[1], "output html file"),
                folderNameOf(args[2]),
                backupDirectoryOf(args[3], backupsToKeep),
                backupsToKeep,
                args.length == ALL_ARGUMENTS ? pageFaviconOf(args[5]) : Optional.empty());
    }

    private static Optional<String> pageFaviconOf(String value) {
        String pageFavicon = value.strip();
        return pageFavicon.isEmpty() ? Optional.empty() : Optional.of(pageFavicon);
    }

    private static Optional<Path> backupDirectoryOf(String value, int backupsToKeep) {
        if (backupsToKeep == NO_BACKUP) {
            return Optional.empty();
        }
        return Optional.of(fullPathOf(value, "backup directory"));
    }

    private static int backupsToKeepOf(String value) {
        try {
            int backupsToKeep = Integer.parseInt(value.strip());
            if (backupsToKeep >= NO_BACKUP) {
                return backupsToKeep;
            }
        } catch (NumberFormatException e) {
            // reported below, like any other invalid number
        }
        throw new ExportException("number of backups to keep must be a whole number of at least 0 "
                + "(0 makes no backup): '" + value + "'");
    }

    private static String folderNameOf(String value) {
        String folderName = value.strip();
        if (folderName.isEmpty()) {
            throw new ExportException("folder name is empty; use " + ALL_BOOKMARKS_KEYWORD + " to export every bookmark");
        }
        return folderName.equals(ALL_BOOKMARKS_KEYWORD) ? ALL_BOOKMARKS : folderName;
    }

    private static Path fullPathOf(String value, String description) {
        Path path = pathOf(value);
        if (!path.isAbsolute()) {
            throw new ExportException(description + " must be a full path: '" + value + "'");
        }
        return path;
    }

    private static Path pathOf(String value) {
        try {
            return Path.of(value);
        } catch (InvalidPathException e) {
            throw new ExportException("invalid path: '" + value + "'", e);
        }
    }
}
