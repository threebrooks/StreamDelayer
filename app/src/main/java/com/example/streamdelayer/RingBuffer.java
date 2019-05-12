package com.example.streamdelayer;
import android.util.Log;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

public class RingBuffer {

    private byte[] buffer;

    private long tail;
    private long head;

    public RingBuffer(int n, long initHeadOffset) {
        buffer = new byte[n];
        for(int idx = 0; idx < n; idx++) buffer[idx] = (byte)(idx%2);
        tail = 0;
        head = initHeadOffset;
    }

    public long wrapped(long pos) {
        return (pos+buffer.length) % buffer.length;
    }

    synchronized public void setHeadOffset(long offset) {
        tail = head-offset;
    }

    public float getHeadPercentage() {
        return wrapped(head)/(float)buffer.length;
    }

    public float getTailPercentage() {
        return wrapped(tail)/(float)buffer.length;
    }

    synchronized public void add(byte[] toAdd, int size) {
        //Log.d(MainActivity.TAG, "add(): head:"+head+" ,tail:"+tail+", dist="+(head-tail));
        if ((wrapped(head)+size) > buffer.length) {
            long firstHalf = buffer.length-wrapped(head);
            System.arraycopy(toAdd, 0, buffer, (int)wrapped(head), (int)firstHalf);
            long secondHalf = size-firstHalf;
            System.arraycopy(toAdd, (int)firstHalf, buffer, 0, (int)secondHalf);
        } else {
            System.arraycopy(toAdd, 0, buffer, (int)wrapped(head), size);
        }
        head += size;
        if ((head-tail) >= buffer.length) throw new BufferOverflowException();
    }

    synchronized public void get(byte[] toGet, int size) {
        //Log.d(MainActivity.TAG, "get(): head:"+head+" ,tail:"+tail+", dist="+(head-tail));
        if ((wrapped(tail)+size) > buffer.length) {
            long firstHalf = buffer.length-wrapped(tail);
            System.arraycopy(buffer, (int)wrapped(tail), toGet, 0, (int)firstHalf);
            long secondHalf = size-firstHalf;
            System.arraycopy(buffer, 0, toGet, (int)firstHalf, (int)secondHalf);
        } else {
            System.arraycopy(buffer, (int)wrapped(tail), toGet, 0, size);
        }
        tail += size;
        if (tail > head) throw new BufferUnderflowException();
    }

    public String toString() {
        return "CustomCircularBuffer(size=" + buffer.length + ", head=" + head + ", tail=" + tail + ")";
    }
}