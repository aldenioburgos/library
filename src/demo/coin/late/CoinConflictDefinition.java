package demo.coin.late;

import demo.coin.core.operation.CoinSingleOperationContext;
import demo.coin.core.transactions.CoinOperation;
import demo.coin.core.transactions.CoinOperation.OP_TYPE;
import demo.coin.util.ByteUtils;
import parallelism.late.ConflictDefinition;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static demo.coin.util.ByteUtils.readUnsignedByte;

public class CoinConflictDefinition implements ConflictDefinition {


    public boolean isDependent(Object r1, Object r2) {
        byte[] op1 = ((CoinSingleOperationContext) r1).operation;
        byte[] op2 = ((CoinSingleOperationContext) r2).operation;
        if (OP_TYPE.BALANCE.ordinal() == op1[0] && OP_TYPE.BALANCE.ordinal() == op2[0]) {
            return false;
        }
        var accounts1 = readAccounts(op1, 1);
        var accounts2 = readAccounts(op2, 1);
        return accounts1.stream().anyMatch(it -> accounts2.contains(it));
    }


    public Set<byte[]> readAccounts(byte[] operation, int offset) {
        Set<byte[]> accountSet = new HashSet<>();

        int numAccounts = readUnsignedByte(operation, offset);
        for (int i = 0; i < numAccounts; i++) {
            var pos = offset + 1 + (i * CoinOperation.ISSUER_SIZE);
            accountSet.add(Arrays.copyOfRange(operation, pos, pos + CoinOperation.ISSUER_SIZE));
        }
        return accountSet;
    }
}
