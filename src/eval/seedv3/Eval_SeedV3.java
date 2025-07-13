package com.ohinteractive.seedv3.impl;

import com.ohinteractive.seedv3.util.Bitboard;
import com.ohinteractive.seedv3.util.Crit;
import com.ohinteractive.seedv3.util.Logger;
import com.ohinteractive.seedv3.util.Magic;
import com.ohinteractive.seedv3.util.Piece;
import com.ohinteractive.seedv3.util.TTable_SeedV3;
import com.ohinteractive.seedv3.util.Value;

public class Eval_SeedV3 {
    
    public static int eval(long board0, long board1, long board2, long board3, long status, long key) {
        // ********************
        // ***              ***
        // *** CHECK TTABLE ***
        // ***              ***
        // ********************
        final long t = table.probe(key).data();
        if(t != TTable_SeedV3.TYPE_INVALID) return (int) t;
        // ************************
        // ***                  ***
        // *** SET UP CONSTANTS ***
        // ***                  ***
        // ************************
        final int thisPlayer = (int) status & Board_MinChessV2Lib.PLAYER_BIT;
        final long whitePieceMask = ~board3;
        final long blackPieceMask = board3;
        final long whiteKing = board0 & ~board1 & ~board2 & whitePieceMask;
        final long whiteQueen = ~board0 & board1 & ~board2 & whitePieceMask;
        final long whiteRook = board0 & board1 & ~board2 & whitePieceMask;
        final long whiteBishop = ~board0 & ~board1 & board2 & whitePieceMask;
        final long whiteKnight = board0 & ~board1 & board2 & whitePieceMask;
        final long whitePawn = ~board0 & board1 & board2 & whitePieceMask;
        final int whiteQueenCount = Long.bitCount(whiteQueen);
        final int whiteRookCount = Long.bitCount(whiteRook);
        final int whiteBishopCount = Long.bitCount(whiteBishop);
        final int whiteKnightCount = Long.bitCount(whiteKnight);
        final long blackKing = board0 & ~board1 & ~board2 & blackPieceMask;
        final long blackQueen = ~board0 & board1 & ~board2 & blackPieceMask;
        final long blackRook = board0 & board1 & ~board2 & blackPieceMask;
        final long blackBishop = ~board0 & ~board1 & board2 & blackPieceMask;
        final long blackKnight = board0 & ~board1 & board2 & blackPieceMask;
        final long blackPawn = ~board0 & board1 & board2 & blackPieceMask;
        final int blackQueenCount = Long.bitCount(blackQueen);
        final int blackRookCount = Long.bitCount(blackRook);
        final int blackBishopCount = Long.bitCount(blackBishop);
        final int blackKnightCount = Long.bitCount(blackKnight);
        final int p = MAX_PHASE - (
            PHASE_VALUE[Piece.QUEEN ][whiteQueenCount  + blackQueenCount ] +
            PHASE_VALUE[Piece.ROOK  ][whiteRookCount   + blackRookCount  ] +
            PHASE_VALUE[Piece.BISHOP][whiteBishopCount + blackBishopCount] +
            PHASE_VALUE[Piece.KNIGHT][whiteKnightCount + blackKnightCount]);
        final int p0 = (p & ~(p >> 31));
        final int p1 = (p0 - MAX_PHASE) >> 31;
        // Clamp phase to [0, MAX_PHASE] branchlessly for faster eval
        final int phase = (p1 & p0) | (MAX_PHASE & ~p1);
        final int[] lsb = LSB;
        final int whiteKingSquare = lsb[(int) (((whiteKing & -whiteKing) * DB) >>> 58)];
        final int whiteKingRank = whiteKingSquare >>> Value.RANK_SHIFT;
        final int whiteKingFile = whiteKingSquare & Value.FILE;
        final int[][] QUEEN_VALUES  = PIECE_VALUE[Piece.QUEEN];
        final int[][] ROOK_VALUES   = PIECE_VALUE[Piece.ROOK];
        final int[][] BISHOP_VALUES = PIECE_VALUE[Piece.BISHOP];
        final int[][] KNIGHT_VALUES = PIECE_VALUE[Piece.KNIGHT];
        final int whitePieceMaterial =
            QUEEN_VALUES [whiteQueenCount ][phase] +
            ROOK_VALUES  [whiteRookCount  ][phase] +
            BISHOP_VALUES[whiteBishopCount][phase] +
            KNIGHT_VALUES[whiteKnightCount][phase];
        final long allOccupancy = board0 | board1 | board2;
        final long whiteOccupancy = allOccupancy & whitePieceMask;
        final long whiteBishopsKnights = whiteBishop | whiteKnight;
        final int blackKingSquare = lsb[(int) (((blackKing & -blackKing) * DB) >>> 58)];
        final int blackKingRank = blackKingSquare >>> Value.RANK_SHIFT;
        final int blackKingFile = blackKingSquare & Value.FILE;
        final int blackPieceMaterial =
            QUEEN_VALUES [blackQueenCount ][phase] +
            ROOK_VALUES  [blackRookCount  ][phase] +
            BISHOP_VALUES[blackBishopCount][phase] +
            KNIGHT_VALUES[blackKnightCount][phase];
        final long blackOccupancy = allOccupancy & blackPieceMask;
        final long blackBishopsKnights = blackBishop | blackKnight;
        final long lightSquares = LIGHT_SQUARES_BITBOARD;
        final long whiteKingRing = KING_RING[Value.WHITE][whiteKingSquare];
        final long blackKingRing = KING_RING[Value.BLACK][blackKingSquare];
        final boolean whiteCastling = (status & 0b110) != 0L;
        final boolean blackCastling = (status & 0b11000) != 0L;
        // **********************
        // ***                ***
        // *** EVALUATE WHITE ***
        // ***                ***
        // **********************
        final long whiteQueenEvalAndSafety = queenEval(whiteQueen, whiteQueenCount, phase, Value.WHITE, whiteBishop, whiteKnight, allOccupancy, whiteOccupancy, blackKingRank, blackKingFile, blackKingSquare, blackKingRing);
        final long whiteRookEvalAndSafety = rookEval(whiteRook, whiteRookCount, whiteCastling, phase, Value.WHITE, whiteKing, whitePawn, allOccupancy, whiteOccupancy, blackPawn, blackQueen, blackKingRank, blackKingFile, blackKingSquare, blackKingRing);
        final long whiteBishopEvalAndSafety = bishopEval(whiteBishop, whiteBishopCount, phase, Value.WHITE, Value.BLACK, allOccupancy, whiteOccupancy, whitePawn, blackPawn, lightSquares, whiteKingRank, whiteKingFile, blackKingRank, blackKingFile, blackKingSquare, blackKingRing);
        final long whiteKnightEvalAndSafety = knightEval(whiteKnight, whiteKnightCount, phase, whitePawn, Value.WHITE, Value.BLACK, whiteOccupancy, blackPawn, whiteKingRank, whiteKingFile, blackKingRank, blackKingFile, blackKingSquare, blackKingRing);
        int whiteEval =
        kingEval(Value.WHITE, whiteKingSquare, phase, whiteKingRank, whiteKingFile, whiteRook, whitePawn, blackPawn, whitePieceMaterial, blackPieceMaterial, blackKingFile, blackKingRank) +
        (int) (whiteQueenEvalAndSafety >> EVAL_SHIFT) +
        (int) (whiteRookEvalAndSafety >> EVAL_SHIFT) +
        (int) (whiteBishopEvalAndSafety >> EVAL_SHIFT) +
        (int) (whiteKnightEvalAndSafety >> EVAL_SHIFT) +
        pawnEval(whitePawn, phase, Value.WHITE, whiteBishopsKnights, blackPawn, whitePieceMaterial, whiteKingRank, whiteKingFile, blackKingRank, blackKingFile, blackPieceMaterial, thisPlayer, whiteKing);
        // **********************
        // ***                ***
        // *** EVALUATE BLACK ***
        // ***                ***
        // **********************
        final long blackQueenEvalAndSafety = queenEval(blackQueen, blackQueenCount, phase, Value.BLACK, blackBishop, blackKnight, allOccupancy, blackOccupancy, whiteKingRank, whiteKingFile, whiteKingSquare, whiteKingRing);
        final long blackRookEvalAndSafety = rookEval(blackRook, blackRookCount, blackCastling, phase, Value.BLACK, blackKing, blackPawn, allOccupancy, blackOccupancy, whitePawn, whiteQueen, whiteKingRank, whiteKingFile, whiteKingSquare, whiteKingRing);
        final long blackBishopEvalAndSafety = bishopEval(blackBishop, blackBishopCount, phase, Value.BLACK, Value.WHITE, allOccupancy, blackOccupancy, blackPawn, whitePawn, lightSquares, blackKingRank, blackKingFile, whiteKingRank, whiteKingFile, whiteKingSquare, whiteKingRing);
        final long blackKnightEvalAndSafety = knightEval(blackKnight, blackKnightCount, phase, blackPawn, Value.BLACK, Value.WHITE, blackOccupancy, whitePawn, blackKingRank, blackKingFile, whiteKingRank, whiteKingFile, whiteKingSquare, whiteKingRing);
        int blackEval =
        kingEval(Value.BLACK, blackKingSquare, phase, blackKingRank, blackKingFile, blackRook, blackPawn, whitePawn, blackPieceMaterial, whitePieceMaterial, whiteKingFile, whiteKingRank) +
        (int) (blackQueenEvalAndSafety >> EVAL_SHIFT) +
        (int) (blackRookEvalAndSafety >> EVAL_SHIFT) +
        (int) (blackBishopEvalAndSafety >> EVAL_SHIFT) +
        (int) (blackKnightEvalAndSafety >> EVAL_SHIFT) +
        pawnEval(blackPawn, phase, Value.BLACK, blackBishopsKnights, whitePawn, blackPieceMaterial, blackKingRank, blackKingFile, whiteKingRank, whiteKingFile, whitePieceMaterial, thisPlayer, blackKing);
        // *****************************
        // ***                       ***
        // *** EVALUATE WHITE SAFETY ***
        // ***                       ***
        // *****************************
        final int whiteSafety = (int) (blackQueenEvalAndSafety & SAFETY_BITS) + (int) (blackRookEvalAndSafety & SAFETY_BITS) + (int) (blackBishopEvalAndSafety & SAFETY_BITS) + (int) (blackKnightEvalAndSafety & SAFETY_BITS);
        final int whiteSafetyClamped = whiteSafety - ((whiteSafety - 99) & ~((whiteSafety - 99) >> 31));
        whiteEval -= SAFETY_VALUE[whiteSafetyClamped];
        // *****************************
        // ***                       ***
        // *** EVALUATE BLACK SAFETY ***
        // ***                       ***
        // *****************************
        final int blackSafety = (int) (whiteQueenEvalAndSafety & SAFETY_BITS) + (int) (whiteRookEvalAndSafety & SAFETY_BITS) + (int) (whiteBishopEvalAndSafety & SAFETY_BITS) + (int) (whiteKnightEvalAndSafety & SAFETY_BITS);
        final int blackSafetyClamped = blackSafety - ((blackSafety - 99) & ~((blackSafety - 99) >> 31));
        blackEval -= SAFETY_VALUE[blackSafetyClamped];
        // **********************************
        // ***                            ***
        // *** PLAYER TO MOVE PERSPECTIVE ***
        // ***                            ***
        // **********************************
        int eval = (thisPlayer == Value.WHITE ? whiteEval - blackEval : blackEval - whiteEval);
        // **********************
        // ***                ***
        // *** EVALUATE DRAWS ***
        // ***                ***
        // **********************
        eval = drawEval(eval, (int) status >>> Board_MinChessV2Lib.HALF_MOVE_CLOCK_SHIFT & Board_MinChessV2Lib.HALF_MOVE_CLOCK_BITS, Long.bitCount(allOccupancy), whiteBishopsKnights, blackBishopsKnights, whiteBishop, blackBishop, Long.bitCount(whiteBishop), Long.bitCount(blackBishop));
        // *****************************
        // ***                       ***
        // *** SAVE NEW TTABLE ENTRY ***
        // ***                       ***
        // *****************************
        table.save(key, 0, TTable_SeedV3.TYPE_EVAL, eval, 0L);
        return eval;
    }

    public static int evalWithLogging(long board0, long board1, long board2, long board3, long status, long key, Logger logger) {
        // ********************
        // ***              ***
        // *** CHECK TTABLE ***
        // ***              ***
        // ********************
        final long t = table.probe(key).data();
        if(t != TTable_SeedV3.TYPE_INVALID) {
            logger.log("eval: TTable hit on key " + Long.toHexString(key) + " with eval " + ((int) t));
            return (int) t;
        }
        // ************************
        // ***                  ***
        // *** SET UP CONSTANTS ***
        // ***                  ***
        // ************************
        final int thisPlayer = (int) status & Board_MinChessV2Lib.PLAYER_BIT;
        final long whitePieceMask = ~board3;
        final long blackPieceMask = board3;
        final long whiteKing = board0 & ~board1 & ~board2 & whitePieceMask;
        final long whiteQueen = ~board0 & board1 & ~board2 & whitePieceMask;
        final long whiteRook = board0 & board1 & ~board2 & whitePieceMask;
        final long whiteBishop = ~board0 & ~board1 & board2 & whitePieceMask;
        final long whiteKnight = board0 & ~board1 & board2 & whitePieceMask;
        final long whitePawn = ~board0 & board1 & board2 & whitePieceMask;
        final int whiteQueenCount = Long.bitCount(whiteQueen);
        final int whiteRookCount = Long.bitCount(whiteRook);
        final int whiteBishopCount = Long.bitCount(whiteBishop);
        final int whiteKnightCount = Long.bitCount(whiteKnight);
        final long blackKing = board0 & ~board1 & ~board2 & blackPieceMask;
        final long blackQueen = ~board0 & board1 & ~board2 & blackPieceMask;
        final long blackRook = board0 & board1 & ~board2 & blackPieceMask;
        final long blackBishop = ~board0 & ~board1 & board2 & blackPieceMask;
        final long blackKnight = board0 & ~board1 & board2 & blackPieceMask;
        final long blackPawn = ~board0 & board1 & board2 & blackPieceMask;
        final int blackQueenCount = Long.bitCount(blackQueen);
        final int blackRookCount = Long.bitCount(blackRook);
        final int blackBishopCount = Long.bitCount(blackBishop);
        final int blackKnightCount = Long.bitCount(blackKnight);
        final int p = MAX_PHASE - (
            PHASE_VALUE[Piece.QUEEN ][whiteQueenCount  + blackQueenCount ] +
            PHASE_VALUE[Piece.ROOK  ][whiteRookCount   + blackRookCount  ] +
            PHASE_VALUE[Piece.BISHOP][whiteBishopCount + blackBishopCount] +
            PHASE_VALUE[Piece.KNIGHT][whiteKnightCount + blackKnightCount]);
        final int p0 = (p & ~(p >> 31));
        final int p1 = (p0 - MAX_PHASE) >> 31;
        // Clamp phase to [0, MAX_PHASE] branchlessly for faster eval
        final int phase = (p1 & p0) | (MAX_PHASE & ~p1);
        final int[] lsb = LSB;
        final int whiteKingSquare = lsb[(int) (((whiteKing & -whiteKing) * DB) >>> 58)];
        final int whiteKingRank = whiteKingSquare >>> Value.RANK_SHIFT;
        final int whiteKingFile = whiteKingSquare & Value.FILE;
        final int[][] QUEEN_VALUES  = PIECE_VALUE[Piece.QUEEN];
        final int[][] ROOK_VALUES   = PIECE_VALUE[Piece.ROOK];
        final int[][] BISHOP_VALUES = PIECE_VALUE[Piece.BISHOP];
        final int[][] KNIGHT_VALUES = PIECE_VALUE[Piece.KNIGHT];
        final int whitePieceMaterial =
            QUEEN_VALUES [whiteQueenCount ][phase] +
            ROOK_VALUES  [whiteRookCount  ][phase] +
            BISHOP_VALUES[whiteBishopCount][phase] +
            KNIGHT_VALUES[whiteKnightCount][phase];
        final long allOccupancy = board0 | board1 | board2;
        final long whiteOccupancy = allOccupancy & whitePieceMask;
        final long whiteBishopsKnights = whiteBishop | whiteKnight;
        final int blackKingSquare = lsb[(int) (((blackKing & -blackKing) * DB) >>> 58)];
        final int blackKingRank = blackKingSquare >>> Value.RANK_SHIFT;
        final int blackKingFile = blackKingSquare & Value.FILE;
        final int blackPieceMaterial =
            QUEEN_VALUES [blackQueenCount ][phase] +
            ROOK_VALUES  [blackRookCount  ][phase] +
            BISHOP_VALUES[blackBishopCount][phase] +
            KNIGHT_VALUES[blackKnightCount][phase];
        final long blackOccupancy = allOccupancy & blackPieceMask;
        final long blackBishopsKnights = blackBishop | blackKnight;
        final long lightSquares = LIGHT_SQUARES_BITBOARD;
        final long whiteKingRing = KING_RING[Value.WHITE][whiteKingSquare];
        final long blackKingRing = KING_RING[Value.BLACK][blackKingSquare];
        final boolean whiteCastling = (status & 0b110) != 0L;
        final boolean blackCastling = (status & 0b11000) != 0L;
        // **********************
        // ***                ***
        // *** EVALUATE WHITE ***
        // ***                ***
        // **********************
        final long whiteQueenEvalAndSafety = queenEvalWithLogging(whiteQueen, whiteQueenCount, phase, Value.WHITE, whiteBishop, whiteKnight, allOccupancy, whiteOccupancy, blackKingRank, blackKingFile, blackKingSquare, blackKingRing, logger);
        logger.log("eval whiteQueenEvalAndSafety eval[" + ((int) (whiteQueenEvalAndSafety >> EVAL_SHIFT)) + "] safety [" + ((int) whiteQueenEvalAndSafety) + "]");
        final long whiteRookEvalAndSafety = rookEvalWithLogging(whiteRook, whiteRookCount, whiteCastling, phase, Value.WHITE, whiteKing, whitePawn, allOccupancy, whiteOccupancy, blackPawn, blackQueen, blackKingRank, blackKingFile, blackKingSquare, blackKingRing, logger);
        logger.log("eval whiteRookEvalAndSafety eval[" + ((int) (whiteRookEvalAndSafety >> EVAL_SHIFT)) + "] safety [" + ((int) whiteRookEvalAndSafety) + "]");
        final long whiteBishopEvalAndSafety = bishopEvalWithLogging(whiteBishop, whiteBishopCount, phase, Value.WHITE, Value.BLACK, allOccupancy, whiteOccupancy, whitePawn, blackPawn, lightSquares, whiteKingRank, whiteKingFile, blackKingRank, blackKingFile, blackKingSquare, blackKingRing, logger);
        logger.log("eval whiteBishopEvalAndSafety eval[" + ((int) (whiteBishopEvalAndSafety >> EVAL_SHIFT)) + "] safety [" + ((int) whiteBishopEvalAndSafety) + "]");
        final long whiteKnightEvalAndSafety = knightEvalWithLogging(whiteKnight, whiteKnightCount, phase, whitePawn, Value.WHITE, Value.BLACK, whiteOccupancy, blackPawn, whiteKingRank, whiteKingFile, blackKingRank, blackKingFile, blackKingSquare, blackKingRing, logger);
        logger.log("eval whiteKnightEvalAndSafety eval[" + ((int) (whiteKnightEvalAndSafety >> EVAL_SHIFT)) + "] safety [" + ((int) whiteKnightEvalAndSafety) + "]");
        int whiteEval =
        kingEvalWithLogging(Value.WHITE, whiteKingSquare, phase, whiteKingRank, whiteKingFile, whiteRook, whitePawn, blackPawn, whitePieceMaterial, blackPieceMaterial, blackKingFile, blackKingRank, logger) +
        (int) (whiteQueenEvalAndSafety >> EVAL_SHIFT) +
        (int) (whiteRookEvalAndSafety >> EVAL_SHIFT) +
        (int) (whiteBishopEvalAndSafety >> EVAL_SHIFT) +
        (int) (whiteKnightEvalAndSafety >> EVAL_SHIFT) +
        pawnEvalWithLogging(whitePawn, phase, Value.WHITE, whiteBishopsKnights, blackPawn, whitePieceMaterial, whiteKingRank, whiteKingFile, blackKingRank, blackKingFile, blackPieceMaterial, thisPlayer, whiteKing, logger);
        // **********************
        // ***                ***
        // *** EVALUATE BLACK ***
        // ***                ***
        // **********************
        final long blackQueenEvalAndSafety = queenEvalWithLogging(blackQueen, blackQueenCount, phase, Value.BLACK, blackBishop, blackKnight, allOccupancy, blackOccupancy, whiteKingRank, whiteKingFile, whiteKingSquare, whiteKingRing, logger);
        logger.log("eval blackQueenEvalAndSafety eval[" + ((int) (blackQueenEvalAndSafety >> EVAL_SHIFT)) + "] safety [" + ((int) blackQueenEvalAndSafety) + "]");
        final long blackRookEvalAndSafety = rookEvalWithLogging(blackRook, blackRookCount, blackCastling, phase, Value.BLACK, blackKing, blackPawn, allOccupancy, blackOccupancy, whitePawn, whiteQueen, whiteKingRank, whiteKingFile, whiteKingSquare, whiteKingRing, logger);
        logger.log("eval blackRookEvalAndSafety eval[" + ((int) (blackRookEvalAndSafety >> EVAL_SHIFT)) + "] safety [" + ((int) blackRookEvalAndSafety) + "]");
        final long blackBishopEvalAndSafety = bishopEvalWithLogging(blackBishop, blackBishopCount, phase, Value.BLACK, Value.WHITE, allOccupancy, blackOccupancy, blackPawn, whitePawn, lightSquares, blackKingRank, blackKingFile, whiteKingRank, whiteKingFile, whiteKingSquare, whiteKingRing, logger);
        logger.log("eval blackBishopEvalAndSafety eval[" + ((int) (blackBishopEvalAndSafety >> EVAL_SHIFT)) + "] safety [" + ((int) blackBishopEvalAndSafety) + "]");
        final long blackKnightEvalAndSafety = knightEvalWithLogging(blackKnight, blackKnightCount, phase, blackPawn, Value.BLACK, Value.WHITE, blackOccupancy, whitePawn, blackKingRank, blackKingFile, whiteKingRank, whiteKingFile, whiteKingSquare, whiteKingRing, logger);
        logger.log("eval blackKnightEvalAndSafety eval[" + ((int) (blackKnightEvalAndSafety >> EVAL_SHIFT)) + "] safety [" + ((int) blackKnightEvalAndSafety) + "]");
        int blackEval =
        kingEvalWithLogging(Value.BLACK, blackKingSquare, phase, blackKingRank, blackKingFile, blackRook, blackPawn, whitePawn, blackPieceMaterial, whitePieceMaterial, whiteKingFile, whiteKingRank, logger) +
        (int) (blackQueenEvalAndSafety >> EVAL_SHIFT) +
        (int) (blackRookEvalAndSafety >> EVAL_SHIFT) +
        (int) (blackBishopEvalAndSafety >> EVAL_SHIFT) +
        (int) (blackKnightEvalAndSafety >> EVAL_SHIFT) +
        pawnEvalWithLogging(blackPawn, phase, Value.BLACK, blackBishopsKnights, whitePawn, blackPieceMaterial, blackKingRank, blackKingFile, whiteKingRank, whiteKingFile, whitePieceMaterial, thisPlayer, blackKing, logger);
        // *****************************
        // ***                       ***
        // *** EVALUATE WHITE SAFETY ***
        // ***                       ***
        // *****************************
        final int whiteSafety = (int) (blackQueenEvalAndSafety & SAFETY_BITS) + (int) (blackRookEvalAndSafety & SAFETY_BITS) + (int) (blackBishopEvalAndSafety & SAFETY_BITS) + (int) (blackKnightEvalAndSafety & SAFETY_BITS);
        final int whiteSafetyClamped = whiteSafety - ((whiteSafety - 99) & ~((whiteSafety - 99) >> 31));
        whiteEval -= SAFETY_VALUE[whiteSafetyClamped];
        // *****************************
        // ***                       ***
        // *** EVALUATE BLACK SAFETY ***
        // ***                       ***
        // *****************************
        final int blackSafety = (int) (whiteQueenEvalAndSafety & SAFETY_BITS) + (int) (whiteRookEvalAndSafety & SAFETY_BITS) + (int) (whiteBishopEvalAndSafety & SAFETY_BITS) + (int) (whiteKnightEvalAndSafety & SAFETY_BITS);
        final int blackSafetyClamped = blackSafety - ((blackSafety - 99) & ~((blackSafety - 99) >> 31));
        blackEval -= SAFETY_VALUE[blackSafetyClamped];
        // **********************************
        // ***                            ***
        // *** PLAYER TO MOVE PERSPECTIVE ***
        // ***                            ***
        // **********************************
        int eval = (thisPlayer == Value.WHITE ? whiteEval - blackEval : blackEval - whiteEval);
        // **********************
        // ***                ***
        // *** EVALUATE DRAWS ***
        // ***                ***
        // **********************
        eval = drawEvalWithLogging(eval, (int) status >>> Board_MinChessV2Lib.HALF_MOVE_CLOCK_SHIFT & Board_MinChessV2Lib.HALF_MOVE_CLOCK_BITS, Long.bitCount(allOccupancy), whiteBishopsKnights, blackBishopsKnights, whiteBishop, blackBishop, Long.bitCount(whiteBishop), Long.bitCount(blackBishop), logger);
        // *****************************
        // ***                       ***
        // *** SAVE NEW TTABLE ENTRY ***
        // ***                       ***
        // *****************************
        table.save(key, 0, TTable_SeedV3.TYPE_EVAL, eval, 0L);
        return eval;
    }

    public static int see(long[] board, int startSquare, int targetSquare) {
        long board0 = board[0];
        long board1 = board[1];
        long board2 = board[2];
        long board3 = board[3];
        int seeValue = 0;
        int startPlayer = (int) board[Board_MinChessV2Lib.STATUS] & Board_MinChessV2Lib.PLAYER_BIT;
        int currentPlayer = startPlayer;
        int startPiece = Board_MinChessV2Lib.getSquare(board0, board1, board2, board3, startSquare);
        int targetPiece = Board_MinChessV2Lib.getSquare(board0, board1, board2, board3, targetSquare);
        long targetSquareBit = 1L << targetSquare;
        int[] VALUE = RAW_PIECE_VALUE;
        while(true) {
            final int mask = -(currentPlayer ^ startPlayer);
            seeValue += ((VALUE[targetPiece] ^ mask) - mask);
            final long pieceMoveBits = (1L << startSquare) | targetSquareBit;
            board0 ^= (-(startPiece & 1) & pieceMoveBits) ^ (-(targetPiece & 1) & targetSquareBit);
            board1 ^= (-(startPiece >>> 1 & 1) & pieceMoveBits) ^ (-(targetPiece >>> 1 & 1) & targetSquareBit);
            board2 ^= (-(startPiece >>> 2 & 1) & pieceMoveBits) ^ (-(targetPiece >>> 2 & 1) & targetSquareBit);
            board3 ^= (-(currentPlayer) & pieceMoveBits) ^ (~(-currentPlayer) & targetSquareBit);
            currentPlayer ^= 1;
            final int nextAttacker = getNextAttackingPiece(board0, board1, board2, board3, targetSquare, currentPlayer);
            if(nextAttacker == Value.INVALID) return seeValue;
            targetPiece = startPiece;
            startPiece = (nextAttacker >>> 6 & Piece.TYPE) | (currentPlayer << 3);
            startSquare = nextAttacker & Board_MinChessV2Lib.SQUARE_BITS;
        }
    }

    private static int drawEval(int eval, int halfMoveClock, int allOccupancyCount, long whiteBishopsKnights, long blackBishopsKnights, long whiteBishop, long blackBishop, int whiteBishopsCount, int blackBishopsCount) {
        if(halfMoveClock >= 100) return 0;
        if(allOccupancyCount < 3) return 0;
        if(allOccupancyCount == 3) {
            if(whiteBishopsKnights != 0L || blackBishopsKnights != 0L) return 0;
        }
        if(allOccupancyCount == 4) {
            if(whiteBishopsCount == 1 && blackBishopsCount == 1) {
                final int whiteSquare = LSB[(int) (((whiteBishop & -whiteBishop) * DB) >>> 58)];
                final int blackSquare = LSB[(int) (((blackBishop & -blackBishop) * DB) >>> 58)];
                if(((whiteSquare >>> 3 & 1) == (whiteSquare & 1)) == ((blackSquare >>> 3 & 1) == (blackSquare & 1))) return 0;
            }
        }
        return eval;
    }

    private static int drawEvalWithLogging(int eval, int halfMoveClock, int allOccupancyCount, long whiteBishopsKnights, long blackBishopsKnights, long whiteBishop, long blackBishop, int whiteBishopsCount, int blackBishopsCount, Logger logger) {
        if(halfMoveClock >= 100) {
            logger.log("drawEval: halfMoveClock >= 100 (" + halfMoveClock + ") 50-move rule early return");
            return 0;
        }
        if(allOccupancyCount < 3) {
            logger.log("drawEval: allOccupancyCount < 3 Insufficient material early return");
            return 0;
        }
        if(allOccupancyCount == 3 && (whiteBishopsKnights != 0L || blackBishopsKnights != 0L)) {
            logger.log("drawEval: only 1 knight early return");
            return 0;
        }
        if(allOccupancyCount == 4 && whiteBishopsCount == 1 && blackBishopsCount == 1) {
                final int whiteSquare = LSB[(int) (((whiteBishop & -whiteBishop) * DB) >>> 58)];
                final int blackSquare = LSB[(int) (((blackBishop & -blackBishop) * DB) >>> 58)];
                if(((whiteSquare >>> 3 & 1) == (whiteSquare & 1)) != ((blackSquare >>> 3 & 1) == (blackSquare & 1))) {
                    logger.log("drawEval: 2 bishops left, opposite color early return");
                    return 0;
                }
        }
        return eval;
    }

    private static TTable_SeedV3 table = new TTable_SeedV3();

    private static final int CENTRE_FILE = 3;
    private static final int CENTRE_RANK = 3;
    private static final int EDGE_WEIGHT = 10;
    private static final int PROXIMITY_WEIGHT = 20;
    private static final int MAX_PHASE = 24;
    private static final int EVAL_SHIFT = 32;
    private static final int SAFETY_BITS = 0xff;
    private static final int WHITE_BACK_RANK = 0;
    private static final int BLACK_BACK_RANK = 7;
    
    private static final int[] SAFETY_VALUE = {
        0,  0,   1,   2,   3,   5,   7,   9,  12,  15,
       18,  22,  26,  30,  35,  39,  44,  50,  56,  62,
       68,  75,  82,  85,  89,  97, 105, 113, 122, 131,
      140, 150, 169, 180, 191, 202, 213, 225, 237, 248,
      260, 272, 283, 295, 307, 319, 330, 342, 354, 366,
      377, 389, 401, 412, 424, 436, 448, 459, 471, 483,
      494, 500, 500, 500, 500, 500, 500, 500, 500, 500,
      500, 500, 500, 500, 500, 500, 500, 500, 500, 500,
      500, 500, 500, 500, 500, 500, 500, 500, 500, 500,
      500, 500, 500, 500, 500, 500, 500, 500, 500, 500
    };

    public static final int[] LSB = {
        0,  1, 48,  2, 57, 49, 28,  3,
		61, 58, 50, 42, 38, 29, 17,  4,
		62, 55, 59, 36, 53, 51, 43, 22,
		45, 39, 33, 30, 24, 18, 12,  5,
		63, 47, 56, 27, 60, 41, 37, 16,
		54, 35, 52, 21, 44, 32, 23, 11,
		46, 26, 40, 15, 34, 20, 31, 10,
		25, 14, 19,  9, 13,  8,  7,  6
    };

	public static final long DB = 0x03f79d71b4cb0a89L;

    private static final long LIGHT_SQUARES_BITBOARD;
    static {
        LIGHT_SQUARES_BITBOARD = Bitboard.BB[Bitboard.SQUARE_COLOR_LIGHT][0];
    }

    private static final long[] LEAP_ATTACKS_BITBOARD = new long[64];
    static {
        for(int square = 0; square < 64; square ++) {
            LEAP_ATTACKS_BITBOARD[square] = Bitboard.BB[Bitboard.LEAP_ATTACKS][square];
        }
    }

    private static final long[][] PAWN_ATTACKS = new long[2][64];
    private static final long[][] KING_RING = new long[2][64];
    private static final long[][] PASSED_PAWNS_FILES = new long[2][8];
    private static final long[][] FORWARD_RANKS = new long[2][8];
    static {
        for(int player = 0; player < 2; player ++) {
            for(int square = 0; square < 64; square ++) {
                PAWN_ATTACKS[player][square] = Bitboard.BB[Bitboard.PAWN_ATTACKS_PLAYER0 + player][square];
                KING_RING[player][square] = Bitboard.BB[Bitboard.KING_RING_PLAYER0 + player][square];
            }
            for(int fileOrRank = 0; fileOrRank < 8; fileOrRank ++) {
                PASSED_PAWNS_FILES[player][fileOrRank] = Bitboard.BB[Bitboard.PASSED_PAWNS_FILES_PLAYER0 + player][fileOrRank];
                FORWARD_RANKS[player][fileOrRank] = Bitboard.BB[Bitboard.FORWARD_RANKS_PLAYER0 + player][fileOrRank];
            }
        }
    }

    private static final long[] FILE_BITBOARD = new long[8];
    private static final long[] RANK_BITBOARD = new long[8];
    static {
        for(int fileOrRank = 0; fileOrRank < 8; fileOrRank ++) {
            FILE_BITBOARD[fileOrRank] = Bitboard.BB[Bitboard.FILE][fileOrRank];
            RANK_BITBOARD[fileOrRank] = Bitboard.BB[Bitboard.RANK][fileOrRank];
        }
    }

    private static final int[][][] PIECE_VALUE = new int[7][11][25];
    static {
        for(int phase = 0; phase < 25; phase ++) {
            for(int numPiece = 1; numPiece < 11; numPiece ++) {
                PIECE_VALUE[Piece.QUEEN][numPiece][phase] = Crit.MATERIAL[Piece.QUEEN][phase] * numPiece;
                PIECE_VALUE[Piece.ROOK][numPiece][phase] = Crit.MATERIAL[Piece.ROOK][phase] * numPiece;
                PIECE_VALUE[Piece.BISHOP][numPiece][phase] = Crit.MATERIAL[Piece.BISHOP][phase] * numPiece;
                PIECE_VALUE[Piece.KNIGHT][numPiece][phase] = Crit.MATERIAL[Piece.KNIGHT][phase] * numPiece;
                PIECE_VALUE[Piece.PAWN][numPiece][phase] = Crit.MATERIAL[Piece.PAWN][phase] * numPiece;
            }
        }
    }

    private static final int[] RAW_PIECE_VALUE = new int[15];
    static {
        for(int player = 0; player < 9; player += 8) {
            RAW_PIECE_VALUE[Piece.QUEEN | player] = PIECE_VALUE[Piece.QUEEN][1][0];
            RAW_PIECE_VALUE[Piece.ROOK | player] = PIECE_VALUE[Piece.ROOK][1][0];
            RAW_PIECE_VALUE[Piece.BISHOP | player] = PIECE_VALUE[Piece.BISHOP][1][0];
            RAW_PIECE_VALUE[Piece.KNIGHT | player] = PIECE_VALUE[Piece.KNIGHT][1][0];
            RAW_PIECE_VALUE[Piece.PAWN | player] = PIECE_VALUE[Piece.PAWN][1][0];
        }
    }
    
    private static final int[][] PHASE_VALUE = new int[7][19];
    static {
        for(int numPiece = 1; numPiece < 19; numPiece ++) {
            PHASE_VALUE[Piece.QUEEN][numPiece] = numPiece * 4;
            PHASE_VALUE[Piece.ROOK][numPiece] = numPiece * 2;
            PHASE_VALUE[Piece.BISHOP][numPiece] = numPiece;
            PHASE_VALUE[Piece.KNIGHT][numPiece] = numPiece;
        }
    }
    private static final int QUEEN_VALUE = PIECE_VALUE[Piece.QUEEN][1][24];
    private static final int[] BISHOP_VALUE = new int[25];
    static {
        for(int phase = 0; phase < 25; phase ++) {
            BISHOP_VALUE[phase] = PIECE_VALUE[Piece.BISHOP][1][phase];
        }
    }
    private static final int[][][][] BONUS = new int[7][2][64][25];
    static {
        for(int type = Piece.KING; type <= Piece.PAWN; type ++) {
            for(int player = 0; player < 2; player ++) {
                for(int square = 0; square < 64; square ++) {
                    for(int phase = 0; phase < 25; phase ++) {
                        BONUS[type][player][square][phase] = Crit.BONUS[type][player][square][phase];
                    }
                }
            }
        }
    }
    private static final int[][] PASSED_PAWN_RANK_BONUS = new int[2][8];
    static {
        for(int player = 0; player < 2; player ++) {
            for(int rank = 0; rank < 8; rank ++) {
                PASSED_PAWN_RANK_BONUS[player][rank] = 50 * (player == 0 ? rank : 7 - rank);
            }
        }
    }
    private static final int[] ROOK_PROTECTS = new int[25];
    private static final int[] KING_BLOCKS_ROOK = new int[25];
    private static final int[] QUEEN_EARLY_DEVELOPMENT = new int[25];
    private static final int[] ROOK_EARLY_DEVELOPMENT = new int[25];
    private static final int[] ROOK_PAIR = new int[25];
    private static final int[] ROOK_OPEN_FILE = new int[25];
    private static final int[] ROOK_ON_QUEEN_FILE = new int[25];
    private static final int[] BISHOP_PAIR = new int[25];
    private static final int[] BISHOP_OUTPOST = new int[25];
    private static final int[] KNIGHT_PAIR = new int[25];
    private static final int[] KNIGHT_OUTPOST = new int[25];
    private static final int[] DOUBLED_PAWN = new int[25];
    private static final int[] WEAK_PAWN = new int[25];
    private static final int[] ISOLATED_PAWN = new int[25];
    private static final int[] PAWN_PROTECTS = new int[25];
    private static final int[] PASSED_PAWN_PHALANX = new int[25];
    static {
        for(int phase = 0; phase < 25; phase ++) {
            ROOK_PROTECTS[phase] = ((int[]) Crit.VALUE[Crit.ROOK_PROTECTS])[phase];
            KING_BLOCKS_ROOK[phase] = ((int[]) Crit.VALUE[Crit.KING_BLOCKS_ROOK])[phase];
            QUEEN_EARLY_DEVELOPMENT[phase] = ((int[]) Crit.VALUE[Crit.QUEEN_EARLY_DEVELOPMENT])[phase];
            ROOK_EARLY_DEVELOPMENT[phase] = ((int[]) Crit.VALUE[Crit.ROOK_EARLY_DEVELOPMENT])[phase];
            ROOK_PAIR[phase] = ((int[]) Crit.VALUE[Crit.ROOK_PAIR])[phase];
            ROOK_OPEN_FILE[phase] = ((int[]) Crit.VALUE[Crit.ROOK_OPEN_FILE])[phase];
            ROOK_ON_QUEEN_FILE[phase] = ((int[]) Crit.VALUE[Crit.ROOK_ON_QUEEN_FILE])[phase];
            BISHOP_PAIR[phase] = ((int[]) Crit.VALUE[Crit.BISHOP_PAIR])[phase];
            BISHOP_OUTPOST[phase] = ((int[]) Crit.VALUE[Crit.BISHOP_OUTPOST])[phase];
            KNIGHT_PAIR[phase] = ((int[]) Crit.VALUE[Crit.KNIGHT_PAIR])[phase];
            KNIGHT_OUTPOST[phase] = ((int[]) Crit.VALUE[Crit.KNIGHT_OUTPOST])[phase];
            DOUBLED_PAWN[phase] = ((int[]) Crit.VALUE[Crit.DOUBLED_PAWN])[phase];
            WEAK_PAWN[phase] = ((int[]) Crit.VALUE[Crit.WEAK_PAWN])[phase];
            ISOLATED_PAWN[phase] = ((int[]) Crit.VALUE[Crit.ISOLATED_PAWN])[phase];
            PAWN_PROTECTS[phase] = ((int[]) Crit.VALUE[Crit.PAWN_PROTECTS])[phase];
            PASSED_PAWN_PHALANX[phase] = ((int[]) Crit.VALUE[Crit.PASSED_PAWN_PHALANX])[phase];
        }
    }
    private static final int[] QUEEN_AFFECTS_KING_SAFETY = new int[9];
    private static final int[] ROOK_AFFECTS_KING_SAFETY = new int[6];
    private static final int[] BISHOP_AFFECTS_KING_SAFETY = new int[5];
    private static final int[] KNIGHT_AFFECTS_KING_SAFETY = new int[4];
    static {
        for(int attacks = 1; attacks < 9; attacks ++) {
            QUEEN_AFFECTS_KING_SAFETY[attacks] = ((int[]) Crit.VALUE[Crit.QUEEN_AFFECTS_KING_SAFETY])[attacks];
            if(attacks < 6) ROOK_AFFECTS_KING_SAFETY[attacks] = ((int[]) Crit.VALUE[Crit.ROOK_AFFECTS_KING_SAFETY])[attacks];
            if(attacks < 5) BISHOP_AFFECTS_KING_SAFETY[attacks] = ((int[]) Crit.VALUE[Crit.BISHOP_AFFECTS_KING_SAFETY])[attacks];
            if(attacks < 4) KNIGHT_AFFECTS_KING_SAFETY[attacks] = ((int[]) Crit.VALUE[Crit.KNIGHT_AFFECTS_KING_SAFETY])[attacks];
        }
    }
    private static final int[][] QUEEN_ENEMY_KING_DISTANCE = new int[15][25];
    private static final int[][] ROOK_ENEMY_KING_DISTANCE = new int[15][25];
    private static final int[][] BISHOP_ENEMY_KING_DISTANCE = new int[15][25];
    private static final int[][] KNIGHT_ENEMY_KING_DISTANCE = new int[15][25];
    private static final int[][] BISHOP_PROTECTOR = new int[15][25];
    private static final int[][] KNIGHT_PROTECTOR = new int[15][25];
    static {
        for(int distance = 1; distance < 15; distance ++) {
            for(int phase = 0; phase < 25; phase ++) {
                QUEEN_ENEMY_KING_DISTANCE[distance][phase] = ((int[][]) Crit.VALUE[Crit.QUEEN_ENEMY_KING_DISTANCE])[distance][phase];
                ROOK_ENEMY_KING_DISTANCE[distance][phase] = ((int[][])  Crit.VALUE[Crit.ROOK_ENEMY_KING_DISTANCE])[distance][phase];
                BISHOP_ENEMY_KING_DISTANCE[distance][phase] = ((int[][])  Crit.VALUE[Crit.BISHOP_ENEMY_KING_DISTANCE])[distance][phase];
                KNIGHT_ENEMY_KING_DISTANCE[distance][phase] = ((int[][])  Crit.VALUE[Crit.KNIGHT_ENEMY_KING_DISTANCE])[distance][phase];
                BISHOP_PROTECTOR[distance][phase] = ((int[][]) Crit.VALUE[Crit.BISHOP_PROTECTOR])[distance][phase];
                KNIGHT_PROTECTOR[distance][phase] = ((int[][]) Crit.VALUE[Crit.KNIGHT_PROTECTOR])[distance][phase];
            }
        }
    }
    private static final int[][] MOBILITY_QUEEN = new int[29][25];
    private static final int[][] MOBILITY_ROOK = new int[15][25];
    private static final int[][] MOBILITY_BISHOP = new int[15][25];
    private static final int[][] MOBILITY_KNIGHT = new int[9][25];
    static {
        for(int phase = 0; phase < 25; phase ++) {
            for(int mobility = 1; mobility < 29; mobility ++) {
                MOBILITY_QUEEN[mobility][phase] = ((int[][]) Crit.VALUE[Crit.MOBILITY_QUEEN])[mobility][phase];
                if(mobility < 15) {
                    MOBILITY_ROOK[mobility][phase] = ((int[][]) Crit.VALUE[Crit.MOBILITY_ROOK])[mobility][phase];
                    MOBILITY_BISHOP[mobility][phase] = ((int[][]) Crit.VALUE[Crit.MOBILITY_BISHOP])[mobility][phase];
                }    
                if(mobility < 9) {
                    MOBILITY_KNIGHT[mobility][phase] = ((int[][]) Crit.VALUE[Crit.MOBILITY_KNIGHT])[mobility][phase];
                }       
            }
        }
    }
    private static final int[][][] PAWN_SHIELD_CLOSE = new int[2][4][25];
    private static final int[][][] PAWN_SHIELD_FAR = new int[2][4][25];
    private static final int[][][] PAWN_STORM_CLOSE = new int[2][4][25];
    private static final int[][][] PAWN_STORM_FAR = new int[2][4][25];
    static {
        for(int side = 0; side < 2; side ++) {
            for(int numPawns = 0; numPawns < 4; numPawns ++) {
                for(int phase = 0; phase < 25; phase ++) {
                    PAWN_SHIELD_CLOSE[side][numPawns][phase] = ((int[][][]) Crit.VALUE[Crit.PAWN_SHIELD_CLOSE])[side][numPawns][phase];
                    PAWN_SHIELD_FAR[side][numPawns][phase] = ((int[][][]) Crit.VALUE[Crit.PAWN_SHIELD_FAR])[side][numPawns][phase];
                    PAWN_STORM_CLOSE[side][numPawns][phase] = ((int[][][]) Crit.VALUE[Crit.PAWN_STORM_CLOSE])[side][numPawns][phase];
                    PAWN_STORM_FAR[side][numPawns][phase] = ((int[][][]) Crit.VALUE[Crit.PAWN_STORM_FAR])[side][numPawns][phase];
                }
            }
        }
    }
    private static final int[][][] ROOK_PAWN = new int[3][9][25];
    private static final int[][][] KNIGHT_PAWN = new int[3][9][25];
    static {
        for(int numPiece = 1; numPiece < 3; numPiece ++) {
            for(int numPawns = 0; numPawns < 9; numPawns ++) {
                for(int phase = 0; phase < 25; phase ++) {
                    ROOK_PAWN[numPiece][numPawns][phase] = ((int[][][]) Crit.VALUE[Crit.ROOK_PAWN])[numPiece][numPawns][phase];
                    KNIGHT_PAWN[numPiece][numPawns][phase] = ((int[][][]) Crit.VALUE[Crit.ROOK_PAWN])[numPiece][numPawns][phase];
                }
            }
        }
    }
    private static final int[][][] BAD_BISHOP = new int[9][9][25];
    static {
        for(int numPawns = 0; numPawns < 9; numPawns ++) {
            for(int otherPawns = 0; otherPawns < 9; otherPawns ++) {
                for(int phase = 0; phase < 25; phase ++) {
                    BAD_BISHOP[numPawns][otherPawns][phase] = ((int[][][]) Crit.VALUE[Crit.BAD_BISHOP])[numPawns][otherPawns][phase];
                }
            }
        }
    }

    private Eval_SeedV3() {}

    private static int kingEval(int player, int kingSquare, int phase, int kingRank, int kingFile, long rookBitboard, long pawnBitboard, long otherPawnBitboard, int pieceMaterial, int otherPieceMaterial, int otherKingFile, int otherKingRank) {
        // init eval and king square bonus
        int eval = BONUS[Piece.KING][player][kingSquare][phase];
        // king on back rank evals - king blocks rook, rook protects king, pawn shield, opponent pawn storm
        if(kingRank == (player == Value.WHITE ? WHITE_BACK_RANK : BLACK_BACK_RANK)) {
            switch(kingFile) {
                case 0: {
                    if(((player == Value.WHITE ? 0x000000000000000eL : 0x0e00000000000000L) & rookBitboard) != 0L) eval += ROOK_PROTECTS[phase];
                    eval += PAWN_SHIELD_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                    break;
                }
                case 1: {
                    if(((player == Value.WHITE ? 0x000000000000000cL : 0x0c00000000000000L) & rookBitboard) != 0L) eval += ROOK_PROTECTS[phase];
                    if((Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0] & 0x0100000000000001L & rookBitboard) != 0L) eval += KING_BLOCKS_ROOK[phase];
                    eval += PAWN_SHIELD_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                    break;
                }
                case 2: {
                    if(((player == Value.WHITE ? 0x0000000000000008L : 0x0800000000000000L) & rookBitboard) != 0L) eval += ROOK_PROTECTS[phase];
                    if((Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0] & 0x0300000000000003L & rookBitboard) != 0L) eval += KING_BLOCKS_ROOK[phase];
                    eval += PAWN_SHIELD_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                    break;
                }
                case 3: {
                    if((Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0] & 0x0700000000000007L & rookBitboard) != 0L) eval += KING_BLOCKS_ROOK[phase];
                    break;
                }
                case 4: {
                    break;
                }
                case 5: {
                    if(((player == Value.WHITE ? 0x0000000000000010L : 0x1000000000000000L) & rookBitboard) != 0L) eval += ROOK_PROTECTS[phase];
                    if((Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0] & 0xe0000000000000e0L & rookBitboard) != 0L) eval += KING_BLOCKS_ROOK[phase];
                    eval += PAWN_SHIELD_CLOSE[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_KINGSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_KINGSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_KINGSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                    break;
                }
                case 6: {
                    if(((player == Value.WHITE ? 0x0000000000000030L : 0x3000000000000000L) & rookBitboard) != 0L) eval += ROOK_PROTECTS[phase];
                    if((Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0] & 0xc0000000000000c0L & rookBitboard) != 0L) eval += KING_BLOCKS_ROOK[phase];
                    eval += PAWN_SHIELD_CLOSE[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_KINGSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_KINGSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_KINGSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                    break;
                }
                case 7: {
                    if(((player == Value.WHITE ? 0x0000000000000070L : 0x7000000000000000L) & rookBitboard) != 0L) eval += ROOK_PROTECTS[phase];
                    eval += PAWN_SHIELD_CLOSE[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_KINGSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_KINGSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_KINGSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                    break;
                }
                default: break;
            }
        }
        // king distance
        if(pieceMaterial > otherPieceMaterial && pieceMaterial <= Piece.VALUE[Piece.QUEEN]) {
            final int otherFileDist = otherKingFile - CENTRE_FILE;
            final int otherFileDistSign = otherFileDist >> 31;
            final int otherRankDist = otherKingRank - CENTRE_RANK;
            final int otherRankDistSign = otherRankDist >> 31;
            final int opponentKingDistanceFromCenter = (otherFileDist ^ otherFileDistSign) - otherFileDistSign + (otherRankDist ^ otherRankDistSign) - otherRankDistSign;
            final int fileDist = kingFile - otherKingFile;
            final int fileDistSign = fileDist >> 31;
            final int rankDist = kingRank - otherKingRank;
            final int rankDistSign = rankDist >> 31;
            final int kingDistance = (fileDist ^ fileDistSign) - fileDistSign + (rankDist ^ rankDistSign) - rankDistSign;
            final int opponentKingEdgePenalty = opponentKingDistanceFromCenter * EDGE_WEIGHT;
            final int kingProximityBonus = (14 - kingDistance) * PROXIMITY_WEIGHT;
            eval += ((opponentKingEdgePenalty + kingProximityBonus) * (MAX_PHASE - phase)) / MAX_PHASE;
        }
        return eval;
    }

    private static int kingEvalWithLogging(int player, int kingSquare, int phase, int kingRank, int kingFile, long rookBitboard, long pawnBitboard, long otherPawnBitboard, int pieceMaterial, int otherPieceMaterial, int otherKingFile, int otherKingRank, Logger logger) {
        logger.setContext("kingEval " + (player == 0 ? "White" : "Black"));
        logger.log("*** KING EVAL ***");
        // init eval and king square bonus
        int eval = BONUS[Piece.KING][player][kingSquare][phase];
        logger.log("bonus [" + eval + "]");
        int log = 0;
        // king on back rank evals - king blocks rook, rook protects king, pawn shield, opponent pawn storm
        if(kingRank == (player == Value.WHITE ? WHITE_BACK_RANK : BLACK_BACK_RANK)) {
            switch(kingFile) {
                case 0: {
                    logger.log("back rank file 0");
                    if(((player == Value.WHITE ? 0x000000000000000eL : 0x0e00000000000000L) & rookBitboard) != 0L) {
                        log = ROOK_PROTECTS[phase];
                        logger.log("rook protects [" + log + "]");
                        eval += log;
                    }
                    log =   PAWN_SHIELD_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                    logger.log("PAWN_SHIELD and PAWN_STORM QYEENSIDE [" + log + "]");
                    eval += log;
                    break;
                }
                case 1: {
                    logger.log("back rank file 1");
                    if(((player == Value.WHITE ? 0x000000000000000cL : 0x0c00000000000000L) & rookBitboard) != 0L) {
                        log = ROOK_PROTECTS[phase];
                        logger.log("rook protects [" + log + "]");
                        eval += log;
                    }
                    if((Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0] & 0x0100000000000001L & rookBitboard) != 0L) {
                        log = KING_BLOCKS_ROOK[phase];
                        logger.log("king blocks rook [" + log + "]");
                        eval += log;
                    }
                    log =   PAWN_SHIELD_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                    logger.log("PAWN_SHIELD and PAWN_STORM QYEENSIDE [" + log + "]");
                    eval += log;
                    break;
                }
                case 2: {
                    logger.log("back rank file 2");
                    if(((player == Value.WHITE ? 0x0000000000000008L : 0x0800000000000000L) & rookBitboard) != 0L) {
                        log = ROOK_PROTECTS[phase];
                        logger.log("rook protects [" + log + "]");
                        eval += log;
                    }
                    if((Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0] & 0x0300000000000003L & rookBitboard) != 0L) {
                        log = KING_BLOCKS_ROOK[phase];
                        logger.log("king blocks rook [" + log + "]");
                        eval += log;
                    }
                    log =   PAWN_SHIELD_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                    logger.log("PAWN_SHIELD and PAWN_STORM QYEENSIDE [" + log + "]");
                    eval += log;
                    break;
                }
                case 3: {
                    logger.log("back rank file 3");
                    if((Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0] & 0x0700000000000007L & rookBitboard) != 0L) {
                        log = KING_BLOCKS_ROOK[phase];
                        logger.log("king blocks rook [" + log + "]");
                        eval += log;
                    }
                    break;
                }
                case 4: {
                    break;
                }
                case 5: {
                    logger.log("back rank file 5");
                    if(((player == Value.WHITE ? 0x0000000000000010L : 0x1000000000000000L) & rookBitboard) != 0L) {
                        log = ROOK_PROTECTS[phase];
                        logger.log("rook protects [" + log + "]");
                        eval += log;
                    }
                    if((Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0] & 0xe0000000000000e0L & rookBitboard) != 0L) {
                        log = KING_BLOCKS_ROOK[phase];
                        logger.log("king blocks rook [" + log + "]");
                        eval += log;
                    }
                    log =   PAWN_SHIELD_CLOSE[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_KINGSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_KINGSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_KINGSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                    logger.log("PAWN_SHIELD and PAWN_STORM QYEENSIDE [" + log + "]");
                    eval += log;
                    break;
                }
                case 6: {
                    logger.log("back rank file 6");
                    if(((player == Value.WHITE ? 0x0000000000000030L : 0x3000000000000000L) & rookBitboard) != 0L) {
                        log = ROOK_PROTECTS[phase];
                        logger.log("rook protects [" + log + "]");
                        eval += log;
                    }
                    if((Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0] & 0xc0000000000000c0L & rookBitboard) != 0L) {
                        log = KING_BLOCKS_ROOK[phase];
                        logger.log("king blocks rook [" + log + "]");
                        eval += log;
                    }
                    log =   PAWN_SHIELD_CLOSE[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_KINGSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_KINGSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_KINGSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                    logger.log("PAWN_SHIELD and PAWN_STORM QYEENSIDE [" + log + "]");
                    eval += log;
                    break;
                }
                case 7: {
                    logger.log("back rank file 6");
                    if(((player == Value.WHITE ? 0x0000000000000070L : 0x7000000000000000L) & rookBitboard) != 0L) {
                        log = ROOK_PROTECTS[phase];
                        logger.log("rook protects [" + log + "]");
                        eval += log;
                    }
                    log =   PAWN_SHIELD_CLOSE[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_KINGSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_KINGSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_KINGSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                    logger.log("PAWN_SHIELD and PAWN_STORM QYEENSIDE [" + log + "]");
                    eval += log;
                    break;
                }
                default: break;
            }
        }
        // king distance
        if(pieceMaterial > otherPieceMaterial && pieceMaterial <= Piece.VALUE[Piece.QUEEN]) {
            final int otherFileDist = otherKingFile - CENTRE_FILE;
            final int otherFileDistSign = otherFileDist >> 31;
            final int otherRankDist = otherKingRank - CENTRE_RANK;
            final int otherRankDistSign = otherRankDist >> 31;
            final int opponentKingDistanceFromCenter = (otherFileDist ^ otherFileDistSign) - otherFileDistSign + (otherRankDist ^ otherRankDistSign) - otherRankDistSign;
            final int fileDist = kingFile - otherKingFile;
            final int fileDistSign = fileDist >> 31;
            final int rankDist = kingRank - otherKingRank;
            final int rankDistSign = rankDist >> 31;
            final int kingDistance = (fileDist ^ fileDistSign) - fileDistSign + (rankDist ^ rankDistSign) - rankDistSign;
            final int opponentKingEdgePenalty = opponentKingDistanceFromCenter * EDGE_WEIGHT;
            final int kingProximityBonus = (14 - kingDistance) * PROXIMITY_WEIGHT;
            log = ((opponentKingEdgePenalty + kingProximityBonus) * (MAX_PHASE - phase)) / MAX_PHASE;
            logger.log("endgame king distance [" + log + "]");
            eval += log;
        }
        logger.clearContext();
        return eval;
    }
    
    private static long queenEval(long bitboard, int numQueens, int phase, int player, long bishopBitboard, long knightBitboard, long allOccupancy, long occupancy, int otherKingRank, int otherKingFile, int otherKingSquare, long otherKingRing) {
        // init eval and material value
        int eval = PIECE_VALUE[Piece.QUEEN][numQueens][phase];
        int safety = 0;
        // early development
        if((bitboard & Bitboard.BB[Bitboard.QUEEN_START_POSITION_PLAYER0 + player][0]) == 0L &&
           (bishopBitboard & Bitboard.BB[Bitboard.BISHOP_START_POSITION_PLAYER0 + player][0]) != 0L &&
           (knightBitboard & Bitboard.BB[Bitboard.KNIGHT_START_POSITION_PLAYER0 + player][0]) != 0L) eval += QUEEN_EARLY_DEVELOPMENT[phase];
        // cache arrays for faster access inside loop
        final int[][] QUEEN_BONUS = BONUS[Piece.QUEEN][player];
        final int[][] QUEEN_MOBILITY = MOBILITY_QUEEN;
        final int[][] QUEEN_DISTANCE = QUEEN_ENEMY_KING_DISTANCE;
        final int[] QUEEN_SAFETY = QUEEN_AFFECTS_KING_SAFETY;
        final int[] lsb = LSB;
        // loop over queens
        while(bitboard != 0L) {
            final long b = bitboard & -bitboard;
            bitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            // piece square bonus
            eval += QUEEN_BONUS[square][phase];
            // mobility
            final long queenAttacks = Magic.queenMoves(square, allOccupancy) & ~occupancy;
            eval += QUEEN_MOBILITY[Long.bitCount(queenAttacks)][phase];
            // other king distance
            final int fileDist = (square & Value.FILE) - otherKingFile;
            final int fileDistSign = fileDist >> 31;
            final int rankDist = (square >>> Value.RANK_SHIFT) - otherKingRank;
            final int rankDistSign = rankDist >> 31;
            eval += QUEEN_DISTANCE[(fileDist ^ fileDistSign) - fileDistSign + (rankDist ^ rankDistSign) - rankDistSign][phase];
            // other king safety
            safety += QUEEN_SAFETY[Long.bitCount(queenAttacks & otherKingRing)];
        }
        return ((long) eval << EVAL_SHIFT) | (safety & SAFETY_BITS);
    }

    private static long queenEvalWithLogging(long bitboard, int numQueens, int phase, int player, long bishopBitboard, long knightBitboard, long allOccupancy, long occupancy, int otherKingRank, int otherKingFile, int otherKingSquare, long otherKingRing, Logger logger) {
        logger.setContext("queenEval " + (player == 0 ? "White" : "Black"));
        logger.log("*** QUEEN EVAL ***");
        // init eval and material value
        int eval = PIECE_VALUE[Piece.QUEEN][numQueens][phase];
        logger.log("queen value [" + eval + "]");
        int safety = 0;
        int log = 0;
        // early development
        if((bitboard & Bitboard.BB[Bitboard.QUEEN_START_POSITION_PLAYER0 + player][0]) == 0L &&
           (bishopBitboard & Bitboard.BB[Bitboard.BISHOP_START_POSITION_PLAYER0 + player][0]) != 0L &&
           (knightBitboard & Bitboard.BB[Bitboard.KNIGHT_START_POSITION_PLAYER0 + player][0]) != 0L) {
            log = QUEEN_EARLY_DEVELOPMENT[phase];
            logger.log("QUEEN_EARLY_DEVELOPMENT [" + log + "]");
            eval += log;
           }
        // cache arrays for faster access inside loop
        final int[][] QUEEN_BONUS = BONUS[Piece.QUEEN][player];
        final int[][] QUEEN_MOBILITY = MOBILITY_QUEEN;
        final int[][] QUEEN_DISTANCE = QUEEN_ENEMY_KING_DISTANCE;
        final int[] QUEEN_SAFETY = QUEEN_AFFECTS_KING_SAFETY;
        final int[] lsb = LSB;
        // loop over queens
        while(bitboard != 0L) {
            final long b = bitboard & -bitboard;
            bitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            logger.log("square [" + square + "]");
            // piece square bonus
            logger.log("bonus [" + QUEEN_BONUS[square][phase] + "]");
            eval += QUEEN_BONUS[square][phase];
            // mobility
            final long queenAttacks = Magic.queenMoves(square, allOccupancy) & ~occupancy;
            eval += QUEEN_MOBILITY[Long.bitCount(queenAttacks)][phase];
            logger.log("mobility [" + QUEEN_MOBILITY[Long.bitCount(queenAttacks)][phase] + "]");
            // other king distance
            final int fileDist = (square & Value.FILE) - otherKingFile;
            final int fileDistSign = fileDist >> 31;
            final int rankDist = (square >>> Value.RANK_SHIFT) - otherKingRank;
            final int rankDistSign = rankDist >> 31;
            eval += QUEEN_DISTANCE[(fileDist ^ fileDistSign) - fileDistSign + (rankDist ^ rankDistSign) - rankDistSign][phase];
            logger.log("other king distance [" + QUEEN_DISTANCE[(fileDist ^ fileDistSign) - fileDistSign + (rankDist ^ rankDistSign) - rankDistSign][phase] + "]");
            // other king safety
            log = QUEEN_SAFETY[Long.bitCount(queenAttacks & otherKingRing)];
            if(log > 0) {
                safety += log;
                logger.log("other king safety [" + log + "]");
            }
        }
        logger.clearContext();
        return ((long) eval << EVAL_SHIFT) | (safety & SAFETY_BITS);
    }

    private static long rookEval(long bitboard, int numRooks, boolean castling, int phase, int player, long kingBitboard, long pawnBitboard, long allOccupancy, long occupancy, long otherPawnBitboard, long otherQueenBitboard, int otherKingRank, int otherKingFile, int otherKingSquare, long otherKingRing) {
        // init and material value
        int eval = PIECE_VALUE[Piece.ROOK][numRooks][phase];
        int safety = 0;
        // early development
        if(Long.bitCount(bitboard & Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0]) < 2 && castling) eval += ROOK_EARLY_DEVELOPMENT[phase];
        // rook pair
        eval += numRooks > 1 ? ROOK_PAIR[phase] : 0;
        // rooks and pawns
        final int numPawns = Long.bitCount(pawnBitboard);
        eval += ROOK_PAWN[numRooks > 2 ? 2 : numRooks][numPawns > 8 ? 8 : numPawns][phase];
        // cache arrays for faster access inside loop
        final int[][] ROOK_BONUS = BONUS[Piece.ROOK][player];
        final int[][] ROOK_MOBILITY = MOBILITY_ROOK;
        final long[] FILE = FILE_BITBOARD;
        final int OPEN_FILE = ROOK_OPEN_FILE[phase];
        final int ROOK_QUEEN_FILE = ROOK_ON_QUEEN_FILE[phase];
        final int[][] ROOK_DISTANCE = ROOK_ENEMY_KING_DISTANCE;
        final int[] ROOK_SAFETY = ROOK_AFFECTS_KING_SAFETY;
        final int[] lsb = LSB;
        // loop over rooks
        while(bitboard != 0L) {
            final long b = bitboard & -bitboard;
            bitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            // piece square bonus
            eval += ROOK_BONUS[square][phase];
            // mobility
            final long rookAttacks = Magic.rookMoves(square, allOccupancy) & ~occupancy;
            eval += ROOK_MOBILITY[Long.bitCount(rookAttacks)][phase];
            // rook open file
            final int rookFile = square & Value.FILE;
            final long rookFileBitboard = FILE[rookFile];
            eval += ((pawnBitboard & rookFileBitboard) == 0L ? OPEN_FILE : 0) + ((otherPawnBitboard & rookFileBitboard) == 0L ? OPEN_FILE : 0);
            // rook opposed enemy queen
            eval += (otherQueenBitboard & rookFileBitboard) != 0L ? ROOK_QUEEN_FILE : 0;
            // other king distance
            final int fileDist = rookFile - otherKingFile;
            final int fileDistSign = fileDist >> 31;
            final int rankDist = (square >>> 3) - otherKingRank;
            final int rankDistSign = rankDist >> 31;
            eval += ROOK_DISTANCE[(fileDist ^ fileDistSign) - fileDistSign + (rankDist ^ rankDistSign) - rankDistSign][phase];
            // other king safety
            safety += ROOK_SAFETY[Long.bitCount(rookAttacks & otherKingRing)];
        }
        return ((long) eval << EVAL_SHIFT) | (safety & SAFETY_BITS);
    }

    private static long rookEvalWithLogging(long bitboard, int numRooks, boolean castling, int phase, int player, long kingBitboard, long pawnBitboard, long allOccupancy, long occupancy, long otherPawnBitboard, long otherQueenBitboard, int otherKingRank, int otherKingFile, int otherKingSquare, long otherKingRing, Logger logger) {
        logger.setContext("rookEval " + (player == 0 ? "White" : "Black"));
        logger.log("*** ROOK EVAL ***");
        // init and material value
        int eval = PIECE_VALUE[Piece.ROOK][numRooks][phase];
        logger.log("rook value [" + eval + "]");
        int safety = 0;
        int log = 0;
        // early development
        if(Long.bitCount(bitboard & Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0]) < 2 && castling) {
            log = ROOK_EARLY_DEVELOPMENT[phase];
            logger.log("ROOK_EARLY_DEVELOPMENT [" + log + "]");
            eval += log;
        }
        // rook pair
        if(numRooks > 1) {
            log = ROOK_PAIR[phase];
            logger.log("ROOK_PAIR [" + log + "]");
            eval += log;
        }
        // rooks and pawns
        final int numPawns = Long.bitCount(pawnBitboard);
        log = ROOK_PAWN[numRooks > 2 ? 2 : numRooks][numPawns > 8 ? 8 : numPawns][phase];
        logger.log("ROOKS_&_PAWNS [" + log + "]");
        eval += log;
        // cache arrays for faster access inside loop
        final int[][] ROOK_BONUS = BONUS[Piece.ROOK][player];
        final int[][] ROOK_MOBILITY = MOBILITY_ROOK;
        final long[] FILE = FILE_BITBOARD;
        final int OPEN_FILE = ROOK_OPEN_FILE[phase];
        final int ROOK_QUEEN_FILE = ROOK_ON_QUEEN_FILE[phase];
        final int[][] ROOK_DISTANCE = ROOK_ENEMY_KING_DISTANCE;
        final int[] ROOK_SAFETY = ROOK_AFFECTS_KING_SAFETY;
        final int[] lsb = LSB;
        // loop over rooks
        while(bitboard != 0L) {
            final long b = bitboard & -bitboard;
            bitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            logger.log("square [" + square + "]");
            // piece square bonus
            log = ROOK_BONUS[square][phase];
            logger.log("bonus [" + log + "]");
            eval += log;
            // mobility
            final long rookAttacks = Magic.rookMoves(square, allOccupancy) & ~occupancy;
            log = ROOK_MOBILITY[Long.bitCount(rookAttacks)][phase];
            eval += log;
            logger.log("mobility [" + log + "]");
            // rook open file
            final int rookFile = square & Value.FILE;
            final long rookFileBitboard = FILE[rookFile];
            log = 0;
            if((pawnBitboard & rookFileBitboard) == 0L) {
                log += OPEN_FILE;
            }
            if((otherPawnBitboard & rookFileBitboard) == 0L) {
                log += OPEN_FILE;
            }
            if(log > 0) {
                logger.log("rook open file [" + log + "]");
                eval += log;
            }
            log = 0;
            // rook opposed enemy queen
            if((otherQueenBitboard & rookFileBitboard) != 0L) {
                log += ROOK_QUEEN_FILE;
                logger.log("rook opposes queen file [" + log + "]");
                eval += log;
            }
            // other king distance
            final int fileDist = rookFile - otherKingFile;
            final int fileDistSign = fileDist >> 31;
            final int rankDist = (square >>> 3) - otherKingRank;
            final int rankDistSign = rankDist >> 31;
            log = ROOK_DISTANCE[(fileDist ^ fileDistSign) - fileDistSign + (rankDist ^ rankDistSign) - rankDistSign][phase];
            logger.log("other king distance [" + log + "]");
            eval += log;
            // other king safety
            log = ROOK_SAFETY[Long.bitCount(rookAttacks & otherKingRing)];
            if(log > 0) {
                logger.log("other king safety [" + log + "]");
                safety += log;
            }
        }
        logger.clearContext();
        return ((long) eval << EVAL_SHIFT) | (safety & SAFETY_BITS);
    }

    private static long bishopEval(long bitboard, int numBishops, int phase, int player, int other, long allOccupancy, long occupancy, long pawnBitboard, long otherPawnBitboard, long lightSquares, int kingRank, int kingFile, int otherKingRank, int otherKingFile, int otherKingSquare, long otherKingRing) {
        // init eval and material value
        int eval = PIECE_VALUE[Piece.BISHOP][numBishops][phase];
        int safety = 0;
        // bishop pair
        eval += numBishops > 1 ? BISHOP_PAIR[phase] : 0;
        // cache arrays for faster access inside loop
        final int[][] BISHOP_BONUS = BONUS[Piece.BISHOP][player];
        final int[][] BISHOP_MOBILITY = MOBILITY_BISHOP;
        final long[] ATTACKS = PAWN_ATTACKS[other];
        final long[] PASSED_FILES = PASSED_PAWNS_FILES[player];
        final long[] FORWARD = FORWARD_RANKS[player];
        final int OUTPOST = BISHOP_OUTPOST[phase];
        final int[][][] BAD = BAD_BISHOP;
        final int[][] PROTECTS = BISHOP_PROTECTOR;
        final int[][] BISHOP_DISTANCE = BISHOP_ENEMY_KING_DISTANCE;
        final int[] BISHOP_SAFETY = BISHOP_AFFECTS_KING_SAFETY;
        final int[] lsb = LSB;
        // loop over bishops
        while(bitboard != 0L) {
            final long b = bitboard & -bitboard;
            bitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            // piece square bonus
            eval += BISHOP_BONUS[square][phase];
            // mobility
            final long bishopAttacks = Magic.bishopMoves(square, allOccupancy) & ~occupancy;
            eval += BISHOP_MOBILITY[Long.bitCount(bishopAttacks)][phase];
            // outpost
            final int bishopFile = square & Value.FILE;
            final int bishopRank = square >>> Value.RANK_SHIFT;
            if((ATTACKS[square] & pawnBitboard) != 0L) {
                if((PASSED_FILES[bishopFile] & FORWARD[bishopRank] & otherPawnBitboard) == 0L) eval += OUTPOST;
            }
            // bad bishop
            final long squareColorBitboard = (square >>> 3 & 1) == (square & 1) ? ~lightSquares : lightSquares;
            eval += BAD[Long.bitCount(pawnBitboard & squareColorBitboard)][Long.bitCount(otherPawnBitboard & squareColorBitboard)][phase];
            // own king distance
            final int fileDist = bishopFile - kingFile;
            final int fileDistSign = fileDist >> 31;
            final int rankDist = bishopRank - kingRank;
            final int rankDistSign = rankDist >> 31;
            eval += PROTECTS[(fileDist ^ fileDistSign) - fileDistSign + (rankDist ^ rankDistSign) - rankDistSign][phase];
            // other king distance
            final int otherFileDist = bishopFile - otherKingFile;
            final int otherFileDistSign = otherFileDist >> 31;
            final int otherRankDist = bishopRank - otherKingRank;
            final int otherRankDistSign = otherRankDist >> 31;
            eval += BISHOP_DISTANCE[(otherFileDist ^ otherFileDistSign) - otherFileDistSign + (otherRankDist ^ otherRankDistSign) - otherRankDistSign][phase];
            // other king safety
            safety += BISHOP_SAFETY[Long.bitCount(bishopAttacks & otherKingRing)];
        }
        return ((long) eval << EVAL_SHIFT) | (safety & SAFETY_BITS);
    }

    private static long bishopEvalWithLogging(long bitboard, int numBishops, int phase, int player, int other, long allOccupancy, long occupancy, long pawnBitboard, long otherPawnBitboard, long lightSquares, int kingRank, int kingFile, int otherKingRank, int otherKingFile, int otherKingSquare, long otherKingRing, Logger logger) {
        logger.setContext("bishopEval " + (player == 0 ? "White" : "Black"));
        logger.log("*** BISHOP EVAL ***");
        // init eval and material value
        int eval = PIECE_VALUE[Piece.BISHOP][numBishops][phase];
        logger.log("bishop value [" + eval + "]");
        int safety = 0;
        int log = 0;
        // bishop pair
        if(numBishops > 1) {
            log = BISHOP_PAIR[phase];
            logger.log("BISHOP_PAIR [" + log + "]");
            eval += log;
        }
        // cache arrays for faster access inside loop
        final int[][] BISHOP_BONUS = BONUS[Piece.BISHOP][player];
        final int[][] BISHOP_MOBILITY = MOBILITY_BISHOP;
        final long[] ATTACKS = PAWN_ATTACKS[other];
        final long[] PASSED_FILES = PASSED_PAWNS_FILES[player];
        final long[] FORWARD = FORWARD_RANKS[player];
        final int OUTPOST = BISHOP_OUTPOST[phase];
        final int[][][] BAD = BAD_BISHOP;
        final int[][] PROTECTS = BISHOP_PROTECTOR;
        final int[][] BISHOP_DISTANCE = BISHOP_ENEMY_KING_DISTANCE;
        final int[] BISHOP_SAFETY = BISHOP_AFFECTS_KING_SAFETY;
        final int[] lsb = LSB;
        // loop over bishops
        while(bitboard != 0L) {
            final long b = bitboard & -bitboard;
            bitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            logger.log("square [" + square + "]");
            // piece square bonus
            log = BISHOP_BONUS[square][phase];
            logger.log("bonus [" + log + "]");
            eval += log;
            // mobility
            final long bishopAttacks = Magic.bishopMoves(square, allOccupancy) & ~occupancy;
            log = BISHOP_MOBILITY[Long.bitCount(bishopAttacks)][phase];
            logger.log("mobility [" + log + "]");
            eval += log;
            // outpost
            final int bishopFile = square & Value.FILE;
            final int bishopRank = square >>> Value.RANK_SHIFT;
            if((ATTACKS[square] & pawnBitboard) != 0L && (PASSED_FILES[bishopFile] & FORWARD[bishopRank] & otherPawnBitboard) == 0L) {
                    log = OUTPOST;
                    logger.log("outpost [" + log + "]");
                    eval += log;
            }
            // bad bishop
            final long squareColorBitboard = (square >>> 3 & 1) == (square & 1) ? ~lightSquares : lightSquares;
            log = BAD[Long.bitCount(pawnBitboard & squareColorBitboard)][Long.bitCount(otherPawnBitboard & squareColorBitboard)][phase];
            logger.log("bad bishop [" + log + "]");
            eval += log;
            // own king distance
            final int fileDist = bishopFile - kingFile;
            final int fileDistSign = fileDist >> 31;
            final int rankDist = bishopRank - kingRank;
            final int rankDistSign = rankDist >> 31;
            log = PROTECTS[(fileDist ^ fileDistSign) - fileDistSign + (rankDist ^ rankDistSign) - rankDistSign][phase];
            logger.log("protector [" + log + "]");
            eval += log;
            // other king distance
            final int otherFileDist = bishopFile - otherKingFile;
            final int otherFileDistSign = otherFileDist >> 31;
            final int otherRankDist = bishopRank - otherKingRank;
            final int otherRankDistSign = otherRankDist >> 31;
            log = BISHOP_DISTANCE[(otherFileDist ^ otherFileDistSign) - otherFileDistSign + (otherRankDist ^ otherRankDistSign) - otherRankDistSign][phase];
            logger.log("other king distance [" + log + "]");
            eval += log;
            // other king safety
            log = BISHOP_SAFETY[Long.bitCount(bishopAttacks & otherKingRing)];
            if(log > 0) {
                logger.log("other king safety [" + log + "]");
                safety += log;
            }
        }
        logger.clearContext();
        return ((long) eval << EVAL_SHIFT) | (safety & SAFETY_BITS);
    }

    private static long knightEval(long bitboard, int numKnights, int phase, long pawnBitboard, int player, int other, long occupancy, long otherPawnBitboard, int kingRank, int kingFile, int otherKingRank, int otherKingFile, int otherKingSquare, long otherKingRing) {
        // init eval and material value
        int eval = PIECE_VALUE[Piece.KNIGHT][numKnights][phase];
        int safety = 0;
        // knight pair
        eval += numKnights > 1 ? KNIGHT_PAIR[phase] : 0;
        // knight and pawns
        final int numPawns = Long.bitCount(pawnBitboard);
        eval += KNIGHT_PAWN[numKnights > 2 ? 2 : numKnights][numPawns > 8 ? 8 : numPawns][phase];
        // cache arrays for faster access inside loop
        final int[][] KNIGHT_BONUS = BONUS[Piece.KNIGHT][player];
        final long[] LEAP = LEAP_ATTACKS_BITBOARD;
        final int[][] KNIGHT_MOBILITY = MOBILITY_KNIGHT;
        final long[] ATTACKS = PAWN_ATTACKS[other];
        final long[] PASSED_FILES = PASSED_PAWNS_FILES[player];
        final long[] FORWARD = FORWARD_RANKS[player];
        final int OUTPOST = KNIGHT_OUTPOST[phase];
        final int[][] PROTECTS = KNIGHT_PROTECTOR;
        final int[][] KNIGHT_DISTANCE = KNIGHT_ENEMY_KING_DISTANCE;
        final int[] KNIGHT_SAFETY = KNIGHT_AFFECTS_KING_SAFETY;
        final int[] lsb = LSB;
        // loop over knights
        while(bitboard != 0L) {
            final long b = bitboard & -bitboard;
            bitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            // piece square bonus
            eval += KNIGHT_BONUS[square][phase];
            // mobility
            final long knightAttacks = LEAP[square] & ~occupancy;
            eval += KNIGHT_MOBILITY[Long.bitCount(knightAttacks)][phase];
            // outpost
            final int knightFile = square & Value.FILE;
            final int knightRank = square >>> Value.RANK_SHIFT;
            if((ATTACKS[square] & pawnBitboard) != 0L) {
                if((PASSED_FILES[knightFile] & FORWARD[knightRank] & otherPawnBitboard) == 0L) eval += OUTPOST;
            }
            // own king distance
            final int fileDist = knightFile - kingFile;
            final int fileDistSign = fileDist >> 31;
            final int rankDist = knightRank - kingRank;
            final int rankDistSign = rankDist >> 31;
            eval += PROTECTS[(fileDist ^ fileDistSign) - fileDistSign + (rankDist ^ rankDistSign) - rankDistSign][phase];
            // other king distance
            final int otherFileDist = knightFile - otherKingFile;
            final int otherFileDistSign = otherFileDist >> 31;
            final int otherRankDist = knightRank - otherKingRank;
            final int otherRankDistSign = otherRankDist >> 31;
            eval += KNIGHT_DISTANCE[(otherFileDist ^ otherFileDistSign) - otherFileDistSign + (otherRankDist ^ otherRankDistSign) - otherRankDistSign][phase];
            // other king safety
            safety += KNIGHT_SAFETY[Long.bitCount(knightAttacks & otherKingRing)];
        }
        return ((long) eval << EVAL_SHIFT) | (safety & SAFETY_BITS);
    }

    private static long knightEvalWithLogging(long bitboard, int numKnights, int phase, long pawnBitboard, int player, int other, long occupancy, long otherPawnBitboard, int kingRank, int kingFile, int otherKingRank, int otherKingFile, int otherKingSquare, long otherKingRing, Logger logger) {
        logger.setContext("knightEval " + (player == 0 ? "White" : "Black"));
        logger.log("*** KNIGHT EVAL ***");
        // init eval and material value
        int eval = PIECE_VALUE[Piece.KNIGHT][numKnights][phase];
        logger.log("knight value [" + eval + "]");
        int safety = 0;
        int log = 0;
        // knight pair
        if(numKnights > 1) {
            log = KNIGHT_PAIR[phase];
            logger.log("knight pair [" + log + "]");
            eval += log;
        }
        // knight and pawns
        final int numPawns = Long.bitCount(pawnBitboard);
        log = KNIGHT_PAWN[numKnights > 2 ? 2 : numKnights][numPawns > 8 ? 8 : numPawns][phase];
        logger.log("knights and pawns [" + log + "]");
        eval += log;
        // cache arrays for faster access inside loop
        final int[][] KNIGHT_BONUS = BONUS[Piece.KNIGHT][player];
        final long[] LEAP = LEAP_ATTACKS_BITBOARD;
        final int[][] KNIGHT_MOBILITY = MOBILITY_KNIGHT;
        final long[] ATTACKS = PAWN_ATTACKS[other];
        final long[] PASSED_FILES = PASSED_PAWNS_FILES[player];
        final long[] FORWARD = FORWARD_RANKS[player];
        final int OUTPOST = KNIGHT_OUTPOST[phase];
        final int[][] PROTECTS = KNIGHT_PROTECTOR;
        final int[][] KNIGHT_DISTANCE = KNIGHT_ENEMY_KING_DISTANCE;
        final int[] KNIGHT_SAFETY = KNIGHT_AFFECTS_KING_SAFETY;
        final int[] lsb = LSB;
        // loop over knights
        while(bitboard != 0L) {
            final long b = bitboard & -bitboard;
            bitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            logger.log("square [" + square + "]");
            // piece square bonus
            log = KNIGHT_BONUS[square][phase];
            logger.log("bonus [" + log + "]");
            eval += log;
            // mobility
            final long knightAttacks = LEAP[square] & ~occupancy;
            log = KNIGHT_MOBILITY[Long.bitCount(knightAttacks)][phase];
            logger.log("mobility [" + log + "]");
            eval += log;
            // outpost
            final int knightFile = square & Value.FILE;
            final int knightRank = square >>> Value.RANK_SHIFT;
            if((ATTACKS[square] & pawnBitboard) != 0L && (PASSED_FILES[knightFile] & FORWARD[knightRank] & otherPawnBitboard) == 0L) {
                log = OUTPOST;
                logger.log("outpost [" + log + "]");
                eval += log;
            }
            // own king distance
            final int fileDist = knightFile - kingFile;
            final int fileDistSign = fileDist >> 31;
            final int rankDist = knightRank - kingRank;
            final int rankDistSign = rankDist >> 31;
            log = PROTECTS[(fileDist ^ fileDistSign) - fileDistSign + (rankDist ^ rankDistSign) - rankDistSign][phase];
            logger.log("protector [" + log + "]");
            eval += log;
            // other king distance
            final int otherFileDist = knightFile - otherKingFile;
            final int otherFileDistSign = otherFileDist >> 31;
            final int otherRankDist = knightRank - otherKingRank;
            final int otherRankDistSign = otherRankDist >> 31;
            log = KNIGHT_DISTANCE[(otherFileDist ^ otherFileDistSign) - otherFileDistSign + (otherRankDist ^ otherRankDistSign) - otherRankDistSign][phase];
            logger.log("other king distance [" + log + "]");
            eval += log;
            // other king safety
            log = KNIGHT_SAFETY[Long.bitCount(knightAttacks & otherKingRing)];
            if(log > 0) {
                logger.log("other king safety [" + log + "]");
                safety += log;
            }
        }
        logger.clearContext();
        return ((long) eval << EVAL_SHIFT) | (safety & SAFETY_BITS);
    }

    private static int pawnEval(long bitboard, int phase, int player, long knightBishopBitboard, long otherPawnBitboard, int materialValuePieces, int kingRank, int kingFile, int otherKingRank, int otherKingFile, int otherMaterialValuePieces, int thisPlayer, long kingBitboard) {
        // init eval and material value
        final int numPawns = Long.bitCount(bitboard);
        int eval = PIECE_VALUE[Piece.PAWN][numPawns][phase];
        long originalBitboard = bitboard;
        // cache arrays for faster access inside loop
        final int[][] PAWN_BONUS = BONUS[Piece.PAWN][player];
        final long[] FILE = FILE_BITBOARD;
        final int DOUBLED = DOUBLED_PAWN[phase];
        final long[] FORWARD = FORWARD_RANKS[player];
        final long[] OTHER_FORWARD = FORWARD_RANKS[1 ^ player];
        final int WEAK = WEAK_PAWN[phase];
        final int ISOLATED = ISOLATED_PAWN[phase];
        final long[] ATTACKS = PAWN_ATTACKS[player];
        final int PROTECTS = PAWN_PROTECTS[phase];
        final int[] RANK_BONUS = PASSED_PAWN_RANK_BONUS[player];
        final long[] RANK = RANK_BITBOARD;
        final int PHALANX = PASSED_PAWN_PHALANX[phase];
        final int VALUE_BISHOP = BISHOP_VALUE[phase];
        final int[] lsb = LSB;
        // loop over pawns
        while(bitboard != 0L) {
            final long b = bitboard & -bitboard;
            bitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            // piece square bonus
            eval += PAWN_BONUS[square][phase];
            // doubled pawns
            final int pawnFile = square & Value.FILE;
            final long pawnFileBitboard = FILE[pawnFile];
            if(Long.bitCount(originalBitboard & pawnFileBitboard) > 1) eval += DOUBLED;
            // weak pawn
            final int pawnRank = square >>> Value.RANK_SHIFT;
            final long adjacentFilesBitboard = (pawnFile > 0 ? FILE[pawnFile - 1] : 0L) | (pawnFile < 7 ? FILE[pawnFile + 1] : 0L);
            final long adjacentFilePawns = originalBitboard & adjacentFilesBitboard;
            if((adjacentFilePawns & (OTHER_FORWARD[pawnRank] | RANK[pawnRank])) == 0L) eval += WEAK;
            // isolated pawn
            if(adjacentFilePawns == 0) eval += ISOLATED;
            // pawn protects
            if((ATTACKS[square] & knightBishopBitboard) != 0L) eval += PROTECTS;
            // passed pawn
            final long forwardRanksBitboard = FORWARD[pawnRank];
            final long otherPassedPawnBlockers = otherPawnBitboard & (pawnFileBitboard | adjacentFilesBitboard) & forwardRanksBitboard;
            if(otherPassedPawnBlockers == 0L) {
                // additional piece square bonus
                eval += RANK_BONUS[pawnRank];
                // phalanx
                eval += (originalBitboard & adjacentFilesBitboard & RANK[pawnRank]) != 0L ? PHALANX : 0;
                // other king distance when low material
                if(materialValuePieces < QUEEN_VALUE) {
                    final int fileDist = pawnFile - kingFile;
                    final int fileDistSign = fileDist >> 31;
                    final int rankDist = pawnRank - kingRank;
                    final int rankDistSign = rankDist >> 31;
                    final int rankAbs = (rankDist ^ rankDistSign) - rankDistSign;
                    final int absDiff = rankAbs - ((fileDist ^ fileDistSign) - fileDistSign);
                    final int kingDist = 8 - (rankAbs - (absDiff & (absDiff >> 31)));
                    final int otherFileDist = pawnFile - otherKingFile;
                    final int otherFileDistSign = otherFileDist >> 31;
                    final int otherRankDist = pawnRank - otherKingRank;
                    final int otherRankDistSign = otherRankDist >> 31;
                    final int otherRankAbs = (otherRankDist ^ otherRankDistSign) - otherRankDistSign;
                    final int otherAbsDiff = otherRankAbs - ((otherFileDist ^ otherFileDistSign) - otherFileDistSign);
                    final int otherKingDist = otherRankAbs - (otherAbsDiff & (otherAbsDiff >> 31));
                    eval += (kingDist * kingDist + otherKingDist * otherKingDist) * (player == 0 ? pawnRank : 7 - pawnRank);
                }
                // other king stops pawn when other has no material
                if(otherMaterialValuePieces == 0 && (originalBitboard & forwardRanksBitboard & pawnFileBitboard) != 0L) {
                    final int pawnPromoteRank = (-(player & 1 ^ 1) & 0b111);
                    final int pawnRankDist = pawnRank - pawnPromoteRank;
                    final int pawnRankDistSign = pawnRankDist >> 31;
                    final int pawnPromoteDist = (pawnRankDist ^ pawnRankDistSign) - pawnRankDistSign + (pawnPromoteRank ^ 0b110);

                    final int otherRankDist = pawnPromoteRank - otherKingRank;
                    final int otherRankDistSign = otherRankDist >> 31;
                    final int otherRankDistAbs = (otherRankDist + otherRankDistSign) ^ otherRankDistSign;
                    final int otherFileDist = pawnFile - otherKingFile;
                    final int otherFileSign = otherFileDist >> 31;
                    final int otherAbsDiff = otherRankDistAbs - ((otherFileDist ^ otherFileSign) - otherFileSign);
                    final int otherKingDistFromPromote = otherRankDistAbs - (otherAbsDiff & (otherAbsDiff >> 31));

                    final int pawnTurnToMove = 1 ^ (player ^ thisPlayer);
                    final int kingTurnToMove = 1 ^ pawnTurnToMove;
                    final long mask = kingBitboard & forwardRanksBitboard & FILE[pawnFile];
                    final int ownKingInFront = (int) ((mask | -mask) >>> 63);

                    final int pawnDist = pawnPromoteDist - pawnTurnToMove + ownKingInFront;
                    final int kingDist = otherKingDistFromPromote - kingTurnToMove;
                    eval += ((pawnDist - kingDist) >>> 31) * VALUE_BISHOP;
                }
            }
        }
        return eval;
    }


    // Variant of pawnEval that provides verbose logging for each evaluation
    // criterion. Mirrors the semantics of pawnEval while outputting details
    // through the provided Logger instance.
    private static int pawnEvalWithLogging(long bitboard, int phase, int player, long knightBishopBitboard, long otherPawnBitboard, int materialValuePieces, int kingRank, int kingFile, int otherKingRank, int otherKingFile, int otherMaterialValuePieces, int thisPlayer, long kingBitboard, Logger logger) {
        logger.setContext("pawnEval " + (player == 0 ? "White" : "Black"));
        logger.log("*** PAWN EVAL ***");
        // init eval and material value
        final int numPawns = Long.bitCount(bitboard);
        int eval = PIECE_VALUE[Piece.PAWN][numPawns][phase];
        logger.log("pawn value [" + eval + "]");
        long originalBitboard = bitboard;
        int log;

        // cache arrays for faster access inside loop
        final int[][] PAWN_BONUS = BONUS[Piece.PAWN][player];
        final long[] FILE = FILE_BITBOARD;
        final int DOUBLED = DOUBLED_PAWN[phase];
        final long[] FORWARD = FORWARD_RANKS[player];
        final long[] OTHER_FORWARD = FORWARD_RANKS[1 ^ player];
        final int WEAK = WEAK_PAWN[phase];
        final int ISOLATED = ISOLATED_PAWN[phase];
        final long[] ATTACKS = PAWN_ATTACKS[player];
        final int PROTECTS = PAWN_PROTECTS[phase];
        final int[] RANK_BONUS = PASSED_PAWN_RANK_BONUS[player];
        final long[] RANK = RANK_BITBOARD;
        final int PHALANX = PASSED_PAWN_PHALANX[phase];
        final int VALUE_BISHOP = BISHOP_VALUE[phase];
        final int[] lsb = LSB;
        // loop over pawns
        while(bitboard != 0L) {
            final long b = bitboard & -bitboard;
            bitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];

            logger.log("square [" + square + "]");
            // piece square bonus
            log = PAWN_BONUS[square][phase];
            logger.log("bonus [" + log + "]");
            eval += log;
            // doubled pawns
            final int pawnFile = square & Value.FILE;
            final long pawnFileBitboard = FILE[pawnFile];
            if(Long.bitCount(originalBitboard & pawnFileBitboard) > 1) {
                log = DOUBLED;
                logger.log("doubled pawn [" + log + "]");
                eval += log;
            }

            // weak pawn
            final int pawnRank = square >>> Value.RANK_SHIFT;
            final long adjacentFilesBitboard = (pawnFile > 0 ? FILE[pawnFile - 1] : 0L) | (pawnFile < 7 ? FILE[pawnFile + 1] : 0L);
            final long adjacentFilePawns = originalBitboard & adjacentFilesBitboard;

            if((adjacentFilePawns & (OTHER_FORWARD[pawnRank] | RANK[pawnRank])) == 0L) {
                log = WEAK;
                logger.log("weak pawn [" + log + "]");
                eval += log;
            }
            // isolated pawn
            if(adjacentFilePawns == 0L) {
                log = ISOLATED;
                logger.log("isolated pawn [" + log + "]");
                eval += log;
            }
            // pawn protects
            if((ATTACKS[square] & knightBishopBitboard) != 0L) {
                log = PROTECTS;
                logger.log("pawn protects [" + log + "]");
                eval += log;
            }

            // passed pawn
            final long forwardRanksBitboard = FORWARD[pawnRank];
            final long otherPassedPawnBlockers = otherPawnBitboard & (pawnFileBitboard | adjacentFilesBitboard) & forwardRanksBitboard;
            if(otherPassedPawnBlockers == 0L) {
                // additional piece square bonus

                log = RANK_BONUS[pawnRank];
                logger.log("passed pawn bonus [" + log + "]");
                eval += log;
                // phalanx
                if((originalBitboard & adjacentFilesBitboard & RANK[pawnRank]) != 0L) {
                    log = PHALANX;
                    logger.log("phalanx [" + log + "]");
                    eval += log;
                }

                // other king distance when low material
                if(materialValuePieces < QUEEN_VALUE) {
                    final int fileDist = pawnFile - kingFile;
                    final int fileDistSign = fileDist >> 31;
                    final int rankDist = pawnRank - kingRank;
                    final int rankDistSign = rankDist >> 31;
                    final int rankAbs = (rankDist ^ rankDistSign) - rankDistSign;
                    final int absDiff = rankAbs - ((fileDist ^ fileDistSign) - fileDistSign);
                    final int kingDist = 8 - (rankAbs - (absDiff & (absDiff >> 31)));
                    final int otherFileDist = pawnFile - otherKingFile;
                    final int otherFileDistSign = otherFileDist >> 31;
                    final int otherRankDist = pawnRank - otherKingRank;
                    final int otherRankDistSign = otherRankDist >> 31;
                    final int otherRankAbs = (otherRankDist ^ otherRankDistSign) - otherRankDistSign;
                    final int otherAbsDiff = otherRankAbs - ((otherFileDist ^ otherFileDistSign) - otherFileDistSign);
                    final int otherKingDist = otherRankAbs - (otherAbsDiff & (otherAbsDiff >> 31));

                    log = (kingDist * kingDist + otherKingDist * otherKingDist) * (player == 0 ? pawnRank : 7 - pawnRank);
                    logger.log("king distance [" + log + "]");
                    eval += log;

                }
                // other king stops pawn when other has no material
                if(otherMaterialValuePieces == 0 && (originalBitboard & forwardRanksBitboard & pawnFileBitboard) != 0L) {
                    final int pawnPromoteRank = (-(player & 1 ^ 1) & 0b111);
                    final int pawnRankDist = pawnRank - pawnPromoteRank;
                    final int pawnRankDistSign = pawnRankDist >> 31;
                    final int pawnPromoteDist = (pawnRankDist ^ pawnRankDistSign) - pawnRankDistSign + (pawnPromoteRank ^ 0b110);

                    final int otherRankDist = pawnPromoteRank - otherKingRank;
                    final int otherRankDistSign = otherRankDist >> 31;
                    final int otherRankDistAbs = (otherRankDist + otherRankDistSign) ^ otherRankDistSign;
                    final int otherFileDist = pawnFile - otherKingFile;
                    final int otherFileSign = otherFileDist >> 31;
                    final int otherAbsDiff = otherRankDistAbs - ((otherFileDist ^ otherFileSign) - otherFileSign);
                    final int otherKingDistFromPromote = otherRankDistAbs - (otherAbsDiff & (otherAbsDiff >> 31));

                    final int pawnTurnToMove = 1 ^ (player ^ thisPlayer);
                    final int kingTurnToMove = 1 ^ pawnTurnToMove;
                    final long mask = kingBitboard & forwardRanksBitboard & FILE[pawnFile];
                    final int ownKingInFront = (int) ((mask | -mask) >>> 63);

                    final int pawnDist = pawnPromoteDist - pawnTurnToMove + ownKingInFront;
                    final int kingDist = otherKingDistFromPromote - kingTurnToMove;

                    log = ((pawnDist - kingDist) >>> 31) * VALUE_BISHOP;
                    if(log != 0) {
                        logger.log("king stops pawn [" + log + "]");
                        eval += log;
                    }
                }
            }
        }
        logger.clearContext();

        return eval;
    }

    private static final int PAWN_VALUE_SHIFTED = 0b1110000000;
    private static final int KNIGHT_VALUE_SHIFTED = 0b101000000;
    private static final int BISHOP_VALUE_SHIFTED = 0b100000000;
    private static final int ROOK_VALUE_SHIFTED = 0b011000000;
    private static final int QUEEN_VALUE_SHIFTED = 0b010000000;

    private static int getNextAttackingPiece(long board0, long board1, long board2, long board3, int square, int player) {
        final int[] lsb = LSB;
        final long colorMask = ~(-player) ^ board3;
        long bitboard = (~board0 & board1 & board2 & colorMask) & Bitboard.BB[Bitboard.PAWN_ATTACKS_PLAYER1 - player][square];
        if(bitboard != 0L) return lsb[(int) (((bitboard & -bitboard) * DB) >>> 58)] | PAWN_VALUE_SHIFTED;
        bitboard = (board0 & ~board1 & board2 & colorMask) & Bitboard.BB[Bitboard.LEAP_ATTACKS][square];
        if(bitboard != 0L) return lsb[(int) (((bitboard & -bitboard) * DB) >>> 58)]  | KNIGHT_VALUE_SHIFTED;
        final long allOccupancy = board0 | board1 | board2;
        final long bishopMagic = Magic.bishopMoves(square, allOccupancy);
        bitboard = (~board0 & ~board1 & board2 & colorMask) & bishopMagic;
        if(bitboard != 0L) return lsb[(int) (((bitboard & -bitboard) * DB) >>> 58)]  | BISHOP_VALUE_SHIFTED;
        final long rookMagic = Magic.rookMoves(square, allOccupancy);
        bitboard = (board0 & board1 & ~board2 & colorMask) & rookMagic;
        if(bitboard != 0L) return lsb[(int) (((bitboard & -bitboard) * DB) >>> 58)]  | ROOK_VALUE_SHIFTED;
        bitboard = (~board0 & board1 & ~board2 & colorMask) & (bishopMagic | rookMagic);
        if(bitboard != 0L) return lsb[(int) (((bitboard & -bitboard) * DB) >>> 58)]  | QUEEN_VALUE_SHIFTED;
        return Value.INVALID;
    }
}

