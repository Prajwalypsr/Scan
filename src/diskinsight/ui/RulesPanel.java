package diskinsight.ui;

import diskinsight.model.FileRecord;
import diskinsight.model.Rule;
import diskinsight.util.Fmt;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Create, switch off and delete the rules that flag files for review. */
public class RulesPanel extends JPanel {

    private final MainFrame app;

    private final JPanel ruleList = new JPanel();
    private final JTextField nameField = Ui.field("Old screen recordings");
    private final JTextField extField = Ui.field("mp4, mov");
    private final JTextField sizeField = Ui.field("500");
    private final JTextField ageField = Ui.field("30");
    private final JComboBox<String> unitBox = Ui.combo(new String[]{"MB", "GB", "KB"});
    private final JLabel error = Ui.label("", Theme.SMALL, Theme.DANGER);
    private final JLabel preview = Ui.mono("");

    public RulesPanel(MainFrame app) {
        super(new BorderLayout());
        this.app = app;
        setOpaque(false);

        ruleList.setLayout(new BoxLayout(ruleList, BoxLayout.Y_AXIS));
        ruleList.setOpaque(false);

        Ui.Card listCard = new Ui.Card(new BorderLayout());
        listCard.flush();
        listCard.add(ruleList, BorderLayout.CENTER);

        Ui.Stack stack = new Ui.Stack();
        stack.push(Ui.sectionHeader("Cleanup rules",
                "A rule describes files worth reviewing. Turn one off to stop "
                + "flagging its matches."), 0);
        stack.push(listCard, Theme.S);
        stack.push(Ui.sectionHeader("Add a rule",
                "Leave a condition blank to ignore it."), Theme.XL);
        stack.push(buildForm(), Theme.S);
        stack.finish();

        add(stack, BorderLayout.CENTER);
    }

    /* ------------------------------------------------------------------ */

    private Ui.Card buildForm() {
        Ui.Card card = new Ui.Card(new BorderLayout(0, Theme.M));

        JPanel fields = new JPanel(new GridLayout(1, 4, Theme.M, 0));
        fields.setOpaque(false);
        fields.add(field("Rule name", nameField, null));
        fields.add(field("File types", extField, "Comma separated, no dots"));
        fields.add(withUnit("Larger than", sizeField, unitBox));
        fields.add(withUnit("Older than", ageField, staticUnit("days")));

        Ui.Btn add = Ui.primary("Add rule");
        add.addActionListener(e -> addRule());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.M, 0));
        actions.setOpaque(false);
        actions.add(add);
        actions.add(preview);

        JPanel south = new JPanel(new BorderLayout(0, Theme.XS));
        south.setOpaque(false);
        south.add(error, BorderLayout.NORTH);
        south.add(actions, BorderLayout.CENTER);

        card.add(fields, BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);

        DocumentListener live = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updatePreview(); }
            @Override public void removeUpdate(DocumentEvent e) { updatePreview(); }
            @Override public void changedUpdate(DocumentEvent e) { updatePreview(); }
        };
        extField.getDocument().addDocumentListener(live);
        sizeField.getDocument().addDocumentListener(live);
        ageField.getDocument().addDocumentListener(live);
        unitBox.addActionListener(e -> updatePreview());

        return card;
    }

    private JPanel field(String caption, JComponent input, String hint) {
        JPanel p = new JPanel(new BorderLayout(0, Theme.XS));
        p.setOpaque(false);
        p.add(Ui.caption(caption), BorderLayout.NORTH);
        input.setPreferredSize(new Dimension(120, 36));
        p.add(input, BorderLayout.CENTER);
        if (hint != null) p.add(Ui.hint(hint), BorderLayout.SOUTH);
        return p;
    }

    private JPanel withUnit(String caption, JComponent input, JComponent unit) {
        JPanel combo = new JPanel(new BorderLayout(Theme.XS, 0));
        combo.setOpaque(false);
        input.setPreferredSize(new Dimension(80, 36));
        unit.setPreferredSize(new Dimension(76, 36));
        combo.add(input, BorderLayout.CENTER);
        combo.add(unit, BorderLayout.EAST);

        JPanel p = new JPanel(new BorderLayout(0, Theme.XS));
        p.setOpaque(false);
        p.add(Ui.caption(caption), BorderLayout.NORTH);
        p.add(combo, BorderLayout.CENTER);
        return p;
    }

    private JComponent staticUnit(String text) {
        JLabel l = Ui.muted(text);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setBorder(new Ui.RoundedLine(Theme.LINE, Theme.RADIUS_CTRL));
        return l;
    }

    /* ------------------------------------------------------------------ */

    /** Rebuilds the rule rows and their live match counts. */
    public void refresh() {
        ruleList.removeAll();

        if (app.rules().isEmpty()) {
            JPanel empty = new JPanel(new BorderLayout(0, Theme.XS));
            empty.setOpaque(false);
            empty.setBorder(BorderFactory.createEmptyBorder(
                    Theme.XL, Theme.CARD_PAD, Theme.XL, Theme.CARD_PAD));
            empty.add(Ui.h3("No rules yet"), BorderLayout.NORTH);
            empty.add(Ui.muted("Add one below to start flagging files."), BorderLayout.CENTER);
            ruleList.add(empty);
        }

        for (int i = 0; i < app.rules().size(); i++) {
            Rule r = app.rules().get(i);
            if (i > 0) ruleList.add(Ui.hairline());
            ruleList.add(buildRuleRow(r));
        }

        updatePreview();
        revalidate();
        repaint();
    }

    private JPanel buildRuleRow(Rule rule) {
        int count = 0;
        long size = 0;
        for (FileRecord f : app.files()) {
            if (new diskinsight.engine.RuleEngine(rule).evaluate(f)) { count++; size += f.size; }
        }

        JPanel row = new JPanel(new BorderLayout(Theme.M, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(15, Theme.CARD_PAD, 15, Theme.CARD_PAD));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));

        Ui.Toggle toggle = new Ui.Toggle(rule.enabled);
        toggle.onChange(() -> {
            rule.enabled = toggle.isOn();
            app.saveRule(rule);
            app.applyRules();
            app.refreshAll();
        });
        JPanel toggleWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toggleWrap.setOpaque(false);
        toggleWrap.add(toggle);

        JPanel info = new JPanel(new GridLayout(0, 1, 0, 2));
        info.setOpaque(false);
        JLabel name = Ui.label(rule.name, Theme.BODY_BOLD,
                rule.enabled ? Theme.INK : Theme.INK_3);
        JLabel desc = Ui.mono(rule.describe());
        if (!rule.enabled) desc.setForeground(Theme.INK_3);
        info.add(name);
        info.add(desc);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.M, 0));
        right.setOpaque(false);
        JLabel matches = Ui.label(Fmt.files(count) + "  \u00b7  " + Fmt.bytes(size),
                Theme.MONO, rule.enabled ? Theme.INK_2 : Theme.INK_3);
        Ui.Btn delete = Ui.ghostSmall("Delete rule");
        delete.addActionListener(e -> {
            app.rules().remove(rule);
            app.deleteRule(rule);
            app.applyRules();
            app.refreshAll();
        });
        right.add(matches);
        right.add(delete);

        row.add(toggleWrap, BorderLayout.WEST);
        row.add(info, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    /* ------------------------------------------------------------------ */

    private long parseSize() {
        String raw = sizeField.getText().trim();
        if (raw.isEmpty()) return 0;
        try {
            double v = Double.parseDouble(raw);
            long unit = switch ((String) unitBox.getSelectedItem()) {
                case "GB" -> Fmt.GB;
                case "KB" -> Fmt.KB;
                default -> Fmt.MB;
            };
            return Math.round(v * unit);
        } catch (NumberFormatException e) {
            return -1; // signals "not a number"
        }
    }

    private int parseAge() {
        String raw = ageField.getText().trim();
        if (raw.isEmpty()) return 0;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void updatePreview() {
        long size = parseSize();
        int age = parseAge();
        String exts = extField.getText().trim();

        if (size < 0 || age < 0) {
            preview.setText("");
            return;
        }
        if (exts.isEmpty() && size == 0 && age == 0) {
            preview.setText("");
            return;
        }

        Rule draft = new Rule(0, "preview", true, exts, size, age);
        int n = 0;
        for (FileRecord f : app.files()) if (new diskinsight.engine.RuleEngine(draft).evaluate(f)) n++;
        preview.setText("This would flag " + n + " of "
                + Fmt.count(app.files().size()) + " files right now");
    }

    private void addRule() {
        String name = nameField.getText().trim();
        long size = parseSize();
        int age = parseAge();
        String exts = extField.getText().trim();

        if (name.isEmpty()) {
            fail("Give the rule a name so you can recognise it later.", nameField);
            return;
        }
        if (size < 0) {
            fail("The size has to be a number, for example 500.", sizeField);
            return;
        }
        if (age < 0) {
            fail("The age has to be a whole number of days, for example 30.", ageField);
            return;
        }
        if (exts.isEmpty() && size == 0 && age == 0) {
            fail("Set at least one condition \u2014 a file type, a size, or an age.", extField);
            return;
        }

        error.setText("");
        int id = (int) (System.currentTimeMillis() % 100_000);
        Rule rule = new Rule(id, name, true, exts, size, age);
        app.rules().add(rule);
        app.saveRule(rule);
        app.applyRules();
        app.refreshAll();

        nameField.setText("");
        extField.setText("");
        sizeField.setText("");
        ageField.setText("");
    }

    private void fail(String message, JComponent focus) {
        error.setText(message);
        focus.requestFocusInWindow();
    }
}
