## bookmark2html

Exports one folder of your Google Chrome bookmarks, or all of them, into a single,
self-contained and pretty HTML page (stylesheet and favicons embedded, works offline, light and
dark mode). Unless told not to, it also keeps zipped backups of the Chrome bookmarks file it
used, deleting the oldest ones beyond the number you choose.

It reads the JSON bookmarks file that Chrome keeps inside each profile, so there is no need to
export anything from the browser first.

### Usage

```
java -jar dist/bookmark2html.jar <BOOKMARKS_FILES> <OUTPUT_HTML_FILE> <FOLDER_NAME> <BACKUP_DIR> <BACKUPS_TO_KEEP> [--favicon=<URL>] [--apple-touch-icon=<URL>]
```

or the same arguments with the launchers `bookmark2html.cmd` (Windows) / `bookmark2html.sh`
(Linux, macOS). The five arguments are mandatory and go in that order; the options are optional
and may go anywhere among them.

- **BOOKMARKS_FILES** is the Chrome bookmarks file, or several candidate files separated by `;`:
  the first one that exists is used, and the program prints which one it read. If none exists
  it stops with an error listing every candidate tried. A candidate may also be a profile
  directory, standing for the `Bookmarks` file inside it.

  Listing several candidates keeps it working across Chrome versions, which have moved the
  bookmarks around: older versions keep them in `Bookmarks`, while recent ones (Chrome 153, for
  example) keep the bookmarks of your Google account in `AccountBookmarks` and `Bookmarks` may
  not even exist. The profile directory is:
  - Windows: `C:\Users\<USER>\AppData\Local\Google\Chrome\User Data\<PROFILE>`
  - macOS: `~/Library/Application Support/Google/Chrome/<PROFILE>`
  - Linux: `~/.config/google-chrome/<PROFILE>`

  `<PROFILE>` is `Default` for the first profile and `Profile 1`, `Profile 2`... for the others.
  Open `chrome://version` and look at *Profile Path* to know yours.
- **OUTPUT_HTML_FILE** is the full path of the HTML file to generate, for example
  `D:\bookmarks\bookmarks.html`. Missing parent directories are created.
- **FOLDER_NAME** is the case-sensitive name of the bookmark folder to export. Its bookmarks and
  all its subfolders are included. If several folders share the name, the first one found
  (bookmark bar first, then other bookmarks, then mobile bookmarks) is used. The Chrome root
  folders themselves (for example `Bookmarks bar`) can be exported too.
  To export every bookmark pass the keyword `ALL_EXISTING_BOOKMARKS`: the Chrome root folders
  (`Bookmarks bar`, `Other bookmarks`, `Mobile bookmarks`) then appear at the top of the tree.
  An empty name (`""`) is not accepted, because `java.exe` on Windows silently drops empty
  arguments. As a consequence, a folder named exactly `ALL_EXISTING_BOOKMARKS` cannot be exported
  on its own.
- **BACKUP_DIR** is the full path of a directory where the Chrome bookmarks file used for the
  export is saved, zipped, as `original_bookmarks_<yyyyMMdd_HHmmss>.zip` (the date and time of
  the export). It is created if it does not exist. The backup is made once the HTML is written,
  so a failed export leaves no backup. When `BACKUPS_TO_KEEP` is `0` this argument is ignored:
  it must still be there, but any value will do (for example `-`).
- **BACKUPS_TO_KEEP** is how many backups are kept in `BACKUP_DIR`, counting the one just made:
  a whole number of at least `0`.
  - `0` means **no backup**: nothing is written to `BACKUP_DIR` and nothing is deleted from it.
  - From `1` on, after each backup the oldest ones beyond that number are deleted; with `5`,
    the 5 newest remain. Only files named `original_bookmarks_<yyyyMMdd_HHmmss>.zip` directly
    inside `BACKUP_DIR` are considered, so anything else in that directory is never touched,
    and the backup just made is never deleted.

Options, for the icons of the generated page itself (not those of its bookmarks):

- **--favicon=URL** is the favicon of the page, the icon the browser shows in its tab:
  `<link rel="icon" href="URL">`.
- **--apple-touch-icon=URL** is the icon of the page when it is added to the home screen of a
  phone: `<link rel="apple-touch-icon" href="URL">`.

`URL` is written into the page exactly as given (only escaped for HTML). It can be absolute
(`/assets/favicon-32.png`, for a page published on a site that has that icon), relative to the
page (`favicon.png`) or a full url. The image is only linked, not embedded, so it must be
reachable from where the page is opened. Without an option the page simply lacks that icon.
An unknown option, an option without a value or the same option twice stops with an error.

The program prints which bookmarks file it read, what it exported, where the backup went (or
that no backup was made) and which old backups it deleted, and ends with exit code `0`.
On any problem (wrong arguments, folder not found, unreadable file...) it prints `Error: ...`
and ends with exit code `1`, which makes it easy to use from scripts.

Example, export the folder `TECHNICAL` from whichever bookmarks file this Chrome version uses,
keeping the last 5 backups:

```
bookmark2html.cmd "C:\Users\me\AppData\Local\Google\Chrome\User Data\Default\AccountBookmarks;C:\Users\me\AppData\Local\Google\Chrome\User Data\Default\Bookmarks" "D:\bookmarks\technical.html" TECHNICAL "D:\bookmarks\backup" 5
```

Export every bookmark, keeping only the latest backup:

```
bookmark2html.cmd "C:\Users\me\AppData\Local\Google\Chrome\User Data\Default" "D:\bookmarks\all.html" ALL_EXISTING_BOOKMARKS "D:\bookmarks\backup" 1
```

Export every bookmark without any backup:

```
bookmark2html.cmd "C:\Users\me\AppData\Local\Google\Chrome\User Data\Default" "D:\bookmarks\all.html" ALL_EXISTING_BOOKMARKS - 0
```

The same, giving the page the icons `favicon.png` and `touch-icon.png` that sit next to it:

```
bookmark2html.cmd "C:\Users\me\AppData\Local\Google\Chrome\User Data\Default" "D:\bookmarks\all.html" ALL_EXISTING_BOOKMARKS - 0 --favicon=favicon.png --apple-touch-icon=touch-icon.png
```

From Git Bash on Windows, a value starting with `/` (`--favicon=/assets/favicon-32.png`) reaches
the program turned into a Windows path by Git Bash itself; set `MSYS_NO_PATHCONV=1` to avoid it.

### The generated page

- A header with the folder name and how many bookmarks and folders it holds.
- The bookmarks as a tree, in the same order as in Chrome: the content of every folder is
  indented one level to the right with a guide line, so the hierarchy is clear at a glance.
- Every folder can be collapsed and shows how many bookmarks it holds.
- Every folder has an anchor made of its name and the names of the folders above it, in lower
  case, without accents and with dashes instead of spaces and symbols: the folder
  `Java > Spring Boot` is `#java/spring-boot`. When two folders would get the same anchor, the
  second one gets `-2`, the third `-3`, and so on. Moving the mouse over a folder shows a `#`
  next to its count: it links to that folder, so right-clicking it copies the link.

  Opening the page with the anchor of a folder at the end of the address
  (`bookmarks.html#java/spring-boot`) shows that folder alone: its name as the title, how many
  bookmarks and folders it holds, and above it the path of folders leading to it, each one a
  link to show that folder instead. The first link of the path goes back to the whole page, and
  so do the browser back button and an anchor that matches no folder.
- Each bookmark shows its favicon, its title (the url when it has none) and its domain.
  Favicons are downloaded (in parallel, once per domain) from Google's favicon service while
  generating the page and embedded as `data:` images, so an internet connection is needed only
  at that moment. A bookmark whose favicon cannot be downloaded just shows no icon.
- The bookmark under the mouse (or with the keyboard focus) is highlighted in yellow.
- A `Back` button above the header, at the left edge of the sheet (of the window with `Width`),
  goes back to the previous page of the browser history,
  whether another page or another folder of this one. It is disabled when there is nothing to
  go back to, for example when the page was opened in a new tab.
- Buttons at the top right corner:
  - `A-` / `A+` make the whole tree smaller or bigger, so more (or less) of it fits on screen.
  - `Host` hides or shows the host at the right of every bookmark.
  - `Width` makes the sheet as wide as the window; pressing it again goes back to the original
    width.

  The browser remembers these choices in its `localStorage` (keys `bookmark2html.*`), not in the
  HTML file, so they survive regenerating the page. Chrome shares them among all the pages opened
  from local files.
- On narrow screens (phones) the hosts are always hidden, and printing leaves the buttons out.
- The indentation width is the `--indent` CSS variable.

### Customizing the look

All the styling lives in [`src/main/resources/style.css`](src/main/resources/style.css) and is
copied into every generated page. The easiest way to restyle it is changing the CSS variables at
the top of the file (colors, font, indentation, icon size...); the class names of every part of the
page are listed there too. Rebuild after editing it.

For a one-off change, edit the `<style>` block of a generated page directly.

The page skeleton is [`src/main/resources/template.html`](src/main/resources/template.html),
with the placeholders `{{title}}`, `{{pageIcons}}`, `{{summary}}`, `{{style}}`, `{{content}}`
and `{{date}}`. `{{pageIcons}}` holds the `<link>` of each icon given with the options, each one
preceded by a line break, and is empty without them.

### Build

Requires Java 17 and Maven.

```
mvn clean package
```

It runs the tests and produces the executable `dist/bookmark2html.jar`, with all its
dependencies inside. That jar is part of the repository, so the program can be used right after
cloning it, with only Java 17 installed; rebuild and commit it again after changing the code.
The dates inside the jar are fixed, so rebuilding unchanged code gives an identical jar.

### Design

```
Bookmark2html          entry point: parses arguments and wires the pieces
BookmarksFileLocator   picks the first existing bookmarks file among the candidates
BookmarkExporter       read -> find folder -> render -> write -> back up -> rotate backups (unless 0 to keep)
reader/                BookmarkReader, ChromeBookmarkReader (Chrome JSON -> model)
model/                 BookmarkFolder, BookmarkLink
finder/                FolderFinder
render/                BookmarkRenderer, PrettyHtmlRenderer, PageTemplate, PageIcons, FolderAnchors
favicon/               FaviconSource, GoogleFaviconSource
writer/                OutputWriter, FileOutputWriter
backup/                BookmarkBackup, ZipBookmarkBackup, BackupRotation, BackupFileName
```

Supporting another browser or another output format means adding a new `BookmarkReader` or
`BookmarkRenderer` implementation; the rest of the code stays the same.

### Credits

The code of this project has been generated by [Claude](https://claude.ai) (Anthropic).
