package io.topiacoin.dht.messages;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class FetchValueResponseTest {

    @Test
    public void testEncodeAndDecode() throws  Exception {

        String key = "TheMeaningOfLife";
        List<String> values = new ArrayList<String>();

        values.add("Love") ;
        values.add("Compassion") ;
        values.add("Mercy") ;
        values.add("Forty Two");

        FetchValueResponse fetchValueResponse = new FetchValueResponse() ;
        fetchValueResponse.setKey(key);
        fetchValueResponse.setValues(values);

        ByteBuffer buffer = ByteBuffer.allocate(65536);

        fetchValueResponse.encodeMessage(buffer);
        buffer.flip();

        FetchValueResponse decodedResponse = new FetchValueResponse(buffer);

        assertEquals ( fetchValueResponse, decodedResponse);
    }

    @Test
    public void testEncodeAndDecodeEmptyMessage() throws  Exception {

        FetchValueResponse fetchValueResponse = new FetchValueResponse() ;

        ByteBuffer buffer = ByteBuffer.allocate(65536);

        fetchValueResponse.encodeMessage(buffer);
        buffer.flip();

        FetchValueResponse decodedResponse = new FetchValueResponse(buffer);

        assertEquals ( fetchValueResponse, decodedResponse);
    }
}
