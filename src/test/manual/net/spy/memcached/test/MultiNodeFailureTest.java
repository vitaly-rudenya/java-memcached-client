package net.spy.memcached.test;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.MemcachedClient;

/**
 * This is an attempt to reproduce a problem where a server fails during a
 * series of gets.
 */
public class MultiNodeFailureTest {

	public static void main(String args[]) throws Exception {
        DefaultConnectionFactory cf = new BinaryConnectionFactory() {
            public long getOperationTimeout() {
                return 10000;
            }
        };

		MemcachedClient c=new MemcachedClient(cf,
			AddrUtil.getAddresses("localhost:11200 localhost:11201 localhost:11202 localhost:11203 localhost:11204"));

		while(true) {
			for(int i=0; i<1000; i++) {
				try {
					c.get("blah1");
				} catch(Exception e) {
					e.printStackTrace();
				}
                Thread.sleep(100);
			}
			System.out.println("Did a thousand.");
		}
	}

}
