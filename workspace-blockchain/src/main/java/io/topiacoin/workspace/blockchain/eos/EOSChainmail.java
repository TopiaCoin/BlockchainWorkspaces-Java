package io.topiacoin.workspace.blockchain.eos;

import io.topiacoin.core.Configuration;
import io.topiacoin.model.MemberNode;
import io.topiacoin.workspace.blockchain.ChainInfo;
import io.topiacoin.workspace.blockchain.Chainmail;
import io.topiacoin.workspace.blockchain.ChainmailCallback;
import io.topiacoin.workspace.blockchain.RPCAdapterManager;
import io.topiacoin.workspace.blockchain.exceptions.ChainAlreadyExistsException;
import io.topiacoin.workspace.blockchain.exceptions.NoSuchChainException;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class EOSChainmail implements Chainmail {

	private String nodeOSExecutable;
	private String KeosExecutable;
	private File EOSConfigBaseDir;
	private String EOSConfigBaseDirButInLinuxStyle;
	private int PORT_RANGE_START;
	private int PORT_RANGE_END;
	private Process keosTerm;
	private Process cmdTerm;
	private ProcessBuilder termBuilder;

	private final Stack<Integer> availablePorts = new Stack<>();
	private final Map<Long, Integer> portsInUse = new HashMap<>();
	private final Map<Long, ChainInfo> chainInfo = new HashMap<>();
	private final Set<ChainmailCallback> blockchainListeners = new HashSet<>();
	private RPCAdapterManager _rpcManager;
	private static final DateFormat timestamp_format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	public EOSChainmail(Configuration config) {
		String nodeOSexe = config.getConfigurationOption("nodeos_install_dir");
		String keosEXE = config.getConfigurationOption("keos_install_dir");
		String blockchain_storage_dir = config.getConfigurationOption("blockchain_storage_dir");
		String blockchain_storage_dir_linux = config.getConfigurationOption("blockchain_storage_dir_linux");
		int portRangeStart = config.getConfigurationOption("chainmail_port_range_start", Integer.class);
		int portRangeEnd = config.getConfigurationOption("chainmail_port_range_end", Integer.class);
		init(nodeOSexe, keosEXE, blockchain_storage_dir, blockchain_storage_dir_linux, portRangeStart, portRangeEnd);
	}

	EOSChainmail() {
		this(9240, 9250);
	}

	EOSChainmail(int portRangeStart, int portRangeEnd) {
		init("/mnt/c/EOS/eos/build/programs/nodeos/nodeos", "/mnt/c/EOS/eos/build/programs/keosd/keosd", "C:\\Users\\csandwith\\AppData\\Roaming\\EOSTestChains", "/mnt/c/Users/csandwith/AppData/Roaming/EOSTestChains", portRangeStart, portRangeEnd);
	}

	private void init(String nodeOSexe, String keosEXE, String baseDir, String baseDirLinux, int portRangeStart, int portRangeEnd) {
		nodeOSExecutable = nodeOSexe;
		KeosExecutable = keosEXE;
		EOSConfigBaseDir = new File(baseDir);
		EOSConfigBaseDirButInLinuxStyle = baseDirLinux;
		termBuilder = new ProcessBuilder("wsl");
		termBuilder.redirectErrorStream(true);
		PORT_RANGE_START = portRangeStart;
		PORT_RANGE_END = portRangeEnd;
	}

	@Override public void start(RPCAdapterManager manager) throws IOException {
		if (PORT_RANGE_END - PORT_RANGE_START < 2) {
			throw new IllegalArgumentException(PORT_RANGE_START + " -> " + PORT_RANGE_END + " must be a range of at least 3");
		}
		if (PORT_RANGE_START <= 0 || PORT_RANGE_END <= 0 || PORT_RANGE_START > 65535 || PORT_RANGE_END > 65535) {
			throw new IllegalArgumentException(PORT_RANGE_START + " and/or " + PORT_RANGE_END + " illegal. Port numbers must be 0 < [port number] < 65536");
		}
		if (manager == null) {
			throw new IllegalArgumentException("RPCAdapterManager must not be null");
		}
		_rpcManager = manager;
		availablePorts.clear();
		portsInUse.clear();
		chainInfo.clear();
		for (int i = PORT_RANGE_START + 1; i <= PORT_RANGE_END - 1; i += 2) {
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
		String walletStartCmd =
				KeosExecutable + " --http-server-address 127.0.0.1:" + PORT_RANGE_START + " --data-dir " + EOSConfigBaseDirButInLinuxStyle + "/ --wallet-dir "
						+ EOSConfigBaseDirButInLinuxStyle + "/wallet" + "\n";
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
		Set<Long> blockchains = portsInUse.keySet();
		for (Long blockchain : blockchains) {
			try {
				stopBlockchain(blockchain);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		cmdTerm.destroy();
		keosTerm.destroy();
	}

	@Override public boolean createBlockchain(String currentUserID, long workspaceID) throws ChainAlreadyExistsException {
		if (chainExists(workspaceID)) {
			throw new ChainAlreadyExistsException();
		}
		try {
			File configDir = new File(EOSConfigBaseDir, "" + workspaceID);
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
			Instant genesis_date = Instant.now();
			BufferedWriter genesisWriter = new BufferedWriter(new FileWriter(genesis));
			String initialTimestamp = genesis_date.toString();
			if(initialTimestamp.endsWith("Z")) {
				initialTimestamp = initialTimestamp.substring(0, initialTimestamp.length() - 1);
			}
			genesisWriter.write("{");
			genesisWriter.newLine();
			genesisWriter.write("\"initial_timestamp\": \"" + initialTimestamp + "\",");
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
			if (startBlockchain(true, currentUserID, workspaceID, null)) {
				stopBlockchain(workspaceID);
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchChainException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override public boolean startBlockchain(String currentUserID, long workspaceID, List<MemberNode> memberNodes) throws IOException, NoSuchChainException {
		return startBlockchain(false, currentUserID, workspaceID, memberNodes);
	}

	private boolean startBlockchain(boolean firstTimeStartup, String userID, long workspaceID, List<MemberNode> memberNodes) throws IOException, NoSuchChainException {
		if (!firstTimeStartup && !chainExists(workspaceID)) {
			throw new NoSuchChainException("Cannot start Blockchain with ID " + workspaceID + " - no such blockchain");
		}
		if (!chainIsRunning(workspaceID)) {
			if (availablePorts.empty()) {
				stopLRUBlockchain();
			}
			int nodePort = availablePorts.pop();
			int rpcPort = nodePort + 1;
			portsInUse.put(workspaceID, nodePort);

			boolean runAgain = true;
			boolean success = false;
			while (runAgain) {
				runAgain = false;
				Process proc = termBuilder.start();
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				StringBuilder memberNodeString = new StringBuilder();
				if (memberNodes != null) {
					for (MemberNode node : memberNodes) {
						memberNodeString.append(" --p2p-peer-address ").append(node.getHostname()).append(":").append(node.getPort());
					}
				}
				OutputStreamWriter writer = new OutputStreamWriter(proc.getOutputStream());
				String startCmd =
						"echo $$\n" + nodeOSExecutable + " -e -p " + userID + " --config-dir " + EOSConfigBaseDirButInLinuxStyle + "/" + workspaceID + (firstTimeStartup ?
								" --genesis-json " + EOSConfigBaseDirButInLinuxStyle + "/" + workspaceID + "/genesis.json" : "")
								+ " --http-server-address 127.0.0.1:" + rpcPort + " --p2p-listen-endpoint 0.0.0.0:" + nodePort
								+ memberNodeString.toString()
								+ " --data-dir " + EOSConfigBaseDirButInLinuxStyle + "/" + workspaceID + "\n";
				writer.write(startCmd);
				writer.flush();
				BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				int lineCounter = 0;
				String line;
				ChainInfo info = new ChainInfo(workspaceID, rpcPort, nodePort, proc);
				while ((line = in.readLine()) != null) {
					System.out.println(line);
					if (lineCounter == 0) {
						info.extraInfo.put("ppid", "" + Integer.parseInt(line));
					}
					lineCounter++;
					if (line.contains("producer plugin:  plugin_startup() end")) {
						chainInfo.put(workspaceID, info);
						success = true;
						break;
					}
					if (line.contains("Segmentation fault")) {
						runAgain = true;
						break;
					}
				}
				if (success) {
					for (ChainmailCallback callback : blockchainListeners) {
						callback.onBlockchainStarted(workspaceID, "127.0.0.1:" + nodePort, "127.0.0.1:" + PORT_RANGE_START);
					}
				}
			}
			return success;
		}
		return true;
	}

	private void stopLRUBlockchain() throws IOException {
		ChainInfo[] chains = chainInfo.values().toArray(new ChainInfo[0]);
		//We need to stop a chain, but which ones?
		//Sort the array by last modified
		Arrays.sort(chains, new Comparator<ChainInfo>() {
			public int compare(ChainInfo o1, ChainInfo o2) {
				if (_rpcManager.getRPCAdapter(o1.workspaceId).getLastBlockTime() < _rpcManager.getRPCAdapter(o2.workspaceId).getLastBlockTime()) {
					return -1;
				} else if (_rpcManager.getRPCAdapter(o1.workspaceId).getLastBlockTime() > _rpcManager.getRPCAdapter(o2.workspaceId).getLastBlockTime()) {
					return 1;
				}
				return 0;
			}
		});
		//Debug
		for (int i = 0; i < chains.length; i++) {
			System.out.println(chains[i].workspaceId + ": " + _rpcManager.getRPCAdapter(chains[i].workspaceId).getLastBlockTime());
		}
		//Stop the chains that need stoppin
		stopBlockchain(chains[0].workspaceId);
	}

	@Override public boolean stopBlockchain(long workspaceID) throws IOException {
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
			} while (!line.trim().isEmpty());

			chainInfo.get(workspaceID).proc.destroy();
			chainInfo.remove(workspaceID);
			Integer i = portsInUse.remove(workspaceID);
			if (i != null) {
				availablePorts.push(i);
			}
			for (ChainmailCallback callback : blockchainListeners) {
				callback.onBlockchainStopped(workspaceID);
			}
			return true;
		}
		return false;
	}

	public void addBlockchainListener(ChainmailCallback callback) {
		blockchainListeners.add(callback);
	}

	@Override public void destroyBlockchain(long workspaceID) throws IOException {
		if (chainExists(workspaceID)) {
			stopBlockchain(workspaceID);
			FileUtils.deleteDirectory(new File(EOSConfigBaseDir, "" + workspaceID));
		}
	}

	private boolean chainExists(long workspaceID) {
		return new File(EOSConfigBaseDir, "" + workspaceID).exists();
	}

	private boolean chainIsRunning(long workspaceID) {
		return chainInfo.containsKey(workspaceID);
	}
}
