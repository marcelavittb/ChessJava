// ========================= src/controller/Game.java =========================
package controller;

import model.board.Board;
import model.board.Position;
import model.pieces.*;
import model.board.Move;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random; // Adicionado para a IA

public class Game {

    private Board board;
    private boolean whiteToMove = true;
    private boolean gameOver = false;
    private String winner = null; // Para armazenar o vencedor

    // Casa-alvo para en passant (a casa "intermediária" após avanço de 2)
    private Position enPassantTarget = null;

    // Histórico simples (ex.: "e2e4", "O-O")
    private final List<String> history = new ArrayList<>();

    public Game() {
        this.board = new Board();
        setupPieces();
    }

    // ==== API usada pela GUI ====

    public Board board() { return board; }

    public boolean whiteToMove() { return whiteToMove; }

    public List<String> history() { return Collections.unmodifiableList(history); }

    public boolean isGameOver() { return gameOver; } // Novo método
    public String getWinner() { return winner; } // Novo método

    /** Retorna movimentos pseudo-legais (sem checar xeque). */
        public List<Position> legalMovesFrom(Position from) {
        Piece p = board.get(from);
        if (p == null) return List.of();
        if (p.isWhite() != whiteToMove) return List.of();

        List<Position> legalMoves = new ArrayList<>();

        // Obtem movimentos pseudo-legais da peça
        List<Position> pseudoMoves = p.getPossibleMoves();

        // Para peões, adiciona movimentos de en passant possíveis
        if (p instanceof Pawn) {
            int dir = p.isWhite() ? -1 : 1;
            int row = from.getRow();
            int col = from.getColumn();

            // En passant à esquerda
            Position epLeft = new Position(row, col - 1);
            if (epLeft.isValid()) {
                Piece adj = board.get(epLeft);
                if (adj instanceof Pawn && adj.isWhite() != p.isWhite() && epLeft.equals(enPassantTarget)) {
                    Position epCapture = new Position(row + dir, col - 1);
                    pseudoMoves.add(epCapture);
                }
            }
            // En passant à direita
            Position epRight = new Position(row, col + 1);
            if (epRight.isValid()) {
                Piece adj = board.get(epRight);
                if (adj instanceof Pawn && adj.isWhite() != p.isWhite() && epRight.equals(enPassantTarget)) {
                    Position epCapture = new Position(row + dir, col + 1);
                    pseudoMoves.add(epCapture);
                }
            }
        }

        // Para cada movimento pseudo-legal, testa se é legal (não deixa o rei em xeque)
        for (Position to : pseudoMoves) {
            Game testGame = snapshot();
            // Para promoções, testamos sem promoção (null), pois só queremos saber se o movimento é legal
            testGame.applyMoveInternal(from, to, null);
            if (!testGame.inCheck(p.isWhite())) {
                legalMoves.add(to);
            }
        }

        return legalMoves;
    }

    /** Verdadeiro se um peão que sai de 'from' e chega em 'to' promove. */
    public boolean isPromotion(Position from, Position to) {
        Piece p = board.get(from);
        if (!(p instanceof Pawn)) return false;
        if (p.isWhite()) return to.getRow() == 0;   // peão branco chegando na 8ª (topo)
        else              return to.getRow() == 7;  // peão preto chegando na 1ª (base)
    }

    /** Executa o lance (detecta roque, en passant e promoção). */
    public void move(Position from, Position to, Character promotion) {
        if (gameOver) return; // Não permite movimentos se o jogo acabou

        Piece p = board.get(from);
        if (p == null) return;
        if (p.isWhite() != whiteToMove) return;

        // --- Validação de movimento legal (agora com xeque) ---
        // Criamos um snapshot para testar o movimento
        Game testGame = snapshot();
        testGame.applyMoveInternal(from, to, promotion); // Aplica o movimento no snapshot

        if (testGame.inCheck(p.isWhite())) {
            // Se o próprio jogador estiver em xeque após o movimento, o movimento é ilegal
            System.out.println("Movimento ilegal: o rei ficaria em xeque.");
            return;
        }
        // --- Fim da validação ---

        applyMoveInternal(from, to, promotion); // Aplica o movimento real

        // Verifica condições de fim de jogo
        checkGameEndConditions();

        if (!gameOver && !whiteToMove) { // Se for a vez das pretas e o jogo não acabou, a IA joga
            makeAIMove();
            checkGameEndConditions(); // Verifica novamente após o movimento da IA
        }
    }

    // Novo método para aplicar o movimento sem a lógica de IA ou verificação de xeque-mate
    private void applyMoveInternal(Position from, Position to, Character promotion) {
        Piece p = board.get(from);
        if (p == null) return; // Should not happen if called after validation

        // -------- ROQUE --------
        boolean isKing = (p instanceof King);
        int dCol = Math.abs(to.getColumn() - from.getColumn());
        if (isKing && dCol == 2) {
            int row = from.getRow();

            // mover o rei
            board.set(to, p);
            board.set(from, null);

            if (to.getColumn() == 6) { // O-O (lado do rei)
                Piece rook = board.get(new Position(row, 7));
                board.set(new Position(row, 5), rook);
                board.set(new Position(row, 7), null);
                if (rook != null) rook.setMoved(true);
                addHistory("O-O");
            } else { // O-O-O (lado da dama)
                Piece rook = board.get(new Position(row, 0));
                board.set(new Position(row, 3), rook);
                board.set(new Position(row, 0), null);
                if (rook != null) rook.setMoved(true);
                addHistory("O-O-O");
            }

            p.setMoved(true);
            enPassantTarget = null; // roque limpa en passant
            whiteToMove = !whiteToMove;
            return;
        }

        // -------- EN PASSANT --------
        boolean isPawn = (p instanceof Pawn);
        boolean diagonal = from.getColumn() != to.getColumn();
        boolean toIsEmpty = board.get(to) == null;
        boolean isEnPassant = isPawn && diagonal && toIsEmpty && to.equals(enPassantTarget);
        if (isEnPassant) {
            board.set(to, p);
            board.set(from, null);
            // remover peão capturado "atrás" do destino
            int dir = p.isWhite() ? 1 : -1;
            board.set(new Position(to.getRow() + dir, to.getColumn()), null);
            p.setMoved(true);
            addHistory(coord(from) + "x" + coord(to) + " e.p.");
            enPassantTarget = null; // só vale no lance imediatamente seguinte
            whiteToMove = !whiteToMove;
            return;
        }

        // -------- LANCE NORMAL (com ou sem captura) --------
        Piece capturedBefore = board.get(to);

        board.set(to, p);
        board.set(from, null);
        p.setMoved(true);

        // -------- MARCA/RESSETA EN PASSANT --------
        if (isPawn && Math.abs(to.getRow() - from.getRow()) == 2) {
            int mid = (to.getRow() + from.getRow()) / 2;
            enPassantTarget = new Position(mid, from.getColumn());
        } else {
            enPassantTarget = null;
        }

        // -------- PROMOÇÃO --------
        if (promotion != null && isPawn && isPromotion(from, to)) {
            Piece np = switch (Character.toUpperCase(promotion)) {
                case 'R' -> new Rook(board, p.isWhite());
                case 'B' -> new Bishop(board, p.isWhite());
                case 'N' -> new Knight(board, p.isWhite());
                default  -> new Queen(board, p.isWhite());
            };
            np.setMoved(true);
            board.set(to, np);
        }

        // histórico simples (poderia virar SAN depois)
        addHistory(coord(from) + (capturedBefore != null ? "x" : "-") + coord(to));

        whiteToMove = !whiteToMove;
    }


    /** Indica se o lado passado está em xeque. */
    public boolean inCheck(boolean whiteSide) {
        Position kingPos = findKingPosition(whiteSide);
        if (kingPos == null) return false; // Rei não encontrado (já capturado)

        // Itera sobre todas as peças do oponente
        boolean opponentIsWhite = !whiteSide;
        for (Piece opponentPiece : board.pieces(opponentIsWhite)) {
            // Para peões, precisamos verificar os ataques, não os movimentos possíveis
            if (opponentPiece instanceof Pawn) {
                for (Position attackPos : ((Pawn) opponentPiece).getAttacks()) {
                    if (attackPos.equals(kingPos)) {
                        return true;
                    }
                }
            } else {
                // Para outras peças, os movimentos possíveis são os ataques
                for (Position possibleMove : opponentPiece.getPossibleMoves()) {
                    if (possibleMove.equals(kingPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Encontra a posição do rei da cor especificada. */
    private Position findKingPosition(boolean whiteSide) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position p = new Position(r, c);
                Piece piece = board.get(p);
                if (piece instanceof King && piece.isWhite() == whiteSide) {
                    return p;
                }
            }
        }
        return null; // Rei não encontrado (já capturado)
    }

    /** Verifica se o jogo terminou (xeque-mate ou afogamento). */
    private void checkGameEndConditions() {
        boolean currentSideHasMoves = false;
        List<Piece> currentPieces = board.pieces(whiteToMove);

        for (Piece p : currentPieces) {
            Position from = p.getPosition();
            List<Position> pseudoLegalMoves = p.getPossibleMoves();

            for (Position to : pseudoLegalMoves) {
                Game testGame = snapshot();
                testGame.applyMoveInternal(from, to, null); // Assume no promotion for checkmate check

                if (!testGame.inCheck(p.isWhite())) {
                    currentSideHasMoves = true;
                    break; // Encontrou um movimento legal, não é xeque-mate
                }
            }
            if (currentSideHasMoves) break;
        }

        if (!currentSideHasMoves) {
            if (inCheck(whiteToMove)) {
                gameOver = true;
                winner = whiteToMove ? "Pretas" : "Brancas";
                System.out.println("XEQUE-MATE! Vencedor: " + winner);
            } else {
                gameOver = true;
                winner = "Empate (Afogamento)";
                System.out.println("AFOGAMENTO! Jogo empatado.");
            }
        }
    }

    /** Reinicia o jogo para a posição inicial. */
    public void resetGame() {
        this.board = new Board();
        setupPieces();
        whiteToMove = true;
        gameOver = false;
        winner = null;
        enPassantTarget = null;
        history.clear();
        System.out.println("Jogo reiniciado.");
    }

    /** Snapshot raso (usa Board.copy()). */
    private Game snapshot() {
        Game g = new Game();
        g.board = this.board.copy();
        g.whiteToMove = this.whiteToMove;
        g.gameOver = this.gameOver;
        g.enPassantTarget = (this.enPassantTarget == null)
                ? null
                : new Position(enPassantTarget.getRow(), enPassantTarget.getColumn());
        g.history.clear();
        g.history.addAll(this.history);
        return g;
    }

    // ==== IA Simples para as peças pretas ====
    private void makeAIMove() {
        System.out.println("IA (Pretas) está pensando...");
        List<Move> possibleAIMoves = new ArrayList<>();
        List<Piece> blackPieces = board.pieces(false); // Peças pretas

        for (Piece p : blackPieces) {
            Position from = p.getPosition();
            List<Position> pseudoLegalMoves = p.getPossibleMoves();

            for (Position to : pseudoLegalMoves) {
                // Verifica se o movimento é legal (não coloca o próprio rei em xeque)
                Game testGame = snapshot();
                testGame.applyMoveInternal(from, to, null); // Assume no promotion for AI simple move

                if (!testGame.inCheck(false)) { // Se o rei preto não estiver em xeque após o movimento
                    possibleAIMoves.add(new Move(from, to, p, board.get(to), false, false, false, null));
                }
            }
        }

        if (possibleAIMoves.isEmpty()) {
            // Se a IA não tiver movimentos legais, pode ser xeque-mate ou afogamento
            // Isso será tratado por checkGameEndConditions()
            return;
        }

        // Escolhe um movimento aleatório entre os movimentos legais
        Random random = new Random();
        Move aiMove = possibleAIMoves.get(random.nextInt(possibleAIMoves.size()));

        // Executa o movimento da IA
        // Para promoção da IA, vamos simplificar e sempre promover para Rainha
        Character promo = null;
        if (aiMove.getMoved() instanceof Pawn && isPromotion(aiMove.getFrom(), aiMove.getTo())) {
            promo = 'Q';
        }
        applyMoveInternal(aiMove.getFrom(), aiMove.getTo(), promo);
        System.out.println("IA moveu: " + coord(aiMove.getFrom()) + " para " + coord(aiMove.getTo()));
    }


    // ==== utilidades ====

    private void addHistory(String moveStr) {
        history.add(moveStr);
    }

    private String coord(Position p) {
        // Converte (row,col) em notação "a1..h8" assumindo 0..7 de cima p/baixo
        char file = (char) ('a' + p.getColumn());
        int rank = 8 - p.getRow();
        return "" + file + rank;
    }

    /** Coloca as peças na posição inicial padrão. */
    private void setupPieces() {
        // Brancas embaixo (linhas 6 e 7)
        board.placePiece(new Rook(board, true),   new Position(7, 0));
        board.placePiece(new Knight(board, true), new Position(7, 1));
        board.placePiece(new Bishop(board, true), new Position(7, 2));
        board.placePiece(new Queen(board, true),  new Position(7, 3));
        board.placePiece(new King(board, true),   new Position(7, 4));
        board.placePiece(new Bishop(board, true), new Position(7, 5));
        board.placePiece(new Knight(board, true), new Position(7, 6));
        board.placePiece(new Rook(board, true),   new Position(7, 7));
        for (int c = 0; c < 8; c++) {
            board.placePiece(new Pawn(board, true), new Position(6, c));
        }

        // Pretas em cima (linhas 0 e 1)
        board.placePiece(new Rook(board, false),   new Position(0, 0));
        board.placePiece(new Knight(board, false), new Position(0, 1));
        board.placePiece(new Bishop(board, false), new Position(0, 2));
        board.placePiece(new Queen(board, false),  new Position(0, 3));
        board.placePiece(new King(board, false),   new Position(0, 4));
        board.placePiece(new Bishop(board, false), new Position(0, 5));
        board.placePiece(new Knight(board, false), new Position(0, 6));
        board.placePiece(new Rook(board, false),   new Position(0, 7));
        for (int c = 0; c < 8; c++) {
            board.placePiece(new Pawn(board, false), new Position(1, c));
        }
    }
}