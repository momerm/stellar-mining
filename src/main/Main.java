package main;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.xdr.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;


public class Main {
    public static void main(String args[]) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException, SQLException, DecoderException {
    //    StellarDB db = new StellarDB("jdbc:postgresql://192.168.250.65:5432/stellar", "dbuser", "stellar");

        List<SignatureEvent> signatureEvents = TransactionParser.getSignatureEvents("4d77105357995160991887908563f2d71a0912a5708bc8e0755de338bf0c074f",
                "AAAAAATHfx3AZPvVeirajtLvcQDevP6EK2tz181Gwvw9Uim/AAAAZAAplwcAAAACAAAAAAAAAAAAAAABAAAAAAAAAAEAAAAAZIgfZxM4Y1vjfyo+5F9Q3tSjRyh670waceCZWl2I93wAAAAAAAAAAACYkWIAAAAAAAAAAT1SKb8AAABAuGlf7cXSubLlvtCcyUPDkiwfTOaz1iYKskH0jFUDjsw2Yupg8Qh/w7u6aBir6P0/+KAfzMrfdRYdkpSZA4G1Cg==",
                "AAAAAAAAAAA=");


        // Print results
        signatureEvents.forEach((e) -> {
            System.out.println("Transaction ID: " + Hex.encodeHexString(e.getTxId()));
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
            if(e.getType() == SignerKeyType.SIGNER_KEY_TYPE_ED25519) {
                byte[] r = Arrays.copyOfRange(e.getSignature(), 0, 32);
                byte[] s = Arrays.copyOfRange(e.getSignature(), 32, 64);
                System.out.println("r: " + Base64.getEncoder().encodeToString(r));
                System.out.println("s: " + Base64.getEncoder().encodeToString(s));
            }
            System.out.println();
        });

      //  db.close();
    }
}
