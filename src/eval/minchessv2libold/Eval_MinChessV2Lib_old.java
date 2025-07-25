package com.ohinteractive.minchessv2lib.impl;

import com.ohinteractive.minchessv2lib.util.BitOps;
import com.ohinteractive.minchessv2lib.util.Bitboard;
import com.ohinteractive.minchessv2lib.util.Crit;
import com.ohinteractive.minchessv2lib.util.Magic;
import com.ohinteractive.minchessv2lib.util.Piece;
import com.ohinteractive.minchessv2lib.util.TTable;
import com.ohinteractive.minchessv2lib.util.Value;

public class EvalOld {

    public EvalOld() {
        this(Board.startingPosition());
    }

    public EvalOld(long[] board) {
        this.board = board;
        this.playerToMove = (int) board[Board.STATUS] & Board.PLAYER_BIT;
        this.playerOccupancy[0] = board[0];
        this.playerOccupancy[1] = board[8];
        this.allOccupancy = this.playerOccupancy[0] | this.playerOccupancy[1];
        long bitboard = board[Piece.WHITE_KING];
        this.playerKingSquare[0] = BitOps.lsb(bitboard);
        this.playerKingRank[0] = this.playerKingSquare[0] >>> 3;
        this.playerKingFile[0] = this.playerKingSquare[0] & 7;
        bitboard = board[Piece.BLACK_KING];
        this.playerKingSquare[1] =  BitOps.lsb(bitboard);
        this.playerKingRank[1] = this.playerKingSquare[1] >>> 3;
        this.playerKingFile[1] = this.playerKingSquare[1] & 7;
        this.phase = Math.min(0, Math.max(24 - ((Long.bitCount(board[Piece.WHITE_QUEEN]) + Long.bitCount(board[Piece.BLACK_QUEEN])) * 4 +
                     (Long.bitCount(board[Piece.WHITE_ROOK])  + Long.bitCount(board[Piece.BLACK_ROOK]))  * 2 +
                     Long.bitCount(board[Piece.WHITE_BISHOP]) + Long.bitCount(board[Piece.BLACK_BISHOP]) +
                     Long.bitCount(board[Piece.WHITE_KNIGHT]) + Long.bitCount(board[Piece.BLACK_KNIGHT])), 24));
    }

    public int eval() {
        final long t = table.probe(this.board[Board.KEY]).data();
        if(t != Long.MIN_VALUE) return (int) t;
        final long whiteKing = board[Piece.WHITE_KING];
        final long whiteQueen = board[Piece.WHITE_QUEEN];
        final long whiteRook = board[Piece.WHITE_ROOK];
        final long whiteBishop = board[Piece.WHITE_BISHOP];
        final long whiteKnight = board[Piece.WHITE_KNIGHT];
        final long whitePawn = board[Piece.WHITE_PAWN];
        final long blackKing = board[Piece.BLACK_KING];
        final long blackQueen = board[Piece.BLACK_QUEEN];
        final long blackRook = board[Piece.BLACK_ROOK];
        final long blackBishop = board[Piece.BLACK_BISHOP];
        final long blackKnight = board[Piece.BLACK_KNIGHT];
        final long blackPawn = board[Piece.BLACK_PAWN];
        int whiteEval = kingEval(Value.WHITE, Value.BLACK, whiteRook, whitePawn, blackPawn)
        + queenEval(Value.WHITE, Value.BLACK, whiteQueen, whiteBishop, whiteKnight)
        + rookEval(Value.WHITE, Value.BLACK, whiteRook, whiteKing, whitePawn, blackPawn, blackQueen)
        + bishopEval(Value.WHITE, Value.BLACK, whiteBishop, whitePawn, blackPawn)
        + knightEval(Value.WHITE, Value.BLACK, whiteKnight, whitePawn, blackPawn)
        + pawnEval(Value.WHITE, Value.BLACK, whitePawn, blackPawn, whiteKing, whiteBishop | whiteKnight);
        
        int blackEval = kingEval(Value.BLACK, Value.WHITE, blackRook, blackPawn, whitePawn)
        + queenEval(Value.BLACK, Value.WHITE, blackQueen, blackBishop, blackKnight)
        + rookEval(Value.BLACK, Value.WHITE, blackRook, blackKing, blackPawn, whitePawn, whiteQueen)
        + bishopEval(Value.BLACK, Value.WHITE, blackBishop, blackPawn, whitePawn)
        + knightEval(Value.BLACK, Value.WHITE, blackKnight, blackPawn, whitePawn)
        + pawnEval(Value.BLACK, Value.WHITE, blackPawn, whitePawn, blackKing, blackBishop | blackKnight);
        whiteEval += SAFETY_VALUE[playerSafety[1]];
        blackEval += SAFETY_VALUE[playerSafety[0]];
        int eval = (this.playerToMove == Value.WHITE ? whiteEval - blackEval : blackEval - whiteEval);
        eval = isDraw(eval) ? 0 : eval;
        table.save(this.board[Board.KEY], 0, TTable.TYPE_EVAL, eval, 0L);
        return eval;
    }

    public static int see(long[] board, int startSquare, int targetSquare) {
        long[] seeBoard = new long[board.length];
        System.arraycopy(board, 0, seeBoard, 0, board.length);
        int seeValue = 0;
        int startPlayer = (int) seeBoard[Board.STATUS] & Board.PLAYER_BIT;
        int currentPlayer = startPlayer;
        int targetPiece = Board.getSquare(seeBoard, targetSquare);
        int startPiece = Board.getSquare(seeBoard, startSquare);
        long targetSquareBit = 1L << targetSquare;
        while(true) {
            seeValue += PIECE_VALUE[targetPiece & Piece.TYPE][1][0] * (currentPlayer == startPlayer ? 1 : -1);
            currentPlayer = 1 ^ currentPlayer;
            long pieceMoveBits = (1L << startSquare) | targetSquareBit;
            seeBoard[startPiece] ^= pieceMoveBits;
            seeBoard[startPiece & 8] ^= pieceMoveBits;
            seeBoard[targetPiece] ^= targetSquareBit;
            seeBoard[targetPiece & 8] ^= targetSquareBit;
            startSquare = getNextAttackingPiece(seeBoard, targetSquare, currentPlayer);
            if(startSquare == Value.INVALID) break;
            targetPiece = startPiece;
            startPiece = Board.getSquare(seeBoard, startSquare);
        }
        return seeValue;
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
    
    private static TTable table = new TTable();

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
                    PAWN_SHIELD_FAR[side][numPawns][phase] = ((int[][][]) Crit.VALUE[Crit.PAWN_SHIELD_CLOSE])[side][numPawns][phase];
                    PAWN_STORM_CLOSE[side][numPawns][phase] = ((int[][][]) Crit.VALUE[Crit.PAWN_SHIELD_CLOSE])[side][numPawns][phase];
                    PAWN_STORM_FAR[side][numPawns][phase] = ((int[][][]) Crit.VALUE[Crit.PAWN_SHIELD_CLOSE])[side][numPawns][phase];
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

    private long[] board;
    private int playerToMove;
    private long allOccupancy;
    private long[] playerOccupancy = new long[2];
    private int[] playerKingSquare = new int[2];
    private int[] playerKingRank = new int[2];
    private int[] playerKingFile = new int[2];
    private int phase;
    private int[] playerSafety = new int[2];

    private static final int CENTRE_FILE = 3;
    private static final int CENTRE_RANK = 3;
    private static final int EDGE_WEIGHT = 10;
    private static final int PROXIMITY_WEIGHT = 20;
    private static final int MAX_PHASE = 24;
    
    private int kingEval(int player, int other, long rookBitboard, long pawnBitboard, long otherPawnBitboard) {
        int eval = 0;
        // king square bonus
        eval += BONUS[Piece.KING][player][this.playerKingSquare[player]][phase];
        // king on back rank evals - king blocks rook, rook protects king, pawn shield, opponent pawn storm
        if(this.playerKingRank[player] == (player == Value.WHITE ? 0 : 7)) {
            switch(this.playerKingFile[player]) {
                case 0: {
                    if(((player == 0 ? 0x000000000000000eL : 0x0e00000000000000L) & rookBitboard) != 0L) eval += ROOK_PROTECTS[phase];
                    eval += PAWN_SHIELD_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                    break;
                }
                case 1: {
                    if(((player == 0 ? 0x000000000000000cL : 0x0c00000000000000L) & rookBitboard) != 0L) eval += ROOK_PROTECTS[phase];
                    if((Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0] & 0x0100000000000001L & rookBitboard) != 0L) eval += KING_BLOCKS_ROOK[phase];
                    eval += PAWN_SHIELD_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                    break;
                }
                case 2: {
                    if(((player == 0 ? 0x0000000000000008L : 0x0800000000000000L) & rookBitboard) != 0L) eval += ROOK_PROTECTS[phase];
                    if((Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0] & 0x0100000000000001L & rookBitboard) != 0L) eval += KING_BLOCKS_ROOK[phase];
                    eval += PAWN_SHIELD_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_QUEENSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[1][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_QUEENSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                    break;
                }
                case 3: {
                    if((Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0] & 0x0100000000000001L & rookBitboard) != 0L) eval += KING_BLOCKS_ROOK[phase];
                    break;
                }
                case 4: {
                    break;
                }
                case 5: {
                    if((Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0] & 0x8000000000000080L & rookBitboard) != 0L) eval += KING_BLOCKS_ROOK[phase];
                    eval += PAWN_SHIELD_CLOSE[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_KINGSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_KINGSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_KINGSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                    break;
                }
                case 6: {
                    if((Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0] & 0x8000000000000080L & rookBitboard) != 0L) eval += KING_BLOCKS_ROOK[phase];
                    eval += PAWN_SHIELD_CLOSE[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER0 + player][0] & pawnBitboard)][phase] +
                            PAWN_SHIELD_FAR[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_SHIELD_KINGSIDE_FAR_PLAYER0 + player][0] & pawnBitboard)][phase] -
                            PAWN_STORM_CLOSE[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_KINGSIDE_CLOSE_PLAYER0 + player][0] & otherPawnBitboard)][phase] -
                            PAWN_STORM_FAR[0][Long.bitCount(Bitboard.BB[Bitboard.PAWN_STORM_KINGSIDE_FAR_PLAYER0 + player][0] & otherPawnBitboard)][phase];
                }
                case 7: {
                    if(((player == 0 ? 0x0000000000000060L : 0x6000000000000000L) & rookBitboard) != 0L) eval += ROOK_PROTECTS[phase];
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
        final int playerPieceMaterial = Board.materialValuePieces(board, player);
        final int otherPieceMaterial = Board.materialValuePieces(board, other);
        if (playerPieceMaterial > otherPieceMaterial && playerPieceMaterial <= Piece.VALUE[Piece.QUEEN]) {
            final int thisKingFile = playerKingFile[player];
            final int thisKingRank = playerKingRank[player];
            final int otherKingFile = playerKingFile[other];
            final int otherKingRank = playerKingRank[other];
            int opponentKingDistanceFromCenter = Math.abs(otherKingFile - CENTRE_FILE) + Math.abs(otherKingRank - CENTRE_RANK);
            int kingDistance = Math.abs(thisKingFile - otherKingFile) + Math.abs(thisKingRank - otherKingRank);
            int opponentKingEdgePenalty = opponentKingDistanceFromCenter * EDGE_WEIGHT;
            int kingProximityBonus = (14 - kingDistance) * PROXIMITY_WEIGHT;
            int phaseScaledAdjustment = ((opponentKingEdgePenalty + kingProximityBonus) * (MAX_PHASE - phase)) / MAX_PHASE;
            eval += phaseScaledAdjustment;
        }
        return eval;
    }

    private int queenEval(int player, int other, long bitboard, long bishopBitboard, long knightBitboard) {
        // material value
        int eval = PIECE_VALUE[Piece.QUEEN][Long.bitCount(bitboard)][phase];
        // early development
        if((bitboard & Bitboard.BB[Bitboard.QUEEN_START_POSITION_PLAYER0 + player][0]) == 0L &&
           (bishopBitboard & Bitboard.BB[Bitboard.BISHOP_START_POSITION_PLAYER0 + player][0]) != 0L &&
           (knightBitboard & Bitboard.BB[Bitboard.KNIGHT_START_POSITION_PLAYER0 + player][0]) != 0L) eval += QUEEN_EARLY_DEVELOPMENT[phase];
        while(bitboard != 0L) {
            int square = BitOps.lsb(bitboard);
            bitboard &= bitboard - 1;
            // piece square bonus
            eval += BONUS[Piece.QUEEN][player][square][phase];
            // mobility
            long queenAttacks = Magic.queenMoves(square, allOccupancy) & ~this.playerOccupancy[player];
            eval += MOBILITY_QUEEN[Long.bitCount(queenAttacks)][phase];
            // other king safety
            playerSafety[other] += QUEEN_AFFECTS_KING_SAFETY[Long.bitCount(queenAttacks & Bitboard.BB[Bitboard.KING_RING_PLAYER1 - player][playerKingSquare[other]])];
            // other king distance
            eval += QUEEN_ENEMY_KING_DISTANCE[Math.abs((square >>> 3) - playerKingRank[other]) + Math.abs((square & 7) - playerKingFile[other])][phase];
        }
        return eval;
    }

    private int rookEval(int player, int other, long bitboard, long kingBitboard, long pawnBitboard, long otherPawnBitboard, long otherQueenBitboard) {
        // material value
        int numRooks = Long.bitCount(bitboard);
        int eval = PIECE_VALUE[Piece.ROOK][numRooks][phase];
        // early development
        if(Long.bitCount(bitboard & Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0]) < 2 && (kingBitboard & Bitboard.BB[Bitboard.KING_START_POSITION_PLAYER0 + player][0]) != 0L) eval += ROOK_EARLY_DEVELOPMENT[phase];
        // rook pair
        if(numRooks > 1) eval += ROOK_PAIR[phase];
        // rooks and pawns
        int numPawns = Long.bitCount(pawnBitboard);
        if(numPawns > 8) Bitboard.drawBitboard(pawnBitboard);
		eval += ROOK_PAWN[numRooks > 2 ? 2 : numRooks][numPawns][phase];
        while(bitboard != 0L) {
            int square = BitOps.lsb(bitboard);
            bitboard &= bitboard - 1;
            // piece square bonus
            eval += BONUS[Piece.ROOK][player][square][phase];
            // mobility
            long rookAttacks = Magic.rookMoves(square, allOccupancy) & ~this.playerOccupancy[player];
            eval += MOBILITY_ROOK[Long.bitCount(rookAttacks)][phase];
            // rook open file
            int rookFile = square & 7;
            eval += ((pawnBitboard & Bitboard.BB[Bitboard.FILE][rookFile]) == 0L ? ROOK_OPEN_FILE[phase] : 0) + ((otherPawnBitboard & Bitboard.BB[Bitboard.FILE][rookFile]) == 0L ? ROOK_OPEN_FILE[phase] : 0);
            // rook on other queen file
            eval += (otherQueenBitboard & Bitboard.BB[Bitboard.FILE][rookFile]) != 0L ? ROOK_ON_QUEEN_FILE[phase] : 0;
            // other king safety
            playerSafety[other] += ROOK_AFFECTS_KING_SAFETY[Long.bitCount(rookAttacks & Bitboard.BB[Bitboard.KING_RING_PLAYER1 - player][playerKingSquare[other]])];
            // other king distance
            eval += ROOK_ENEMY_KING_DISTANCE[Math.abs((square >>> 3) - playerKingRank[other]) + Math.abs(rookFile - playerKingFile[other])][phase];
        }
        return eval;
    }

    private int bishopEval(int player, int other, long bitboard, long pawnBitboard, long otherPawnBitboard) {
        // material value
        int numBishops = Long.bitCount(bitboard);
        int eval = PIECE_VALUE[Piece.BISHOP][numBishops][phase];
        // bishop pair
        eval += (numBishops > 1 ? BISHOP_PAIR[phase] : 0);
        long squareColorBitboard;
        while(bitboard != 0L) {
            int square = BitOps.lsb(bitboard);
            bitboard &= bitboard - 1;
            // piece square bonus
            eval += BONUS[Piece.BISHOP][player][square][phase];
            // mobility
            long bishopAttacks = Magic.bishopMoves(square, allOccupancy) & ~this.playerOccupancy[player];
            eval += MOBILITY_BISHOP[Long.bitCount(bishopAttacks)][phase];
            // outpost
            int bishopFile = square & 7;
            int bishopRank = square >>> 3;
            if((Bitboard.BB[Bitboard.PAWN_ATTACKS_PLAYER1 - player][square] & pawnBitboard) != 0L) {
                if((Bitboard.BB[Bitboard.PASSED_PAWNS_FILES_PLAYER0 + player][bishopFile] & Bitboard.BB[Bitboard.FORWARD_RANKS_PLAYER0 + player][bishopRank] & otherPawnBitboard) == 0L) eval += BISHOP_OUTPOST[phase];
            }
            // bad bishop
            squareColorBitboard = (Bitboard.BB[Bitboard.SQUARE_COLOR_LIGHT][0] & (1L << square)) != 0L ? Bitboard.BB[Bitboard.SQUARE_COLOR_LIGHT][0] : Bitboard.BB[Bitboard.SQUARE_COLOR_DARK][0];
            eval += BAD_BISHOP[Long.bitCount(pawnBitboard & squareColorBitboard)][Long.bitCount(otherPawnBitboard & squareColorBitboard)][phase];
            // own king distance
            eval += BISHOP_PROTECTOR[Math.abs(bishopRank - playerKingRank[player]) + Math.abs(bishopFile - playerKingFile[player])][phase];
            // other king safety
            playerSafety[other] += BISHOP_AFFECTS_KING_SAFETY[Long.bitCount(bishopAttacks & Bitboard.BB[Bitboard.KING_RING_PLAYER1 - player][playerKingSquare[other]])];
            // other king distance
            eval += BISHOP_ENEMY_KING_DISTANCE[Math.abs(bishopRank - playerKingRank[other]) + Math.abs(bishopFile - playerKingFile[other])][phase];
        }
        return eval;
    }

    private int knightEval(int player, int other, long bitboard, long pawnBitboard, long otherPawnBitboard) {
        // material value
        int numKnights = Long.bitCount(bitboard);
        int eval = PIECE_VALUE[Piece.KNIGHT][numKnights][phase];
        // knight pair
        eval += (numKnights > 1 ? KNIGHT_PAIR[phase] : 0);
        // knight and pawns
        int numPawns = Long.bitCount(pawnBitboard);
        eval += KNIGHT_PAWN[numKnights > 2 ? 2 : numKnights][numPawns][phase];
        while(bitboard != 0L) {
            int square = BitOps.lsb(bitboard);
            bitboard &= bitboard - 1;
            // piece square bonus
            eval += BONUS[Piece.KNIGHT][player][square][phase];
            // mobility
            long knightAttacks = Bitboard.BB[Bitboard.LEAP_ATTACKS][square] & ~this.playerOccupancy[player];
            eval += MOBILITY_KNIGHT[Long.bitCount(knightAttacks)][phase];
            // outpost
            int knightFile = square & 7;
            int knightRank = square >>> 3;
            if((Bitboard.BB[Bitboard.PAWN_ATTACKS_PLAYER1 - player][square] & pawnBitboard) != 0L) {
                if((Bitboard.BB[Bitboard.PASSED_PAWNS_FILES_PLAYER0 + player][knightFile] & Bitboard.BB[Bitboard.FORWARD_RANKS_PLAYER0 + player][knightRank] & otherPawnBitboard) == 0L) eval += KNIGHT_OUTPOST[phase];
            }
            // own king distance
            eval += KNIGHT_PROTECTOR[Math.abs(knightRank - playerKingRank[player]) + Math.abs(knightFile - playerKingFile[player])][phase];
            // other king safety
            playerSafety[other] += KNIGHT_AFFECTS_KING_SAFETY[Long.bitCount(knightAttacks & Bitboard.BB[Bitboard.KING_RING_PLAYER1 - player][playerKingSquare[other]])];
            // other king distance
            eval += KNIGHT_ENEMY_KING_DISTANCE[(Math.abs(knightRank - playerKingRank[other]) + Math.abs(knightFile - playerKingFile[other]))][phase];
        }
        return eval;
    }

    private int pawnEval(int player, int other, long bitboard, long otherPawnBitboard, long kingBitboard, long knightBishopBitboard) {
        // material value
        int numPawns = Long.bitCount(bitboard);
        int eval = PIECE_VALUE[Piece.PAWN][numPawns][phase];
        long originalBitboard = bitboard;
        while(bitboard != 0L) {
            int square = BitOps.lsb(bitboard);
            bitboard &= bitboard - 1;
            // piece square bonus
            eval += BONUS[Piece.PAWN][player][square][phase];
            // doubled pawns
            int pawnFile = square & 7;
            long pawnFileBitboard = Bitboard.BB[Bitboard.FILE][pawnFile];
            if(Long.bitCount(bitboard & pawnFileBitboard) > 1) eval += DOUBLED_PAWN[phase];
            // weak pawn
            int pawnRank = square >>> 3;
            long adjacentFilesBitboard = (pawnFile > 0 ? Bitboard.BB[Bitboard.FILE][pawnFile - 1] : 0L) | (pawnFile < 7 ? Bitboard.BB[Bitboard.FILE][pawnFile + 1] : 0L);
            long adjacentFilePawns = originalBitboard & adjacentFilesBitboard;
            if((adjacentFilePawns & Bitboard.BB[Bitboard.FORWARD_RANKS_PLAYER1 - player][player == 0 ? pawnRank + 1 : pawnRank - 1]) == 0L) eval += WEAK_PAWN[phase];
            // isolated pawn
            if(adjacentFilePawns == 0L) eval += ISOLATED_PAWN[phase];
            // pawn protects
            if((Bitboard.BB[Bitboard.PAWN_ATTACKS_PLAYER0 + player][square] & knightBishopBitboard) != 0L) eval += PAWN_PROTECTS[phase];
            // pawn storm when own king on opposite side
            /*
            if(pawnFile < 3) {
                if(playerKingFile[player] > 4 && playerKingRank[player] == (player == Value.WHITE ? 0 : 7)) eval += PAWN_STORM_OWN_KING_OPPOSITE;
            }
            if(pawnFile > 4) {
                if(playerKingFile[player] < 3 && playerKingRank[player] == (player == Value.WHITE ? 0 : 7)) eval += PAWN_STORM_OWN_KING_OPPOSITE;
            }
            */
            // passed pawn
            long forwardRanksBitboard = Bitboard.BB[Bitboard.FORWARD_RANKS_PLAYER0 + player][pawnRank];
            long otherPassedPawnBlockers = otherPawnBitboard & (pawnFileBitboard | adjacentFilesBitboard) & forwardRanksBitboard;
            if(otherPassedPawnBlockers == 0L) {
                // additional piece square bonus
                eval += 50 * player == 0 ? pawnRank : (7 - pawnRank);
                // phalanx
                eval += (originalBitboard & adjacentFilesBitboard & Bitboard.BB[Bitboard.RANK][pawnRank]) > 0L ? PASSED_PAWN_PHALANX[phase] : 0;
                // other king distance when low material
                if(Board.materialValuePieces(board, player) < PIECE_VALUE[Piece.QUEEN][1][24]) {
                    int kingDist = 8 - Math.max(Math.abs(playerKingRank[player] - pawnRank), Math.abs(playerKingFile[player] - pawnFile));
                    int otherKingDist = Math.max(Math.abs(playerKingRank[other] - pawnRank), Math.abs(playerKingFile[other] - pawnFile));
				    eval += (kingDist * kingDist + otherKingDist * otherKingDist) * (player == 0 ? pawnRank : 7 - pawnRank);
                }
                // other king stops pawn when other has no material
                if(Board.countMaterialPieces(board, other) == 0 && (originalBitboard & Bitboard.BB[Bitboard.FORWARD_RANKS_PLAYER0 + player][pawnRank] & Bitboard.BB[Bitboard.FILE][pawnFile]) != 0L) {
                    int pawnPromoteDist = Math.abs((player == 0 ? 7 : 0) - pawnRank) + (pawnRank == (player == 0 ? 1 : 6) ? 1 : 0);
                    int otherKingDistFromPromote = Math.max(Math.abs((player == 0 ? 7 : 0) - playerKingRank[other]), Math.abs(pawnFile - playerKingFile[other]));
                    int pawnTurnToMove = player == this.playerToMove  ? 1 : 0;
                    int kingTurnToMove = 1 ^ pawnTurnToMove;
                    int ownKingInFront = (kingBitboard & Bitboard.BB[Bitboard.FORWARD_RANKS_PLAYER0 + player][pawnRank] & Bitboard.BB[Bitboard.FILE][pawnFile]) != 0L ? 1 : 0;
                    int pawnDist = pawnPromoteDist - pawnTurnToMove + ownKingInFront;
                    int kingDist = otherKingDistFromPromote - kingTurnToMove;
                    if(kingDist > pawnDist) {
                        eval += PIECE_VALUE[Piece.BISHOP][1][phase];
                    }
                }
            }
        }
        return eval;
    }

    private static int getNextAttackingPiece(long[] board, int square, int player) {
        int playerBit = player << 3;
        long bitboard = Bitboard.BB[Bitboard.PAWN_ATTACKS_PLAYER1 - player][square] & board[Piece.PAWN | playerBit];
        if (bitboard != 0L) return BitOps.lsb(bitboard);
        bitboard = Bitboard.BB[Bitboard.LEAP_ATTACKS][square] & board[Piece.KNIGHT | playerBit];
        if (bitboard != 0L) return BitOps.lsb(bitboard);
        long allOccupancy = board[Value.WHITE_BIT] | board[Value.BLACK_BIT];
        bitboard = Magic.bishopMoves(square, allOccupancy) & board[Piece.BISHOP | playerBit];
        if (bitboard != 0L) return BitOps.lsb(bitboard);
        bitboard = Magic.rookMoves(square, allOccupancy) & board[Piece.ROOK | playerBit];
        if (bitboard != 0L) return BitOps.lsb(bitboard);
        bitboard = Magic.queenMoves(square, allOccupancy) & board[Piece.QUEEN | playerBit];
        if (bitboard != 0L) return BitOps.lsb(bitboard);
        return Value.INVALID;
    }

    private boolean isDraw(int eval) {
        int stronger = eval >= 0 ? this.playerToMove : 1 ^ this.playerToMove;
        int strongerBit = stronger << 3;
		if(Long.bitCount(this.board[Piece.PAWN | strongerBit]) > 0) {
			return false;
		}
		int weaker = 1 ^ stronger;
        if(Board.materialValuePieces(this.board, stronger) < 400) {
        	return true;
        }
        int weakerBit = weaker << 3;
        if(Long.bitCount(this.board[Piece.PAWN | weakerBit]) == 0 && Board.materialValue(this.board, stronger) == Board.KNIGHT_VALUES[2]) {
            return true;
        }
        return false;
    }
}

