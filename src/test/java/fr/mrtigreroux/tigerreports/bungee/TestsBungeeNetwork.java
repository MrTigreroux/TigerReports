package fr.mrtigreroux.tigerreports.bungee;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.mockito.MockedStatic;

import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.TestsTaskScheduler;
import fr.mrtigreroux.tigerreports.utils.CollectionUtils;

/**
 * @author MrTigreroux
 */
public class TestsBungeeNetwork {

	public static final long DEFAULT_FAKE_COMMUNICATION_DELAY = 5L;

	private static Basic2ServersNetwork basic2ServersNetwork;

	/**
	 * Warning: Returns the same Basic2ServersNetwork, using the same BungeeManager instances (not cleaned). The TestsTaskScheduler instances are cleaned.
	 * 
	 * @return
	 */
	public static Basic2ServersNetwork getBasic2ServersNetwork() {
		if (basic2ServersNetwork == null) {
			basic2ServersNetwork = new Basic2ServersNetwork();
		}
		basic2ServersNetwork.cleanTaskSchedulersBeforeUse();

		return basic2ServersNetwork;
	}

	public static class Basic2ServersNetwork extends TestsBungeeNetwork {

		public final FakeServer server1;
		public final FakeServer server2;

		public Basic2ServersNetwork() {
			super();

			server1 = new FakeServer("server1");
			server2 = new FakeServer("server2");
			server1.setTestsTaskScheduler(new TestsTaskScheduler(server1.name, 2));
			server2.setTestsTaskScheduler(new TestsTaskScheduler(server2.name, 2));
			addFakeServer(server1);
			addFakeServer(server2);

			TestsBungeeManager server1TestsBm = TestsBungeeManager.newBasicTestsBungeeManager(server1, this);
			TestsBungeeManager server2TestsBm = TestsBungeeManager.newBasicTestsBungeeManager(server2, this);
			server1.setTestsBm(server1TestsBm);
			server2.setTestsBm(server2TestsBm);
		}

	}

	private final Map<String, FakeServer> servers = new HashMap<>();
	private final long fakeCommunicationDelay;

	public TestsBungeeNetwork() {
		this(DEFAULT_FAKE_COMMUNICATION_DELAY);
	}

	public TestsBungeeNetwork(long fakeCommunicationDelay) {
		this.fakeCommunicationDelay = fakeCommunicationDelay;
	}

	public void cleanTaskSchedulersBeforeUse() {
		for (FakeServer server : servers.values()) {
			TestsTaskScheduler testsTaskScheduler = server.getTestsTaskScheduler();
			if (testsTaskScheduler != null) {
				testsTaskScheduler.cleanBeforeUse();
			}
		}
	}

	/**
	 * Clean task schedulers of all fake servers after having used it (typically after a test execution).
	 * 
	 * @return true if all tasks have not failed (no error, no cancellation...).
	 */
	public boolean cleanTaskSchedulersAfterUse() {
		boolean noError = true;
		for (FakeServer server : servers.values()) {
			TestsTaskScheduler testsTaskScheduler = server.getTestsTaskScheduler();
			if (testsTaskScheduler != null) {
				noError &= testsTaskScheduler.cleanAfterUse();
			}
		}
		return noError;
	}

	public long getFakeCommunicationDelay() {
		return fakeCommunicationDelay;
	}

	public void addFakeServer(FakeServer server) {
		if (servers.put(server.name, server) != null) {
			throw new IllegalArgumentException("The fake server named " + server.name + " was already added");
		}
	}

	public FakeServer getFakeServer(String name) {
		return servers.get(name);
	}

	public Set<String> getFakeServersName() {
		return servers.keySet();
	}

	public boolean isPresent(FakeServer server) {
		return server == getFakeServer(server.name);
	}

	public Collection<FakeServer> getFakeServers() {
		return servers.values();
	}

	public static class FakeServer {

		public final String name;
		public final List<Player> players = new ArrayList<>();
		private TestsBungeeManager testsBm;
		private TestsTaskScheduler testsTs;

		public FakeServer(String name) {
			this.name = name;
		}

		public TestsBungeeManager getTestsBm() {
			return testsBm;
		}

		public void setTestsBm(TestsBungeeManager testsBm) {
			this.testsBm = testsBm;
		}

		public TestsTaskScheduler getTestsTaskScheduler() {
			return testsTs;
		}

		public void setTestsTaskScheduler(TestsTaskScheduler testsTs) {
			this.testsTs = testsTs;
		}

		public void playerConnection(Player p) {
			players.add(p);
			
			User userMock = mock(User.class);
			when(userMock.getLastMessagesMinDatetimeOfInsertableMessages()).thenReturn(null);
			setupUMForGetOnlineUser(testsBm.um, p, userMock);
			// testsBm.um.processUserConnection(p);
			try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
				bukkit.when(Bukkit::getOnlinePlayers).thenReturn(players);
				testsBm.bm.processPlayerConnection(p);
			}
		}

		public static void setupUMForGetOnlineUser(UsersManager um, Player playerMock, User userMock) {
			when(um.getOnlineUser(playerMock)).thenReturn(userMock);
			// doAnswer((invocation) -> {
			// 	Player p = invocation.getArgument(0);
			// 	if (p == playerMock) {
			// 		return userMock;
			// 	}
			// 	return null;
			// }).when(um).getOnlineUser(any(Player.class));
		}

		public void playerDisconnection(Player p) {
			// testsBm.um.processUserDisconnection(p.getUniqueId(), testsBm.vm, testsTs);
			try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
				bukkit.when(Bukkit::getOnlinePlayers).thenReturn(players);
				testsBm.bm.processPlayerDisconnection(p.getName(), p.getUniqueId());
			}
			players.remove(p);
		}

		public List<String> getPlayersName() {
			return players.stream().map((p) -> p.getName()).collect(Collectors.toList());
		}

		@Override
		public String toString() {
			return "FakeServer [name=" + name + ", players=" + CollectionUtils.toString(players) + "]";
		}

		public static Player newFakeOnlinePlayer(String name) {
			Player mock = mock(Player.class);
			setupOnlinePlayerMock(mock, name);
			return mock;
		}

		public static void setupOnlinePlayerMock(Player mock, String name) {
			when(mock.getUniqueId()).thenReturn(UUID.randomUUID());
			when(mock.getName()).thenReturn(name);
			when(mock.isOnline()).thenReturn(true);
		}

	}
}
