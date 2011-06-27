package net.spy.memcached;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import net.spy.memcached.vbucket.ConfigurationProviderHTTP;
import net.spy.memcached.vbucket.config.Bucket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.membase.jmembase.JMembase;

/**
 * Tests case when 50% of nodes are fail.
 * User: vitaly.rudenya
 * Date: 22.06.11
 * Time: 08:49
 */
public class MultiNodeFailureTest extends TestCase {

    private static final String OBJ_KEY = "blah1";
    private static final String BASE_LIST_URL = "http://localhost:8091/pools";
    private static final String BUCKET_NAME = "default";
    private static final String USER_PASSWORD = "password";
    private static final String USER_NAME = "Administrator";
    private static final List<URI> BASE_LIST = new ArrayList<URI>();

    static {
        try {
            BASE_LIST.add(new URI(BASE_LIST_URL));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private JMembase jMembase;
    private MemcachedClient memcachedClient;
    private Thread jMembaseRunner;

    private Log logger = LogFactory.getLog(this.getClass());

    public void setUp() throws Exception {
        try {
            jMembase = new JMembase(8091, 5, 1024, JMembase.BucketType.BASE);
            jMembaseRunner = new Thread(jMembase);
            jMembaseRunner.setDaemon(true);
            jMembaseRunner.start();

            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                logger.error(ex);
            }

            memcachedClient = new MemcachedClient(BASE_LIST, BUCKET_NAME, USER_NAME, USER_PASSWORD);
        } catch (Exception e) {
            logger.error(e);
            throw e;
        }
    }

    public void testNodeFail() throws Exception {
        memcachedClient.set(OBJ_KEY, 100000, OBJ_KEY);

        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            logger.error(ex);
        }

        failPrimaryNode();

        try {
            memcachedClient.get(OBJ_KEY);
        } catch (Exception e) {
            logger.error(e);
            fail("Fail during getting data with 50% non active nodes");
        }
    }

    public void testReconfiguration() throws Exception {
        memcachedClient.set(OBJ_KEY, 100000, OBJ_KEY);

        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            logger.error(ex);
        }

        failPrimaryNode();

        //TODO client should has function to get bucket information
        ConfigurationProviderHTTP configurationProvider = new ConfigurationProviderHTTP(BASE_LIST, USER_NAME, USER_PASSWORD);
        Bucket bucket = configurationProvider.getBucketConfiguration(BUCKET_NAME);

        memcachedClient.reconfigure(bucket);

        try {
            memcachedClient.get(OBJ_KEY);
        } catch (Exception e) {
            logger.error(e);
            fail("Fail during getting after bucket reconfiguration");
        }
    }

    public void tearDown() throws Exception {
        jMembaseRunner.interrupt();
        jMembase.close();
    }

    /**
     * Fail primary node
     */
    private void failPrimaryNode() {
        MemcachedNode node = memcachedClient.getNodeLocator().getPrimary(OBJ_KEY);
        InetSocketAddress address = (InetSocketAddress) node.getSocketAddress();
        jMembase.failNode(address.getPort());
    }
}
