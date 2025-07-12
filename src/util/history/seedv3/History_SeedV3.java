package com.ohinteractive.seedv3.util;

public class History_SeedV3 {
    
    public History_SeedV3() {
        this(512);
    }

    public History_SeedV3(int capacity) {
        this.history = new Entry[capacity];
        this.repetitionMap = new HistoryMap_SeedV3();
        this.size = 0;
        this.repetitionDetected = false;
        this.repetitionCount = 0;
    }

    public void add(long key, long move) {
        history[size ++] = new Entry(key, move);
        if(repetitionMap.increment(key) == 3) {
            repetitionCount ++;
            repetitionDetected = true;
        }
    }

    public long getKey(int index) {
        return this.history[index].key;
    }

    public long getMove(int index) {
        return this.history[index].move;
    }

    public void pop() {
        if(size == 0) return;
        Entry entry = this.history[-- size];
        if(repetitionMap.decrement(entry.key) == 2) {
            if(repetitionCount > 0) repetitionCount --;
        }
        this.history[size] = new Entry(0L, 0L);
        repetitionDetected = repetitionCount > 0;
    }

    public void reset() {
        this.size = 0;
        this.repetitionDetected = false;
        this.repetitionCount = 0;
        repetitionMap.reset();
    }

    public boolean isRepetition() {
        return this.repetitionDetected;
    }

    public int size() {
        return this.size;
    }

    private record Entry(long key, long move) {}
    private final Entry[] history;
    private final HistoryMap_SeedV3 repetitionMap;
    private int size;
    private boolean repetitionDetected;
    private int repetitionCount;

}
