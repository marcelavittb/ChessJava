package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import model.board.Board;
import model.board.Move;
import model.board.Position;
import model.pieces.*;

public class Game {

    private Board board;
    private boolean whiteToMove = true;
    private boolean gameOver = false;
    private String winner = null;

    private boolean allowIllegalMoves = false;
    private Position enPassantTarget = null;
    private final List<String> history = new ArrayList<>();

    private final Random rng = new Random();

    private final AIPlayer aiPlayer;
    private int aiDifficulty = 3;

    public Game() {
        this.board = new Board();
        setupPieces();
        this.aiPlayer = new AIPlayer(this);
    }

    public Board board() { return board; }
    public boolean whiteToMove() { return whiteToMove; }
    public List<String> history() { return Collections.unmodifiableList(history); }
    public boolean isGameOver() { return gameOver; }
    public String getWinner() { return winner; }
    public void setAllowIllegalMoves(boolean allow) { this.allowIllegalMoves = allow; }
    public boolean allowIllegalMoves() { return allowIllegalMoves; }
    public void setAIDifficulty(int difficulty) {
        this.aiDifficulty = difficulty;
    }

    /** Retorna movimentos pseudo-legais (sem checar xeque), ou todos se allowIllegalMoves=true. */
    public List<Position> legalMovesFrom(Position from) {
        Piece p = board.get(from);
        if (p == null) return List.of();
        if (p.isWhite() != whiteToMove) return List.of();

        List<Position> pseudoMoves = new ArrayList<>(p.getPossibleMoves());

        if (p instanceof Pawn) {
            int dir = p.isWhite() ? -1 : 1;
            int row = from.getRow();
            int col = from.getColumn();

            Position epLeft = new Position(row, col - 1);
            if (epLeft.isValid()) {
                Piece adj = board.get(epLeft);
                if (adj instanceof Pawn && adj.isWhite() != p.isWhite() && epLeft.equals(enPassantTarget)) {
                    Position epCapture = new Position(row + dir, col - 1);
                    if (epCapture.isValid()) {
                        pseudoMoves.add(epCapture);
                    }
                }
            }
            Position epRight = new Position(row, col + 1);
            if (epRight.isValid()) {
                Piece adj = board.get(epRight);
                if (adj instanceof Pawn && adj.isWhite() != p.isWhite() && epRight.equals(enPassantTarget)) {
                    Position epCapture = new Position(row + dir, col + 1);
                    if (epCapture.isValid()) {
                        pseudoMoves.add(epCapture);
                    }
                }
            }
        }

        if (allowIllegalMoves) {
            return pseudoMoves;
        }

        List<Position> legalMoves = new ArrayList<>();
        for (Position to : pseudoMoves) {
            Game testGame = snapshot();
            testGame.applyMoveInternal(from, to, null, false);
            if (!testGame.inCheck(p.isWhite())) {
                legalMoves.add(to);
            }
        }

        if (p instanceof King && !p.hasMoved()) {
            Position kingSideRookPos = new Position(from.getRow(), 7);
            Piece kingSideRook = board.get(kingSideRookPos);
            if (kingSideRook instanceof Rook && !kingSideRook.hasMoved() &&
                board.get(new Position(from.getRow(), 5)) == null &&
                board.get(new Position(from.getRow(), 6)) == null &&
                !inCheck(p.isWhite()) &&
                !isSquareAttacked(new Position(from.getRow(), 5), p.isWhite()) &&
                !isSquareAttacked(new Position(from.getRow(), 6), p.isWhite())) {
                legalMoves.add(new Position(from.getRow(), 6));
            }

            Position queenSideRookPos = new Position(from.getRow(), 0);
            Piece queenSideRook = board.get(queenSideRookPos);
            if (queenSideRook instanceof Rook && !queenSideRook.hasMoved() &&
                board.get(new Position(from.getRow(), 1)) == null &&
                board.get(new Position(from.getRow(), 2)) == null &&
                board.get(new Position(from.getRow(), 3)) == null &&
                !inCheck(p.isWhite()) &&
                !isSquareAttacked(new Position(from.getRow(), 2), p.isWhite()) &&
                !isSquareAttacked(new Position(from.getRow(), 3), p.isWhite())) {
                legalMoves.add(new Position(from.getRow(), 2));
            }
        }

        return legalMoves;
    }

    public boolean isPromotion(Position from, Position to) {
        Piece p = board.get(from);
        if (!(p instanceof Pawn)) return false;
        if (p.isWhite()) return to.getRow() == 0;
        else return to.getRow() == 7;
    }

    public void move(Position from, Position to, Character promotion) {
        move(from, to, promotion, false);
    }

    public void move(Position from, Position to, Character promotion, boolean forceMove) {
        if (gameOver) return;

        Piece p = board.get(from);
        if (p == null) return;
        if (p.isWhite() != whiteToMove) return;

        if (!forceMove && !allowIllegalMoves) {
            List<Position> legalMoves = legalMovesFrom(from);
            if (!legalMoves.contains(to)) {
                System.out.println("Movimento ilegal: não está na lista de movimentos legais.");
                return;
            }
        }

        applyMoveInternal(from, to, promotion, true);

        checkGameEndConditions();

        if (!gameOver && !whiteToMove) {
            makeAIMove();
            checkGameEndConditions();
        }
    }

    /**
     * Aplica o movimento no tabuleiro.
     * Se realMove==false então é um teste (snapshot) e não deve alterar efeitos "globais" como declarar vencedor,
     * porém o tabuleiro e whiteToMove são atualizados para simulação correta.
     */
    public void applyMoveInternal(Position from, Position to, Character promotion, boolean realMove) {
        Piece p = board.get(from);
        if (p == null) return;

        boolean isKing = (p instanceof King);
        int dCol = Math.abs(to.getColumn() - from.getColumn());
        if (isKing && dCol == 2) {
            int row = from.getRow();

            board.set(to, p);
            board.set(from, null);

            if (to.getColumn() == 6) {
                Piece rook = board.get(new Position(row, 7));
                board.set(new Position(row, 5), rook);
                board.set(new Position(row, 7), null);
                if (rook != null) rook.setMoved(true);
                if (realMove) addHistory("O-O");
            } else {
                Piece rook = board.get(new Position(row, 0));
                board.set(new Position(row, 3), rook);
                board.set(new Position(row, 0), null);
                if (rook != null) rook.setMoved(true);
                if (realMove) addHistory("O-O-O");
            }

            p.setMoved(true);
            enPassantTarget = null;
            whiteToMove = !whiteToMove;
            return;
        }

        boolean isPawn = (p instanceof Pawn);
        boolean diagonal = from.getColumn() != to.getColumn();
        boolean toIsEmpty = board.get(to) == null;
        boolean isEnPassant = isPawn && diagonal && toIsEmpty && to.equals(enPassantTarget);
        if (isEnPassant) {
            board.set(to, p);
            board.set(from, null);
            int dir = p.isWhite() ? 1 : -1;
            board.set(new Position(to.getRow() + dir, to.getColumn()), null);
            p.setMoved(true);
            if (realMove) addHistory(coord(from) + "x" + coord(to) + " e.p.");
            enPassantTarget = null;
            whiteToMove = !whiteToMove;
            return;
        }

        Piece capturedBefore = board.get(to);
        board.set(to, p);
        board.set(from, null);
        p.setMoved(true);

        if (isPawn && Math.abs(to.getRow() - from.getRow()) == 2) {
            int mid = (to.getRow() + from.getRow()) / 2;
            enPassantTarget = new Position(mid, from.getColumn());
        } else {
            enPassantTarget = null;
        }

        if (promotion != null && isPawn && isPromotion(from, to)) {
            Piece np = switch (Character.toUpperCase(promotion)) {
                case 'R' -> new Rook(board, p.isWhite());
                case 'N' -> new Knight(board, p.isWhite());
                case 'B' -> new Bishop(board, p.isWhite());
                case 'Q' -> new Queen(board, p.isWhite());
                default -> new Queen(board, p.isWhite());
            };
            np.setMoved(true);
            board.set(to, np);
            p = np;
        }

        if (realMove) {
            String moveNotation = coord(from) + (capturedBefore != null ? "x" : "-") + coord(to);
            if (promotion != null && isPawn && isPromotion(from, to)) {
                moveNotation += "=" + Character.toUpperCase(promotion);
            }
            addHistory(moveNotation);
        }

        if (realMove && capturedBefore instanceof King) {
            this.gameOver = true;
            this.winner = p.isWhite() ? "Brancas" : "Pretas";
            System.out.println("Rei capturado! Vencedor: " + this.winner);
        }

        whiteToMove = !whiteToMove;
    }

    public boolean inCheck(boolean whiteSide) {
        Position kingPos = findKingPosition(whiteSide);
        if (kingPos == null) return false;

        return isSquareAttacked(kingPos, whiteSide);
    }

    public boolean isSquareAttacked(Position targetPos, boolean byWhite) {
        boolean opponentIsWhite = !byWhite;
        for (Piece opponentPiece : board.pieces(opponentIsWhite)) {
            if (opponentPiece instanceof Pawn) {
                for (Position attackPos : ((Pawn) opponentPiece).getAttacks()) {
                    if (attackPos.equals(targetPos)) {
                        return true;
                    }
                }
            } else {
                for (Position possibleMove : opponentPiece.getPossibleMoves()) {
                    if (possibleMove.equals(targetPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Position findKingPosition(boolean whiteSide) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position p = new Position(r, c);
                Piece piece = board.get(p);
                if (piece instanceof King && piece.isWhite() == whiteSide) {
                    return p;
                }
            }
        }
        return null;
    }

    private void checkGameEndConditions() {
        if (gameOver) return;

        boolean currentSideHasMoves = false;
        List<Piece> currentPieces = board.pieces(whiteToMove);

        for (Piece p : currentPieces) {
            Position from = p.getPosition();
            List<Position> legalMoves = legalMovesFrom(from);
            if (!legalMoves.isEmpty()) {
                currentSideHasMoves = true;
                break;
            }
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

    public Game snapshot() {
        Game g = new Game();
        g.board = this.board.copy();
        g.whiteToMove = this.whiteToMove;
        g.gameOver = this.gameOver;
        g.enPassantTarget = (this.enPassantTarget == null)
                ? null
                : new Position(enPassantTarget.getRow(), enPassantTarget.getColumn());
        g.history.clear();
        g.history.addAll(this.history);
        g.allowIllegalMoves = this.allowIllegalMoves;
        g.aiPlayer.setGame(g);
        return g;
    }

    public void makeAIMove() {
        System.out.println("IA (Pretas) está pensando com dificuldade " + aiDifficulty + "...");
        Move bestMove = aiPlayer.findBestMove(aiDifficulty);

        if (bestMove == null) {
            System.out.println("IA não encontrou movimentos legais.");
            return;
        }

        Character promo = null;
        if (bestMove.getMoved() instanceof Pawn && isPromotion(bestMove.getFrom(), bestMove.getTo())) {
            promo = 'Q';
        }
        applyMoveInternal(bestMove.getFrom(), bestMove.getTo(), promo, true);
        System.out.println("IA moveu: " + coord(bestMove.getFrom()) + " para " + coord(bestMove.getTo()) + (promo != null ? "=" + promo : ""));
    }

    private void addHistory(String moveStr) {
        history.add(moveStr);
    }

    private String coord(Position p) {
        char file = (char) ('a' + p.getColumn());
        int rank = 8 - p.getRow();
        return "" + file + rank;
    }

    private void setupPieces() {
        board.placePiece(new Rook(board, true), new Position(7, 0));
        board.placePiece(new Knight(board, true), new Position(7, 1));
        board.placePiece(new Bishop(board, true), new Position(7, 2));
        board.placePiece(new Queen(board, true), new Position(7, 3));
        board.placePiece(new King(board, true), new Position(7, 4));
        board.placePiece(new Bishop(board, true), new Position(7, 5));
        board.placePiece(new Knight(board, true), new Position(7, 6));
        board.placePiece(new Rook(board, true), new Position(7, 7));
        for (int c = 0; c < 8; c++) {
            board.placePiece(new Pawn(board, true), new Position(6, c));
        }

        board.placePiece(new Rook(board, false), new Position(0, 0));
        board.placePiece(new Knight(board, false), new Position(0, 1));
        board.placePiece(new Bishop(board, false), new Position(0, 2));
        board.placePiece(new Queen(board, false), new Position(0, 3));
        board.placePiece(new King(board, false), new Position(0, 4));
        board.placePiece(new Bishop(board, false), new Position(0, 5));
        board.placePiece(new Knight(board, false), new Position(0, 6));
        board.placePiece(new Rook(board, false), new Position(0, 7));
        for (int c = 0; c < 8; c++) {
            board.placePiece(new Pawn(board, false), new Position(1, c));
        }
    }
}