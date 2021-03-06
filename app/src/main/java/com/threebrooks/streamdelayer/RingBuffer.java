package com.threebrooks.streamdelayer;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

public class RingBuffer {

    private byte[] buffer;

    volatile private long tail;
    volatile private long head;

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
        if (((head-offset) >= buffer.length) || ((head-offset) <= 0)) return;
        tail = head-offset;
    }

    synchronized public void addToHeadOffset(long offset) {
        if (((head-(tail-offset)) >= buffer.length) || ((head-(tail-offset)) <= 0)) return;
        Log.d(MainActivity.TAG,"Tail before: "+tail);
        tail -= offset;
        Log.d(MainActivity.TAG,"Tail after: "+tail);
    }

    public float getHeadPercentage() {
        return wrapped(head)/(float)buffer.length;
    }

    public long getHeadPos() {return head;}

    public float getTailPercentage() {
        return wrapped(tail)/(float)buffer.length;
    }

    public long getTailPos() {return tail;}

    synchronized public int add(byte[] toAdd, int size) {
        if (((head+size)-tail) >= buffer.length) return -1;
        if ((wrapped(head)+size) > buffer.length) {
            long firstHalf = buffer.length-wrapped(head);
            System.arraycopy(toAdd, 0, buffer, (int)wrapped(head), (int)firstHalf);
            long secondHalf = size-firstHalf;
            System.arraycopy(toAdd, (int)firstHalf, buffer, 0, (int)secondHalf);
        } else {
            System.arraycopy(toAdd, 0, buffer, (int)wrapped(head), size);
        }
        head += size;
        return size;
    }

    synchronized public int get(byte[] toGet, int size) {
        if (tail+size > head) return -1;
        if ((wrapped(tail)+size) > buffer.length) {
            long firstHalf = buffer.length-wrapped(tail);
            System.arraycopy(buffer, (int)wrapped(tail), toGet, 0, (int)firstHalf);
            long secondHalf = size-firstHalf;
            System.arraycopy(buffer, 0, toGet, (int)firstHalf, (int)secondHalf);
        } else {
            System.arraycopy(buffer, (int)wrapped(tail), toGet, 0, size);
        }
        tail += size;
        return size;
    }

    public String toString() {
        return "CustomCircularBuffer(size=" + buffer.length + ", head=" + head + ", tail=" + tail + ")";
    }
}