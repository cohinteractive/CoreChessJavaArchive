package com.ohinteractive.minchessv2lib.util;

import com.ohinteractive.minchessv2lib.impl.Board;
import com.ohinteractive.minchessv2lib.impl.Gen;

public class Sort_MinChessV2Lib {
    
    public static void sortByEval(long[] array) {
        int size = (int) array[Gen.MOVELIST_SIZE];
        hybridSort(array, 0, size - 1, OPTIMAL_THRESHOLD[size]);
    }

    public static void sortByCaptures(long[] board, long[] array) {
        int moveListSize = (int) array[Gen.MOVELIST_SIZE];
        if(moveListSize < 2) return;
        int other = (int) ((8 ^ array[0]) & 8) >>> 3;
        for(int i = 0; i < moveListSize; i ++) {
            long move = array[i];
            long sortScore = 0;
            int startType = (int) move >>> Board.START_PIECE_SHIFT & Piece.TYPE;
            int targetType = (int) move >>> Board.TARGET_PIECE_SHIFT & Piece.TYPE;
            int promoteType = (int) move >>> Board.PROMOTE_PIECE_SHIFT & Piece.TYPE;
            int targetSquare = (int) move >>> Board.TARGET_SQUARE_SHIFT & Board.SQUARE_BITS;
            if(promoteType != Piece.EMPTY) {
                sortScore += PROMOTE + Piece.VALUE[promoteType] + (targetType != Piece.EMPTY ? CAPTURE : 0);
            } else {
                if(targetType != Piece.EMPTY) {
                    int valueDifference = Piece.VALUE[targetType] - Piece.VALUE[startType];
                    sortScore += valueDifference + (valueDifference > 50 ? HIGH + valueDifference : valueDifference > -50 ? LOW + valueDifference : !Board.isSquareAttackedByPlayer(board, targetSquare, other) ? LESS : 0);
                }
            }
            array[i] = (move & 0xffffffffL) | (sortScore << 32);
        }
        hybridSort(array, 0, moveListSize - 1, OPTIMAL_THRESHOLD[moveListSize]);
        for(int i = 0; i < moveListSize; i ++) {
            array[i] &= 0xffffffffL;
        }
    }

    public static void sortByHistory(long[] array, int[] history) {
        int arrayLength = (int) array[Gen.MOVELIST_SIZE];
        if(arrayLength < 2) return;
        for(int moveIndex = 0; moveIndex < arrayLength; moveIndex ++) {
            long move = array[moveIndex];
            int squareTarget = (int) move & 0xfff;
            array[moveIndex] = (move & 0xffffffffL) | ((long) history[squareTarget] << 32);
        }
        hybridSort(array, 0, arrayLength - 1, OPTIMAL_THRESHOLD[arrayLength]);
        for(int i = 0; i < arrayLength; i ++) {
            array[i] &= 0xffffffffL;
        }
    }

    public static void sortByKiller(long[] board, long[] array, int killer1, int killer2) {
        sortByHashMove(board, array, killer1, killer2, 0);
    }

    public static void sortByHashMove(long[] board, long[] array, int killer1, int killer2, long hashMove) {
        int arrayLength = (int) array[Gen.MOVELIST_SIZE];
        if(arrayLength < 2) return;
        int other = (int) ((8 ^ array[0]) & 8) >>> 3;
        for(int moveIndex = 0; moveIndex < arrayLength; moveIndex ++) {
            long move = array[moveIndex];
            long sortScore = 0;
            int startTarget = (int) move & 0xfff;
            if(startTarget == (hashMove & 0xfffL)) {
                array[moveIndex] = (move & 0xffffffffL) | ((long) HASH << 32);
                continue;
            }
            int startType = (int) move >>> Board.START_PIECE_SHIFT & Piece.TYPE;
            int targetType = (int) move >>> Board.TARGET_PIECE_SHIFT & Piece.TYPE;
            int promoteType = (int) move >>> Board.PROMOTE_PIECE_SHIFT & Piece.TYPE;
            int targetSquare = (int) move >>> Board.TARGET_SQUARE_SHIFT & Board.SQUARE_BITS;
            if(promoteType != Piece.EMPTY) {
                sortScore += PROMOTE + Piece.VALUE[promoteType] + (targetType != Piece.EMPTY ? CAPTURE : 0);
            } else {
                if(targetType != Piece.EMPTY) {
                    int valueDifference = Piece.VALUE[targetType] - Piece.VALUE[startType];
                    sortScore += valueDifference + (valueDifference > 50 ? HIGH + valueDifference : valueDifference > -50 ? LOW + valueDifference : !Board.isSquareAttackedByPlayer(board, targetSquare, other) ? LESS : 0);
                }
            }
            sortScore += (startTarget == (killer1 & 0xfff)) ? KILLER_HIGH : (startTarget == (killer2 & 0xfff)) ? KILLER_LOW : 0;
            array[moveIndex] = (move & 0xffffffffL) | (sortScore << 32);
        }
        hybridSort(array, 0, arrayLength - 1, OPTIMAL_THRESHOLD[arrayLength]);
        for(int i = 0; i < arrayLength; i ++) {
            array[i] &= 0xffffffffL;
        }
    }

    private static final int HASH = 200000;
    private static final int PROMOTE = 100000;
    private static final int HIGH = 80000;
    private static final int LOW = 60000;
    private static final int KILLER_HIGH = 40000;
    private static final int KILLER_LOW = 30000;
    private static final int CAPTURE = 20000;
    private static final int LESS = 10000;
    private static final int[] OPTIMAL_THRESHOLD = {
        0, 1, 2, 3, 4, 4, 5, 5, 5, 5, 5, 6,
        6, 6, 6, 6, 6, 6, 6, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 8, 8, 8, 8, 8, 8, 8, 8,
        8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
        8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
        8, 8, 9, 9, 9, 9, 9, 9, 9, 9,
        9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
        9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
        9, 9, 9, 9, 9, 9, 9, 9, 9, 9
    };

    private Sort_MinChessV2Lib() {}

    private static void hybridSort(long[] array, int begin, int end, int threshold) {
        if(end - begin <= threshold) {
            insertionSort(array, begin, end);
            return;
        }
        int partitionIndex = partition(array, begin, end);
        hybridSort(array, begin, partitionIndex - 1, threshold);
        hybridSort(array, partitionIndex + 1, end, threshold);
    }

    private static void insertionSort(long[] array, int begin, int end) {
        for(int i = begin + 1; i <= end; i ++) {
            long key = array[i];
            int j = i - 1;
            while(j >= begin && array[j] < key) {
                array[j + 1] = array[j --];
            }
            array[j + 1] = key;
        }
    }

    private static int partition(long[] array, int begin, int end) {
        long pivot = array[end];
        int i = begin - 1;
        long temp;
        for(int j = begin; j < end; j ++) {
            if(array[j] > pivot) {
                temp = array[++ i];
                array[i] = array[j];
                array[j] = temp;
            }
        }
        temp = array[++ i];
        array[i] = array[end];
        array[end] = temp;
        return i;
    }

}
