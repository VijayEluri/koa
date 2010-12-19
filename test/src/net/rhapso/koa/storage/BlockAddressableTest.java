/*
 * The MIT License
 *
 * Copyright (c) 2010 Fabrice Medio <fmedio@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.rhapso.koa.storage;

import clutter.BaseTestCase;
import clutter.Fallible;

import java.nio.ByteBuffer;

import static org.mockito.Mockito.*;

public class BlockAddressableTest extends BaseTestCase {
    private Addressable underlying;

    public void testFlushCache() throws Exception {
        BlockAddressable blockAddressable = new BlockAddressable(underlying, new BlockSize(4), 1);
        blockAddressable.writeInt(1);
        verify(underlying, times(0)).write(new byte[]{0, 0, 0, 1});
        blockAddressable.writeInt(2);
        verify(underlying, times(1)).write(new byte[]{0, 0, 0, 1});
    }

    public void testCommit() throws Exception {
        BlockAddressable blockAddressable = new BlockAddressable(underlying, new BlockSize(4), Integer.MAX_VALUE);
        blockAddressable.writeInt(randomInt);
        blockAddressable.flush();
        byte[] expectedResult = fillBuffer(randomInt);
        verify(underlying, times(1)).write(expectedResult);
        assertEquals(new Offset(4l), blockAddressable.getPosition());
    }

    public void testFlush() throws Exception {
        BlockAddressable blockAddressable = new BlockAddressable(underlying, new BlockSize(4), Integer.MAX_VALUE);
        blockAddressable.writeInt(randomInt);
        blockAddressable.flush();
        verify(underlying, times(2)).write(any(byte[].class));
        reset(underlying);
        blockAddressable.flush();
        verify(underlying, times(1)).flush();
    }

    public void testNextInsertLocation() throws Exception {
        final BlockAddressable addressable = new BlockAddressable(underlying, new BlockSize(42), Integer.MAX_VALUE);
        assertEquals(0l, addressable.nextInsertionLocation(new Offset(0), 42).asLong());
        assertEquals(42l, addressable.nextInsertionLocation(new Offset(1), 42).asLong());
        assertEquals(42l, addressable.nextInsertionLocation(new Offset(41), 12).asLong());
        assertFailure(IllegalArgumentException.class, new Fallible() {
            @Override
            public void execute() throws Exception {
                addressable.nextInsertionLocation(new Offset(0), 43);
            }
        });
    }

    public void testOnlyFlushDirtyBlocks() {
        BlockAddressable addressable = new BlockAddressable(underlying, new BlockSize(2), 2);
        addressable.write(new byte[2]);
        addressable.write(new byte[2]);
        addressable.flush();
        reset(underlying);
        addressable.seek(0);
        addressable.write(new byte[2]);
        addressable.flush();
        verify(underlying, times(1)).seek(0);
        verify(underlying, times(1)).write(new byte[2]);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        underlying = mock(Addressable.class);
    }

    private byte[] fillBuffer(int value) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(value);
        return byteBuffer.array();
    }
}