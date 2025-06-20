package fr.classcord.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import fr.classcord.model.User;

public class UserCellRenderer extends JPanel implements ListCellRenderer<User> {
    private final JLabel nameLabel = new JLabel();
    private final StatusIndicator statusIndicator = new StatusIndicator();

    public UserCellRenderer() {
        setLayout(new BorderLayout(5, 0));
        add(nameLabel, BorderLayout.CENTER);
        add(statusIndicator, BorderLayout.EAST);
        setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends User> list, User user, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        nameLabel.setText(user.getUsername());
        statusIndicator.setStatus(user.getStatus());

        setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        nameLabel.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());

        return this;
    }

    private static class StatusIndicator extends JPanel {
        private String status = "online";

        public void setStatus(String status) {
            this.status = status;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Color color = switch (status) {
                case "online"    -> Color.GREEN;
                case "away"      -> Color.ORANGE;
                case "dnd"       -> Color.RED;
                case "invisible" -> Color.GRAY;
                default          -> Color.LIGHT_GRAY;
            };

            int d = 10;
            int x = (getWidth() - d) / 2;
            int y = (getHeight() - d) / 2;

            g.setColor(color);
            g.fillOval(x, y, d, d);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(20, 20);
        }
    }
}
