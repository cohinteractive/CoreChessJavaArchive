package com.ohinteractive.minchessv2lib.impl;

import com.ohinteractive.minchessv2lib.util.Bitboard;
import com.ohinteractive.minchessv2lib.util.Magic;
import com.ohinteractive.minchessv2lib.util.Piece;
import com.ohinteractive.minchessv2lib.util.Value;

public class Gen_MinChessV2Lib {
    
    public static final int MAX_MOVELIST_SIZE = 100;
    public static final int MOVELIST_SIZE = MAX_MOVELIST_SIZE - 1;

    public static long[] gen(long[] board, boolean legal, boolean tactical, long[] movesBuffer) {
        final int status = (int) board[Board.STATUS];
        final int player = status & Board.PLAYER_BIT;
        final int playerBit = player << Board.PLAYER_SHIFT;
        final long board0 = board[0];
        final long board1 = board[1];
        final long board2 = board[2];
        final long board3 = board[3];
        final long allOccupancy = board0 | board1 | board2;
        final long colorMask = ~(-(player) ^ board3);
        final long otherOccupancy = allOccupancy & ~colorMask;
        int moveListLength = 0;
        moveListLength = getKingMoves(board0, board1, board2, board3, colorMask, status, movesBuffer, Piece.KING | playerBit, moveListLength, player, allOccupancy, otherOccupancy, tactical);
        moveListLength = getQueenMoves(board0, board1, board2, board3, colorMask, movesBuffer, Piece.QUEEN | playerBit, player, moveListLength, allOccupancy, otherOccupancy, tactical);
        moveListLength = getPawnMoves(board0, board1, board2, board3, colorMask, status, movesBuffer, Piece.PAWN | playerBit, moveListLength, player, allOccupancy, otherOccupancy, tactical);
        moveListLength = getRookMoves(board0, board1, board2, board3, colorMask, movesBuffer, Piece.ROOK | playerBit, player, moveListLength, allOccupancy, otherOccupancy, tactical);
        moveListLength = getBishopMoves(board0, board1, board2, board3, colorMask, movesBuffer, Piece.BISHOP | playerBit, player, moveListLength, allOccupancy, otherOccupancy, tactical);
        moveListLength = getKnightMoves(board0, board1, board2, board3, colorMask, player, movesBuffer, Piece.KNIGHT | playerBit, moveListLength, allOccupancy, otherOccupancy, tactical);
        if(legal) {
            moveListLength = purgeIllegalMoves(board, movesBuffer, player, moveListLength);
        }
        long[] moves = new long[moveListLength];
        System.arraycopy(movesBuffer, 0, moves, 0, moveListLength);
        return moves;
    }

    private Gen_MinChessV2Lib() {}

    private static int purgeIllegalMoves(long[] board, long[] moves, int player, int moveListLength) {
        int legalMoveCount = 0;
        for(int i = 0; i < moveListLength; i ++) {
            final long move = moves[i];
            final long[] boardAfterMove = Board.makeMove(board, move);
            if(!Board.isPlayerInCheck(boardAfterMove, player)) moves[legalMoveCount ++] = move;
        }
        return legalMoveCount;
    }

    private static final int WHITE_KINGSIDE_CASTLING_BIT_UNSHIFTED = 0b10;
    private static final int WHITE_QUEENSIDE_CASTLING_BIT_UNSHIFTED = 0b100;
    private static final int BLACK_KINGSIDE_CASTLING_BIT_UNSHIFTED = 0b1000;
    private static final int BLACK_QUEENSIDE_CASTLING_BIT_UNSHIFTED = 0b10000;
    private static final long WHITE_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES = 0x0000000000000060L;
    private static final long WHITE_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES = 0x000000000000000eL;
    private static final long BLACK_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES = 0x6000000000000000L;
    private static final long BLACK_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES = 0x0e00000000000000L;

    private static int getKingMoves(long board0, long board1, long board2, long board3, long colorMask, int status, long[] moves, int piece, int moveListLength, int player, long allOccupancy, long otherOccupancy, boolean tactical) {
        final long pieceBitboard = board0 & ~board1 & ~board2 & colorMask;
        final int[] lsb = LSB;
        final int square = lsb[(int) (((pieceBitboard & -pieceBitboard) * DB) >>> 58)];
        final long kingAttacks = KING_ATTACKS[square];
        long moveBitboard = kingAttacks & otherOccupancy;
        final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
        while(moveBitboard != 0L) {
            final long b = moveBitboard & -moveBitboard;
            moveBitboard ^= b;
            final int targetSquare = lsb[(int) ((b * DB) >>> 58)];
            moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
        }
        if(tactical) return moveListLength;
        moveBitboard = kingAttacks & ~allOccupancy;
        while(moveBitboard!= 0L) {
            final long b = moveBitboard & -moveBitboard;
            moveBitboard ^= b;
            moves[moveListLength ++] = moveInfo | (lsb[(int) ((b * DB) >>> 58)] << Board.TARGET_SQUARE_SHIFT);
        }
        final int blackCastlingBitsMask = -player;
        final int whiteCastlingBitsMask = ~blackCastlingBitsMask; 
        final boolean kingSide =  (status & ((WHITE_KINGSIDE_CASTLING_BIT_UNSHIFTED  & whiteCastlingBitsMask) | (BLACK_KINGSIDE_CASTLING_BIT_UNSHIFTED  & blackCastlingBitsMask))) != Value.NONE;
        final boolean queenSide = (status & ((WHITE_QUEENSIDE_CASTLING_BIT_UNSHIFTED & whiteCastlingBitsMask) | (BLACK_QUEENSIDE_CASTLING_BIT_UNSHIFTED & blackCastlingBitsMask))) != Value.NONE;
        if(kingSide || queenSide) {
            final int other = 1 ^ player;
            if(!Board.isSquareAttackedByPlayer(board0, board1, board2, board3, square, other)) {
                if(kingSide) {
                    if((allOccupancy & ((WHITE_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES  & whiteCastlingBitsMask) | (BLACK_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES  & blackCastlingBitsMask))) == 0L && !Board.isSquareAttackedByPlayer(board0, board1, board2, board3, square + 1, other))
                        moves[moveListLength ++] = moveInfo | ((square + 2) << Board.TARGET_SQUARE_SHIFT);
                }
                if(queenSide) {
                    if((allOccupancy & ((WHITE_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES & whiteCastlingBitsMask) | (BLACK_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES & blackCastlingBitsMask))) == 0L && !Board.isSquareAttackedByPlayer(board0, board1, board2, board3, square - 1, other))
                        moves[moveListLength ++] = moveInfo | ((square - 2) << Board.TARGET_SQUARE_SHIFT);
                }
            }
        }
        return moveListLength;
    }

    private static int getKnightMoves(long board0, long board1, long board2, long board3, long colorMask, int player, long[] moves, int piece, int moveListLength, long allOccupancy, long otherOccupancy, boolean tactical) {
        long pieceBitboard = board0 & ~board1 & board2 & colorMask;
        final int[] lsb = LSB;
        final long[] leapAttacks = LEAP_ATTACKS;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long knightAttacks = leapAttacks[square];
            long moveBitboard = knightAttacks & otherOccupancy;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
            }
            if(tactical) continue;
            moveBitboard = knightAttacks & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] = moveInfo | (lsb[(int) ((b2 * DB) >>> 58)] << Board.TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getPawnMoves(long board0, long board1, long board2, long board3, long colorMask, int status, long[] moves, int piece, int moveListLength, int player, long allOccupancy, long otherOccupancy, boolean tactical) {
        long pieceBitboard = ~board0 & board1 & board2 & colorMask;
        final int[] lsb = LSB;
        final int playerBit = player << Board.PLAYER_SHIFT;
        final int eSquare = status >>> Board.ESQUARE_SHIFT & Board.SQUARE_BITS;
        otherOccupancy |= (eSquare > 0 ? (1L << eSquare) : 0L);
        final long[] pawnAttacks = PAWN_ATTACKS[player];
        final long[] pawnAdvanceSingle = PAWN_ADVANCE_SINGLE[player];
        final long[] pawnAdvanceDouble = PAWN_ADVANCE_DOUBLE[player];
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            long moveBitboard = pawnAttacks[square] & otherOccupancy;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT); 
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetRank = targetSquare >>> 3;
                final int promoteInfo = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
                if(targetRank == (7 & ~(-player))) {
                    moves[moveListLength++] = promoteInfo | ((Piece.QUEEN | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = promoteInfo | ((Piece.ROOK | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = promoteInfo | ((Piece.BISHOP | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = promoteInfo | ((Piece.KNIGHT | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                } else {
                    moves[moveListLength ++] = promoteInfo;
                }
            }
            if(tactical) continue;
            moveBitboard = pawnAdvanceSingle[square] & ~allOccupancy;
            if(moveBitboard != 0L) {
                moveBitboard = (moveBitboard | pawnAdvanceDouble[square]) & ~allOccupancy;
            }
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetRank = targetSquare >>> 3;
                final int promoteInfo = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT);
                if(targetRank == (7 & ~(-player))) {
                    moves[moveListLength++] = promoteInfo | ((Piece.QUEEN | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = promoteInfo | ((Piece.BISHOP | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = promoteInfo | ((Piece.KNIGHT | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = promoteInfo | ((Piece.ROOK | playerBit) << Board.PROMOTE_PIECE_SHIFT);
                } else {
                    moves[moveListLength ++] = promoteInfo;
                }
            }
        }
        return moveListLength;
    }

    private static int getQueenMoves(long board0, long board1, long board2,long board3, long colorMask, long[] moves, int piece, int player, int moveListLength, long allOccupancy, long otherOccupancy, boolean tactical) {
        long pieceBitboard = ~board0 & board1 & ~board2 & colorMask;
        final int[] lsb = LSB;
        final long[][] rookMoves = Magic.ROOK_MOVES;
        final long[][] bishopMoves = Magic.BISHOP_MOVES;
        final long[] rookMovement = Magic.ROOK_MOVEMENT;
        final long[] bishopMovement = Magic.BISHOP_MOVEMENT;
        final long[] rookMagics = Magic.ROOK_MAGIC_NUMBER;
        final long[] bishopMagics = Magic.BISHOP_MAGIC_NUMBER;
        final int[] rookShifts = Magic.ROOK_SHIFT;
        final int[] bishopShifts = Magic.BISHOP_SHIFT;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long magic = 
                rookMoves[square][(int)   ((allOccupancy & rookMovement[square])   * rookMagics[square]   >>> rookShifts[square])] |
                bishopMoves[square][(int) ((allOccupancy & bishopMovement[square]) * bishopMagics[square] >>> bishopShifts[square])];
            long moveBitboard = magic & otherOccupancy;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
            }
            if(tactical) continue;
            moveBitboard = magic & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] = moveInfo | (lsb[(int) ((b2 * DB) >>> 58)] << Board.TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getRookMoves(long board0, long board1, long board2, long board3, long colorMask, long[] moves, int piece, int player, int moveListLength, long allOccupancy, long otherOccupancy, boolean tactical) {
        long pieceBitboard = board0 & board1 & ~board2 & colorMask;
        final int[] lsb = LSB;
        final long[][] rookMoves = Magic.ROOK_MOVES;
        final long[] rookMovement = Magic.ROOK_MOVEMENT;
        final long[] rookMagics = Magic.ROOK_MAGIC_NUMBER;
        final int[] rookShifts = Magic.ROOK_SHIFT;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long magic = rookMoves[square][(int) ((allOccupancy & rookMovement[square]) * rookMagics[square] >>> rookShifts[square])];
            long moveBitboard = magic & otherOccupancy;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
            }
            if(tactical) continue;
            moveBitboard = magic & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] = moveInfo | (lsb[(int) ((b2 * DB) >>> 58)] << Board.TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getBishopMoves(long board0, long board1, long board2, long board3, long colorMask, long[] moves, int piece, int player, int moveListLength, long allOccupancy, long otherOccupancy, boolean tactical) {
        long pieceBitboard = ~board0 & ~board1 & board2 & colorMask;
        final int[] lsb = LSB;
        final long[][] bishopMoves = Magic.BISHOP_MOVES;
        final long[] bishopMovement = Magic.BISHOP_MOVEMENT;
        final long[] bishopMagics = Magic.BISHOP_MAGIC_NUMBER;
        final int[] bishopShifts = Magic.BISHOP_SHIFT;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long magic = bishopMoves[square][(int) ((allOccupancy & bishopMovement[square]) * bishopMagics[square] >>> bishopShifts[square])];
            long moveBitboard = magic & otherOccupancy;
            final int moveInfo = square | (piece << Board.START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                moves[moveListLength ++] = moveInfo | (targetSquare << Board.TARGET_SQUARE_SHIFT) | ((int) (((board3 >>> targetSquare & 1) << 3) | ((board2 >>> targetSquare & 1) << 2) | ((board1 >>> targetSquare & 1) << 1) | (board0 >>> targetSquare & 1)) << Board.TARGET_PIECE_SHIFT);
            }
            if(tactical) continue;
            moveBitboard = magic & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] = moveInfo | (lsb[(int) ((b2 * DB) >>> 58)] << Board.TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static final long[] KING_ATTACKS = new long[64];
    private static final long[] LEAP_ATTACKS = new long[64];
    private static final long[][] PAWN_ATTACKS = new long[2][64];
    private static final long[][] PAWN_ADVANCE_SINGLE = new long[2][64];
    private static final long[][] PAWN_ADVANCE_DOUBLE = new long[2][64];
    static {
        for(int square = 0; square < 64; square ++) {
            KING_ATTACKS[square] = Bitboard.BB[Bitboard.KING_ATTACKS][square];
            LEAP_ATTACKS[square] = Bitboard.BB[Bitboard.LEAP_ATTACKS][square];
            for(int player = 0; player < 2; player ++) {
                PAWN_ATTACKS[player][square] = Bitboard.BB[Bitboard.PAWN_ATTACKS_PLAYER0 + player][square];
                PAWN_ADVANCE_SINGLE[player][square] = Bitboard.BB[Bitboard.PAWN_ADVANCE_1_PLAYER0 + player][square];
                PAWN_ADVANCE_DOUBLE[player][square] = Bitboard.BB[Bitboard.PAWN_ADVANCE_2_PLAYER0 + player][square];
            }
        }
    }

    /*
	 * To get the LSB from a long, use:
	 * int lsbIndex = LSB[(int) (((someLong & -someLong) * DB) >>> 58)];
	 */
	private static final int[] LSB = {
        0,  1, 48,  2, 57, 49, 28,  3,
		61, 58, 50, 42, 38, 29, 17,  4,
		62, 55, 59, 36, 53, 51, 43, 22,
		45, 39, 33, 30, 24, 18, 12,  5,
		63, 47, 56, 27, 60, 41, 37, 16,
		54, 35, 52, 21, 44, 32, 23, 11,
		46, 26, 40, 15, 34, 20, 31, 10,
		25, 14, 19,  9, 13,  8,  7,  6
    };

	private static final long DB = 0x03f79d71b4cb0a89L;

}
