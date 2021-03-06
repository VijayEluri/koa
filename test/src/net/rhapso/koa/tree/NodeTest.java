/*
 * The MIT License
 *
 * Copyright (c) 2010 Fabrice Medio
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

package net.rhapso.koa.tree;

import com.google.common.base.Joiner;
import net.rhapso.koa.BaseTreeTestCase;
import net.rhapso.koa.storage.Addressable;
import org.junit.Test;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NodeTest extends BaseTreeTestCase {
    @Test
    public void testCursorFrom() throws Exception {
        Tree tree = init(new Order(10));
        tree.put(key(1), new Value(0));
        tree.put(key(2), new Value(0));
        tree.put(key(3), new Value(0));
        Iterator<Key> iterator = tree.cursorAtOrAfter(key(0));
        List<Integer> keys = new LinkedList<Integer>();
        while (iterator.hasNext()) {
            keys.add((int) iterator.next().bytes()[0]);
        }

        assertEquals("1 2 3", Joiner.on(" ").join(keys));
    }

    @Test
    public void testCursorAt() throws Exception {
        Tree tree = runTest(new Order(4));
        Key first = tree.cursorAtOrAfter(new Key(new byte[0])).next();
        Iterator<Key> iterator = tree.cursorAt(first);
        int count = 0;
        while (iterator.hasNext()) {
            Key key = iterator.next();
            count++;
        }
        assertEquals(101, count);
    }

    @Test
    public void testCursorAtOrAfter() throws Exception {
        Tree tree = runTest(new Order(4));
        Iterator<Key> iterator = tree.cursorAtOrAfter(new Key(new byte[0]));
        int count = 0;
        while (iterator.hasNext()) {
            Key key = iterator.next();
            count++;
        }
        assertEquals(101, count);
    }

    @Test
    public void testInsertEvenOrder() throws Exception {
        runTest(new Order(4));
    }

    @Test
    public void testInsertOddOrder() throws Exception {
        runTest(new Order(5));
    }

    @Test
    public void testBlockIO() throws Exception {
        runTest(new Order(5));
    }

    private Tree init(Order order) {
        Addressable addressable = makeAddressable();
        TreeControl treeControl = TreeControl.initialize(addressable, order);
        NodeFactory nodeFactory = new NodeFactory(addressable, treeControl);
        return new Koa(nodeFactory, treeControl);
    }

    public Tree runTest(Order order) throws Exception {
        Tree tree = init(order);
        tree.put(new Key(new byte[100]), new Value(42));

        for (int i = 0; i < 100; i++) {
            Value randomValue = new Value(RANDOM.nextLong());
            Value updatedValue = new Value(RANDOM.nextLong());

            byte[] bytes = makeRandomBytes(100);
            Key key = new Key(bytes);

            assertFalse(tree.put(key, randomValue));
            assertTrue(tree.contains(key));
            assertEquals(randomValue, tree.get(key));

            assertTrue(tree.put(key, updatedValue));
            assertEquals(updatedValue, tree.get(key));

            KeyRef ref = tree.referenceOf(key);
            Key result = tree.key(ref);
            assertEquals(key, result);
        }

        assertEquals(101, tree.count());
        return tree;
    }
}
