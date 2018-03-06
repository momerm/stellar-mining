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
        byte[] txbody = Base64.getDecoder().decode("AAAAAERmsKL73CyLV/HvjyQCERDXXpWE70Xhyb6MR5qPO3yQAAAAZAAIbkEAAAATAAAAAAAAAANfnyLrtrcJnWJ3nXVFfIwgEiEJZgAAAAAAAAAAAAAAAAAAAAEAAAABAAAAAP1qe44j+i4uIT+arbD4QDQBt8ryEeJd7a0jskQ3nwDeAAAAAQAAAACMeNF28DnnCSCEKAXA9o9jW52f/KL1uSismOAQsmeU7QAAAAAAAAAAD39IOAAAAAAAAAABjzt8kAAAAEA/6WVMUbxXjNZmkLc+Lmb0Wdqy9AJQzdmKoqC31RhdTykICq4/cx79+Ir6bW6qGR5orNY7DjUac4Mibb+nHrcM");
        InputStream stream = new ByteArrayInputStream(txbody);
        XdrDataInputStream xdrStream = new XdrDataInputStream(stream);
        TransactionEnvelope txEnvelope = TransactionEnvelope.decode(xdrStream);
        xdrStream.close();
        stream.close();

        byte[] txmeta = Base64.getDecoder().decode("AAAAAAAAAAEAAAAEAAAAAwAIgTMAAAAAAAAAAIx40XbwOecJIIQoBcD2j2NbnZ/8ovW5KKyY4BCyZ5TtAAAAB3In0PwACIEDAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAQAIgTMAAAAAAAAAAIx40XbwOecJIIQoBcD2j2NbnZ/8ovW5KKyY4BCyZ5TtAAAAB4GnGTQACIEDAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAwAIgTMAAAAAAAAAAP1qe44j+i4uIT+arbD4QDQBt8ryEeJd7a0jskQ3nwDeAAKJwEvUnFYACD1BAAAAHgAAAAoAAAAAAAAAAAAAAAABAAAAAAAACgAAAAARC07BokpLTOF+/vVKBwiAlop7hHGJTNeGGlY4MoPykwAAAAEAAAAAK+Lzfd3yDD+Ov0GbYu1g7SaIBrKZeBUxoCunkLuI7aoAAAABAAAAAERmsKL73CyLV/HvjyQCERDXXpWE70Xhyb6MR5qPO3yQAAAAAQAAAABSORGwAdyuanN3sNOHqNSpACyYdkUM3L8VafUu69EvEgAAAAEAAAAAeCzqJNkMM/jLvyuMIfyFHljBlLCtDyj17RMycPuNtRMAAAABAAAAAIEi4R7juq15ymL00DNlAddunyFT4FyUD4muC4t3bobdAAAAAQAAAACaNpLL5YMfjOTdXVEqrAh99LM12sN6He6pHgCRAa1f1QAAAAEAAAAAqB+lfAPV9ak+Zkv4aTNZwGaFFAfui4+yhM3dGhoYJ+sAAAABAAAAAMNJrEvdMg6M+M+n4BDIdzsVSj/ZI9SvAp7mOOsvAD/WAAAAAQAAAADbHA6xiKB1+G79mVqpsHMOleOqKa5mxDpP5KEp/Xdz9wAAAAEAAAAAAAAAAAAAAAEACIEzAAAAAAAAAAD9anuOI/ouLiE/mq2w+EA0AbfK8hHiXe2tI7JEN58A3gACicA8VVQeAAg9QQAAAB4AAAAKAAAAAAAAAAAAAAAAAQAAAAAAAAoAAAAAEQtOwaJKS0zhfv71SgcIgJaKe4RxiUzXhhpWODKD8pMAAAABAAAAACvi833d8gw/jr9Bm2LtYO0miAaymXgVMaArp5C7iO2qAAAAAQAAAABEZrCi+9wsi1fx748kAhEQ116VhO9F4cm+jEeajzt8kAAAAAEAAAAAUjkRsAHcrmpzd7DTh6jUqQAsmHZFDNy/FWn1LuvRLxIAAAABAAAAAHgs6iTZDDP4y78rjCH8hR5YwZSwrQ8o9e0TMnD7jbUTAAAAAQAAAACBIuEe47qtecpi9NAzZQHXbp8hU+BclA+JrguLd26G3QAAAAEAAAAAmjaSy+WDH4zk3V1RKqwIffSzNdrDeh3uqR4AkQGtX9UAAAABAAAAAKgfpXwD1fWpPmZL+GkzWcBmhRQH7ouPsoTN3RoaGCfrAAAAAQAAAADDSaxL3TIOjPjPp+AQyHc7FUo/2SPUrwKe5jjrLwA/1gAAAAEAAAAA2xwOsYigdfhu/ZlaqbBzDpXjqimuZsQ6T+ShKf13c/cAAAABAAAAAAAAAAA=");
        stream = new ByteArrayInputStream(txmeta);
        xdrStream = new XdrDataInputStream(stream);
        TransactionMeta txMeta = TransactionMeta.decode(xdrStream);
        xdrStream.close();
        stream.close();

        // Get all accountIDS and Signer Keys in the meta data
        HashMap<AccountID, Signer[]> accountAndSigners = new HashMap<>();
        ArrayList<AccountID> accountIDs = new ArrayList<>();
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
