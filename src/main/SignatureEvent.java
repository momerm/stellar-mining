package main;

import org.stellar.sdk.xdr.AccountID;
import org.stellar.sdk.xdr.SignerKeyType;

public class SignatureEvent {
    private byte[] txId;
    private AccountID accountID; // Account to which the signer key belongs
    private SignerKeyType type; // Type of signature
    private byte[] signerKey;
    private byte[] signature;

    public SignatureEvent(byte[] txId, AccountID accountID, SignerKeyType type, byte[] signerKey, byte[] signature) {
        this.txId = txId;
        this.accountID = accountID;
        this.type = type;
        this.signerKey = signerKey;
        this.signature = signature;
    }

    public byte[] getTxId() {
        return  txId;
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
