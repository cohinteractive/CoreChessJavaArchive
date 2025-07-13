package com.ohinteractive.minchessv2lib.util;

public class TTable_MinChessV2Lib {

    public record TEntry(long data, long hashMove) {}

    static final class StripeLock {
        /*
         * Each StripeLock object has unused padding to separate it from other
         * nearby StripeLock objects in memory to assist with cache-line collisions
         * A cache-line collision means that two variables being worked on in
         * separate threads might exist in the same cache-line, usually a
         * 64 byte block, and they will get
         * synchronized so only one can be worked on at a time.
         * Adding padding means that each StripeLock has enough room not to be
         * synchronized by the CPU and they are almost guaranteed to be able to
         * work independently
         */

        @SuppressWarnings("unused")
        private volatile long
        p0, p1, p2, p3, p4, p5, p6,
        p7, p8, p9, p10, p11, p12, p13;

    }
    
    public static final int TYPE_EVAL = -1;
    public static final int TYPE_EXACT = 0;
    public static final int TYPE_LOWER = 1;
    public static final int TYPE_UPPER = 2;
    public static final long TYPE_INVALID = Long.MIN_VALUE;
    public static final TEntry NULL_ENTRY = new TEntry(TYPE_INVALID, 0L);

    public TTable_MinChessV2Lib() {
        this(DEFAULT_TABLE_SIZE_IN_MB);
    }

    public TTable_MinChessV2Lib(int sizeInMb) {
        int entrySizeInBytes = 24; // three longs, key + data + hashMove
        int totalBytes = sizeInMb * 1024 * 1024;
        int rawEntries = totalBytes / entrySizeInBytes;
        int entryCount = Integer.highestOneBit(rawEntries);
        this.key = new long[entryCount];
        this.data = new long[entryCount];
        this.hashMove = new long[entryCount];
        this.indexMask = entryCount - 1;
        this.locks = new StripeLock[STRIPE_COUNT];
        for(int i = 0; i < STRIPE_COUNT; i ++) {
            locks[i] = new StripeLock();
        }
        this.generation = 0;
    }

    public void advanceGeneration() {
        generation ++;
    }

    public TEntry probe(long key) {
        int index = (int) key & indexMask;
        int stripe = index & STRIPE_MASK;
        synchronized (locks[stripe]) {
            if(this.key[index] == key) return new TEntry(this.data[index], this.hashMove[index]);
            return NULL_ENTRY;
        }
    }

    public void save(long key, int depth, int type, int score, long hashMove) {
        int index = (int) key & indexMask;
        int stripe = index & (STRIPE_MASK);
        synchronized (locks[stripe]) {
            if(type == TYPE_EVAL) {
                this.key[index] = key;
                this.data[index] = score;
                return;
            }
            long existingKey = this.key[index];
            long existingData = this.data[index];
            int existingDepth = (int) existingData & 0x3f;
            int existingType = (int) existingData >>> 6 & 0x3;
            int existingGen = (int) existingData >>> 8 & 0xff;
            if(key != existingKey && existingGen == (this.generation & 0xff) && depth <= existingDepth && type >= existingType) return;
            if(key == existingKey && depth <= existingDepth && type >= existingType) return;
            this.key[index] = key;
            this.data[index] = ((long) score << 32) | ((long) (generation & 0xff) << 8) | ((long) (type & 0x3) << 6) | (depth & 0x3f);
            this.hashMove[index] = hashMove;
        }
    }

    private static final int DEFAULT_TABLE_SIZE_IN_MB = 192;
    private static final int STRIPE_COUNT = 32;
    private static final int STRIPE_MASK = STRIPE_COUNT - 1;

    private final long[] key;
    private final long[] data;
    private final long[] hashMove;
    private final StripeLock[] locks;
    private final int indexMask;
    private int generation;

}
