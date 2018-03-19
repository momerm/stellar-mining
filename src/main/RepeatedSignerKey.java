package main;

import java.util.ArrayList;
import java.util.List;

public class RepeatedSignerKey {
    private List<String> accountIDs; // Accounts that used the signer key
    private String signerKey;
    private int count; // Number of transactions signed with this key

    public RepeatedSignerKey(String signerKey, int count) {
        this.accountIDs = new ArrayList<>();
        this.signerKey = signerKey;
        this.count = count;
    }

    public String getSignerKey() {
        return signerKey;
    }

    public int getCount() {
        return count;
    }

    public void addAccountID(String accountID) {
        accountIDs.add(accountID);
    }

    public String getAccountID(int i) {
        return accountIDs.get(i);
    }
}
