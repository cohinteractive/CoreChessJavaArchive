package com.ohinteractive.minchessv2gnew.util;

import java.util.concurrent.ConcurrentHashMap;

public class TTable_MinChessV2gNew {

    public static final int INVALID = -1;
    public static final int ALPHA = 0;
    public static final int BETA = 1;
    public static final int EXACT = 2;
    
    public static class TTEntry {

        public TTEntry() {
            this.type = INVALID;
        }

        public TTEntry(long key, int eval, int depth, int type, long hashMove) {
            this.key = key;
            this.eval = eval;
            this.depth = depth;
            this.type = type;
            this.hashMove = hashMove;
        }

        public int eval() {
            return this.eval;
        }

        public int depth() {
            return this.depth;
        }

        public int type() {
            return this.type;
        }

        public long hashMove() {
            return this.hashMove;
        }

        private long key;
        private int eval;
        private int depth;
        private int type;
        private long hashMove;

    }

    public TTable_MinChessV2gNew() {
        this(DEFAULT_TABLE_SIZE_IN_MB);
    }

    public TTable_MinChessV2gNew(int sizeInMB) {
        long totalBytes = (long) sizeInMB * 1024 * 1024;
        int entries = (int) (totalBytes / ENTRY_SIZE_IN_BYTES);
        this.tableSize = calculateTableSize(entries);
        this.table = new ConcurrentHashMap<>(this.tableSize + 1);
    }

    public TTEntry probe(long key) {
        TTEntry entry = this.table.get(key);
        if(entry == null) return new TTEntry();
        return entry.key == key ? entry : new TTEntry();
    }

    public void save(long key, int eval, int depth, int type, long hashKey) {
        TTEntry entry = new TTEntry(key, eval, depth, type, hashKey);
        this.table.merge(key, entry, (oldEntry, newEntry) -> {
            if(newEntry.depth > oldEntry.depth) return newEntry;
            if(newEntry.depth == oldEntry.depth && newEntry.type > oldEntry.type) return newEntry;
            return oldEntry;
        });
    }

    private static final int ENTRY_SIZE_IN_BYTES = 28;
    private static final int DEFAULT_TABLE_SIZE_IN_MB = 128;

    private int tableSize;
    private ConcurrentHashMap<Long, TTEntry> table;

    private int calculateTableSize(int minEntries) {
        int n = 1;
        while((1 << n) - 1 < minEntries) n ++;
        return (1 << n) - 1;
    }  

}
