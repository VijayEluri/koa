package net.rhapso.koa.mapreduce;

import net.rhapso.koa.StorageFactory;
import net.rhapso.koa.bag.TreeBag;
import net.rhapso.koa.tree.Key;
import net.rhapso.koa.tree.Value;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MapTask<I extends Serializable, K extends Serializable & Comparable<K>, IV extends Serializable> implements Runnable {
    private TreeEmitter<K, IV> emitter;
    private List<I> inputs;
    private Mapper<I, K, IV> mapper;

    public MapTask(int taskId, StorageFactory storageFactory, Mapper<I, K, IV> mapper) {
        this.mapper = mapper;
        emitter = new TreeEmitter<K, IV>(storageFactory, taskId);
        inputs = new LinkedList<I>();
    }

    public void add(I input) {
        inputs.add(input);
    }

    @Override
    public void run() {
        for (I input : inputs) {
            mapper.map(input, emitter);
        }
    }

    public Iterator<K> keys() {
        return new Iterator<K>() {
            TreeBag tree = emitter.getMultiTree();
            Iterator<Key> iterator = tree.cursorAtOrAfter(new Key(new byte[0]));

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public K next() {
                Key key = iterator.next();
                return (K) key.asPOJO();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public Iterator<IV> values(final K key) {
        return new Iterator<IV>() {
            Iterator<Value> values = emitter.getMultiTree().getValues(new Key(key));

            @Override
            public boolean hasNext() {
                return values.hasNext();
            }

            @Override
            public IV next() {
                return (IV) values.next().asPOJO();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}