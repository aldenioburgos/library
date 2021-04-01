package demo.coin.util;

import demo.coin.core.transactions.CoinOperation;

import java.io.*;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashSet;
import java.util.Set;

public class CryptoUtil {


    private static final String EC_Gen_Parameter_Std_name = "secp256r1";
    private static final String PKI_Algorithm_Name = "EC";
    private static final String HASH_Algorithm_Name = "SHA-256";
    private static final String Signature_Algorithm_Name = "SHA256withECDSA";


    public static KeyPair generateKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        return generateKeyPair(1).iterator().next();
    }

    public static Set<KeyPair> generateKeyPair(int number) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        KeyPairGenerator g = KeyPairGenerator.getInstance(PKI_Algorithm_Name);
        ECGenParameterSpec ecSpec = new ECGenParameterSpec(EC_Gen_Parameter_Std_name);
        g.initialize(ecSpec, new SecureRandom());
        Set<KeyPair> keyPairs = new HashSet<>(number);
        while (keyPairs.size() < number) {
            keyPairs.add(g.generateKeyPair());
        }
        return keyPairs;
    }

    public static PrivateKey loadPrivateKey(byte[] privateKeyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyGen = KeyFactory.getInstance(PKI_Algorithm_Name);
        return keyGen.generatePrivate(privateKeySpec);
    }

    public static PublicKey loadPublicKey(byte[] publicKeyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyGen = KeyFactory.getInstance(PKI_Algorithm_Name);
        return keyGen.generatePublic(pubKeySpec);
    }

    public static byte[] sign(PrivateKey privateKey, byte[] hashOfData) {
        //@formatter:off
        if (privateKey == null)                                 throw new IllegalArgumentException();
        if (hashOfData == null || hashOfData.length != 32)      throw new IllegalArgumentException();
        //@formatter:on
        try {
            Signature signerEngine = Signature.getInstance(Signature_Algorithm_Name);
            signerEngine.initSign(privateKey);
            signerEngine.update(hashOfData);
            return signerEngine.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] hash(byte[] data) {
        if (data == null)
            throw new IllegalArgumentException();
        try {
            return MessageDigest.getInstance(HASH_Algorithm_Name).digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkSignature(byte[] publicKeyBytes, byte[] signedHashOfData, byte[] signature) {
        //@formatter:off
        if (publicKeyBytes == null || publicKeyBytes.length != CoinOperation.ISSUER_SIZE)      throw new IllegalArgumentException();
        if (signedHashOfData == null || signedHashOfData.length != CoinOperation.HASH_SIZE)    throw new IllegalArgumentException();
        if (signature == null)                                                                 throw new IllegalArgumentException();
        //@formatter:on

        try {
            Signature signerEngine = Signature.getInstance(Signature_Algorithm_Name);
            PublicKey publicKey = loadPublicKey(publicKeyBytes);
            signerEngine.initVerify(publicKey);
            signerEngine.update(signedHashOfData);
            return signerEngine.verify(signature);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }


    // Public key: MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE4m9Lgsza+etaZasC+ZepzANXy4ieMx1bloRubAWP7f9A8KJ6SzY1nRPGXoik9iWYtpMM4sPA/ReEVFC8OmRaBw==
    // Private key: MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCBURQlyhb6n+QI2P342ZkptOnb7IdCWykOIFOiHFK40gQ==
    public static void main(String[] args) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, IOException {
        var number = (args.length > 0) ? Integer.parseInt(args[0]) : 1;
        var outputPath = (args.length > 1) ? args[1] : "users.txt";
        var bin = args.length > 2 && Boolean.parseBoolean(args[2]);
        Set<KeyPair> keyPairs = generateKeyPair(number);
        try (var fos = new FileOutputStream(outputPath);
             var dos = new DataOutputStream(fos)) {
            dos.writeInt(keyPairs.size());
            for (var keyPair : keyPairs) {
                var pub = keyPair.getPublic().getEncoded();
                var pri = keyPair.getPrivate().getEncoded();
                if (bin) {
                    dos.writeInt(pub.length);
                    dos.write(pub);
                    dos.writeInt(pri.length);
                    dos.write(pri);
                } else {
                    dos.writeChars(ByteUtils.convertToText(pub) + "," + ByteUtils.convertToText(pri));
                }
            }
        }
    }
}
