package io.topiacoin.dht.content;

import io.topiacoin.dht.intf.ValueStorage;

public class InMemoryValueStorageTest extends AbstractValueStorageTest {
    @Override
    protected ValueStorage getValueStorage() {

        InMemoryValueStorage valueStorage = new InMemoryValueStorage();

        valueStorage.initialize();

        return valueStorage;
    }
}
