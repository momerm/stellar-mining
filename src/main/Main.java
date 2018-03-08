package main;

import org.stellar.sdk.KeyPair;
import org.stellar.sdk.xdr.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.*;


public class Main {
    public static void main(String args[]) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException, SQLException {
    //    StellarDB db = new StellarDB("jdbc:postgresql://192.168.250.65:5432/stellar", "dbuser", "stellar");

        // Parse XDR
        byte[] txbody = Base64.getDecoder().decode("AAAAAATHfx3AZPvVeirajtLvcQDevP6EK2tz181Gwvw9Uim/AAAAZAAplwcAAAABAAAAAAAAAAAAAAABAAAAAAAAAAEAAAAAZIgfZxM4Y1vjfyo+5F9Q3tSjRyh670waceCZWl2I93wAAAAAAAAAAACYkcYAAAAAAAAAAT1SKb8AAABAQkqDVUKEHgUGSC+xYIq1nK3awhp1vUUSgfUMIkJG1jO3oy/HgHCqlU5CLFSgVfQMmRQ7Z0r3gxK5nVLSuTxfAg==");
        InputStream stream = new ByteArrayInputStream(txbody);
        XdrDataInputStream xdrStream = new XdrDataInputStream(stream);
        TransactionEnvelope txEnvelope = TransactionEnvelope.decode(xdrStream);
        xdrStream.close();
        stream.close();

        byte[] txmeta = Base64.getDecoder().decode("AAAAAAAAAAA=");
        stream = new ByteArrayInputStream(txmeta);
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
                    signatureEvents.add(new SignatureEvent(accountID,
                            SignerKeyType.SIGNER_KEY_TYPE_ED25519,
                            accountID.getAccountID().getEd25519().getUint256(),
                            signature));
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
                                signatureEvents.add(new SignatureEvent(accountID,
                                        SignerKeyType.SIGNER_KEY_TYPE_ED25519,
                                        key,
                                        signature));
                                break sigloop;
                            }
                            break;
                        case SIGNER_KEY_TYPE_PRE_AUTH_TX:
                            key = signer.getKey().getPreAuthTx().getUint256();
                            if(key[28] == hint[0] && key[29] == hint[1] && key[30] == hint[2] && key[31] == hint[3]) {
                                signatureEvents.add(new SignatureEvent(accountID,
                                        SignerKeyType.SIGNER_KEY_TYPE_PRE_AUTH_TX,
                                        key,
                                        signature));
                                break sigloop;
                            }
                            break;
                        case SIGNER_KEY_TYPE_HASH_X:
                            key = signer.getKey().getHashX().getUint256();
                            if(key[28] == hint[0] && key[29] == hint[1] && key[30] == hint[2] && key[31] == hint[3]) {
                                signatureEvents.add(new SignatureEvent(accountID,
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

        // Print results
        signatureEvents.forEach((e) -> {
            KeyPair keyPair = KeyPair.fromXdrPublicKey(e.getAccountID().getAccountID());
            System.out.println("Account ID: " + keyPair.getAccountId());
            System.out.println("Signature Type: " + e.getType().name());
            System.out.print("Signer Key: " + Base64.getEncoder().encodeToString(e.getSignerKey()));
            if(Arrays.equals(e.getSignerKey(), e.getAccountID().getAccountID().getEd25519().getUint256())) {
                System.out.println(" (Same as Account ID)");
            }
            else {
                System.out.println();
            }
            System.out.println("Signature: " + Base64.getEncoder().encodeToString(e.getSignature()));
            System.out.println();
        });

      //  db.close();
    }
}
