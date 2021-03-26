package demo.coin.core.requestresponse;

import parallelism.HoldsClassIdInterface;

public interface OperationContext extends HoldsClassIdInterface {

    boolean isConcurrent();
}
