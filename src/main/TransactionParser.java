package main;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.xdr.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class TransactionParser {
    public static List<SignatureEvent> getSignatureEvents(String txId_hex, String txEnvelope_base64, String txMeta_base64) throws DecoderException, IOException {
        byte[] txId_bytes = Hex.decodeHex(txId_hex);

        // Parse XDR
        byte[] txEnvelope_bytes = Base64.getDecoder().decode(txEnvelope_base64);
        InputStream stream = new ByteArrayInputStream(txEnvelope_bytes);
        XdrDataInputStream xdrStream = new XdrDataInputStream(stream);
        TransactionEnvelope txEnvelope = TransactionEnvelope.decode(xdrStream);
        xdrStream.close();
        stream.close();

        byte[] txMeta_bytes = Base64.getDecoder().decode(txMeta_base64);
        stream = new ByteArrayInputStream(txMeta_bytes);
        xdrStream = new XdrDataInputStream(stream);
        TransactionMeta txMeta = TransactionMeta.decode(xdrStream);
        xdrStream.close();
        stream.close();


        HashMap<AccountID, Signer[]> accountAndSigners = new HashMap<>();
        ArrayList<AccountID> accountIDs = new ArrayList<>();

        // Get all source account IDs in the transaction
        accountIDs.add(txEnvelope.getTx().getSourceAccount());
        for(Operation op : txEnvelope.getTx().getOperations()) {
            if(op.getSourceAccount() != null)
                accountIDs.add(op.getSourceAccount());
        }

        // Get all account IDs and Signer Keys in the meta data
        for(OperationMeta opMeta : txMeta.getOperations()) {
            for(LedgerEntryChange lChange : opMeta.getChanges().getLedgerEntryChanges()) {
                LedgerEntry.LedgerEntryData ledgerEntryData = null;
                LedgerKey ledgerKey = null;
                switch (lChange.getDiscriminant()) {
                    case LEDGER_ENTRY_CREATED:
                        ledgerEntryData = lChange.getCreated().getData();
                        break;
                    case LEDGER_ENTRY_UPDATED:
                        ledgerEntryData = lChange.getUpdated().getData();
                        break;
                    case LEDGER_ENTRY_REMOVED:
                        ledgerKey = lChange.getRemoved();
                        break;
                    case LEDGER_ENTRY_STATE:
                        ledgerEntryData = lChange.getState().getData();
                        break;
                }
                if(ledgerEntryData != null) {
                    switch (ledgerEntryData.getDiscriminant()) {
                        case ACCOUNT:
                            // Check if this is a source account
                            accountIDs.add(ledgerEntryData.getAccount().getAccountID());
                            accountAndSigners.put(ledgerEntryData.getAccount().getAccountID(), ledgerEntryData.getAccount().getSigners());
                            break;
                        case TRUSTLINE:
                            accountIDs.add(ledgerEntryData.getTrustLine().getAccountID());
                            break;
                        case OFFER:
                            accountIDs.add(ledgerEntryData.getOffer().getSellerID());
                            break;
                        case DATA:
                            accountIDs.add(ledgerEntryData.getData().getAccountID());
                            break;
                    }
                }
                if(ledgerKey != null) {
                    accountIDs.add(ledgerKey.getAccount().getAccountID());
                }
            }
        }
        assert accountAndSigners.size() > 0;


        // Store signatures and corresponding public keys. Use signer
        ArrayList<SignatureEvent> signatureEvents = new ArrayList<>();

        // For each signature try to find a public key using the hint.
        sigloop:
        for(DecoratedSignature decoratedSignature : txEnvelope.getSignatures()) {
            byte[] hint = decoratedSignature.getHint().getSignatureHint();
            byte[] signature = decoratedSignature.getSignature().getSignature();
            // Search accountIDs
            for(AccountID accountID : accountIDs) {
                assert accountID.getAccountID().getDiscriminant() == PublicKeyType.PUBLIC_KEY_TYPE_ED25519;
                byte key[] = accountID.getAccountID().getEd25519().getUint256();
                if(key[28] == hint[0] && key[29] == hint[1] && key[30] == hint[2] && key[31] == hint[3]) {
                    // Verify signature to make sure we have the right key
                    KeyPair keyPair = KeyPair.fromPublicKey(key);
                    if(keyPair.verify(txId_bytes, signature)) {
                        signatureEvents.add(new SignatureEvent(
                                txId_bytes,
                                accountID,
                                SignerKeyType.SIGNER_KEY_TYPE_ED25519,
                                accountID.getAccountID().getEd25519().getUint256(),
                                signature));
                        break sigloop;
                    }
                }
            }
            // Search in signers
            for(Map.Entry<AccountID, Signer[]> entry : accountAndSigners.entrySet()) {
                AccountID accountID = entry.getKey();
                Signer[] signers = entry.getValue();

                byte key[];
                for(Signer signer : signers) {
                    switch (signer.getKey().getDiscriminant()) {
                        case SIGNER_KEY_TYPE_ED25519:
                            key = signer.getKey().getEd25519().getUint256();
                            if(key[28] == hint[0] && key[29] == hint[1] && key[30] == hint[2] && key[31] == hint[3]) {
                                // Verify signature to make sure we have the right key
                                KeyPair keyPair = KeyPair.fromPublicKey(key);
                                if(keyPair.verify(txId_bytes, signature)) {
                                    signatureEvents.add(new SignatureEvent(
                                            txId_bytes,
                                            accountID,
                                            SignerKeyType.SIGNER_KEY_TYPE_ED25519,
                                            key,
                                            signature));
                                    break sigloop;
                                }
                            }
                            break;
                        case SIGNER_KEY_TYPE_PRE_AUTH_TX:
                            // TODO: Verify this signature to make sure we have the right key
                            key = signer.getKey().getPreAuthTx().getUint256();
                            if(key[28] == hint[0] && key[29] == hint[1] && key[30] == hint[2] && key[31] == hint[3]) {
                                signatureEvents.add(new SignatureEvent(
                                        txId_bytes,
                                        accountID,
                                        SignerKeyType.SIGNER_KEY_TYPE_PRE_AUTH_TX,
                                        key,
                                        signature));
                                break sigloop;
                            }
                            break;
                        case SIGNER_KEY_TYPE_HASH_X:
                            // TODO: Verify this signature to make sure we have the right key
                            key = signer.getKey().getHashX().getUint256();
                            if(key[28] == hint[0] && key[29] == hint[1] && key[30] == hint[2] && key[31] == hint[3]) {
                                signatureEvents.add(new SignatureEvent(
                                        txId_bytes,
                                        accountID,
                                        SignerKeyType.SIGNER_KEY_TYPE_HASH_X,
                                        key,
                                        signature));
                                break sigloop;
                            }
                            break;
                    }
                }
            }
        }
        return signatureEvents;
    }
}
