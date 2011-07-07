package net.spy.memcached;

import org.membase.jmembase.JMembase;

/**
 * User: vitaly.rudenya
 * Date: 07.07.11
 * Time: 10:11
 */
public class MultiNodeFailureBaseBucketTest extends AbstractMultiNodeFailure {
    @Override
    protected JMembase.BucketType getBucketType() {
        return JMembase.BucketType.BASE;
    }
}
