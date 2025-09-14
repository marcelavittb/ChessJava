// ========================= src/view/ChessGUI.java =========================
package view;

import controller.AIPlayer;
import controller.Game;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import model.board.Position;
import model.pieces.Pawn;
import model.pieces.Piece;

public class ChessGUI extends JFrame {

    private final Game game;
    private final AIPlayer ai;

    private final JPanel boardPanel;
    private final JButton[][] squares = new JButton[8][8];

    private final JLabel status;
    private final JTextArea history;
    private final JScrollPane historyScroll;
    private final JButton resetButton;
    private final JCheckBox trainingMode;
    private final JComboBox<String> aiDifficultySelector;

    private Position selected = null;
    private List<Position> legalForSelected = new ArrayList<>();

    private static final Border BORDER_SELECTED = BorderFactory.createLineBorder(Color.BLUE, 3);
    private static final Border BORDER_LEGAL = BorderFactory.createLineBorder(new Color(0, 128, 0), 3);
    private static final Border BORDER_CHECK = BorderFactory.createLineBorder(Color.RED, 3);

    private boolean gameOverMessageShown = false;
    private boolean aiMessageShown = false;

    public ChessGUI() {
        super("ChessGame");
        this.game = new Game();
        this.ai = new AIPlayer(game);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        // Painel do tabuleiro
        boardPanel = new JPanel(new GridLayout(8, 8, 0, 0));
        boardPanel.setBackground(Color.PINK);

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                final int rr = r;
                final int cc = c;
                JButton b = new JButton();
                b.setMargin(new Insets(0, 0, 0, 0));
                b.setFocusPainted(false);
                b.setOpaque(true);
                b.setBorderPainted(true);
                b.setContentAreaFilled(true);
                b.setFont(b.getFont().deriveFont(Font.BOLD, 24f));
                b.addActionListener(e -> handleClick(new Position(rr, cc)));
                squares[r][c] = b;
                boardPanel.add(b);
            }
        }

        // Barra inferior
        status = new JLabel("Vez: Brancas");
        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        resetButton = new JButton("Reiniciar Jogo");
        resetButton.addActionListener(e -> {
            game.resetGame();
            selected = null;
            legalForSelected.clear();
            gameOverMessageShown = false;
            aiMessageShown = false;
            refresh();
        });

        trainingMode = new JCheckBox("Modo Treino (rei pode morrer)");
        trainingMode.setFocusable(false);
        trainingMode.addActionListener(e -> {
            game.setAllowIllegalMoves(trainingMode.isSelected());
            selected = null;
            legalForSelected.clear();
            refresh();
            JOptionPane.showMessageDialog(this, 
            "Modo Treino ativado: agora o rei pode ser capturado. Xeque-mate não é obrigatório!",
            "Modo Treino", JOptionPane.INFORMATION_MESSAGE);
        });

        String[] difficulties = {"Fácil (Prof. 1)", "Médio (Prof. 2)", "Difícil (Prof. 3)", "Muito Difícil (Prof. 4)"};
        aiDifficultySelector = new JComboBox<>(difficulties);
        aiDifficultySelector.setSelectedIndex(2);
        aiDifficultySelector.addActionListener(e -> {
            int selectedIndex = aiDifficultySelector.getSelectedIndex();
            game.setAIDifficulty(selectedIndex + 1);
        });

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(status, BorderLayout.CENTER);

        JPanel rightBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightBottom.add(new JLabel("Dificuldade IA:"));
        rightBottom.add(aiDifficultySelector);
        rightBottom.add(trainingMode);
        rightBottom.add(resetButton);
        bottomPanel.add(rightBottom, BorderLayout.EAST);

        history = new JTextArea(10, 20);
        history.setEditable(false);
        history.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        historyScroll = new JScrollPane(history);

        JPanel rightPanel = new JPanel(new BorderLayout(6, 6));
        rightPanel.add(new JLabel("Histórico de lances:"), BorderLayout.NORTH);
        rightPanel.add(historyScroll, BorderLayout.CENTER);

        add(boardPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);

        boardPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refresh();
            }
        });

        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setVisible(true);

        refresh();
    }

    public void handleClick(Position clicked) {
        if (game.isGameOver()) {
            if (!gameOverMessageShown) {
                JOptionPane.showMessageDialog(this, "O jogo acabou! " + game.getWinner(), "Fim de Jogo", JOptionPane.INFORMATION_MESSAGE);
                gameOverMessageShown = true;
            }
            return;
        }

        Piece p = game.board().get(clicked);

        if (!game.whiteToMove() && !game.isGameOver()) {
            JOptionPane.showMessageDialog(this, "É a vez das Pretas (IA)!", "Aguarde", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (selected == null) {
            if (p != null && p.isWhite() == game.whiteToMove()) {
                List<Position> moves = game.legalMovesFrom(clicked);
                if (!moves.isEmpty()) {
                    selected = clicked;
                    legalForSelected = moves;
                }
            }
        } else {
            if (legalForSelected.contains(clicked)) {
                Character promo = null;
                Piece moving = game.board().get(selected);
                if (moving != null && moving instanceof Pawn && game.isPromotion(selected, clicked)) {
                    promo = askPromotion();
                }
                game.move(selected, clicked, promo);
                selected = null;
                legalForSelected.clear();
            } else {
                if (p != null && p.isWhite() == game.whiteToMove()) {
                    List<Position> moves = game.legalMovesFrom(clicked);
                    if (!moves.isEmpty()) {
                        selected = clicked;
                        legalForSelected = moves;
                    } else {
                        selected = null;
                        legalForSelected.clear();
                    }
                } else {
                    selected = null;
                    legalForSelected.clear();
                }
            }
        }
        refresh();
    }

    private Character askPromotion() {
        String[] opts = {"Rainha", "Torre", "Bispo", "Cavalo"};
        int ch = JOptionPane.showOptionDialog(
            this,
            "Escolha a peça para promoção:",
            "Promoção",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            opts,
            opts[0]
        );
        return switch (ch) {
            case 1 -> 'R';
            case 2 -> 'B';
            case 3 -> 'N';
            default -> 'Q';
        };
    }

    private void refresh() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                boolean light = (r + c) % 2 == 0;
                Color base = light ? new Color(240, 217, 181) : new Color(181, 136, 99);
                JButton b = squares[r][c];
                b.setBackground(base);
                b.setBorder(null);
            }
        }

        if (selected != null) {
            squares[selected.getRow()][selected.getColumn()].setBorder(BORDER_SELECTED);
            for (Position d : legalForSelected) {
                squares[d.getRow()][d.getColumn()].setBorder(BORDER_LEGAL);
            }
        }

        int iconSize = computeSquareIconSize();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = game.board().get(new Position(r, c));
                JButton b = squares[r][c];

                if (p == null) {
                    b.setIcon(null);
                    b.setText("");
                    continue;
                }

                char sym = p.getSymbol().charAt(0);
                ImageIcon icon = ImageUtil.getPieceIcon(p.isWhite(), sym, iconSize);
                if (icon != null) {
                    b.setIcon(icon);
                    b.setText("");
                } else {
                    b.setIcon(null);
                    b.setText(toUnicode(p.getSymbol(), p.isWhite()));
                }
            }
        }

        if (game.inCheck(game.whiteToMove())) {
            Position kingPos = game.findKingPosition(game.whiteToMove());
            if (kingPos != null) {
                squares[kingPos.getRow()][kingPos.getColumn()].setBorder(BORDER_CHECK);
            }
        }

        String statusText;
        if (game.isGameOver()) {
            statusText = "FIM DE JOGO! " + game.getWinner();
            if (!gameOverMessageShown) {
                JOptionPane.showMessageDialog(this, statusText, "Fim de Jogo", JOptionPane.INFORMATION_MESSAGE);
                gameOverMessageShown = true;
            }
        } else {
            String side = game.whiteToMove() ? "Brancas" : "Pretas";
            String chk = game.inCheck(game.whiteToMove()) ? " — Xeque!" : "";
            statusText = "Vez: " + side + chk;

            if (game.whiteToMove()) {
                aiMessageShown = false;
            } else {
                if (!aiMessageShown) {
                    JOptionPane.showMessageDialog(this, "É a vez das Pretas (IA)!", "Aguarde", JOptionPane.INFORMATION_MESSAGE);
                    aiMessageShown = true;
                }
            }
        }
        status.setText(statusText);

        StringBuilder sb = new StringBuilder();
        var hist = game.history();
        for (int i = 0; i < hist.size(); i++) {
            if (i % 2 == 0) sb.append((i / 2) + 1).append('.').append(' ');
            sb.append(hist.get(i)).append(' ');
            if (i % 2 == 1) sb.append('\n');
        }
        history.setText(sb.toString());
        history.setCaretPosition(history.getDocument().getLength());
    }

    private String toUnicode(String sym, boolean white) {
        return switch (sym) {
            case "K" -> white ? "\u2654" : "\u265A";
            case "Q" -> white ? "\u2655" : "\u265B";
            case "R" -> white ? "\u2656" : "\u265C";
            case "B" -> white ? "\u2657" : "\u265D";
            case "N" -> white ? "\u2658" : "\u265E";
            case "P" -> white ? "\u2659" : "\u265F";
            default -> "";
        };
    }

    private int computeSquareIconSize() {
        JButton b = squares[0][0];
        int w = Math.max(1, b.getWidth());
        int h = Math.max(1, b.getHeight());
        int side = Math.min(w, h);
        return side <= 1 ? 64 : Math.max(24, side - 6);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessGUI::new);
    }
}