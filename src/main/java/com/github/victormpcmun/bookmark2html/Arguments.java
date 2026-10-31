package com.github.victormpcmun.bookmark2html;

import com.github.victormpcmun.bookmark2html.render.PageIcons;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Command line arguments: bookmarks file (the first existing one among several candidates), output
 * html file, folder to export, the directory where the bookmarks file is backed up and how many
 * backups are kept there; then, anywhere among them, the optional --name=value options with the
 * icons of the generated page.
 * The keyword ALL_EXISTING_BOOKMARKS is turned into an empty folder name, which means every bookmark.
 * Keeping 0 backups means no backup at all: the backup directory is then ignored and left empty here.
 */
public record Arguments(Path bookmarksFile, Path outputFile, String folderName,
                        Optional<Path> backupDirectory, int backupsToKeep, PageIcons pageIcons) {

    static final int NO_BACKUP = 0;

    static final String ALL_BOOKMARKS_KEYWORD = "ALL_EXISTING_BOOKMARKS";
    static final String ALL_BOOKMARKS = "";

    static final String FAVICON_OPTION = "favicon";
    static final String APPLE_TOUCH_ICON_OPTION = "apple-touch-icon";

    private static final List<String> OPTIONS = List.of(FAVICON_OPTION, APPLE_TOUCH_ICON_OPTION);
    private static final String OPTION_PREFIX = "--";

    private static final int EXPECTED_ARGUMENTS = 5;

    private static final String USAGE = """
            expected 5 arguments, plus optionally the --name=value options
              1. Chrome bookmarks file, or several candidates separated by ';' (the first existing one
                 is used); a profile directory stands for the Bookmarks file inside it
              2. Output html file, as a full path
              3. Name of the bookmark folder to export (ALL_EXISTING_BOOKMARKS exports every bookmark)
              4. Directory where the Chrome bookmarks file is backed up as a zip, as a full path
                 (ignored when 5 is 0)
              5. Number of backups to keep in that directory, counting the new one; older ones are deleted.
                 0 makes no backup at all
            options, each one at most once and anywhere among the arguments:
              --favicon=URL           favicon of the generated page, for example /assets/favicon-32.png
              --apple-touch-icon=URL  icon of the page when added to the home screen of a phone
              URL is written into the page exactly as given; without an option the page lacks that icon
            example:
              java -jar bookmark2html.jar \
            "C:\\Chrome\\User Data\\Default\\AccountBookmarks;C:\\Chrome\\User Data\\Default\\Bookmarks" \
            "D:\\bookmarks\\bookmarks.html" TECHNICAL "D:\\bookmarks\\backup" 5 --favicon=favicon.png""";

    public static Arguments parse(String[] args) {
        List<String> arguments = new ArrayList<>();
        Map<String, String> options = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith(OPTION_PREFIX)) {
                addOption(options, arg);
            } else {
                arguments.add(arg);
            }
        }
        if (arguments.size() != EXPECTED_ARGUMENTS) {
            throw new ExportException(USAGE);
        }
        int backupsToKeep = backupsToKeepOf(arguments.get(4));
        return new Arguments(
                BookmarksFileLocator.firstExisting(arguments.get(0)),
                fullPathOf(arguments.get(1), "output html file"),
                folderNameOf(arguments.get(2)),
                backupDirectoryOf(arguments.get(3), backupsToKeep),
                backupsToKeep,
                new PageIcons(Optional.ofNullable(options.get(FAVICON_OPTION)),
                        Optional.ofNullable(options.get(APPLE_TOUCH_ICON_OPTION))));
    }

    private static void addOption(Map<String, String> options, String arg) {
        int equals = arg.indexOf('=');
        String name = arg.substring(OPTION_PREFIX.length(), equals < 0 ? arg.length() : equals);
        if (!OPTIONS.contains(name)) {
            throw new ExportException("unknown option '" + arg + "'; the options are --"
                    + String.join("=URL, --", OPTIONS) + "=URL");
        }
        String value = equals < 0 ? "" : arg.substring(equals + 1).strip();
        if (value.isEmpty()) {
            throw new ExportException("option --" + name + " has no value: write it as --" + name + "=URL");
        }
        if (options.putIfAbsent(name, value) != null) {
            throw new ExportException("option --" + name + " is given more than once");
        }
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
