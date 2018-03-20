package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BadRandom {
    public static class BadRandomContext {
        private final String txID;
        private final String signature;
        private final String accountID;
        private final String signerKey;

        public BadRandomContext(String txID, String signature, String accountID, String signerKey) {
            this.txID = txID;
            this.signature = signature;
            this.accountID = accountID;
            this.signerKey = signerKey;
        }

        public String getTxID() {
            return txID;
        }

        public String getSignature() {
            return signature;
        }

        public String getAccountID() {
            return accountID;
        }

        public String getSignerKey() {
            return signerKey;
        }
    }

    private final String random;
    private final List<BadRandomContext> badRandomUses;

    public BadRandom(String random) {
        this.random = random;
        badRandomUses = new ArrayList<>();
    }

    public String getRandom() {
        return random;
    }

    public int getnUses() {
        return badRandomUses.size();
    }

    public void addUse(BadRandomContext badRandomContext) {
        badRandomUses.add(badRandomContext);
    }

    public BadRandomContext getUse(int i) {
        return badRandomUses.get(i);
    }

    public Map<String, List<BadRandomContext>> groupUsesBySignerKey() {
        return badRandomUses.stream().collect(Collectors.groupingBy(w -> w.signerKey));
    }
}
