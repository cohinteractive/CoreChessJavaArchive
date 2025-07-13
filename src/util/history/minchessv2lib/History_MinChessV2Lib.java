package com.ohinteractive.minchessv2lib.util;

public class History_MinChessV2Lib {
    
    public History_MinChessV2Lib() {
        this(512);
    }

    public History_MinChessV2Lib(int capacity) {
        this.history = new Entry[capacity];
        this.repetitionMap = new HistoryMap_MinChessV2Lib();
        this.size = 0;
        this.repetitionDetected = false;
    }

    public void add(long key, long move) {
        history[size ++] = new Entry(key, move);
        if(repetitionMap.increment(key) == 3) {
            repetitionDetected = true;
        }
    }

    public void reset() {
        this.size = 0;
        this.repetitionDetected = false;
        repetitionMap.reset();
    }

    public boolean isRepetition() {
        return this.repetitionDetected;
    }

    private record Entry(long key, long move) {}
    private final Entry[] history;
    private final HistoryMap_MinChessV2Lib repetitionMap;
    private int size;
    private boolean repetitionDetected;

}
