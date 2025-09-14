package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import model.board.*;
import model.pieces.*;

public class AIPlayer {

    private Game game;
    private final Random random = new Random();

    private static final double PAWN_VALUE = 100;
    private static final double KNIGHT_VALUE = 320;
    private static final double BISHOP_VALUE = 330;
    private static final double ROOK_VALUE = 500;
    private static final double QUEEN_VALUE = 900;
    private static final double KING_VALUE = 100000;

    public AIPlayer(Game game) {
        this.game = game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    /** Encontra o melhor movimento usando Minimax com Alpha-Beta */
    public Move findBestMove(int depth) {
        double bestValue = Double.NEGATIVE_INFINITY;
        Move bestMove = null;
        List<Move> possibleMoves = generateAllLegalMoves(false); // IA = Pretas
        Collections.shuffle(possibleMoves, random);

        for (Move move : possibleMoves) {
            Game childGame = game.snapshot();
            Character promo = null;
            if (move.getMoved() instanceof Pawn && childGame.isPromotion(move.getFrom(), move.getTo())) {
                promo = 'Q';
            }
            childGame.applyMoveInternal(move.getFrom(), move.getTo(), promo, false);
            double moveValue = minimax(childGame, depth - 1, true, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

            if (moveValue > bestValue) {
                bestValue = moveValue;
                bestMove = move;
            }
        }
        return bestMove;
    }

    /** Minimax com poda Alpha-Beta */
    private double minimax(Game currentGame, int depth, boolean maximizingPlayer, double alpha, double beta) {
        if (depth == 0 || currentGame.isGameOver()) {
            return evaluateBoard(currentGame);
        }

        List<Move> possibleMoves = generateAllLegalMoves(maximizingPlayer);
        if (possibleMoves.isEmpty()) return evaluateBoard(currentGame);

        if (maximizingPlayer) {
            double maxEval = Double.NEGATIVE_INFINITY;
            for (Move move : possibleMoves) {
                Game child = currentGame.snapshot();
                Character promo = null;
                if (move.getMoved() instanceof Pawn && child.isPromotion(move.getFrom(), move.getTo())) {
                    promo = 'Q';
                }
                child.applyMoveInternal(move.getFrom(), move.getTo(), promo, false);
                double eval = minimax(child, depth - 1, false, alpha, beta);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else { // vez da IA (pretas = minimizing)
            double minEval = Double.POSITIVE_INFINITY;
            for (Move move : possibleMoves) {
                Game child = currentGame.snapshot();
                Character promo = null;
                if (move.getMoved() instanceof Pawn && child.isPromotion(move.getFrom(), move.getTo())) {
                    promo = 'Q';
                }
                child.applyMoveInternal(move.getFrom(), move.getTo(), promo, false);
                double eval = minimax(child, depth - 1, true, alpha, beta);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    /** Avalia tabuleiro do ponto de vista das Brancas (+ = vantagem Brancas, - = vantagem Pretas) */
    private double evaluateBoard(Game currentGame) {
        if (currentGame.isGameOver()) {
            if (currentGame.getWinner() != null) {
                if (currentGame.getWinner().equals("Brancas")) return KING_VALUE * 100;
                if (currentGame.getWinner().equals("Pretas")) return -KING_VALUE * 100;
                return 0;
            }
        }

        double score = 0;
        Board board = currentGame.board();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position pos = new Position(r, c);
                Piece piece = board.get(pos);
                if (piece == null) continue;

                double pieceValue = getPieceValue(piece);
                score += piece.isWhite() ? pieceValue : -pieceValue;

                // Controle do centro (quadrados 2-5)
                if (r >= 2 && r <= 5 && c >= 2 && c <= 5) {
                    score += piece.isWhite() ? 0.2 * pieceValue / 100 : -0.2 * pieceValue / 100;
                }

                // Peões avançados
                if (piece instanceof Pawn) {
                    if (piece.isWhite()) score += (7 - r) * 2;
                    else score -= r * 2;
                }

                // Incentivo para peças pretas atacarem
                if (!piece.isWhite()) {
                    List<Position> moves = currentGame.legalMovesFrom(piece.getPosition());
                    for (Position target : moves) {
                        Piece targetPiece = board.get(target);
                        if (targetPiece != null && targetPiece.isWhite()) {
                            score -= getPieceValue(targetPiece) * 0.05; // valoriza ameaçar peças brancas
                            if (targetPiece instanceof King) {
                                score -= 500; // recompensa forte por atacar o rei branco
                            }
                        }
                    }
                }
            }
        }

        // Pontuação para xeque
        if (currentGame.inCheck(true)) score -= 500;   // se brancas estão em xeque, bom p/ pretas
        if (currentGame.inCheck(false)) score += 300;  // se pretas estão em xeque, ruim p/ IA

        return score;
    }

    private double getPieceValue(Piece piece) {
        if (piece instanceof Pawn) return PAWN_VALUE;
        if (piece instanceof Knight) return KNIGHT_VALUE;
        if (piece instanceof Bishop) return BISHOP_VALUE;
        if (piece instanceof Rook) return ROOK_VALUE;
        if (piece instanceof Queen) return QUEEN_VALUE;
        if (piece instanceof King) return KING_VALUE;
        return 0;
    }

    /** Gera movimentos legais (IA ou Brancas) */
    public List<Move> generateAllLegalMoves(boolean forWhite) {
        List<Move> legalMoves = new ArrayList<>();
        List<Piece> pieces = game.board().pieces(forWhite);

        for (Piece p : pieces) {
            Position from = p.getPosition();
            List<Position> destinations = game.legalMovesFrom(from);
            for (Position to : destinations) {
                Character promo = null;
                if (p instanceof Pawn && game.isPromotion(from, to)) promo = 'Q';
                legalMoves.add(new Move(from, to, p, game.board().get(to), false, false, false, promo));
            }
        }

        // Priorizar capturas (peças de maior valor primeiro)
        legalMoves.sort((m1, m2) -> {
            double v1 = m1.getCaptured() != null ? getPieceValue(m1.getCaptured()) : 0;
            double v2 = m2.getCaptured() != null ? getPieceValue(m2.getCaptured()) : 0;
            return Double.compare(v2, v1);
        });

        return legalMoves;
    }
}
