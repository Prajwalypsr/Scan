# DiskInsight

A desktop application that shows what is taking up space in a folder, and points
at files worth reviewing — without ever deleting anything.

Built with Java Swing. MySQL is optional: the application runs without it.

---

## Running it

You need JDK 17 or newer (built and tested on JDK 21).

**Linux / macOS**

```bash
./run.sh
```

**Windows**

```
run.bat
```

**Or by hand**

```bash
javac -d out src/diskinsight/*.java
java -cp out diskinsight.DiskInsightApp
```

The window opens on the sample folder so every screen has something to show.
Click **Choose folder…** to scan your own files.

---

## The four screens

| Screen | What it does |
|---|---|
| **Overview** | Total size and file count, the storage tape broken down by file type, quick counts, and one card per active rule |
| **Files** | Every file, with search, filters by type / size / age, sorting, and tick-boxes to collect paths |
| **Rules** | Turn rules on and off, delete them, add new ones with a live match preview |
| **Scan history** | Past scans of the same folder, with the change in size between them |

The **storage tape** on the Overview screen is the main idea: one bar for the
whole folder, split into blocks sized by how much space each file type takes,
with a ruler marked in GB underneath. Hover a block for the figures, click one to
open that group in the file list.

---

## Project structure

```
src/diskinsight/
│
├── DiskInsightApp.java   entry point — main()
├── MainFrame.java        the window: top bar, tabs, scan orchestration, shared state
│
├── Theme.java            ALL colours, fonts, spacing, radii            ← styling lives here
├── Ui.java               cards, buttons, toggles, chips, stat tiles, fields
├── Skin.java             flat replacements for stock combo box / scroll bar painting
├── StorageTape.java      the storage tape and its ruler
│
├── OverviewPanel.java    screen 1
├── FilesPanel.java       screen 2 — JTable, model, renderers, filters
├── RulesPanel.java       screen 3 — rule rows and the add-rule form
├── HistoryPanel.java     screen 4
│
├── FileRecord.java       one scanned file
├── Rule.java             a rule + the matching logic
├── ScanRecord.java       one completed scan
├── Category.java         file type groups and their colours
├── Fmt.java              size / date / percentage formatting
│
├── FolderScanner.java    reads a real folder on a background thread
├── DemoData.java         the built-in sample folder
└── Database.java         MySQL storage (optional)
```

---

## How the styling stays uniform

Two rules, and the whole application follows them:

1. **No screen defines a colour, font or gap.** Every value comes from
   `Theme.java`. There is one spacing scale (6 / 10 / 16 / 24 / 36), one corner
   radius, one card padding, one table row height.
2. **No screen paints its own surfaces.** Cards, buttons, toggles, chips and
   stat tiles all come from `Ui.java`, so a card on one screen is identical to a
   card on another.

That means a change to the look is a change to one file. To make the whole
application darker, greener, or more spacious, edit the tokens at the top of
`Theme.java` and every screen follows — no screen needs touching.

`Skin.java` exists because Swing's default combo boxes and scroll bars keep the
old Metal look (bevelled grey arrows) that clashes with the flat cards. It
replaces their painting only; behaviour is untouched.

---

## How the rules work

A rule holds up to three conditions:

- **file types** — `zip, rar, 7z` (blank means any type)
- **minimum size** — in KB, MB or GB (blank means any size)
- **minimum age** — in days (blank means any age)

A file matches when it satisfies **every** condition that has been set. Blank
conditions are ignored. The logic is one method, `Rule.matches()`:

```java
public boolean matches(FileRecord f) {
    if (!extensions.isEmpty() && !extensions.contains(f.extension)) return false;
    if (minSize > 0 && f.size < minSize) return false;
    if (olderThanDays > 0 && Fmt.daysOld(f.modified) < olderThanDays) return false;
    return true;
}
```

`MainFrame.applyRules()` runs every file past every enabled rule and records
which rules matched, so the file list can explain *why* something is flagged.

**A match never deletes anything.** The strongest action in the whole
application is copying a list of file paths to the clipboard. That is deliberate,
and it is stated on the Overview screen so the user knows what they are getting.

---

## Turning MySQL on

The application runs fine without a database — `Database.connect()` catches the
failure, the footer shows `Database: offline`, and everything works from memory.

To switch it on:

1. **Create the tables**

   ```bash
   mysql -u root -p < schema.sql
   ```

2. **Get the driver.** Download `mysql-connector-j-9.x.x.jar` and put it in a
   `lib/` folder next to `src/`. Both run scripts pick it up automatically.

3. **Edit the connection settings** at the top of `Database.java`:

   ```java
   public static final String URL      = "jdbc:mysql://localhost:3306/diskinsight?useSSL=false&serverTimezone=UTC";
   public static final String USER     = "root";
   public static final String PASSWORD = "";
   ```

Once connected, scans and rules are saved automatically and the footer says
`Database: connected to diskinsight`.

Notes on the schema: `size_bytes` is `BIGINT`, not `INT`, because an `INT` stops
at 2.1 GB and a single video file can be larger. Inserts are batched 500 at a
time inside one transaction, so a 10,000-file scan is one commit rather than
10,000 round trips.

---

## Things you may be asked to change, and where

| Ask | Where to change it |
|---|---|
| Different colours, fonts, or spacing | `Theme.java` — the token block at the top |
| Add a file type group (e.g. Code, Fonts) | `Category.java` — add one enum constant with its colour and extensions |
| Change what the "At a glance" tiles count | `OverviewPanel.buildStats()` |
| Add a column to the file table | `FilesPanel.FilesModel` + a renderer, and widths in `buildTable()` |
| Add a filter (e.g. "files I opened this week") | `FilesPanel.applyFilters()` |
| Change the starting rules | `Rule.defaults()` and the `INSERT` block in `schema.sql` |
| Scan only the top folder, not subfolders | `MainFrame.startScan()` — pass `false` to `FolderScanner` |
| Run rule matching in SQL instead of Java | `schema.sql` has a worked example at the bottom |

---

## Notes for the report

- **Threading.** The scan runs in `FolderScanner extends SwingWorker`. Swing is
  single-threaded: if the folder walk ran on the Event Dispatch Thread the window
  would freeze until it finished. `doInBackground()` walks the tree,
  `publish()`/`process()` stream the current path to the progress screen, and
  `done()` hands the result back on the EDT.
- **Permissions.** `visitFileFailed()` counts unreadable files and carries on
  rather than aborting the scan. The count is reported in the footer.
- **Custom painting.** The storage tape, stat tiles, buttons, toggles and every
  table cell are drawn in `paintComponent()` with antialiasing enabled, which is
  why they do not look like default Swing.
- **Sample data.** `DemoData` uses a fixed seed, so the sample folder is
  identical on every run — useful when demonstrating. Its size distribution is
  deliberately skewed: most files small, a few large, which is how a real
  Downloads folder behaves. The earlier rows on the Scan history screen are
  illustrative and only appear for the sample folder; real scans add real rows.
