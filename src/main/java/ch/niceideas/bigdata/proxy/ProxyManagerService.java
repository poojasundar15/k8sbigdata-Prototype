package ch.niceideas.bigdata.proxy;

import ch.niceideas.bigdata.model.service.proxy.ProxyTunnelConfig;
import ch.niceideas.bigdata.services.ConnectionManagerException;
import ch.niceideas.bigdata.types.Node;
import ch.niceideas.bigdata.types.Service;
import ch.niceideas.bigdata.types.ServiceWebId;
import org.apache.hc.core5.http.HttpHost;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public interface ProxyManagerService {

    int LOCAL_PORT_RANGE_START = 39152;

    int PORT_GENERATION_MAX_ATTEMPT_COUNT = 10000;

    HttpHost getServerHost(ServiceWebId serviceId);

    String getServerURI(Service serviceName, String pathInfo);

    Node extractHostFromPathInfo(String pathInfo);

    List<ProxyTunnelConfig> getTunnelConfigForHost (Node host);

    void updateServerForService(Service service, Node runtimeNode) throws ConnectionManagerException;

    void removeServerForService(Service service, Node runtimeNode);

    Collection<ServiceWebId> getAllTunnelConfigKeys();

    ProxyTunnelConfig getTunnelConfig(ServiceWebId serviceId);

    /** get a port number from 49152 to 65535 */
    static int generateLocalPort() {
        int portNumber;
        int tryCount = 0;
        do {
            int randInc = ThreadLocalRandom.current().nextInt(65534 - LOCAL_PORT_RANGE_START);
            portNumber = LOCAL_PORT_RANGE_START + randInc;
            tryCount++;
        } while (isLocalPortInUse(portNumber) && tryCount < PORT_GENERATION_MAX_ATTEMPT_COUNT);
        if (tryCount >= PORT_GENERATION_MAX_ATTEMPT_COUNT) {
            throw new IllegalStateException();
        }
        return portNumber;
    }

    private static boolean isLocalPortInUse(int port) {
        try {
            // ServerSocket try to open a LOCAL port
            new ServerSocket(port).close();
            // local port can be opened, it's available
            return false;
        } catch(IOException e) {
            // local port cannot be opened, it's in use
            return true;
        }
    }
}
