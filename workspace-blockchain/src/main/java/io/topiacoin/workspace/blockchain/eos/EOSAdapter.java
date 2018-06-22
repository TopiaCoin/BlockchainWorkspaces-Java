package io.topiacoin.workspace.blockchain.eos;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.topiacoin.core.Configuration;
import io.topiacoin.eosrpcadapter.EOSRPCAdapter;
import io.topiacoin.eosrpcadapter.exceptions.ChainException;
import io.topiacoin.eosrpcadapter.messages.ChainInfo;
import io.topiacoin.workspace.blockchain.exceptions.BlockchainException;
import io.topiacoin.workspace.blockchain.requests.Error;
import io.topiacoin.workspace.blockchain.requests.GetTableRows;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class EOSAdapter {

    private String eosNodeURL;
    private String eosWalletURL;
    private URL nodeURL;
    private URL walletURL;
    private EOSRPCAdapter _eosRpcAdapter;
    private long _lastModified = 0;

    public EOSAdapter(String eosNodeURL, String eosWalletURL) {
        this.eosNodeURL = eosNodeURL;
        this.eosWalletURL = eosWalletURL;
    }

    @PostConstruct
    public void initialize() throws RuntimeException {
        try {
            nodeURL = new URL(eosNodeURL + "/");
            walletURL = new URL(eosWalletURL + "/");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to parse the Node URL", e);
        }

        _eosRpcAdapter = new EOSRPCAdapter(nodeURL, walletURL);
    }

    @PreDestroy
    public void shutdown() {

    }

    // ======== Public Methods ========

    public void initializeWorkspace() {

    }

    public void getWorkspaceInfo() {

    }

    public void setWorkspaceDescription() {

    }

    public void addMember () {

    }

    public void removeMember() {

    }

    public void getMembers() {

    }

    public void acceptInvitation() {

    }

    public void declineInvitation() {

    }

    public void addFile ( ) {

    }

    public void removeFile() {

    }

    public void getFiles() {

    }

    public void getFile() {

    }

    public void acknowledgeFile() {

    }

    public void addMessage() {

    }

    public void getMessages() {

    }

    public void getMessage() {

    }

    public void acknowledgeMessage() {

    }

    public void addFileTag() {

    }

    public void removeFileTag() {

    }








    // ======== Testing Methods ========

    public String getInfo() throws BlockchainException {

        String info = null;

        try {
            ChainInfo chainInfo = _eosRpcAdapter.chain().getInfo();
            info = chainInfo.toString();
        } catch (ChainException e) {
            throw new BlockchainException( e) ;
        }

        return info;
    }

    public List<String> getTable(String tableName) {
        List<String> info = null;

        String method = "get_table_rows";
        int requestID = 0;

        try {

            URI getTableURI = nodeURL.toURI().resolve("./" + method) ;
            HttpClient client = HttpClients.createDefault();

            GetTableRows.Request getTableRows = new GetTableRows.Request();
            getTableRows.scope = "inita";
            getTableRows.code = "inita";
            getTableRows.table = tableName;

            ObjectMapper mapper = new ObjectMapper();
            String requestString = mapper.writeValueAsString(getTableRows);
            StringEntity requestEntity = new StringEntity(
                    requestString);

            System.out.println ( "Request: " + requestString);
            HttpPost getInfoRequest = new HttpPost(getTableURI);
            getInfoRequest.setEntity(requestEntity);
            HttpResponse getInfoResponse = client.execute(getInfoRequest);

            System.out.println ( "Response Code: " + getInfoResponse.getStatusLine().getStatusCode() );

                System.out.println(getInfoResponse);

                InputStream is = getInfoResponse.getEntity().getContent();

            if ( getInfoResponse.getStatusLine().getStatusCode() == 200 ) {

                GetTableRows.Response response = mapper.readValue(is, GetTableRows.Response.class);

                System.out.println(response);

                info = response.rows;
            } else if ( getInfoResponse.getStatusLine().getStatusCode() == 500 ) {

//                List<String> lines = org.apache.commons.io.IOUtils.readLines(is, "UTF-8") ;
//                for ( String line :  lines) {
//                    System.out.println( line) ;
//                }

                Error response = mapper.readValue(is, Error.class);

                System.out.println(response);

            }


            System.out.println(info);

            // TODO - Implement this method
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return info;
    }

    public long getLastBlockTime() {
        return _lastModified;
    }

    void updateLastBlockTime(long time) {
        _lastModified = time;
    }
}
