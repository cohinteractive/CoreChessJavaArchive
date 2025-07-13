package com.ohinteractive.minchessv2gnew.impl;

import com.ohinteractive.minchessv2gnew.util.B;
import com.ohinteractive.minchessv2gnew.util.Magic;
import com.ohinteractive.minchessv2gnew.util.Piece;
import com.ohinteractive.minchessv2gnew.util.Value;

public class Gen_MinChessV2gNew {
    
    public static final int MAX_MOVELIST_SIZE = 100;
    public static final int MOVELIST_SIZE = MAX_MOVELIST_SIZE - 1;

    public static long[] gen(final long[] board, final boolean legal, final boolean tactical) {
        final int player = (int) board[Board_MinChessV2Lib.STATUS] & Board_MinChessV2Lib.PLAYER_BIT;
        final int playerBit = player << Board_MinChessV2Lib.PLAYER_SHIFT;
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
        //if (moveListLength > MAX_MOVELIST_SIZE - 2) throw new RuntimeException("Move list overflow"); // this is probably never executed as the array would have already overflowed
        moves[MOVELIST_SIZE] = moveListLength;
        return legal ? purgeIllegalMoves(board, moves, player) : moves;
    }

    private Gen_MinChessV2gNew() {}

    private static long[] purgeIllegalMoves(final long[] board, long[] moves, final int player) {
        long[] legalMoves = new long[MAX_MOVELIST_SIZE];
        int legalMoveCount = 0;
        long[] boardAfterMove;
        long move;
        for(int i = 0; i < moves[99]; i ++) {
            move = moves[i];
            boardAfterMove = Board_MinChessV2Lib.makeMove(board, move);
            if(!Board_MinChessV2Lib.isPlayerInCheck(boardAfterMove, player)) legalMoves[legalMoveCount ++] = move;
        }
        legalMoves[MOVELIST_SIZE] = legalMoveCount;
        return legalMoves;
    }

    private static int getKingMoves(final long[] board, long[] moves, final int piece, int moveListLength, final int player, final long allOccupancy, final long otherOccupancy, final boolean tactical) {
        final long bitboard = board[piece];
        final int square = B.LSB[(int) (((bitboard & -bitboard) * B.DB) >>> 58)];
        final long kingAttacks = B.BB[B.KING_ATTACKS][square];
        long moveBitboard = kingAttacks & otherOccupancy;
        int targetSquare;
        while(moveBitboard != 0L) {
            targetSquare = B.LSB[(int) (((moveBitboard & -moveBitboard) * B.DB) >>> 58)];
            moveBitboard &= moveBitboard - 1;
            moves[moveListLength ++] = square | (targetSquare << Board_MinChessV2Lib.TARGET_SQUARE_SHIFT) | (piece << Board_MinChessV2Lib.START_PIECE_SHIFT) | (Board_MinChessV2Lib.getSquare(board, targetSquare) << Board_MinChessV2Lib.TARGET_PIECE_SHIFT);
        }
        if(tactical) return moveListLength;
        moveBitboard = kingAttacks & ~allOccupancy;
        while(moveBitboard!= 0L) {
            moves[moveListLength ++] = square | (B.LSB[(int) (((moveBitboard & -moveBitboard) * B.DB) >>> 58)] << Board_MinChessV2Lib.TARGET_SQUARE_SHIFT) | (piece << Board_MinChessV2Lib.START_PIECE_SHIFT);
            moveBitboard &= moveBitboard - 1;
        }
        final int castling = (int) (board[Board_MinChessV2Lib.STATUS] >>> Board_MinChessV2Lib.CASTLING_SHIFT) & Board_MinChessV2Lib.CASTLING_BITS;
        final boolean kingSide = (castling & (player == Value.WHITE ? 0b1 : 0b100)) != Value.NONE;
        final boolean queenSide = (castling & (player == Value.WHITE ? 0b10 : 0b1000)) != Value.NONE;
        if(kingSide || queenSide) {
            final int other = 1 ^ player;
            if(!Board_MinChessV2Lib.isSquareAttackedByPlayer(board, square, other)) {
                if(kingSide) {
                    if((allOccupancy & (player == Value.WHITE ? 0x0000000000000060L : 0x6000000000000000L)) == 0L && !Board_MinChessV2Lib.isSquareAttackedByPlayer(board, square + 1, other))
                        moves[moveListLength ++] = square | ((square + 2) << Board_MinChessV2Lib.TARGET_SQUARE_SHIFT) | (piece << Board_MinChessV2Lib.START_PIECE_SHIFT);
                }
                if(queenSide) {
                    if((allOccupancy & (player == Value.WHITE ? 0x000000000000000eL : 0x0e00000000000000L)) == 0L && !Board_MinChessV2Lib.isSquareAttackedByPlayer(board, square - 1, other))
                        moves[moveListLength ++] = square | ((square - 2) << Board_MinChessV2Lib.TARGET_SQUARE_SHIFT) | (piece << Board_MinChessV2Lib.START_PIECE_SHIFT);
                }
            }
        }
        return moveListLength;
    }

    private static int getKnightMoves(final long[] board, long[] moves, final int piece, int moveListLength, final long allOccupancy, final long otherOccupancy, final boolean tactical) {
        long knightBitboard = board[piece];
        int square;
        long knightAttacks;
        long moveBitboard;
        int targetSquare;
        while(knightBitboard != 0L) {
            square = B.LSB[(int) (((knightBitboard & -knightBitboard) * B.DB) >>> 58)];
            knightBitboard &= knightBitboard - 1;
            knightAttacks = B.BB[B.LEAP_ATTACKS][square];
            moveBitboard = knightAttacks & otherOccupancy;
            while(moveBitboard != 0L) {
                targetSquare = B.LSB[(int) (((moveBitboard & -moveBitboard) * B.DB) >>> 58)];
                moveBitboard &= moveBitboard - 1;
                moves[moveListLength ++] = square | (targetSquare << Board_MinChessV2Lib.TARGET_SQUARE_SHIFT) | (piece << Board_MinChessV2Lib.START_PIECE_SHIFT) | (Board_MinChessV2Lib.getSquare(board, targetSquare) << Board_MinChessV2Lib.TARGET_PIECE_SHIFT);
            }
            if(tactical) continue;
            moveBitboard = knightAttacks & ~allOccupancy;
            while(moveBitboard != 0L) {
                moves[moveListLength ++] = square | (B.LSB[(int) (((moveBitboard & -moveBitboard) * B.DB) >>> 58)] << Board_MinChessV2Lib.TARGET_SQUARE_SHIFT) | (piece << Board_MinChessV2Lib.START_PIECE_SHIFT);
                moveBitboard &= moveBitboard - 1;
            }
        }
        return moveListLength;
    }

    private static int getPawnMoves(final long[] board, long[] moves, final int piece, int moveListLength, final int player, final long allOccupancy, long otherOccupancy, final boolean tactical) {
        long pawnBitboard = board[piece];
        final int playerBit = player << Board_MinChessV2Lib.PLAYER_SHIFT;
        int square;
        final int eSquare = (int) board[Board_MinChessV2Lib.STATUS] >>> Board_MinChessV2Lib.ESQUARE_SHIFT & Board_MinChessV2Lib.SQUARE_BITS;
        otherOccupancy |= (eSquare > 0 ? (1L << eSquare) : 0L);
        final int pawnAttacks = B.PAWN_ATTACKS_PLAYER0 + player;
        int targetSquare;
        int targetRank;
        int moveInfo;
        long moveBitboard;
        final int pawnAdvanceSingle = B.PAWN_ADVANCE_1_PLAYER0 + player;
        final int pawnAdvanceDouble = B.PAWN_ADVANCE_2_PLAYER0 + player;
        while(pawnBitboard != 0L) {
            square = B.LSB[(int) (((pawnBitboard & -pawnBitboard) * B.DB) >>> 58)];
            pawnBitboard &= pawnBitboard - 1;
            moveBitboard = B.BB[pawnAttacks][square] & otherOccupancy;
            while(moveBitboard != 0L) {
                targetSquare = B.LSB[(int) (((moveBitboard & -moveBitboard) * B.DB) >>> 58)];
                moveBitboard &= moveBitboard - 1;
                targetRank = targetSquare >>> 3;
                if(targetRank == (player == Value.WHITE ? 7 : 0)) {
                    moveInfo = square | (targetSquare << Board_MinChessV2Lib.TARGET_SQUARE_SHIFT) | (piece << Board_MinChessV2Lib.START_PIECE_SHIFT) | (Board_MinChessV2Lib.getSquare(board, targetSquare) << Board_MinChessV2Lib.TARGET_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfo | ((Piece.QUEEN | playerBit) << Board_MinChessV2Lib.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfo | ((Piece.ROOK | playerBit) << Board_MinChessV2Lib.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfo | ((Piece.BISHOP | playerBit) << Board_MinChessV2Lib.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfo | ((Piece.KNIGHT | playerBit) << Board_MinChessV2Lib.PROMOTE_PIECE_SHIFT);
                } else {
                    moves[moveListLength ++] = square | (targetSquare << Board_MinChessV2Lib.TARGET_SQUARE_SHIFT) | (piece << Board_MinChessV2Lib.START_PIECE_SHIFT) | (Board_MinChessV2Lib.getSquare(board, targetSquare) << Board_MinChessV2Lib.TARGET_PIECE_SHIFT);
                }
            }
            if(tactical) continue;
            moveBitboard = B.BB[pawnAdvanceSingle][square] & ~allOccupancy;
            if(moveBitboard != 0L) {
                moveBitboard = (moveBitboard | B.BB[pawnAdvanceDouble][square]) & ~allOccupancy;
            }
            while(moveBitboard != 0L) {
                targetSquare = B.LSB[(int) (((moveBitboard & -moveBitboard) * B.DB) >>> 58)];
                moveBitboard &= moveBitboard - 1;
                targetRank = targetSquare >>> 3;
                if(targetRank == (player == Value.WHITE ? 7 : 0)) {
                    moveInfo = square | (targetSquare << Board_MinChessV2Lib.TARGET_SQUARE_SHIFT) | (piece << Board_MinChessV2Lib.START_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfo | ((Piece.QUEEN | playerBit) << Board_MinChessV2Lib.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfo | ((Piece.BISHOP | playerBit) << Board_MinChessV2Lib.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfo | ((Piece.KNIGHT | playerBit) << Board_MinChessV2Lib.PROMOTE_PIECE_SHIFT);
                    moves[moveListLength++] = moveInfo | ((Piece.ROOK | playerBit) << Board_MinChessV2Lib.PROMOTE_PIECE_SHIFT);
                } else {
                    moves[moveListLength ++] = square | (targetSquare << Board_MinChessV2Lib.TARGET_SQUARE_SHIFT) | (piece << Board_MinChessV2Lib.START_PIECE_SHIFT);
                }
            }
        }
        return moveListLength;
    }

    private static int getQueenMoves(final long[] board, long[] moves, final int piece, final int player, int moveListLength, final long allOccupancy, final long otherOccupancy, final boolean tactical) {
        long queenBitboard = board[piece];
        int square;
        long moveBitboard;
        long magic;
        int targetSquare;
        while(queenBitboard != 0L) {
            square = B.LSB[(int) (((queenBitboard & -queenBitboard) * B.DB) >>> 58)];
            queenBitboard &= queenBitboard - 1;
            magic = Magic.queenMoves(square, allOccupancy);
            moveBitboard = magic & otherOccupancy;
            while(moveBitboard != 0L) {
                targetSquare = B.LSB[(int) (((moveBitboard & -moveBitboard) * B.DB) >>> 58)];
                moveBitboard &= moveBitboard - 1;
                moves[moveListLength ++] = square | (targetSquare << Board_MinChessV2Lib.TARGET_SQUARE_SHIFT) | (piece << Board_MinChessV2Lib.START_PIECE_SHIFT) | (Board_MinChessV2Lib.getSquare(board, targetSquare) << Board_MinChessV2Lib.TARGET_PIECE_SHIFT);
            }
            if(tactical) continue;
            moveBitboard = magic & ~allOccupancy;
            while(moveBitboard != 0L) {
                moves[moveListLength ++] = square | (B.LSB[(int) (((moveBitboard & -moveBitboard) * B.DB) >>> 58)] << Board_MinChessV2Lib.TARGET_SQUARE_SHIFT) | (piece << Board_MinChessV2Lib.START_PIECE_SHIFT);
                moveBitboard &= moveBitboard - 1;
            }
        }
        return moveListLength;
    }

    private static int getRookMoves(final long[] board, long[] moves, final int piece, final int player, int moveListLength, final long allOccupancy, final long otherOccupancy, final boolean tactical) {
        long rookBitboard = board[piece];
        int square;
        long moveBitboard;
        long magic;
        int targetSquare;
        while(rookBitboard != 0L) {
            square = B.LSB[(int) (((rookBitboard & -rookBitboard) * B.DB) >>> 58)];
            rookBitboard &= rookBitboard - 1;
            magic = Magic.rookMoves(square, allOccupancy);
            moveBitboard = magic & otherOccupancy;
            while(moveBitboard != 0L) {
                targetSquare = B.LSB[(int) (((moveBitboard & -moveBitboard) * B.DB) >>> 58)];
                moveBitboard &= moveBitboard - 1;
                moves[moveListLength ++] = square | (targetSquare << Board_MinChessV2Lib.TARGET_SQUARE_SHIFT) | (piece << Board_MinChessV2Lib.START_PIECE_SHIFT) | (Board_MinChessV2Lib.getSquare(board, targetSquare) << Board_MinChessV2Lib.TARGET_PIECE_SHIFT);
            }
            if(tactical) continue;
            moveBitboard = magic & ~allOccupancy;
            while(moveBitboard != 0L) {
                moves[moveListLength ++] = square | (B.LSB[(int) (((moveBitboard & -moveBitboard) * B.DB) >>> 58)] << Board_MinChessV2Lib.TARGET_SQUARE_SHIFT) | (piece << Board_MinChessV2Lib.START_PIECE_SHIFT);
                moveBitboard &= moveBitboard - 1;
            }
        }
        return moveListLength;
    }

    private static int getBishopMoves(final long[] board, long[] moves, final int piece, final int player, int moveListLength, final long allOccupancy, final long otherOccupancy, final boolean tactical) {
        long bishopBitboard = board[piece];
        int square;
        long moveBitboard;
        long magic;
        int targetSquare;
        while(bishopBitboard != 0L) {
            square = B.LSB[(int) (((bishopBitboard & -bishopBitboard) * B.DB) >>> 58)];
            bishopBitboard &= bishopBitboard - 1;
            magic = Magic.bishopMoves(square, allOccupancy);
            moveBitboard = magic & otherOccupancy;
            while(moveBitboard != 0L) {
                targetSquare = B.LSB[(int) (((moveBitboard & -moveBitboard) * B.DB) >>> 58)];
                moveBitboard &= moveBitboard - 1;
                moves[moveListLength ++] = square | (targetSquare << Board_MinChessV2Lib.TARGET_SQUARE_SHIFT) | (piece << Board_MinChessV2Lib.START_PIECE_SHIFT) | (Board_MinChessV2Lib.getSquare(board, targetSquare) << Board_MinChessV2Lib.TARGET_PIECE_SHIFT);
            }
            if(tactical) continue;
            moveBitboard = magic & ~allOccupancy;
            while(moveBitboard != 0L) {
                moves[moveListLength ++] = square | (B.LSB[(int) (((moveBitboard & -moveBitboard) * B.DB) >>> 58)] << Board_MinChessV2Lib.TARGET_SQUARE_SHIFT) | (piece << Board_MinChessV2Lib.START_PIECE_SHIFT);
                moveBitboard &= moveBitboard - 1;
            }
        }
        return moveListLength;
    }

}
