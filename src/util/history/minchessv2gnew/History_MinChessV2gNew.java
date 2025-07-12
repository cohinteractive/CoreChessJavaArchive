package com.ohinteractive.minchessv2gnew.util;

import com.ohinteractive.minchessv2gnew.impl.Board;

public class History_MinChessV2gNew {
    
    public History_MinChessV2gNew(int capacity, long[] board) {
        this.history = new long[capacity][]; // stores all board state and the move used to get to a state
        for(int i = 0; i < capacity; i ++) {
            this.history[i] = new long[MAX_ELEMENTS];
        }
        System.arraycopy(board, 0, this.history[0], 0, board.length);
    }

    public void addState(long[] board, long move) {
        this.index ++;
        System.arraycopy(board, 0, this.history[this.index], 0, board.length);
        this.history[this.index][MOVE_ELEMENT] = move;
        this.history[this.index + 1][MOVE_ELEMENT] = 0L;
    }

    public long[] undo() {
        if(this.index == 0) return new long[0];
        return getNewBoard(-- this.index);
    }

    public long key() {
        return this.history[this.index][KEY_ELEMENT];
    }

    public long[] redo() {
        if(this.history[this.index + 1][MOVE_ELEMENT] == 0L) return new long[0];
        return getNewBoard(++ this.index);
    }

    public int index() {
        return this.index;
    }

    public long[] createKeyHistory() {
        long[] keyHistory = new long[this.index + 1];
        for(int i = 0; i <= this.index; i ++) {
            keyHistory[i] = this.history[i][KEY_ELEMENT];
        }
        return keyHistory;
    }

    public long[] createMoveHistory() {
        if(this.index == 0) return new long[0];
        long[] moveHistory = new long[this.index];
        for(int i = 0; i < this.index; i ++) {
            moveHistory[i] = this.history[i + 1][MOVE_ELEMENT];
        }
        return moveHistory;
    }

    public boolean isEmpty() {
        return this.index == 0;
    }

    private static final int MAX_ELEMENTS = Board.MAX_BITBOARDS + 1;
    private static final int MOVE_ELEMENT = MAX_ELEMENTS - 1;
    private static final int KEY_ELEMENT = Board.KEY;

    private int index;
    private long[][] history;

    private long[] getNewBoard(int index) {
        long[] newBoard = new long[MAX_ELEMENTS];
        System.arraycopy(this.history[index], 0, newBoard, 0, newBoard.length);
        return newBoard;
    }

}
