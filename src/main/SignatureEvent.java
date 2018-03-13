package main;

import org.stellar.sdk.KeyPair;
import org.stellar.sdk.xdr.AccountID;
import org.stellar.sdk.xdr.SignerKey;

public class SignatureEvent {
    private byte[] txId;
    private AccountID accountID; // Account to which the signer key belongs
    private SignerKey signerKey;
    private byte[] signature;

    public SignatureEvent(byte[] txId, AccountID accountID, SignerKey signerKey, byte[] signature) {
        this.txId = txId;
        this.accountID = accountID;
        this.signerKey = signerKey;
        this.signature = signature;
    }

    public byte[] getTxId() {
        return  txId;
    }

    public AccountID getAccountID() {
        return accountID;
    }

    public String getAccountIDstr() {
        KeyPair keyPair = KeyPair.fromXdrPublicKey(accountID.getAccountID());
        return keyPair.getAccountId();
    }

    public SignerKey getSignerKey() {
        return signerKey;
    }

    public String getSignerKeyStr() {
        KeyPair keyPair = KeyPair.fromXdrSignerKey(signerKey);
        return keyPair.getAccountId();
    }

    public byte[] getSignature() {
        return signature;
    }
}
