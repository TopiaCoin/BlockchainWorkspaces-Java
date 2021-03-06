package io.topiacoin.model.provider.memory;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.impl.DefaultConfiguration;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.provider.DataModelFileChunkTest;
import io.topiacoin.model.provider.DataModelMemberTest;

public class MemoryDataModelMemberTest extends DataModelMemberTest {
	@Override public DataModel initDataModel() {
		Configuration config = new DefaultConfiguration();
		config.setConfigurationOption("model.storage.type", "memory");
		DataModel.initialize(config);
		return DataModel.getInstance();
	}
	@Override public void tearDownDataModel() {
		DataModel.getInstance().close();
	}
}
