/*
 * Board_SeedV3.c
 * Auto-converted from Board_SeedV3.java
 * Conversion date: 2023-11-24
 * Note: This is a direct translation and may require further
 * adjustments for full C functionality and integration.
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdint.h>

/* Stubs for external utilities. These should be implemented elsewhere. */
struct ZobristStub {
    uint64_t WHITEMOVE;
    uint64_t ENPASSANT_FILE[8];
    uint64_t PIECE[8][2][64];
    uint64_t KING_SIDE[2];
    uint64_t QUEEN_SIDE[2];
};
static struct ZobristStub Zobrist; /* Values should be initialized */

struct BitboardStub {
    uint64_t BB[10][64]; /* size adjusted as needed */
    enum { LEAP_ATTACKS = 0, KING_ATTACKS = 1, PAWN_ATTACKS_PLAYER0 = 2 };
};
static struct BitboardStub Bitboard; /* Should be initialized */

struct MagicStub {
    uint64_t (*bishopMoves)(int square, uint64_t occupancy);
    uint64_t (*rookMoves)(int square, uint64_t occupancy);
};
static struct MagicStub Magic; /* Function pointers should be set */

struct FenStub {
    int* (*getPieces)(const char* fen);
    int (*getWhiteToMove)(const char* fen);
    int (*getCastling)(const char* fen);
    int (*getEnPassantSquare)(const char* fen);
    int (*getHalfMoveClock)(const char* fen);
    int (*getFullMoveNumber)(const char* fen);
};
static struct FenStub Fen; /* Functions should be implemented */

struct PieceStub {
    enum { TYPE = 7, QUEEN = 9, ROOK = 5, BISHOP = 7, KNIGHT = 3,
           KING = 1, PAWN = 2 };
    const char SHORT_STRING[16];
};
static struct PieceStub Piece; /* Fill with correct values */

struct ValueStub {
    enum { NONE = 0, INVALID = -1 };
    enum { WHITE = 0, BLACK = 1 };
    const char* FILE_STRING;
    uint64_t KINGSIDE_BIT[2];
    uint64_t QUEENSIDE_BIT[2];
};
static struct ValueStub Value; /* Populate as needed */

struct BitOpsStub {
    int LSB[64];
    uint64_t DB;
};
static struct BitOpsStub BitOps; /* Initialize with proper values */


#define STATUS 4
#define MAX_BITBOARDS 6
#define KEY (MAX_BITBOARDS - 1)
#define PLAYER_BIT 1

static inline int player(const int64_t* board) {
    return (int)(board[STATUS] & PLAYER_BIT);
}

#define WHITE_KINGSIDE_BIT 1
#define BLACK_KINGSIDE_BIT 4
#define WHITE_KINGSIDE_BIT_UNSHIFTED 2
#define BLACK_KINGSIDE_BIT_UNSHIFTED 8

static inline int kingSide(const int64_t* board, int playerFlag) {
    return (board[STATUS] & ((WHITE_KINGSIDE_BIT_UNSHIFTED & ~(-playerFlag)) |
            (BLACK_KINGSIDE_BIT_UNSHIFTED & -playerFlag))) != 0LL;
}

#define WHITE_QUEENSIDE_BIT 2
#define BLACK_QUEENSIDE_BIT 8
#define WHITE_QUEENSIDE_BIT_UNSHIFTED 4
#define BLACK_QUEENSIDE_BIT_UNSHIFTED 16

static inline int queenSide(const int64_t* board, int playerFlag) {
    return (board[STATUS] & ((WHITE_QUEENSIDE_BIT_UNSHIFTED & ~(-playerFlag)) |
            (BLACK_QUEENSIDE_BIT_UNSHIFTED & -playerFlag))) != 0LL;
}

#define WHITE_ENPASSANT_SQUARES 0x0000ff0000000000LL
#define BLACK_ENPASSANT_SQUARES 0x0000000000ff0000LL

static inline int isValidEnPassantSquareForPlayer(int square, int playerFlag) {
    return ((1LL << square) & ((WHITE_ENPASSANT_SQUARES & ~(-playerFlag)) |
            (BLACK_ENPASSANT_SQUARES & -playerFlag))) != 0LL;
}

#define ESQUARE_SHIFT 5
#define SQUARE_BITS 0x3f

static inline int hasValidEnPassantSquare(const int64_t* board) {
    int status = (int)board[STATUS];
    int p = status & PLAYER_BIT;
    return ( (1LL << (status >> ESQUARE_SHIFT & SQUARE_BITS)) &
            ((WHITE_ENPASSANT_SQUARES & ~(-p)) | (BLACK_ENPASSANT_SQUARES & -p)) ) != 0LL;
}

static inline int enPassantSquare(const int64_t* board) {
    int status = (int)board[STATUS];
    int p = status & PLAYER_BIT;
    int eSq = status >> ESQUARE_SHIFT & SQUARE_BITS;
    return ((1LL << eSq) & ((WHITE_ENPASSANT_SQUARES & ~(-p)) |
            (BLACK_ENPASSANT_SQUARES & -p))) != 0LL ? eSq : Value.INVALID;
}

#define HALF_MOVE_CLOCK_SHIFT 11
#define HALF_MOVE_CLOCK_BITS 0x7f

static inline int halfMoveClock(const int64_t* board) {
    return (int)(board[STATUS] >> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS);
}

#define FULL_MOVE_NUMBER_SHIFT 18
#define FULL_MOVE_NUMBER_BITS 0x3ff

static inline int fullMoveNumber(const int64_t* board) {
    return (int)(board[STATUS] >> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS);
}

static inline int64_t key(const int64_t* board) {
    return board[KEY];
}

#define IN_CHECK_SHIFT 28
#define IN_CHECK_BIT 1
#define IN_CHECK_BIT_UNSHIFTED (1<<28)
#define HAS_CHECKED_SHIFT 29
#define HAS_CHECKED_BIT 1
#define HAS_CHECKED_BIT_UNSHIFTED (1<<29)

static const char* FEN_STARTING_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

int64_t* startingPosition() {
    return fromFen(FEN_STARTING_POSITION);
}

#define CASTLING_SHIFT 1
#define CASTLING_BITS 0xF
#define OCCUPANCY_BIT 8
#define SQUARE_A1 0
#define SQUARE_A8 56
#define SQUARE_H1 7
#define SQUARE_H8 63

int64_t* fromFen(const char* fen) {
    int64_t* board = (int64_t*)malloc(sizeof(int64_t)*MAX_BITBOARDS);
    int* pieces = Fen.getPieces ? Fen.getPieces(fen) : NULL;
    int64_t board0=0, board1=0, board2=0, board3=0;
    for(int square=SQUARE_A1; square<=SQUARE_H8; square++) {
        int piece = pieces ? pieces[square] : Value.NONE;
        if(piece != Value.NONE) {
            int64_t squareBit = 1LL << square;
            board0 |= -(piece & 1) & squareBit;
            board1 |= -((piece>>1) & 1) & squareBit;
            board2 |= -((piece>>2) & 1) & squareBit;
            board3 |= -((piece>>3) & 1) & squareBit;
        }
    }
    board[0]=board0; board[1]=board1; board[2]=board2; board[3]=board3;
    int whiteToMove = Fen.getWhiteToMove ? Fen.getWhiteToMove(fen) : 1;
    int castling = Fen.getCastling ? Fen.getCastling(fen) : 0;
    int64_t status = (whiteToMove ? Value.WHITE : Value.BLACK) ^ ((int64_t)castling << CASTLING_SHIFT);
    int eSquare = Fen.getEnPassantSquare ? Fen.getEnPassantSquare(fen) : Value.INVALID;
    if((whiteToMove && (eSquare > 39 && eSquare < 48)) || (!whiteToMove && (eSquare > 15 && eSquare < 24))) {
        status ^= (int64_t)eSquare << ESQUARE_SHIFT;
    } else {
        eSquare = Value.INVALID;
    }
    int half = Fen.getHalfMoveClock ? Fen.getHalfMoveClock(fen) : 0;
    int full = Fen.getFullMoveNumber ? Fen.getFullMoveNumber(fen) : 1;
    board[STATUS] = status ^ ((int64_t)half << HALF_MOVE_CLOCK_SHIFT) ^ ((int64_t)full << FULL_MOVE_NUMBER_SHIFT);
    board[KEY] = Zobrist.getKey ? Zobrist.getKey(pieces, whiteToMove, (castling & WHITE_KINGSIDE_BIT) != 0,
            (castling & WHITE_QUEENSIDE_BIT) != 0, (castling & BLACK_KINGSIDE_BIT) != 0,
            (castling & BLACK_QUEENSIDE_BIT) != 0, eSquare) : 0;
    return board;
}

#define PLAYER_SHIFT 3
#define START_PIECE_SHIFT 16
#define PIECE_BITS 0xF
#define TARGET_SQUARE_SHIFT 6
#define TARGET_PIECE_SHIFT 20
#define PROMOTE_PIECE_SHIFT 12
#define WHITE_CASTLING_BITS (WHITE_KINGSIDE_BIT | WHITE_QUEENSIDE_BIT)
#define BLACK_CASTLING_BITS (BLACK_KINGSIDE_BIT | BLACK_QUEENSIDE_BIT)

int64_t* makeMove(const int64_t* board, int64_t move) {
    int64_t* newBoard = (int64_t*)malloc(sizeof(int64_t)*MAX_BITBOARDS);
    memcpy(newBoard, board, sizeof(int64_t)*MAX_BITBOARDS);
    int status = (int)newBoard[STATUS];
    int castling = status >> CASTLING_SHIFT & CASTLING_BITS;
    int playerFlag = status & PLAYER_BIT;
    int other = 1 ^ playerFlag;
    int64_t blackMask = -playerFlag;
    int64_t whiteMask = ~blackMask;
    int eSquare = status >> ESQUARE_SHIFT & SQUARE_BITS;
    eSquare = ((1LL<<eSquare) & ((WHITE_ENPASSANT_SQUARES & whiteMask) | (BLACK_ENPASSANT_SQUARES & blackMask))) != 0LL ? eSquare : Value.INVALID;
    int originalESquare = eSquare;
    int halfMoveClockVal = (status >> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS) + 1;
    int fullMoveNumberVal = status >> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS;
    int64_t keyVal = board[KEY];
    int startSquare = (int)(move & SQUARE_BITS);
    int startPiece = (int)(move >> START_PIECE_SHIFT & PIECE_BITS);
    int startPieceType = startPiece & Piece.TYPE;
    int targetSquare = (int)(move >> TARGET_SQUARE_SHIFT & SQUARE_BITS);
    int targetPiece = (int)(move >> TARGET_PIECE_SHIFT & PIECE_BITS);
    int targetPieceType = targetPiece & Piece.TYPE;
    int64_t targetSquareBit = 1LL << targetSquare;
    int squareDiff = startSquare - targetSquare;
    int squareDiffSign = squareDiff >> 31;
    int squareDiffAbs = (squareDiff ^ squareDiffSign) - squareDiffSign;
    int64_t board0=newBoard[0], board1=newBoard[1], board2=newBoard[2], board3=newBoard[3];
    if(eSquare != Value.INVALID) {
        keyVal ^= Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
        eSquare = Value.INVALID;
    }
    if(targetPiece != Value.NONE) {
        halfMoveClockVal = 0;
        board0 ^= -(targetPiece & 1) & targetSquareBit;
        board1 ^= -((targetPiece>>1)&1) & targetSquareBit;
        board2 ^= -((targetPiece>>2)&1) & targetSquareBit;
        board3 ^= whiteMask & targetSquareBit;
        keyVal ^= Zobrist.PIECE[targetPieceType][other][targetSquare];
    }
    switch(startPieceType) {
        case Piece.QUEEN:
        case Piece.BISHOP:
        case Piece.KNIGHT: {
            int64_t pieceMoveBits = (1LL<<startSquare) | targetSquareBit;
            board0 ^= -(startPiece & 1) & pieceMoveBits;
            board1 ^= -((startPiece>>1)&1) & pieceMoveBits;
            board2 ^= -((startPiece>>2)&1) & pieceMoveBits;
            board3 ^= blackMask & pieceMoveBits;
            keyVal ^= Zobrist.PIECE[startPieceType][playerFlag][startSquare] ^
                      Zobrist.PIECE[startPieceType][playerFlag][targetSquare];
            break;
        }
        case Piece.KING: {
            int64_t pieceMoveBits = (1LL<<startSquare) | targetSquareBit;
            board0 ^= pieceMoveBits;
            board3 ^= blackMask & pieceMoveBits;
            keyVal ^= Zobrist.PIECE[Piece.KING][playerFlag][startSquare] ^
                      Zobrist.PIECE[Piece.KING][playerFlag][targetSquare];
            int playerKingSideCastling  = (castling & ((WHITE_KINGSIDE_BIT & whiteMask) |
                                                      (BLACK_KINGSIDE_BIT & blackMask))) != 0;
            int playerQueenSideCastling = (castling & ((WHITE_QUEENSIDE_BIT & whiteMask) |
                                                      (BLACK_QUEENSIDE_BIT & blackMask))) != 0;
            if(playerKingSideCastling || playerQueenSideCastling) {
                keyVal ^= (playerKingSideCastling ? Zobrist.KING_SIDE[playerFlag] : 0) ^
                          (playerQueenSideCastling ? Zobrist.QUEEN_SIDE[playerFlag] : 0);
                castling &= ~((WHITE_CASTLING_BITS & whiteMask) | (BLACK_CASTLING_BITS & blackMask));
            }
            if(squareDiffAbs == 2) {
                int64_t rookMoveBits;
                if((targetSquare & Value.FILE) == Value.FILE_G) {
                    rookMoveBits = (1LL << (targetSquare+1)) | (1LL << (targetSquare-1));
                    keyVal ^= Zobrist.PIECE[Piece.ROOK][playerFlag][targetSquare+1] ^
                              Zobrist.PIECE[Piece.ROOK][playerFlag][targetSquare-1];
                } else {
                    rookMoveBits = (1LL << (targetSquare-2)) | (1LL << (targetSquare+1));
                    keyVal ^= Zobrist.PIECE[Piece.ROOK][playerFlag][targetSquare-2] ^
                              Zobrist.PIECE[Piece.ROOK][playerFlag][targetSquare+1];
                }
                board0 ^= rookMoveBits;
                board1 ^= rookMoveBits;
                board3 ^= blackMask & rookMoveBits;
            }
            break;
        }
        case Piece.ROOK: {
            int64_t pieceMoveBits = (1LL<<startSquare) | targetSquareBit;
            board0 ^= pieceMoveBits;
            board1 ^= pieceMoveBits;
            board3 ^= blackMask & pieceMoveBits;
            keyVal ^= Zobrist.PIECE[Piece.ROOK][playerFlag][startSquare] ^
                      Zobrist.PIECE[Piece.ROOK][playerFlag][targetSquare];
            int64_t kingSideBit = Value.KINGSIDE_BIT[playerFlag];
            int64_t queenSideBit = Value.QUEENSIDE_BIT[playerFlag];
            if((castling & kingSideBit) != Value.NONE) {
                if(startSquare == ((SQUARE_H1 & whiteMask) | (SQUARE_H8 & blackMask))) {
                    castling ^= kingSideBit;
                    keyVal ^= Zobrist.KING_SIDE[playerFlag];
                }
            }
            if((castling & queenSideBit) != Value.NONE) {
                if(startSquare == ((SQUARE_A1 & whiteMask) | (SQUARE_A8 & blackMask))) {
                    castling ^= queenSideBit;
                    keyVal ^= Zobrist.QUEEN_SIDE[playerFlag];
                }
            }
            break;
        }
        case Piece.PAWN: {
            int promotePiece = (int)(move >> PROMOTE_PIECE_SHIFT & PIECE_BITS);
            halfMoveClockVal = 0;
            if(promotePiece == Value.NONE) {
                int64_t pieceMoveBits = (1LL<<startSquare) | targetSquareBit;
                board1 ^= pieceMoveBits;
                board2 ^= pieceMoveBits;
                board3 ^= blackMask & pieceMoveBits;
                keyVal ^= Zobrist.PIECE[startPieceType][playerFlag][startSquare] ^
                          Zobrist.PIECE[startPieceType][playerFlag][targetSquare];
            } else {
                int64_t startSquareBit = 1LL << startSquare;
                board0 ^= -(promotePiece & 1) & targetSquareBit;
                board1 ^= startSquareBit | (-( (promotePiece>>1) & 1) & targetSquareBit);
                board2 ^= startSquareBit | (-( (promotePiece>>2) & 1) & targetSquareBit);
                board3 ^= blackMask & (startSquareBit | targetSquareBit);
                keyVal ^= Zobrist.PIECE[startPieceType][playerFlag][startSquare] ^
                          Zobrist.PIECE[promotePiece & Piece.TYPE][playerFlag][targetSquare];
            }
            if(targetSquare == originalESquare) {
                int captureSquare = targetSquare + (int)((-8 & whiteMask) | (8 & blackMask));
                int64_t captureSquareBit = 1LL << captureSquare;
                board1 ^= captureSquareBit;
                board2 ^= captureSquareBit;
                board3 ^= whiteMask & captureSquareBit;
                keyVal ^= Zobrist.PIECE[Piece.PAWN][other][captureSquare];
            }
            if(squareDiffAbs == 16) {
                eSquare = startSquare + (int)((8 & whiteMask) | (-8 & blackMask));
                keyVal ^= Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
            }
            break;
        }
        default: break;
    }
    if(targetPieceType == Piece.ROOK) {
        if((castling & ((WHITE_KINGSIDE_BIT & blackMask) | (BLACK_KINGSIDE_BIT & whiteMask))) != Value.NONE) {
            if(targetSquare == ((SQUARE_H1 & blackMask) | (SQUARE_H8 & whiteMask))) {
                castling ^= ((WHITE_KINGSIDE_BIT & blackMask) | (BLACK_KINGSIDE_BIT & whiteMask));
                keyVal ^= Zobrist.KING_SIDE[other];
            }
        }
        if((castling & ((WHITE_QUEENSIDE_BIT & blackMask) | (BLACK_QUEENSIDE_BIT & whiteMask))) != Value.NONE) {
            if(targetSquare == ((SQUARE_A1 & blackMask) | (SQUARE_A8 & whiteMask))) {
                castling ^= ((WHITE_QUEENSIDE_BIT & blackMask) | (BLACK_QUEENSIDE_BIT & whiteMask));
                keyVal ^= Zobrist.QUEEN_SIDE[other];
            }
        }
    }
    newBoard[0]=board0; newBoard[1]=board1; newBoard[2]=board2; newBoard[3]=board3;
    newBoard[STATUS] = (1 ^ playerFlag) |
                       ((int64_t)castling << CASTLING_SHIFT) |
                       (eSquare != Value.INVALID ? ((int64_t)eSquare << ESQUARE_SHIFT) : 0) |
                       ((int64_t)halfMoveClockVal << HALF_MOVE_CLOCK_SHIFT) |
                       ((int64_t)(fullMoveNumberVal + playerFlag) << FULL_MOVE_NUMBER_SHIFT);
    newBoard[KEY] = keyVal ^ Zobrist.WHITEMOVE;
    return newBoard;
}

void makeMoveWith(int64_t* board, int64_t move) {
    int status = (int)board[STATUS];
    int castling = status >> CASTLING_SHIFT & CASTLING_BITS;
    int playerFlag = status & PLAYER_BIT;
    int other = 1 ^ playerFlag;
    int64_t blackMask = -playerFlag;
    int64_t whiteMask = ~blackMask;
    int eSquare = status >> ESQUARE_SHIFT & SQUARE_BITS;
    eSquare = ((1LL<<eSquare) & ((WHITE_ENPASSANT_SQUARES & whiteMask) | (BLACK_ENPASSANT_SQUARES & blackMask))) != 0LL ? eSquare : Value.INVALID;
    int originalESquare = eSquare;
    int halfMoveClockVal = (status >> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS) + 1;
    int fullMoveNumberVal = status >> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS;
    int64_t keyVal = board[KEY];
    int startSquare = (int)(move & SQUARE_BITS);
    int startPiece = (int)(move >> START_PIECE_SHIFT & PIECE_BITS);
    int startPieceType = startPiece & Piece.TYPE;
    int targetSquare = (int)(move >> TARGET_SQUARE_SHIFT & SQUARE_BITS);
    int targetPiece = (int)(move >> TARGET_PIECE_SHIFT & PIECE_BITS);
    int targetPieceType = targetPiece & Piece.TYPE;
    int64_t targetSquareBit = 1LL << targetSquare;
    int squareDiff = startSquare - targetSquare;
    int squareDiffSign = squareDiff >> 31;
    int squareDiffAbs = (squareDiff ^ squareDiffSign) - squareDiffSign;
    int64_t board0=board[0], board1=board[1], board2=board[2], board3=board[3];
    if(eSquare != Value.INVALID) {
        keyVal ^= Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
        eSquare = Value.INVALID;
    }
    if(targetPiece != Value.NONE) {
        halfMoveClockVal = 0;
        board0 ^= -(targetPiece & 1) & targetSquareBit;
        board1 ^= -((targetPiece>>1)&1) & targetSquareBit;
        board2 ^= -((targetPiece>>2)&1) & targetSquareBit;
        board3 ^= whiteMask & targetSquareBit;
        keyVal ^= Zobrist.PIECE[targetPieceType][other][targetSquare];
    }
    switch(startPieceType) {
        case Piece.QUEEN:
        case Piece.BISHOP:
        case Piece.KNIGHT: {
            int64_t pieceMoveBits = (1LL<<startSquare) | targetSquareBit;
            board0 ^= -(startPiece & 1) & pieceMoveBits;
            board1 ^= -((startPiece>>1)&1) & pieceMoveBits;
            board2 ^= -((startPiece>>2)&1) & pieceMoveBits;
            board3 ^= blackMask & pieceMoveBits;
            keyVal ^= Zobrist.PIECE[startPieceType][playerFlag][startSquare] ^
                      Zobrist.PIECE[startPieceType][playerFlag][targetSquare];
            break;
        }
        case Piece.KING: {
            int64_t pieceMoveBits = (1LL<<startSquare) | targetSquareBit;
            board0 ^= pieceMoveBits;
            board3 ^= blackMask & pieceMoveBits;
            keyVal ^= Zobrist.PIECE[Piece.KING][playerFlag][startSquare] ^
                      Zobrist.PIECE[Piece.KING][playerFlag][targetSquare];
            int playerKingSideCastling  = (castling & ((WHITE_KINGSIDE_BIT & whiteMask) |
                                                      (BLACK_KINGSIDE_BIT & blackMask))) != 0;
            int playerQueenSideCastling = (castling & ((WHITE_QUEENSIDE_BIT & whiteMask) |
                                                      (BLACK_QUEENSIDE_BIT & blackMask))) != 0;
            if(playerKingSideCastling || playerQueenSideCastling) {
                keyVal ^= (playerKingSideCastling ? Zobrist.KING_SIDE[playerFlag] : 0) ^
                          (playerQueenSideCastling ? Zobrist.QUEEN_SIDE[playerFlag] : 0);
                castling &= ~((WHITE_CASTLING_BITS & whiteMask) | (BLACK_CASTLING_BITS & blackMask));
            }
            if(squareDiffAbs == 2) {
                int64_t rookMoveBits;
                if((targetSquare & Value.FILE) == Value.FILE_G) {
                    rookMoveBits = (1LL << (targetSquare+1)) | (1LL << (targetSquare-1));
                    keyVal ^= Zobrist.PIECE[Piece.ROOK][playerFlag][targetSquare+1] ^
                              Zobrist.PIECE[Piece.ROOK][playerFlag][targetSquare-1];
                } else {
                    rookMoveBits = (1LL << (targetSquare-2)) | (1LL << (targetSquare+1));
                    keyVal ^= Zobrist.PIECE[Piece.ROOK][playerFlag][targetSquare-2] ^
                              Zobrist.PIECE[Piece.ROOK][playerFlag][targetSquare+1];
                }
                board0 ^= rookMoveBits;
                board1 ^= rookMoveBits;
                board3 ^= blackMask & rookMoveBits;
            }
            break;
        }
        case Piece.ROOK: {
            int64_t pieceMoveBits = (1LL<<startSquare) | targetSquareBit;
            board0 ^= pieceMoveBits;
            board1 ^= pieceMoveBits;
            board3 ^= blackMask & pieceMoveBits;
            keyVal ^= Zobrist.PIECE[Piece.ROOK][playerFlag][startSquare] ^
                      Zobrist.PIECE[Piece.ROOK][playerFlag][targetSquare];
            int64_t kingSideBit = Value.KINGSIDE_BIT[playerFlag];
            int64_t queenSideBit = Value.QUEENSIDE_BIT[playerFlag];
            if((castling & kingSideBit) != Value.NONE) {
                if(startSquare == ((SQUARE_H1 & whiteMask) | (SQUARE_H8 & blackMask))) {
                    castling ^= kingSideBit;
                    keyVal ^= Zobrist.KING_SIDE[playerFlag];
                }
            }
            if((castling & queenSideBit) != Value.NONE) {
                if(startSquare == ((SQUARE_A1 & whiteMask) | (SQUARE_A8 & blackMask))) {
                    castling ^= queenSideBit;
                    keyVal ^= Zobrist.QUEEN_SIDE[playerFlag];
                }
            }
            break;
        }
        case Piece.PAWN: {
            int promotePiece = (int)(move >> PROMOTE_PIECE_SHIFT & PIECE_BITS);
            halfMoveClockVal = 0;
            if(promotePiece == Value.NONE) {
                int64_t pieceMoveBits = (1LL<<startSquare) | targetSquareBit;
                board1 ^= pieceMoveBits;
                board2 ^= pieceMoveBits;
                board3 ^= blackMask & pieceMoveBits;
                keyVal ^= Zobrist.PIECE[startPieceType][playerFlag][startSquare] ^
                          Zobrist.PIECE[startPieceType][playerFlag][targetSquare];
            } else {
                int64_t startSquareBit = 1LL << startSquare;
                board0 ^= -(promotePiece & 1) & targetSquareBit;
                board1 ^= startSquareBit | (-((promotePiece>>1)&1) & targetSquareBit);
                board2 ^= startSquareBit | (-((promotePiece>>2)&1) & targetSquareBit);
                board3 ^= blackMask & (startSquareBit | targetSquareBit);
                keyVal ^= Zobrist.PIECE[startPieceType][playerFlag][startSquare] ^
                          Zobrist.PIECE[promotePiece & Piece.TYPE][playerFlag][targetSquare];
            }
            if(targetSquare == originalESquare) {
                int captureSquare = targetSquare + (int)((-8 & whiteMask) | (8 & blackMask));
                int64_t captureSquareBit = 1LL << captureSquare;
                board1 ^= captureSquareBit;
                board2 ^= captureSquareBit;
                board3 ^= whiteMask & captureSquareBit;
                keyVal ^= Zobrist.PIECE[Piece.PAWN][other][captureSquare];
            }
            if(squareDiffAbs == 16) {
                eSquare = startSquare + (int)((8 & whiteMask) | (-8 & blackMask));
                keyVal ^= Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
            }
            break;
        }
        default: break;
    }
    if(targetPieceType == Piece.ROOK) {
        if((castling & ((WHITE_KINGSIDE_BIT & blackMask) | (BLACK_KINGSIDE_BIT & whiteMask))) != Value.NONE) {
            if(targetSquare == ((SQUARE_H1 & blackMask) | (SQUARE_H8 & whiteMask))) {
                castling ^= ((WHITE_KINGSIDE_BIT & blackMask) | (BLACK_KINGSIDE_BIT & whiteMask));
                keyVal ^= Zobrist.KING_SIDE[other];
            }
        }
        if((castling & ((WHITE_QUEENSIDE_BIT & blackMask) | (BLACK_QUEENSIDE_BIT & whiteMask))) != Value.NONE) {
            if(targetSquare == ((SQUARE_A1 & blackMask) | (SQUARE_A8 & whiteMask))) {
                castling ^= ((WHITE_QUEENSIDE_BIT & blackMask) | (BLACK_QUEENSIDE_BIT & whiteMask));
                keyVal ^= Zobrist.QUEEN_SIDE[other];
            }
        }
    }
    board[0]=board0; board[1]=board1; board[2]=board2; board[3]=board3;
    board[STATUS] = (1 ^ playerFlag) |
                    ((int64_t)castling << CASTLING_SHIFT) |
                    (eSquare != Value.INVALID ? ((int64_t)eSquare << ESQUARE_SHIFT) : 0) |
                    ((int64_t)halfMoveClockVal << HALF_MOVE_CLOCK_SHIFT) |
                    ((int64_t)(fullMoveNumberVal + playerFlag) << FULL_MOVE_NUMBER_SHIFT);
    board[KEY] = keyVal ^ Zobrist.WHITEMOVE;
}

#define ENPASSANT_RESET_BITS (~((int64_t)SQUARE_BITS << ESQUARE_SHIFT))

int64_t* nullMove(const int64_t* board) {
    int64_t* newBoard = (int64_t*)malloc(sizeof(int64_t)*MAX_BITBOARDS);
    memcpy(newBoard, board, sizeof(int64_t)*MAX_BITBOARDS);
    int64_t keyVal = board[KEY];
    int eSquare = (int)(newBoard[STATUS] >> ESQUARE_SHIFT & SQUARE_BITS);
    if(eSquare > 0) {
        keyVal ^= Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
    }
    keyVal ^= Zobrist.WHITEMOVE;
    newBoard[STATUS] = (board[STATUS] ^ PLAYER_BIT) & ENPASSANT_RESET_BITS;
    newBoard[KEY] = keyVal;
    return newBoard;
}

void nullMoveWith(int64_t* board) {
    int64_t keyVal = board[KEY];
    int eSquare = (int)(board[STATUS] >> ESQUARE_SHIFT & SQUARE_BITS);
    if(eSquare > 0) {
        keyVal ^= Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
    }
    keyVal ^= Zobrist.WHITEMOVE;
    board[STATUS] = (board[STATUS] ^ PLAYER_BIT) & ENPASSANT_RESET_BITS;
    board[KEY] = keyVal;
}

int getSquare_arr(const int64_t* board, int square) {
    return (int)(((board[3] >> square & 1) << 3) |
                 ((board[2] >> square & 1) << 2) |
                 ((board[1] >> square & 1) << 1) |
                 ((board[0] >> square & 1)));
}

int getSquare_vals(int64_t board0, int64_t board1, int64_t board2, int64_t board3, int square) {
    return (int)(((board3 >> square & 1) << 3) |
                 ((board2 >> square & 1) << 2) |
                 ((board1 >> square & 1) << 1) |
                 ((board0 >> square & 1)));
}

int isSquareAttackedByPlayer_arr(const int64_t* board, int square, int playerFlag) {
    int other = 1 ^ playerFlag;
    int64_t colorMask = -other ^ board[3];
    if((LEAP_ATTACKS[square] & board[0] & ~board[1] & board[2] & colorMask) != 0LL) return 1;
    if((PAWN_ATTACKS[other][square] & ~board[0] & board[1] & board[2] & colorMask) != 0LL) return 1;
    if((KING_ATTACKS[square] & board[0] & ~board[1] & ~board[2] & colorMask) != 0LL) return 1;
    int64_t allOcc = board[0] | board[1] | board[2];
    if((Magic.bishopMoves ? Magic.bishopMoves(square, allOcc) : 0LL) & ~board[0] & (board[1]^board[2]) & colorMask) return 1;
    if((Magic.rookMoves ? Magic.rookMoves(square, allOcc) : 0LL) & board[1] & ~board[2] & colorMask) return 1;
    return 0;
}

int isSquareAttackedByPlayer_vals(int64_t board0, int64_t board1, int64_t board2, int64_t board3, int square, int playerFlag) {
    int other = 1 ^ playerFlag;
    int64_t colorMask = -other ^ board3;
    if((LEAP_ATTACKS[square] & board0 & ~board1 & board2 & colorMask) != 0LL) return 1;
    if((PAWN_ATTACKS[other][square] & ~board0 & board1 & board2 & colorMask) != 0LL) return 1;
    if((KING_ATTACKS[square] & board0 & ~board1 & ~board2 & colorMask) != 0LL) return 1;
    int64_t allOcc = board0 | board1 | board2;
    if((Magic.bishopMoves ? Magic.bishopMoves(square, allOcc) : 0LL) & ~board0 & (board1^board2) & colorMask) return 1;
    if((Magic.rookMoves ? Magic.rookMoves(square, allOcc) : 0LL) & board1 & ~board2 & colorMask) return 1;
    return 0;
}

int isPlayerInCheck_arr(const int64_t* board, int playerFlag) {
    int64_t board0 = board[0];
    int64_t board1 = board[1];
    int64_t board2 = board[2];
    int64_t colorMask = -playerFlag ^ board[3];
    int64_t bitboard = board0 & ~board1 & ~board2 & ~colorMask;
    int square = BitOps.LSB[(int)(((bitboard & -bitboard) * BitOps.DB) >> 58)];
    if((LEAP_ATTACKS[square] & board0 & ~board1 & board2 & colorMask) != 0LL) return 1;
    if((PAWN_ATTACKS[playerFlag][square] & ~board0 & board1 & board2 & colorMask) != 0LL) return 1;
    if((KING_ATTACKS[square] & board0 & ~board1 & ~board2 & colorMask) != 0LL) return 1;
    int64_t allOcc = board0 | board1 | board2;
    if((Magic.bishopMoves ? Magic.bishopMoves(square, allOcc) : 0LL) & ~board0 & (board1^board2) & colorMask) return 1;
    if((Magic.rookMoves ? Magic.rookMoves(square, allOcc) : 0LL) & board1 & ~board2 & colorMask) return 1;
    return 0;
}

int isPlayerInCheck_vals(int64_t board0, int64_t board1, int64_t board2, int64_t board3, int playerFlag) {
    int64_t colorMask = -playerFlag ^ board3;
    int64_t bitboard = board0 & ~board1 & ~board2 & ~colorMask;
    int square = LSB[(int)(((bitboard & -bitboard) * DB) >> 58)];
    if((LEAP_ATTACKS[square] & board0 & ~board1 & board2 & colorMask) != 0LL) return 1;
    if((PAWN_ATTACKS[playerFlag][square] & ~board0 & board1 & board2 & colorMask) != 0LL) return 1;
    if((KING_ATTACKS[square] & board0 & ~board1 & ~board2 & colorMask) != 0LL) return 1;
    int64_t allOcc = board0 | board1 | board2;
    if((Magic.bishopMoves ? Magic.bishopMoves(square, allOcc) : 0LL) & ~board0 & (board1^board2) & colorMask) return 1;
    if((Magic.rookMoves ? Magic.rookMoves(square, allOcc) : 0LL) & board1 & ~board2 & colorMask) return 1;
    return 0;
}

int countPiece(const int64_t* board, int piece) {
    int64_t bb = (-(piece & 1) & board[0]) &
                 (-((piece>>1)&1) & board[1]) &
                 (-((piece>>2)&1) & board[2]) &
                 (-((piece>>3)&1) & board[3]);
    /* substitute for Long.bitCount */
    int count=0; uint64_t x = (uint64_t)bb; while(x){count+=x&1; x>>=1;} return count;
}

int countMaterialPieces(const int64_t* board, int playerFlag) {
    int playerBit = playerFlag << 3;
    return countPiece(board, Piece.QUEEN | playerBit) +
           countPiece(board, Piece.ROOK  | playerBit) +
           countPiece(board, Piece.BISHOP| playerBit) +
           countPiece(board, Piece.KNIGHT| playerBit);
}

char* boardString(const int64_t* board) {
    char* str = (char*)malloc(64*2 + 8); /* rough size */
    char* p = str;
    for(int i=SQUARE_A1;i<=SQUARE_H8;i++) {
        int square = i ^ 0x38;
        int piece = getSquare_arr(board, square);
        char ch = (piece != Value.NONE) ? Piece.SHORT_STRING[piece] : '.';
        *p++ = ch;
        *p++ = ((i & 7)==7) ? '\n' : ' ';
    }
    *p = '\0';
    return str;
}

char* squareToString(int square) {
    char* buf = (char*)malloc(3);
    buf[0] = Value.FILE_STRING[square & Value.FILE];
    buf[1] = '0' + ((square >> 3) + 1);
    buf[2] = '\0';
    return buf;
}

static int64_t LEAP_ATTACKS[64];
static int64_t KING_ATTACKS_ARR[64];
static int64_t PAWN_ATTACKS[2][64];

__attribute__((constructor)) static void init_arrays() {
    for(int sq=0; sq<64; sq++) {
        LEAP_ATTACKS[sq] = Bitboard.BB[Bitboard.LEAP_ATTACKS][sq];
        KING_ATTACKS_ARR[sq] = Bitboard.BB[Bitboard.KING_ATTACKS][sq];
    }
    for(int pl=0; pl<2; pl++) {
        for(int sq=0; sq<64; sq++) {
            PAWN_ATTACKS[pl][sq] = Bitboard.BB[Bitboard.PAWN_ATTACKS_PLAYER0 + pl][sq];
        }
    }
}

char* toFen(const int64_t* board) {
    char* fen = (char*)malloc(128); /* approximate */
    char tmp[4];
    fen[0]='\0';
    char* p = fen;
    for(int rank=7; rank>=0; rank--) {
        int empty=0;
        for(int file=0; file<8; file++) {
            int square = rank<<3 | file;
            int piece = getSquare_arr(board, square);
            if(piece != Value.NONE) {
                if(empty>0) { sprintf(tmp, "%d", empty); strcat(p,tmp); p+=strlen(tmp); empty=0; }
                char ch = Piece.SHORT_STRING[piece];
                *p++ = ch; *p = '\0';
            } else {
                empty++;
            }
        }
        if(empty>0) { sprintf(tmp, "%d", empty); strcat(p,tmp); p+=strlen(tmp); }
        if(rank>0) { *p++='/'; *p='\0'; }
    }
    sprintf(p," %c ", player(board)==Value.WHITE?'w':'b'); p+=strlen(p);
    if(((int)board[STATUS]>>CASTLING_SHIFT & CASTLING_BITS)==0) {
        strcat(p,"- ");
    } else {
        if(kingSide(board, Value.WHITE)) strcat(p,"K");
        if(queenSide(board, Value.WHITE)) strcat(p,"Q");
        if(kingSide(board, Value.BLACK)) strcat(p,"k");
        if(queenSide(board, Value.BLACK)) strcat(p,"q");
        strcat(p," ");
    }
    if(hasValidEnPassantSquare(board)) {
        char* sq = squareToString(enPassantSquare(board));
        strcat(p,sq); free(sq); strcat(p," ");
    } else {
        strcat(p,"- ");
    }
    sprintf(tmp, "%d", halfMoveClock(board)); strcat(p,tmp); strcat(p," ");
    sprintf(tmp, "%d", fullMoveNumber(board)); strcat(p,tmp);
    return fen;
}

char* boardToString(const int64_t* board) {
    char* buf = (char*)malloc(256);
    sprintf(buf, "Player: %s\nCastling Rights: %s%s%s%s\nEnPassant Square: %d\nHalf Move Clock: %d\nFull Move Number: %d\nZobrist Key Decimal: %lld\nZobrist Key Hex: %llx",
        player(board)==0?"White":"Black",
        kingSide(board,0)?"K":"",
        queenSide(board,0)?"Q":"",
        kingSide(board,1)?"k":"",
        queenSide(board,1)?"q":"",
        enPassantSquare(board),
        halfMoveClock(board),
        fullMoveNumber(board),
        (long long)board[KEY],
        (unsigned long long)board[KEY]);
    return buf;
}

static const int LSB[] = {
        0,  1, 48,  2, 57, 49, 28,  3,
                61, 58, 50, 42, 38, 29, 17,  4,
                62, 55, 59, 36, 53, 51, 43, 22,
                45, 39, 33, 30, 24, 18, 12,  5,
                63, 47, 56, 27, 60, 41, 37, 16,
                54, 35, 52, 21, 44, 32, 23, 11,
                46, 26, 40, 15, 34, 20, 31, 10,
                25, 14, 19,  9, 13,  8,  7,  6
};
static const uint64_t DB = 0x03f79d71b4cb0a89ULL;

/*
 * How to use the 4 bitboard board:
 * (Comments preserved from Java source)
 */

