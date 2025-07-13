package com.ohinteractive.minchessv2lib.impl;

import com.ohinteractive.minchessv2lib.util.BitOps;
import com.ohinteractive.minchessv2lib.util.Bitboard;
import com.ohinteractive.minchessv2lib.util.Fen;
import com.ohinteractive.minchessv2lib.util.Magic;
import com.ohinteractive.minchessv2lib.util.Piece;
import com.ohinteractive.minchessv2lib.util.Value;
import com.ohinteractive.minchessv2lib.util.Zobrist;

public class Board {

    public static final int STATUS = 7;
    public static final int MAX_BITBOARDS = 16;
    public static final int KEY = MAX_BITBOARDS - 1;
    public static final int PLAYER_BIT = 1;

    public static int player(long[] board) {
        return (int) board[STATUS] & PLAYER_BIT;
    }

    public static final int WHITE_KINGSIDE_BIT = 0b1;
    public static final int BLACK_KINGSIDE_BIT = 0b100;
    public static final int WHITE_KINGSIDE_BIT_UNSHIFTED = 0b10;
    public static final int BLACK_KINGSIDE_BIT_UNSHIFTED = 0b1000;

    public static boolean kingSide(long[] board, int player) {
        return (board[STATUS] & (player == Value.WHITE ? WHITE_KINGSIDE_BIT_UNSHIFTED : BLACK_KINGSIDE_BIT_UNSHIFTED)) != 0L;
    }

    public static final int WHITE_QUEENSIDE_BIT = 0b10;
    public static final int BLACK_QUEENSIDE_BIT = 0b1000;
    public static final int WHITE_QUEENSIDE_BIT_UNSHIFTED = 0b100;
    public static final int BLACK_QUEENSIDE_BIT_UNSHIFTED = 0b10000;

    public static boolean queenSide(long[] board, int player) {
        return (board[STATUS] & (player == Value.WHITE ? WHITE_QUEENSIDE_BIT_UNSHIFTED : BLACK_QUEENSIDE_BIT_UNSHIFTED)) != 0L;
    }

    public static final long WHITE_ENPASSANT_SQUARES = 0x0000ff0000000000L;
    public static final long BLACK_ENPASSANT_SQUARES = 0x0000000000ff0000L;

    public static boolean isValidEnPassantSquareForPlayer(int square, int player) {
        return ((1L << square) & (player == Value.WHITE ? WHITE_ENPASSANT_SQUARES : BLACK_ENPASSANT_SQUARES)) != 0L;
    }

    public static final int ESQUARE_SHIFT = 5;
    public static final int SQUARE_BITS = 0b111111;
    
    public static boolean hasValidEnPassantSquare(long[] board) {
        return ((1L << ((int) board[STATUS] >>> ESQUARE_SHIFT & SQUARE_BITS)) & (((int) board[STATUS] & PLAYER_BIT) == 0 ? WHITE_ENPASSANT_SQUARES : BLACK_ENPASSANT_SQUARES)) != 0L;
    }

    public static int enPassantSquare(long[] board) {
        int eSquare = (int) board[STATUS] >>> ESQUARE_SHIFT & SQUARE_BITS;
        return ((1L << eSquare) & (((int) board[STATUS] & PLAYER_BIT) == 0 ? WHITE_ENPASSANT_SQUARES : BLACK_ENPASSANT_SQUARES)) != 0L ? eSquare : Value.INVALID;
    }

    public static final int HALF_MOVE_CLOCK_SHIFT = 11;
    public static final int HALF_MOVE_CLOCK_BITS = 0b1111111;

    public static int halfMoveClock(long[] board) {
        return (int) board[STATUS] >>> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS;
    }

    public static final int FULL_MOVE_NUMBER_SHIFT = 18;
    public static final int FULL_MOVE_NUMBER_BITS = 0b1111111111;

    public static int fullMoveNumber(long[] board) {
        return (int) board[STATUS] >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS;
    }

    public static final int IN_CHECK_SHIFT = 28;
    public static final int IN_CHECK_BIT = 0b1;
    public static final int IN_CHECK_BIT_UNSHIFTED = 0b10000000000000000000000000000;

    public static long key(long[] board) {
        return board[KEY];
    }

    public static final String FEN_STARTING_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public static long[] startingPosition() {
        return fromFen(FEN_STARTING_POSITION);
    }

    public static final int CASTLING_SHIFT = 1;
    public static final int CASTLING_BITS = 0b1111;
    public static final int OCCUPANCY_BIT = 0b1000;

    public static long[] fromFen(String fen) {
        long[] board = new long[MAX_BITBOARDS];
        int[] pieces = Fen.getPieces(fen);
        for(int square = SQUARE_A1; square <= SQUARE_H8; square ++) {
            int piece = pieces[square];
            if(piece != Value.NONE) {
                long squareBit = 1L << square;
                board[piece] |= squareBit;
                board[piece & OCCUPANCY_BIT] |= squareBit;
            }
        }
        boolean whiteToMove = Fen.getWhiteToMove(fen);
        long status = whiteToMove ? Value.WHITE : Value.BLACK;
        int castling = Fen.getCastling(fen);
        status ^= castling << CASTLING_SHIFT;
        int eSquare = Fen.getEnPassantSquare(fen);
        if((whiteToMove && (eSquare > 39 && eSquare < 48)) || (!whiteToMove && (eSquare > 15 && eSquare < 24))) {
            status  ^= eSquare << ESQUARE_SHIFT;
        } else {
            eSquare = Value.INVALID;
        }
        status ^= Fen.getHalfMoveClock(fen) << HALF_MOVE_CLOCK_SHIFT;
        board[STATUS] = status ^ Fen.getFullMoveNumber(fen) << FULL_MOVE_NUMBER_SHIFT;
        board[KEY] = Zobrist.getKey(pieces, whiteToMove, (castling & WHITE_KINGSIDE_BIT) != 0,
            (castling & WHITE_QUEENSIDE_BIT) != 0, (castling & BLACK_KINGSIDE_BIT) != 0,
            (castling & BLACK_QUEENSIDE_BIT) != 0, eSquare);
        return board;
    }

    public static final int PLAYER_SHIFT = 3;
    public static final int START_PIECE_SHIFT = 16;
    public static final int PIECE_BITS = 0b1111;
    public static final int TARGET_SQUARE_SHIFT = 6;
    public static final int TARGET_PIECE_SHIFT = 20;
    public static final int PROMOTE_PIECE_SHIFT = 12;
    public static final int SQUARE_A1 = 0;
    public static final int SQUARE_A8 = 56;
    public static final int SQUARE_H1 = 7;
    public static final int SQUARE_H8 = 63;
    public static final int WHITE_CASTLING_BITS = WHITE_KINGSIDE_BIT | WHITE_QUEENSIDE_BIT;
    public static final int BLACK_CASTLING_BITS = BLACK_KINGSIDE_BIT | BLACK_QUEENSIDE_BIT;

    public static long[] makeMove(long[] board, long move) {
        long[] newBoard = new long[board.length];
        System.arraycopy(board, 0, newBoard, 0, board.length);
        int status = (int) newBoard[STATUS];
        int castling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        int eSquare = enPassantSquare(newBoard);
        int originalESquare = eSquare;
        int halfMoveClock = status >>> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS;
        int fullMoveNumber = status >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS;
        long key = newBoard[KEY];
        int startSquare = (int) move & SQUARE_BITS;
        int startPiece = (int) move >>> START_PIECE_SHIFT & PIECE_BITS;
        int startPieceType = startPiece & Piece.TYPE;
        int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        int targetPiece = (int) move >>> TARGET_PIECE_SHIFT & PIECE_BITS;
        long targetSquareBit = 1L << targetSquare;
        int player = status & PLAYER_BIT;
        if(eSquare != Value.INVALID) {
            key ^= Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
            eSquare = Value.INVALID;
        }
        if(targetPiece != Value.NONE) {
            halfMoveClock = 0;
            int other = 1 ^ player;
            newBoard[targetPiece] ^= targetSquareBit;
            newBoard[other << PLAYER_SHIFT] ^= targetSquareBit;
            key ^= Zobrist.PIECE[targetPiece & Piece.TYPE][other][targetSquare];
        }
        switch (startPieceType) {
            case Piece.QUEEN:
            case Piece.BISHOP:
            case Piece.KNIGHT: {
                long pieceMoveBits = (1L << startSquare) | targetSquareBit;
                newBoard[startPiece] ^= pieceMoveBits;
                newBoard[player << PLAYER_SHIFT] ^= pieceMoveBits;
                key ^= Zobrist.PIECE[startPieceType][player][startSquare]
                    ^  Zobrist.PIECE[startPieceType][player][targetSquare];
                break;
            }
            case Piece.KING: {
                int playerBit = player << PLAYER_SHIFT;
                long pieceMoveBits = (1L << startSquare) | targetSquareBit;
                newBoard[startPiece] ^= pieceMoveBits;
                newBoard[playerBit] ^= pieceMoveBits;
                key ^= Zobrist.PIECE[Piece.KING][player][startSquare]
                    ^  Zobrist.PIECE[Piece.KING][player][targetSquare];
                boolean playerKingSideCastling  = (castling & (player == Value.WHITE ? WHITE_KINGSIDE_BIT : BLACK_KINGSIDE_BIT))  != 0;
                boolean playerQueenSideCastling = (castling & (player == Value.WHITE ? WHITE_QUEENSIDE_BIT : BLACK_QUEENSIDE_BIT)) != 0;
                if(playerKingSideCastling || playerQueenSideCastling) {
                    key ^= (playerKingSideCastling  ? Zobrist.KING_SIDE[player]  : 0)
                        ^  (playerQueenSideCastling ? Zobrist.QUEEN_SIDE[player] : 0);
                    castling &= ~(player == Value.WHITE ? WHITE_CASTLING_BITS : BLACK_CASTLING_BITS);
                }
                if(Math.abs(startSquare - targetSquare) == 2) {
                    if((targetSquare & Value.FILE) == Value.FILE_G) {
                        long rookMoveBits = (1L << (targetSquare + 1)) | (1L << (targetSquare - 1));
                        newBoard[Piece.ROOK | playerBit] ^= rookMoveBits;
                        newBoard[playerBit] ^= rookMoveBits;
                        key ^= Zobrist.PIECE[Piece.ROOK][player][targetSquare + 1]
                            ^  Zobrist.PIECE[Piece.ROOK][player][targetSquare - 1];
                    } else {
                        long rookMoveBits = (1L << (targetSquare - 2)) | (1L << (targetSquare + 1));
                        newBoard[Piece.ROOK | playerBit] ^= rookMoveBits;
                        newBoard[playerBit] ^= rookMoveBits;
                        key ^= Zobrist.PIECE[Piece.ROOK][player][targetSquare - 2]
                            ^  Zobrist.PIECE[Piece.ROOK][player][targetSquare + 1];
                    }
                }
                break;
            }
            case Piece.ROOK: {
                long pieceMoveBits = (1L << startSquare) | targetSquareBit;
                newBoard[startPiece] ^= pieceMoveBits;
                newBoard[player << PLAYER_SHIFT] ^= pieceMoveBits;
                key ^= Zobrist.PIECE[Piece.ROOK][player][startSquare]
                    ^  Zobrist.PIECE[Piece.ROOK][player][targetSquare];
                if((castling & Value.KINGSIDE_BIT[player]) != Value.NONE) {
                    if(startSquare == (player == Value.WHITE ? SQUARE_H1 : SQUARE_H8)) {
                        castling ^= Value.KINGSIDE_BIT[player];
                        key ^= Zobrist.KING_SIDE[player];
                    }
                }
                if((castling & Value.QUEENSIDE_BIT[player]) != Value.NONE) {
                    if(startSquare == (player == Value.WHITE ? SQUARE_A1 : SQUARE_A8)) {
                        castling ^= Value.QUEENSIDE_BIT[player];
                        key ^= Zobrist.QUEEN_SIDE[player];
                    }
                }
                break;
            }
            case Piece.PAWN: {
                int promotePiece = (int) move >>> PROMOTE_PIECE_SHIFT & PIECE_BITS;
                int playerBit = player << PLAYER_SHIFT;
                halfMoveClock = 0;
                if(promotePiece == Value.NONE) {
                    long pieceMoveBits = (1L << startSquare) | targetSquareBit;
                    newBoard[startPiece] ^= pieceMoveBits;
                    newBoard[playerBit] ^= pieceMoveBits;
                    key ^= Zobrist.PIECE[startPieceType][player][startSquare]
                        ^  Zobrist.PIECE[startPieceType][player][targetSquare];
                } else {
                    long startSquareBit = 1L << startSquare;
                    newBoard[startPiece] ^= startSquareBit;
                    newBoard[promotePiece] ^= targetSquareBit;
                    newBoard[playerBit] ^= startSquareBit | targetSquareBit;
                    key ^= Zobrist.PIECE[startPieceType][player][startSquare]
                        ^  Zobrist.PIECE[promotePiece & Piece.TYPE][player][targetSquare];
                }
                if(targetSquare == originalESquare) {
                    int other = 1 ^ player;
                    int otherBit = other << PLAYER_SHIFT;
                    int captureSquare = targetSquare + (player == Value.WHITE ? -8 : 8);
                    long captureSquareBit = 1L << captureSquare;
                    newBoard[Piece.PAWN | otherBit] ^= captureSquareBit;
                    newBoard[otherBit] ^= captureSquareBit;
                    key ^= Zobrist.PIECE[Piece.PAWN][other][captureSquare];
                }
                if(Math.abs(startSquare - targetSquare) == 16) {
                    eSquare = startSquare + (player == Value.WHITE ? 8 : -8);
                    key ^= Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
                }
                break;
            }
            default:
                break;
        }
        if((targetPiece & Piece.TYPE) == Piece.ROOK) {
            int other = 1 ^ player;
            if((castling & (other == Value.WHITE ? WHITE_KINGSIDE_BIT : BLACK_KINGSIDE_BIT)) != Value.NONE) {
                if(targetSquare == (other == Value.WHITE ? SQUARE_H1 : SQUARE_H8)) {
                    castling ^= (other == Value.WHITE ? WHITE_KINGSIDE_BIT : BLACK_KINGSIDE_BIT);
                    key ^= Zobrist.KING_SIDE[other];
                }
            }
            if((castling & (other == Value.WHITE ? WHITE_QUEENSIDE_BIT : BLACK_QUEENSIDE_BIT)) != Value.NONE) {
                if(targetSquare == (other == Value.WHITE ? SQUARE_A1 : SQUARE_A8)) {
                    castling ^= (other == Value.WHITE ? WHITE_QUEENSIDE_BIT : BLACK_QUEENSIDE_BIT);
                    key ^= Zobrist.QUEEN_SIDE[other];
                }
            }
        }
        key ^= Zobrist.WHITEMOVE;
        newBoard[STATUS] = (1 ^ player) | (castling << CASTLING_SHIFT)
                         | (eSquare != Value.INVALID ? (eSquare << ESQUARE_SHIFT) : 0)
                         | (halfMoveClock << HALF_MOVE_CLOCK_SHIFT)
                         | ((fullMoveNumber + player) << FULL_MOVE_NUMBER_SHIFT);
        newBoard[KEY] = key;
        return newBoard;
    }

    public static void makeMoveWith(long[] board, long move) {
        int status = (int) board[STATUS];
        int castling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        int eSquare = enPassantSquare(board);
        int originalESquare = eSquare;
        int halfMoveClock = status >>> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS;
        int fullMoveNumber = status >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS;
        long key = board[KEY];
        int startSquare = (int) move & SQUARE_BITS;
        int startPiece = (int) move >>> START_PIECE_SHIFT & PIECE_BITS;
        int startPieceType = startPiece & Piece.TYPE;
        int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        int targetPiece = (int) move >>> TARGET_PIECE_SHIFT & PIECE_BITS;
        long targetSquareBit = 1L << targetSquare;
        int player = status & PLAYER_BIT;
        if(eSquare != Value.INVALID) {
            key ^= Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
            eSquare = Value.INVALID;
        }
        if(targetPiece != Value.NONE) {
            halfMoveClock = 0;
            int other = 1 ^ player;
            board[targetPiece] ^= targetSquareBit;
            board[other << PLAYER_SHIFT] ^= targetSquareBit;
            key ^= Zobrist.PIECE[targetPiece & Piece.TYPE][other][targetSquare];
        }
        switch (startPieceType) {
            case Piece.QUEEN:
            case Piece.BISHOP:
            case Piece.KNIGHT: {
                long pieceMoveBits = (1L << startSquare) | targetSquareBit;
                board[startPiece] ^= pieceMoveBits;
                board[player << PLAYER_SHIFT] ^= pieceMoveBits;
                key ^= Zobrist.PIECE[startPieceType][player][startSquare]
                    ^  Zobrist.PIECE[startPieceType][player][targetSquare];
                break;
            }
            case Piece.KING: {
                int playerBit = player << PLAYER_SHIFT;
                long pieceMoveBits = (1L << startSquare) | targetSquareBit;
                board[startPiece] ^= pieceMoveBits;
                board[playerBit] ^= pieceMoveBits;
                key ^= Zobrist.PIECE[Piece.KING][player][startSquare]
                    ^  Zobrist.PIECE[Piece.KING][player][targetSquare];
                boolean playerKingSideCastling  = (castling & (player == Value.WHITE ? WHITE_KINGSIDE_BIT : BLACK_KINGSIDE_BIT))  != 0;
                boolean playerQueenSideCastling = (castling & (player == Value.WHITE ? WHITE_QUEENSIDE_BIT : BLACK_QUEENSIDE_BIT)) != 0;
                if(playerKingSideCastling || playerQueenSideCastling) {
                    key ^= (playerKingSideCastling  ? Zobrist.KING_SIDE[player]  : 0)
                        ^  (playerQueenSideCastling ? Zobrist.QUEEN_SIDE[player] : 0);
                    castling &= ~(player == Value.WHITE ? WHITE_CASTLING_BITS : BLACK_CASTLING_BITS);
                }
                if(Math.abs(startSquare - targetSquare) == 2) {
                    if((targetSquare & Value.FILE) == Value.FILE_G) {
                        long rookMoveBits = (1L << (targetSquare + 1)) | (1L << (targetSquare - 1));
                        board[Piece.ROOK | playerBit] ^= rookMoveBits;
                        board[playerBit] ^= rookMoveBits;
                        key ^= Zobrist.PIECE[Piece.ROOK][player][targetSquare + 1]
                            ^  Zobrist.PIECE[Piece.ROOK][player][targetSquare - 1];
                    } else {
                        long rookMoveBits = (1L << (targetSquare - 2)) | (1L << (targetSquare + 1));
                        board[Piece.ROOK | playerBit] ^= rookMoveBits;
                        board[playerBit] ^= rookMoveBits;
                        key ^= Zobrist.PIECE[Piece.ROOK][player][targetSquare - 2]
                            ^  Zobrist.PIECE[Piece.ROOK][player][targetSquare + 1];
                    }
                }
                break;
            }
            case Piece.ROOK: {
                long pieceMoveBits = (1L << startSquare) | targetSquareBit;
                board[startPiece] ^= pieceMoveBits;
                board[player << PLAYER_SHIFT] ^= pieceMoveBits;
                key ^= Zobrist.PIECE[Piece.ROOK][player][startSquare]
                    ^  Zobrist.PIECE[Piece.ROOK][player][targetSquare];
                if((castling & Value.KINGSIDE_BIT[player]) != Value.NONE) {
                    if(startSquare == (player == Value.WHITE ? SQUARE_H1 : SQUARE_H8)) {
                        castling ^= Value.KINGSIDE_BIT[player];
                        key ^= Zobrist.KING_SIDE[player];
                    }
                }
                if((castling & Value.QUEENSIDE_BIT[player]) != Value.NONE) {
                    if(startSquare == (player == Value.WHITE ? SQUARE_A1 : SQUARE_A8)) {
                        castling ^= Value.QUEENSIDE_BIT[player];
                        key ^= Zobrist.QUEEN_SIDE[player];
                    }
                }
                break;
            }
            case Piece.PAWN: {
                int promotePiece = (int) move >>> PROMOTE_PIECE_SHIFT & PIECE_BITS;
                int playerBit = player << PLAYER_SHIFT;
                halfMoveClock = 0;
                if(promotePiece == Value.NONE) {
                    long pieceMoveBits = (1L << startSquare) | targetSquareBit;
                    board[startPiece] ^= pieceMoveBits;
                    board[playerBit] ^= pieceMoveBits;
                    key ^= Zobrist.PIECE[startPieceType][player][startSquare]
                        ^  Zobrist.PIECE[startPieceType][player][targetSquare];
                } else {
                    long startSquareBit = 1L << startSquare;
                    board[startPiece] ^= startSquareBit;
                    board[promotePiece] ^= targetSquareBit;
                    board[playerBit] ^= startSquareBit | targetSquareBit;
                    key ^= Zobrist.PIECE[startPieceType][player][startSquare]
                        ^  Zobrist.PIECE[promotePiece & Piece.TYPE][player][targetSquare];
                }
                if(targetSquare == originalESquare) {
                    int other = 1 ^ player;
                    int otherBit = other << PLAYER_SHIFT;
                    int captureSquare = targetSquare + (player == Value.WHITE ? -8 : 8);
                    long captureSquareBit = 1L << captureSquare;
                    board[Piece.PAWN | otherBit] ^= captureSquareBit;
                    board[otherBit] ^= captureSquareBit;
                    key ^= Zobrist.PIECE[Piece.PAWN][other][captureSquare];
                }
                if(Math.abs(startSquare - targetSquare) == 16) {
                    eSquare = startSquare + (player == Value.WHITE ? 8 : -8);
                    key ^= Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
                }
                break;
            }
            default:
                break;
        }
        if((targetPiece & Piece.TYPE) == Piece.ROOK) {
            int other = 1 ^ player;
            if((castling & (other == Value.WHITE ? WHITE_KINGSIDE_BIT : BLACK_KINGSIDE_BIT)) != Value.NONE) {
                if(targetSquare == (other == Value.WHITE ? SQUARE_H1 : SQUARE_H8)) {
                    castling ^= (other == Value.WHITE ? WHITE_KINGSIDE_BIT : BLACK_KINGSIDE_BIT);
                    key ^= Zobrist.KING_SIDE[other];
                }
            }
            if((castling & (other == Value.WHITE ? WHITE_QUEENSIDE_BIT : BLACK_QUEENSIDE_BIT)) != Value.NONE) {
                if(targetSquare == (other == Value.WHITE ? SQUARE_A1 : SQUARE_A8)) {
                    castling ^= (other == Value.WHITE ? WHITE_QUEENSIDE_BIT : BLACK_QUEENSIDE_BIT);
                    key ^= Zobrist.QUEEN_SIDE[other];
                }
            }
        }
        key ^= Zobrist.WHITEMOVE;
        board[STATUS] = (1 ^ player) | (castling << CASTLING_SHIFT)
                         | (eSquare != Value.INVALID ? (eSquare << ESQUARE_SHIFT) : 0)
                         | (halfMoveClock << HALF_MOVE_CLOCK_SHIFT)
                         | ((fullMoveNumber + player) << FULL_MOVE_NUMBER_SHIFT);
        board[KEY] = key;
    }

    public static final long ENPASSANT_RESET_BITS = ~(SQUARE_BITS << ESQUARE_SHIFT); 

    public static long[] nullMove(long[] board) {
        long[] newBoard = new long[MAX_BITBOARDS];
        System.arraycopy(board, 0, newBoard, 0, MAX_BITBOARDS);
        long key = board[KEY];
        int eSquare = (int) newBoard[STATUS] >>> ESQUARE_SHIFT & SQUARE_BITS;
        if(eSquare > 0) {
            key ^= Zobrist.ENPASSANT_FILE[eSquare & 7];
        }
        key ^= Zobrist.WHITEMOVE;
        newBoard[STATUS] = (board[STATUS] ^ PLAYER_BIT) & ENPASSANT_RESET_BITS;
        newBoard[KEY] = key;
        return newBoard;
    }

    public static void nullMoveWith(long[] board) {
        long key = board[KEY];
        int eSquare = (int) board[STATUS] >>> ESQUARE_SHIFT & SQUARE_BITS;
        if(eSquare > 0) {
            key ^= Zobrist.ENPASSANT_FILE[eSquare & 7];
        }
        key ^= Zobrist.WHITEMOVE;
        board[STATUS] = (board[STATUS] ^ PLAYER_BIT) & ENPASSANT_RESET_BITS;
        board[KEY] = key;
    }

    public static int getSquare(long[] board, int square) {
        long squareBit = 1L << square;
        if((board[Value.WHITE_BIT] & squareBit) != 0L) {
            if((board[Piece.WHITE_PAWN] & squareBit) != 0L)   return Piece.WHITE_PAWN;
            if((board[Piece.WHITE_KNIGHT] & squareBit) != 0L) return Piece.WHITE_KNIGHT;
            if((board[Piece.WHITE_BISHOP] & squareBit) != 0L) return Piece.WHITE_BISHOP;
            if((board[Piece.WHITE_ROOK] & squareBit) != 0L)   return Piece.WHITE_ROOK;
            if((board[Piece.WHITE_QUEEN] & squareBit) != 0L)  return Piece.WHITE_QUEEN;
                                                              return Piece.WHITE_KING;
        }
        if((board[Value.BLACK_BIT] & squareBit) == 0L)    return Value.NONE;
        if((board[Piece.BLACK_PAWN] & squareBit) != 0L)   return Piece.BLACK_PAWN;
        if((board[Piece.BLACK_KNIGHT] & squareBit) != 0L) return Piece.BLACK_KNIGHT;
        if((board[Piece.BLACK_BISHOP] & squareBit) != 0L) return Piece.BLACK_BISHOP;
        if((board[Piece.BLACK_ROOK] & squareBit) != 0L)   return Piece.BLACK_ROOK;
        if((board[Piece.BLACK_QUEEN] & squareBit) != 0L)  return Piece.BLACK_QUEEN;
                                                          return Piece.BLACK_KING;
    }

    public static boolean isSquareAttackedByPlayer(long[] board, int square, int player) {
        int playerBit = player << 3;
        if((Bitboard.BB[Bitboard.LEAP_ATTACKS][square] & board[Piece.KNIGHT | playerBit]) != 0L) return true;
        if((Bitboard.BB[Bitboard.PAWN_ATTACKS_PLAYER1 - player][square] & board[Piece.PAWN | playerBit]) != 0L) return true;
        if((Bitboard.BB[Bitboard.KING_ATTACKS][square] & board[Piece.KING | playerBit]) != 0L) return true;
        long allOccupancy = board[Value.WHITE_BIT] | board[Value.BLACK_BIT];
        if((Magic.bishopMoves(square, allOccupancy) & (board[Piece.BISHOP | playerBit]
           | board[Piece.QUEEN | playerBit])) != 0L) return true;
        if((Magic.rookMoves(square, allOccupancy) & (board[Piece.ROOK | playerBit]
           | board[Piece.QUEEN | playerBit])) != 0L) return true;
        return false;
    }

    public static boolean isPlayerInCheck(long[] board, int player) {
        int playerBit = player << 3;
        long bitboard = board[Piece.KING | playerBit];
        int square = BitOps.lsb(bitboard);
        int otherBit = 8 ^ playerBit;
        if((Bitboard.BB[Bitboard.LEAP_ATTACKS][square] & board[Piece.KNIGHT | otherBit]) != 0L) return true;
        if((Bitboard.BB[Bitboard.PAWN_ATTACKS_PLAYER1 - player][square] & board[Piece.PAWN | otherBit]) != 0L) return true;
        if((Bitboard.BB[Bitboard.KING_ATTACKS][square] & board[Piece.KING | otherBit]) != 0L) return true;
        long allOccupancy = board[Value.WHITE_BIT] | board[Value.BLACK_BIT];
        if((Magic.bishopMoves(square, allOccupancy) & (board[Piece.BISHOP | otherBit]
           | board[Piece.QUEEN | otherBit])) != 0L) return true;
        if((Magic.rookMoves(square, allOccupancy) & (board[Piece.ROOK | otherBit]
           | board[Piece.QUEEN | otherBit])) != 0L) return true;
        return false;
    }

    public static void drawText(long[] board) {
        System.out.println(boardString(board));
    }

    public static String boardString(long[] board) {
        StringBuilder boardString = new StringBuilder();
        for(int i = SQUARE_A1; i <= SQUARE_H8; i ++) {
            int square = i ^ 0x38;
            int piece = getSquare(board, square);
            boardString.append((piece != Value.NONE ? Piece.SHORT_STRING[piece] : ".")).append((i & 7) == 7 ? "\n" : " ");
        }
        return boardString.toString();
    }

    public static String squareToString(int square) {
        return Value.FILE_STRING.charAt(square & Value.FILE) + Integer.toString((square >>> 3) + 1);
    }

    public static String toFenString(long[] board) {
        StringBuilder fen = new StringBuilder();
        for(int rank = 7; rank >= 0; rank --) {
            int empty = 0;
            for(int file = 0; file < 8; file ++) {
                int square = rank << 3 | file;
                int piece = getSquare(board, square);
                if(piece != Value.NONE) {
                    if(empty > 0) {
                        fen.append(empty);
                        empty = 0;
                    }
                    fen.append(Piece.SHORT_STRING[piece]);
                } else {
                    empty ++;
                }
            }
            if(empty > 0) {
                fen.append(empty);
            }
            if(rank > 0) {
                fen.append('/');
            }
        }
        fen.append(" " + (player(board) == Value.WHITE ? "w " : "b "));
        if(((int) board[STATUS] >>> CASTLING_SHIFT & CASTLING_BITS) == 0) {
            fen.append("- ");
        } else {
            fen.append((kingSide(board, Value.WHITE) ? "K" : "")
                    + (queenSide(board, Value.WHITE) ? "Q" : "")
                    + (kingSide(board, Value.BLACK) ? "k" : "")
                    + (queenSide(board, Value.BLACK) ? "q" : "")
                    + " ");
        }
        if (hasValidEnPassantSquare(board)) {
            fen.append(squareToString(enPassantSquare(board)) + " ");
        } else {
            fen.append("- ");
        }
        fen.append(Integer.toString(halfMoveClock(board)) + " " + Integer.toString(fullMoveNumber(board)));
        return fen.toString();
    }

    public static int countPiece(long[] board, int piece) {
        return Long.bitCount(board[piece]);
    }

    public static int countMaterialPieces(long[] board, int player) {
        int playerBit = player << 3;
        return Long.bitCount(board[Piece.QUEEN | playerBit]) + Long.bitCount(board[Piece.ROOK | playerBit]) + Long.bitCount(board[Piece.BISHOP | playerBit]) + Long.bitCount(board[Piece.KNIGHT | playerBit]);
    }

    public static final int[] QUEEN_VALUES = new int[10];
    public static final int[] ROOK_VALUES = new int[11];
    public static final int[] BISHOP_VALUES = new int[11];
    public static final int[] KNIGHT_VALUES = new int[11];
    public static final int[] PAWN_VALUES = new int[9];
    static {
        for(int num = 1; num < 11; num ++) {
            ROOK_VALUES[num] = Piece.VALUE[Piece.ROOK] * num;
            BISHOP_VALUES[num] = Piece.VALUE[Piece.BISHOP] * num;
            KNIGHT_VALUES[num] = Piece.VALUE[Piece.KNIGHT] * num;
            if(num < 10) QUEEN_VALUES[num] = Piece.VALUE[Piece.QUEEN] * num;
            if(num < 9) PAWN_VALUES[num] = Piece.VALUE[Piece.PAWN] * num;
        }
    }

    public static int materialValue(long[] board, int player) {
        int playerBit = player << PLAYER_SHIFT;
        return QUEEN_VALUES[Long.bitCount(board[Piece.QUEEN | playerBit])]
        +      ROOK_VALUES[Long.bitCount(board[Piece.ROOK | playerBit])]
        +      BISHOP_VALUES[Long.bitCount(board[Piece.BISHOP | playerBit])]
        +      KNIGHT_VALUES[Long.bitCount(board[Piece.KNIGHT | playerBit])]
        +      PAWN_VALUES[Long.bitCount(board[Piece.PAWN | playerBit])];
    }

    public static int materialValuePieces(long[] board, int player) {
        int playerBit = player << PLAYER_SHIFT;
        return QUEEN_VALUES[Long.bitCount(board[Piece.QUEEN | playerBit])]
        +      ROOK_VALUES[Long.bitCount(board[Piece.ROOK | playerBit])]
        +      BISHOP_VALUES[Long.bitCount(board[Piece.BISHOP | playerBit])]
        +      KNIGHT_VALUES[Long.bitCount(board[Piece.KNIGHT | playerBit])];
    }

    public static String toString(long[] board) {
        StringBuilder string = new StringBuilder();
		string.append("Player: " + (player(board) == 0 ? "White" : "Black"));
		string.append("\nCastling Rights: " + (kingSide(board, 0) ? "K" : "") + (queenSide(board, 0) ? "Q" : "") + (kingSide(board, 1) ? "k" : "") + (queenSide(board, 1) ? "q" : ""));
		string.append("\nEnPassant Square: " + (enPassantSquare(board) > 0 ? "abcdefgh".charAt(enPassantSquare(board) & 7) + Integer.toString((enPassantSquare(board) >>> 3) + 1) : "None"));
		string.append("\nHalf Move Clock: " + halfMoveClock(board));
		string.append("\nFull Move Number: " + fullMoveNumber(board));
		string.append("\nZobrist Key Decimal: " + board[KEY]);
		string.append("\nZobrist Key Hex: " + Long.toHexString(board[KEY]));
		return string.toString();
    }

    private Board() {}
}
