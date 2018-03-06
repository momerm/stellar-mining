package main;

import org.stellar.sdk.xdr.AccountID;
import org.stellar.sdk.xdr.SignerKeyType;

public class SignatureEvent {
    private AccountID accountID; // Account to which the key belongs
    private SignerKeyType type; // Type of signature
    private byte[] signerKey;
    private byte[] signature;

    public SignatureEvent(AccountID accountID, SignerKeyType type, byte[] signerKey, byte[] signature) {
        this.accountID = accountID;
        this.type = type;
        this.signerKey = signerKey;
        this.signature = signature;
    }

    public AccountID getAccountID() {
        return accountID;
    }

    public SignerKeyType getType() {
        return type;
    }

    public byte[] getSignerKey() {
        return signerKey;
    }

    public byte[] getSignature() {
        return signature;
    }
}
