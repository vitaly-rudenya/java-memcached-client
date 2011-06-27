package net.spy.memcached;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
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

    private static final int NUM_OF_LOOPS = 1;
    private static final int LOOP_SLEEP_TIME = 100;
    private static final String OBJ_KEY = "blah1";
    private static final String BASE_LIST_URL = "http://localhost:8091/pools";
    private static final String BUCKET_NAME = "default";
    private static final String USER_PASSWORD = "password";
    private static final String USER_NAME = "Administrator";

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

            List<URI> baselist = new ArrayList<URI>();
            baselist.add(new URI(BASE_LIST_URL));

            memcachedClient = new MemcachedClient(baselist, BUCKET_NAME, USER_NAME, USER_PASSWORD);
        } catch (Exception e) {
            logger.error(e);
            throw e;
        }
    }

    public void testNodeFail() throws Exception {
        memcachedClient.set(OBJ_KEY, 100000, OBJ_KEY);

        //Fail primary node
        MemcachedNode node = memcachedClient.getNodeLocator().getPrimary(OBJ_KEY);
        InetSocketAddress address = (InetSocketAddress) node.getSocketAddress();
        jMembase.failNode(address.getPort());

        try {
            for (int i = 0; i < NUM_OF_LOOPS; i++) {
                memcachedClient.get(OBJ_KEY);
                Thread.sleep(LOOP_SLEEP_TIME);
            }
        } catch (Exception e) {
            logger.error(e);
            fail("Fail during setting data with 50% non active nodes");
        }
    }

    public void tearDown() throws Exception {
        jMembaseRunner.interrupt();
        jMembase.close();
    }
}
