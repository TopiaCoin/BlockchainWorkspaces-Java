package io.topiacoin.workspace.blockchain.requests;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

public class GetTableRows {

    public static class Request {

        public String scope;
        public String code;
        public String table;
        public boolean json = true;

        public int limit = 500;
    }

    public static class Response {
        public List<String> rows ;
        public boolean more;
    }
}
