package io.topiacoin.workspace.blockchain;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.ExecCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Ignore
public class DockerTest {

	@Test
	public void testDockerClient() {
		System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
		try {
			// Create a client based on DOCKER_HOST and DOCKER_CERT_PATH env vars
			final DockerClient docker = DefaultDockerClient.fromEnv().build();

			ContainerInfo info = docker.inspectContainer("testContainer");
			// Pull an image
			//docker.pull("busybox");

			// Bind container ports to host ports
			final String[] ports = { "80", "22" };
			final Map<String, List<PortBinding>> portBindings = new HashMap<>();
			for (String port : ports) {
				List<PortBinding> hostPorts = new ArrayList<>();
				hostPorts.add(PortBinding.of("0.0.0.0", port));
				portBindings.put(port, hostPorts);
			}

			// Bind container port 443 to an automatically allocated available host port.
			List<PortBinding> randomPort = new ArrayList<>();
			randomPort.add(PortBinding.randomPort("0.0.0.0"));
			portBindings.put("443", randomPort);

			final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();

			// Create container with exposed ports
			final ContainerConfig containerConfig = ContainerConfig.builder()
					.hostConfig(hostConfig)
					.image("busybox").exposedPorts(ports)
					.cmd("sh", "-c", "while :; do sleep 1; done")
					.build();

			final ContainerCreation creation = docker.createContainer(containerConfig, "testContainer");
			final String id = creation.id();

			// Inspect container
			info = docker.inspectContainer(id);

			// Start container
			docker.startContainer(id);

			docker.killContainer(id);

			info = docker.inspectContainer(id);

			docker.startContainer("testContainer");

			info = docker.inspectContainer("testContainer");

			// Exec command inside running container with attached STDOUT and STDERR
			final String[] command = { "sh", "-c", "echo foo" };
			final ExecCreation execCreation = docker.execCreate(
					id, command, DockerClient.ExecCreateParam.attachStdout(),
					DockerClient.ExecCreateParam.attachStderr());
			final LogStream output = docker.execStart(execCreation.id());
			final String execOutput = output.readFully();
			System.out.println(execOutput);

			// Kill container
			docker.killContainer("testContainer");

			// Remove container
			docker.removeContainer("testContainer");

			// Close the docker client
			docker.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
}
