// ========================= src/view/ChessGUI.java =========================
package view;

import controller.Game;
import model.board.Position;
import model.pieces.Pawn;
import model.pieces.King;
import model.pieces.Piece;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

public class ChessGUI extends JFrame {

    private final Game game;

    private final JPanel boardPanel;
    private final JButton[][] squares = new JButton[8][8];

    private final JLabel status;
    private final JTextArea history;
    private final JScrollPane historyScroll;
    private final JButton resetButton; // Novo botão de reset

    // Seleção atual e movimentos legais
    private Position selected = null;
    private List<Position> legalForSelected = new ArrayList<>();

    // Bordas para destacar seleção e destinos
    private static final Border BORDER_SELECTED = BorderFactory.createLineBorder(Color.BLUE, 3);
    private static final Border BORDER_LEGAL = BorderFactory.createLineBorder(new Color(0, 128, 0), 3);
    private static final Border BORDER_CHECK = BorderFactory.createLineBorder(Color.RED, 3); // Nova borda para xeque

    public ChessGUI() {
        super("ChessGame");
        this.game = new Game();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        // Painel do tabuleiro (8x8)
        boardPanel = new JPanel(new GridLayout(8, 8, 0, 0));
        boardPanel.setBackground(Color.DARK_GRAY);

        // Cria botões das casas
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
                b.setFont(b.getFont().deriveFont(Font.BOLD, 24f)); // fallback com Unicode
                b.addActionListener(e -> handleClick(new Position(rr, cc)));
                squares[r][c] = b;
                boardPanel.add(b);
            }
        }

        // Barra inferior de status
        status = new JLabel("Vez: Brancas");
        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // Botão de Reset
        resetButton = new JButton("Reiniciar Jogo");
        resetButton.addActionListener(e -> {
            game.resetGame();
            selected = null;
            legalForSelected.clear();
            refresh();
        });

        // Painel inferior para status e botão de reset
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(status, BorderLayout.CENTER);
        bottomPanel.add(resetButton, BorderLayout.EAST);


        // Histórico
        history = new JTextArea(10, 20);
        history.setEditable(false);
        history.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        historyScroll = new JScrollPane(history);

        // Layout principal: tabuleiro à esquerda, histórico à direita
        JPanel rightPanel = new JPanel(new BorderLayout(6, 6));
        rightPanel.add(new JLabel("Histórico de lances:"), BorderLayout.NORTH);
        rightPanel.add(historyScroll, BorderLayout.CENTER);

        add(boardPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH); // Adiciona o novo painel inferior
        add(rightPanel, BorderLayout.EAST);

        // Atualiza ícones conforme a janela/painel muda de tamanho
        boardPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refresh(); // recarrega ícones ajustando o tamanho
            }
        });

        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setVisible(true);

        refresh();
    }

    /**
     * Lida com clique numa casa do tabuleiro.
     */
        private void handleClick(Position clicked) {
        if (game.isGameOver()) {
            JOptionPane.showMessageDialog(this, "O jogo acabou! " + game.getWinner(), "Fim de Jogo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!game.whiteToMove()) { // Impede o jogador de mover as peças da IA
            JOptionPane.showMessageDialog(this, "É a vez das Pretas (IA)!", "Aguarde", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Piece p = game.board().get(clicked);

        if (selected == null) {
            // Só seleciona se for peça da vez e tiver movimentos legais
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
            } else if (p != null && p.isWhite() == game.whiteToMove()) {
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
        refresh();
}

    /**
     * Diálogo de escolha de peça para promoção.
     * Retorna 'Q','R','B','N' de acordo com a escolha.
     */
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

    /**
     * Atualiza cores, bordas, ícones das peças, status e histórico.
     * Ajusta o tamanho do ícone dinamicamente (quadrado do botão).
     */
    private void refresh() {
        // 1) Cores base e limpa bordas
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                boolean light = (r + c) % 2 == 0;
                Color base = light ? new Color(240, 217, 181) : new Color(181, 136, 99);
                JButton b = squares[r][c];
                b.setBackground(base);
                b.setBorder(null);
            }
        }

        // 2) Realce seleção e movimentos legais
        if (selected != null) {
            squares[selected.getRow()][selected.getColumn()].setBorder(BORDER_SELECTED);
            for (Position d : legalForSelected) {
                squares[d.getRow()][d.getColumn()].setBorder(BORDER_LEGAL);
            }
        }

        // 3) Ícones das peças (ou Unicode como fallback)
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

                char sym = p.getSymbol().charAt(0); // "K","Q","R","B","N","P"
                ImageIcon icon = ImageUtil.getPieceIcon(p.isWhite(), sym, iconSize);
                if (icon != null) {
                    b.setIcon(icon);
                    b.setText("");
                } else {
                    // Fallback: Unicode
                    b.setIcon(null);
                    b.setText(toUnicode(p.getSymbol(), p.isWhite()));
                }
            }
        }

        // 4) Realce do rei em xeque
        // Esta lógica precisa ser mais robusta para encontrar o rei e aplicar a borda
        // A verificação de xeque é feita no Game, então podemos usá-la aqui.
        // Para simplificar, vamos apenas verificar se o lado atual está em xeque.
        if (game.inCheck(game.whiteToMove())) {
            // Encontrar a posição do rei do lado que está em xeque
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    Position currentPos = new Position(r, c);
                    Piece piece = game.board().get(currentPos);
                    if (piece instanceof King && piece.isWhite() == game.whiteToMove()) {
                        squares[r][c].setBorder(BORDER_CHECK);
                        break;
                    }
                }
            }
        }


        // 5) Status e histórico
        String statusText;
        if (game.isGameOver()) {
            statusText = "FIM DE JOGO! " + game.getWinner();
            JOptionPane.showMessageDialog(this, statusText, "Fim de Jogo", JOptionPane.INFORMATION_MESSAGE);
        } else {
            String side = game.whiteToMove() ? "Brancas" : "Pretas";
            String chk = game.inCheck(game.whiteToMove()) ? " — Xeque!" : "";
            statusText = "Vez: " + side + chk;
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

    /**
     * Converte símbolo da peça em caractere Unicode (fallback).
     */
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

    /**
     * Calcula o tamanho do ícone com base no tamanho atual das casas.
     * Usa o menor lado do primeiro botão como referência, aplicando uma pequena margem.
     */
    private int computeSquareIconSize() {
        // Pega um botão representante (0,0)
        JButton b = squares[0][0];
        int w = Math.max(1, b.getWidth());
        int h = Math.max(1, b.getHeight());
        int side = Math.min(w, h);
        if (side <= 1) {
            // Janela ainda não renderizou completamente → tamanho padrão
            return 64;
        }
        // pequena margem para não encostar na borda
        return Math.max(24, side - 6);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessGUI::new);
    }
}