package io.topiacoin.workspace.blockchain.chainmailImpl;

import io.topiacoin.chainmail.multichainstuff.exception.ChainAlreadyExistsException;
import io.topiacoin.workspace.blockchain.ChainInfo;
import io.topiacoin.workspace.blockchain.Chainmail;
import io.topiacoin.workspace.blockchain.RPCAdapter;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class EOSChainmail implements Chainmail {

	private final String EOSExe;
	private final String KeyOSExe;
	private final File EOSConfigBaseDir;
	private final String EOSConfigBaseDirButInLinuxStyle;
	private final int PORT_RANGE_START;
	private final int PORT_RANGE_END;
	private Process keosTerm;
	private Process cmdTerm;
	private final ProcessBuilder termBuilder;

	private final Stack<Integer> availablePorts = new Stack<>();
	private final Map<String, Integer> portsInUse = new HashMap<>();
	Map<String, ChainInfo> chainInfo = new HashMap<>();

	public EOSChainmail() {
		this(9240, 9250);
	}

	public EOSChainmail(int portRangeStart, int portRangeEnd) {
		this("/mnt/c/EOS/eos/build/programs/nodeos/nodeos", "/mnt/c/EOS/eos/build/programs/keosd/keosd", "C:\\Users\\csandwith\\AppData\\Roaming\\EOSTestChains", "/mnt/c/Users/csandwith/AppData/Roaming/EOSTestChains", portRangeStart, portRangeEnd);
	}

	EOSChainmail(String nodeOSexe, String keosEXE, String baseDir, String baseDirLinux, int portRangeStart, int portRangeEnd) {
		EOSExe = nodeOSexe;
		KeyOSExe = keosEXE;
		EOSConfigBaseDir = new File(baseDir);
		EOSConfigBaseDirButInLinuxStyle = baseDirLinux;
		termBuilder = new ProcessBuilder("wsl");
		termBuilder.redirectErrorStream(true);
		PORT_RANGE_START = portRangeStart;
		PORT_RANGE_END = portRangeEnd;
	}

	@Override public void start() throws IOException {
		if(PORT_RANGE_END - PORT_RANGE_START < 2) {
			throw new IllegalArgumentException(PORT_RANGE_START + " -> " + PORT_RANGE_END + " must be a range of at least 3");
		}
		if(PORT_RANGE_START <= 0 || PORT_RANGE_END <= 0 || PORT_RANGE_START > 65535 || PORT_RANGE_END > 65535) {
			throw new IllegalArgumentException(PORT_RANGE_START + " and/or " + PORT_RANGE_END + " illegal. Port numbers must be 0 < [port number] < 65536");
		}
		availablePorts.clear();
		portsInUse.clear();
		chainInfo.clear();
		for (int i = PORT_RANGE_START+1; i <= PORT_RANGE_END - 1; i += 2) {
			availablePorts.push(i);
		}
		cmdTerm = termBuilder.start();
		keosTerm = termBuilder.start();
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		OutputStreamWriter writer = new OutputStreamWriter(keosTerm.getOutputStream());
		String walletStartCmd = KeyOSExe + " --http-server-address 127.0.0.1:" + PORT_RANGE_START + " --data-dir " + EOSConfigBaseDirButInLinuxStyle + "/ --wallet-dir " + EOSConfigBaseDirButInLinuxStyle + "/wallet" + "\n";
		writer.write(walletStartCmd);
		writer.flush();
		BufferedReader in = new BufferedReader(new InputStreamReader(keosTerm.getInputStream()));
		String line;
		while ((line = in.readLine()) != null) {
			System.out.println(line);
			if (line.contains("add api url: /v1/wallet/unlock")) {
				break;
			}
		}
	}

	@Override public void stop() {
		Set<String> blockchains = portsInUse.keySet();
		for (String blockchain : blockchains) {
			try {
				stopBlockchain(blockchain);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		cmdTerm.destroy();
		keosTerm.destroy();
	}

	@Override public void createBlockchain(String workspaceID) throws ChainAlreadyExistsException {
		if (chainExists(workspaceID)) {
			throw new ChainAlreadyExistsException();
		}
		try {
			File configDir = new File(EOSConfigBaseDir, workspaceID);
			if (!configDir.mkdir()) {
				throw new IOException();
			}
			File configIni = new File(configDir, "config.ini");
			if (!configIni.createNewFile()) {
				throw new IOException();
			}
			File genesis = new File(configDir, "genesis.json");
			if (!genesis.createNewFile()) {
				throw new IOException();
			}
			BufferedWriter confWriter = new BufferedWriter(new FileWriter(configIni));
			confWriter.write("bnet-no-trx = false");
			confWriter.newLine();
			confWriter.write("chain-state-db-size-mb = 1024");
			confWriter.newLine();
			confWriter.write("reversible-blocks-db-size-mb = 340");
			confWriter.newLine();
			confWriter.write("contracts-console = false");
			confWriter.newLine();
			confWriter.write("https-client-validate-peers = 1");
			confWriter.newLine();
			confWriter.write("access-control-allow-credentials = false");
			confWriter.newLine();
			confWriter.write("p2p-max-nodes-per-host = 1");
			confWriter.newLine();
			confWriter.write("agent-name = \"EOS Test Agent\"");
			confWriter.newLine();
			confWriter.write("allowed-connection = any");
			confWriter.newLine();
			confWriter.write("max-clients = 25");
			confWriter.newLine();
			confWriter.write("connection-cleanup-period = 30");
			confWriter.newLine();
			confWriter.write("network-version-match = 0");
			confWriter.newLine();
			confWriter.write("max-implicit-request = 1500");
			confWriter.newLine();
			confWriter.write("enable-stale-production = false");
			confWriter.newLine();
			confWriter.write("pause-on-startup = false");
			confWriter.newLine();
			confWriter.write("max-transaction-time = 30");
			confWriter.newLine();
			confWriter.write("max-irreversible-block-age = -1");
			confWriter.newLine();
			confWriter.write("signature-provider = EOS6MRyAjQq8ud7hVNYcfnVPJqcVpscN5So8BhtHuGYqET5GDW5CV=KEY:5KQwrPbwdL6PhXujxW37FSSQZ1JiwsST4cqQzDeyXtP79zkvFD3");
			confWriter.newLine();
			confWriter.write("keosd-provider-timeout = 5");
			confWriter.newLine();
			confWriter.write("txn-reference-block-lag = 0");
			confWriter.newLine();
			confWriter.write("unlock-timeout = 900");
			confWriter.newLine();
			confWriter.write("plugin = eosio::chain_api_plugin");
			confWriter.newLine();
			confWriter.write("plugin = eosio::history_api_plugin");
			confWriter.newLine();
			confWriter.write("plugin = eosio::net_api_plugin");
			confWriter.close();
			BufferedWriter genesisWriter = new BufferedWriter(new FileWriter(genesis));
			genesisWriter.write("{");
			genesisWriter.newLine();
			genesisWriter.write("\"initial_timestamp\": \"2018-06-03T12:00:00.000\",");
			genesisWriter.newLine();
			genesisWriter.write("\"initial_key\": \"EOS6MRyAjQq8ud7hVNYcfnVPJqcVpscN5So8BhtHuGYqET5GDW5CV\",");
			genesisWriter.newLine();
			genesisWriter.write("\"initial_configuration\": {");
			genesisWriter.newLine();
			genesisWriter.write("\"max_block_net_usage\": 1048576,");
			genesisWriter.newLine();
			genesisWriter.write("\"target_block_net_usage_pct\": 1000,");
			genesisWriter.newLine();
			genesisWriter.write("\"max_transaction_net_usage\": 524288,");
			genesisWriter.newLine();
			genesisWriter.write("\"base_per_transaction_net_usage\": 12,");
			genesisWriter.newLine();
			genesisWriter.write("\"net_usage_leeway\": 500,");
			genesisWriter.newLine();
			genesisWriter.write("\"context_free_discount_net_usage_num\": 20,");
			genesisWriter.newLine();
			genesisWriter.write("\"context_free_discount_net_usage_den\": 100,");
			genesisWriter.newLine();
			genesisWriter.write("\"max_block_cpu_usage\": 200000,");
			genesisWriter.newLine();
			genesisWriter.write("\"target_block_cpu_usage_pct\": 1000,");
			genesisWriter.newLine();
			genesisWriter.write("\"max_transaction_cpu_usage\": 150000,");
			genesisWriter.newLine();
			genesisWriter.write("\"min_transaction_cpu_usage\": 100,");
			genesisWriter.newLine();
			genesisWriter.write("\"max_transaction_lifetime\": 3600,");
			genesisWriter.newLine();
			genesisWriter.write("\"deferred_trx_expiration_window\": 600,");
			genesisWriter.newLine();
			genesisWriter.write("\"max_transaction_delay\": 3888000,");
			genesisWriter.newLine();
			genesisWriter.write("\"max_inline_action_size\": 4096,");
			genesisWriter.newLine();
			genesisWriter.write("\"max_inline_action_depth\": 4,");
			genesisWriter.newLine();
			genesisWriter.write("\"max_authority_depth\": 6");
			genesisWriter.newLine();
			genesisWriter.write("}");
			genesisWriter.newLine();
			genesisWriter.write("}");
			genesisWriter.close();
			startBlockchain(true, workspaceID);
			stopBlockchain(workspaceID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override public void startBlockchain(String workspaceID) throws IOException {
		startBlockchain(false, workspaceID);
	}

	private void startBlockchain(boolean includeGenesis, String workspaceID) throws IOException {
		if(availablePorts.empty()) {
			stopLRUBlockchain();
		}
		int nodePort = availablePorts.pop();
		int rpcPort = nodePort + 1;
		portsInUse.put(workspaceID, nodePort);

		boolean runAgain = true;
		while (runAgain) {
			runAgain = false;
			Process proc = termBuilder.start();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			OutputStreamWriter writer = new OutputStreamWriter(proc.getOutputStream());
			String startCmd = "echo $$\n" + EOSExe + " -e -p eosio --config-dir " + EOSConfigBaseDirButInLinuxStyle + "/" + workspaceID + (includeGenesis ?
					" --genesis-json " + EOSConfigBaseDirButInLinuxStyle + "/" + workspaceID + "/genesis.json" :
					"") + " --http-server-address 127.0.0.1:" + rpcPort + " --p2p-listen-endpoint 0.0.0.0:" + nodePort + " --data-dir "
					+ EOSConfigBaseDirButInLinuxStyle + "/" + workspaceID + "\n";
			writer.write(startCmd);
			writer.flush();
			BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			int lineCounter = 0;
			String line;
			ChainInfo info = new ChainInfo(workspaceID, rpcPort, nodePort, proc, new RPCAdapter());
			while ((line = in.readLine()) != null) {
				System.out.println(line);
				if (lineCounter == 0) {
					info.extraInfo.put("ppid", "" + Integer.parseInt(line));
				}
				lineCounter++;
				if (line.contains("producer plugin:  plugin_startup() end")) {
					chainInfo.put(workspaceID, info);
					break;
				}
				if (line.contains("Segmentation fault")) {
					runAgain = true;
					break;
				}
			}
		}
	}

	private void stopLRUBlockchain() throws IOException {
		ChainInfo[] chains = chainInfo.values().toArray(new ChainInfo[0]);
		//We need to stop a chain, but which ones?
		//Sort the array by last modified
		Arrays.sort(chains, new Comparator<ChainInfo>() {
			public int compare(ChainInfo o1, ChainInfo o2) {
				if(o1.rpcAdapter.getLastModified() < o2.rpcAdapter.getLastModified()) {
					return -1;
				} else if(o1.rpcAdapter.getLastModified() > o2.rpcAdapter.getLastModified()) {
					return 1;
				}
				return 0;
			}
		});
		//Debug
		for(int i = 0; i < chains.length; i++) {
			System.out.println(chains[i].rpcAdapter.getLastModified());
		}
		//Stop the chains that need stoppin
		stopBlockchain(chains[0].workspaceId);
	}

	@Override public boolean stopBlockchain(String workspaceID) throws IOException {
		if (chainInfo.containsKey(workspaceID)) {
			OutputStreamWriter cmdWriter = new OutputStreamWriter(cmdTerm.getOutputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(cmdTerm.getInputStream()));
			Integer sid = Integer.parseInt(chainInfo.get(workspaceID).extraInfo.get("ppid"));
			cmdWriter.write("pkill -P " + sid + "\n");
			cmdWriter.flush();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String line;
			do {
				cmdWriter.write("ps -f --ppid=" + sid + " --no-headers || echo ''\n");
				cmdWriter.flush();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				line = in.readLine();
				System.out.println(line);
			} while(!line.trim().isEmpty());

			chainInfo.get(workspaceID).proc.destroy();
			chainInfo.remove(workspaceID);
			Integer i = portsInUse.remove(workspaceID);
			if (i != null) {
				availablePorts.push(i);
			}
			return true;
		}
		return false;
	}

	private boolean chainExists(String workspaceID) {
		return new File(EOSConfigBaseDir, workspaceID).exists();
	}

	void destroyBlockchain(String workspaceID) throws IOException {
		if (chainExists(workspaceID)) {
			stopBlockchain(workspaceID);
			FileUtils.deleteDirectory(new File(EOSConfigBaseDir, workspaceID));
		}
	}
}
