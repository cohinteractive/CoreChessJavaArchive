package com.ohinteractive.minchessv2lib.impl;

import com.ohinteractive.minchessv2lib.util.BitOps;
import com.ohinteractive.minchessv2lib.util.Bitboard;
import com.ohinteractive.minchessv2lib.util.Magic;
import com.ohinteractive.minchessv2lib.util.Piece;
import com.ohinteractive.minchessv2lib.util.Value;

public class Gen {
    
    public static final int MAX_MOVELIST_SIZE = 100;
    public static final int MOVELIST_SIZE = MAX_MOVELIST_SIZE - 1;

    public static long[] gen(final long[] board, final boolean legal, final boolean tactical) {
        final int player = (int) board[Board.STATUS] & Board.PLAYER_BIT;
        final int playerBit = player << Board.PLAYER_SHIFT;
        final int otherBit = 8 ^ playerBit;
        final long allOccupancy = board[playerBit] | board[otherBit];
        final long otherOccupancy = board[otherBit];
        long[] moves = new long[MAX_MOVELIST_SIZE];
        int moveListLength = 0;
        moveListLength = getKingMoves(board, moves, Piece.KING | playerBit, moveListLength, player, allOccupancy, otherOccupancy, tactical);
        moveListLength = getQueenMoves(board, moves, Piece.QUEEN | playerBit, player, moveListLength, allOccupancy, otherOccupancy, tactical);
        moveListLength = getPawnMoves(board, moves, Piece.PAWN | playerBit, moveListLength, player, allOccupancy, otherOccupancy, tactical);
        moveListLength = getRookMoves(board, moves, Piece.ROOK | playerBit, player, moveListLength, allOccupancy, otherOccupancy, tactical);
        moveListLength = getBishopMoves(board, moves, Piece.BISHOP | playerBit, player, moveListLength, allOccupancy, otherOccupancy, tactical);
        moveListLength = getKnightMoves(board, moves, Piece.KNIGHT | playerBit, moveListLength, allOccupancy, otherOccupancy, tactical);
        moves[MOVELIST_SIZE] = moveListLength;
        return legal ? purgeIllegalMoves(board, moves, player) : moves;
    }

    private Gen() {}

    private static long[] purgeIllegalMoves(final long[] board, long[] moves, final int player) {
        long[] legalMoves = new long[MAX_MOVELIST_SIZE];
        int legalMoveCount = 0;
        for(int i = 0; i < moves[99]; i ++) {
            long move = moves[i];
            long[] boardAfterMove = Board.makeMove(board, move);
            if(!Board.isPlayerInCheck(boardAfterMove, player)) legalMoves[legalMoveCount ++] = move;
        }
        legalMoves[MOVELIST_SIZE] = legalMoveCount;
        return legalMoves;
    }

    private static int getKingMoves(final long[] board, long[] moves, final int piece, int moveListLength, final int player, final long allOccupancy, final long otherOccupancy, final boolean tactical) {
        final long bitboard = board[piece];
        final int square = BitOps.lsb(bitboard);
        final long kingAttacks = Bitboard.BB[Bitboard.KING_ATTACKS][square];
        long moveBitboard = kingAttacks & otherOccupancy;
        while(moveBitboard != 0L) {
            int targetSquare = BitOps.lsb(moveBitboard);
            moveBitboard &= moveBitboard - 1;
            moves[moveListLength ++] = square | (targetSquare << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT) | (Board.getSquare(board, targetSquare) << Board.TARGET_PIECE_SHIFT);
        }
        if(tactical) return moveListLength;
        moveBitboard = kingAttacks & ~allOccupancy;
        while(moveBitboard!= 0L) {
            moves[moveListLength ++] = square | (BitOps.lsb(moveBitboard) << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT);
            moveBitboard &= moveBitboard - 1;
        }
        final int castling = (int) (board[Board.STATUS] >>> Board.CASTLING_SHIFT) & Board.CASTLING_BITS;
        final boolean kingSide = (castling & (player == Value.WHITE ? 0b1 : 0b100)) != Value.NONE;
        final boolean queenSide = (castling & (player == Value.WHITE ? 0b10 : 0b1000)) != Value.NONE;
        if(kingSide || queenSide) {
            final int other = 1 ^ player;
            if(!Board.isSquareAttackedByPlayer(board, square, other)) {
                if(kingSide) {
                    if((allOccupancy & (player == Value.WHITE ? 0x0000000000000060L : 0x6000000000000000L)) == 0L && !Board.isSquareAttackedByPlayer(board, square + 1, other))
                        moves[moveListLength ++] = square | ((square + 2) << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT);
                }
                if(queenSide) {
                    if((allOccupancy & (player == Value.WHITE ? 0x000000000000000eL : 0x0e00000000000000L)) == 0L && !Board.isSquareAttackedByPlayer(board, square - 1, other))
                        moves[moveListLength ++] = square | ((square - 2) << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT);
                }
            }
        }
        return moveListLength;
    }

    private static int getKnightMoves(final long[] board, long[] moves, final int piece, int moveListLength, final long allOccupancy, final long otherOccupancy, final boolean tactical) {
        long knightBitboard = board[piece];
        while(knightBitboard != 0L) {
            int square = BitOps.lsb(knightBitboard);
            knightBitboard &= knightBitboard - 1;
            long knightAttacks = Bitboard.BB[Bitboard.LEAP_ATTACKS][square];
            long moveBitboard = knightAttacks & otherOccupancy;
            while(moveBitboard != 0L) {
                int targetSquare = BitOps.lsb(moveBitboard);
                moveBitboard &= moveBitboard - 1;
                moves[moveListLength ++] = square | (targetSquare << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT) | (Board.getSquare(board, targetSquare) << Board.TARGET_PIECE_SHIFT);
            }
            if(tactical) continue;
            moveBitboard = knightAttacks & ~allOccupancy;
            while(moveBitboard != 0L) {
                moves[moveListLength ++] = square | (BitOps.lsb(moveBitboard) << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT);
                moveBitboard &= moveBitboard - 1;
            }
        }
        return moveListLength;
    }

    private static int getPawnMoves(final long[] board, long[] moves, final int piece, int moveListLength, final int player, final long allOccupancy, long otherOccupancy, final boolean tactical) {
        long pawnBitboard = board[piece];
        final int playerBit = player << Board.PLAYER_SHIFT;
        final int eSquare = (int) board[Board.STATUS] >>> Board.ESQUARE_SHIFT & Board.SQUARE_BITS;
        otherOccupancy |= (eSquare > 0 ? (1L << eSquare) : 0L);
        final int pawnAttacks = Bitboard.PAWN_ATTACKS_PLAYER0 + player;
        final int pawnAdvanceSingle = Bitboard.PAWN_ADVANCE_1_PLAYER0 + player;
        final int pawnAdvanceDouble = Bitboard.PAWN_ADVANCE_2_PLAYER0 + player;
        while(pawnBitboard != 0L) {
            int square = BitOps.lsb(pawnBitboard);
            pawnBitboard &= pawnBitboard - 1;
            long moveBitboard = Bitboard.BB[pawnAttacks][square] & otherOccupancy;
            while(moveBitboard != 0L) {
                int targetSquare = BitOps.lsb(moveBitboard);
                moveBitboard &= moveBitboard - 1;
                int targetRank = targetSquare >>> 3;
                if(targetRank == (player == Value.WHITE ? 7 : 0)) {
                    int moveInfo = square | (targetSquare << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT) | (Board.getSquare(board, targetSquare) << Board.TARGET_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfo | ((Piece.QUEEN | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfo | ((Piece.ROOK | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfo | ((Piece.BISHOP | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfo | ((Piece.KNIGHT | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                } else {
                    moves[moveListLength ++] = square | (targetSquare << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT) | (Board.getSquare(board, targetSquare) << Board.TARGET_PIECE_SHIFT);
                }
            }
            if(tactical) continue;
            moveBitboard = Bitboard.BB[pawnAdvanceSingle][square] & ~allOccupancy;
            if(moveBitboard != 0L) {
                moveBitboard = (moveBitboard | Bitboard.BB[pawnAdvanceDouble][square]) & ~allOccupancy;
            }
            while(moveBitboard != 0L) {
                int targetSquare = BitOps.lsb(moveBitboard);
                moveBitboard &= moveBitboard - 1;
                int targetRank = targetSquare >>> 3;
                if(targetRank == (player == Value.WHITE ? 7 : 0)) {
                    int moveInfo = square | (targetSquare << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfo | ((Piece.QUEEN | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfo | ((Piece.BISHOP | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfo | ((Piece.KNIGHT | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfo | ((Piece.ROOK | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                } else {
                    moves[moveListLength ++] = square | (targetSquare << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT);
                }
            }
        }
        return moveListLength;
    }

    private static int getQueenMoves(final long[] board, long[] moves, final int piece, final int player, int moveListLength, final long allOccupancy, final long otherOccupancy, final boolean tactical) {
        long queenBitboard = board[piece];
        while(queenBitboard != 0L) {
            int square = BitOps.lsb(queenBitboard);
            queenBitboard &= queenBitboard - 1;
            long magic = Magic.queenMoves(square, allOccupancy);
            long moveBitboard = magic & otherOccupancy;
            while(moveBitboard != 0L) {
                int targetSquare = BitOps.lsb(moveBitboard);
                moveBitboard &= moveBitboard - 1;
                moves[moveListLength ++] = square | (targetSquare << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT) | (Board.getSquare(board, targetSquare) << Board.TARGET_PIECE_SHIFT);
            }
            if(tactical) continue;
            moveBitboard = magic & ~allOccupancy;
            while(moveBitboard != 0L) {
                moves[moveListLength ++] = square | (BitOps.lsb(moveBitboard) << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT);
                moveBitboard &= moveBitboard - 1;
            }
        }
        return moveListLength;
    }

    private static int getRookMoves(final long[] board, long[] moves, final int piece, final int player, int moveListLength, final long allOccupancy, final long otherOccupancy, final boolean tactical) {
        long rookBitboard = board[piece];
        while(rookBitboard != 0L) {
            int square = BitOps.lsb(rookBitboard);
            rookBitboard &= rookBitboard - 1;
            long magic = Magic.rookMoves(square, allOccupancy);
            long moveBitboard = magic & otherOccupancy;
            while(moveBitboard != 0L) {
                int targetSquare = BitOps.lsb(moveBitboard);
                moveBitboard &= moveBitboard - 1;
                moves[moveListLength ++] = square | (targetSquare << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT) | (Board.getSquare(board, targetSquare) << Board.TARGET_PIECE_SHIFT);
            }
            if(tactical) continue;
            moveBitboard = magic & ~allOccupancy;
            while(moveBitboard != 0L) {
                moves[moveListLength ++] = square | (BitOps.lsb(moveBitboard) << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT);
                moveBitboard &= moveBitboard - 1;
            }
        }
        return moveListLength;
    }

    private static int getBishopMoves(final long[] board, long[] moves, final int piece, final int player, int moveListLength, final long allOccupancy, final long otherOccupancy, final boolean tactical) {
        long bishopBitboard = board[piece];
        while(bishopBitboard != 0L) {
            int square = BitOps.lsb(bishopBitboard);
            bishopBitboard &= bishopBitboard - 1;
            long magic = Magic.bishopMoves(square, allOccupancy);
            long moveBitboard = magic & otherOccupancy;
            while(moveBitboard != 0L) {
                int targetSquare = BitOps.lsb(moveBitboard);
                moveBitboard &= moveBitboard - 1;
                moves[moveListLength ++] = square | (targetSquare << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT) | (Board.getSquare(board, targetSquare) << Board.TARGET_PIECE_SHIFT);
            }
            if(tactical) continue;
            moveBitboard = magic & ~allOccupancy;
            while(moveBitboard != 0L) {
                moves[moveListLength ++] = square | (BitOps.lsb(moveBitboard) << Board.TARGET_SQUARE_SHIFT) | (piece << Board.START_PIECE_SHIFT);
                moveBitboard &= moveBitboard - 1;
            }
        }
        return moveListLength;
    }

}
