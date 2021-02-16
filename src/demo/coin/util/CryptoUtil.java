package demo.coin.util;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class CryptoUtil {

    public static KeyPair generateKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(ecSpec, new SecureRandom());
        return g.generateKeyPair();
    }

    public static PrivateKey loadPrivateKey(byte[] privateKeyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyGen = KeyFactory.getInstance("EC");
        return keyGen.generatePrivate(privateKeySpec);
    }
    public static PublicKey loadPublicKey(byte[] publicKeyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyGen = KeyFactory.getInstance("EC");
        return keyGen.generatePublic(pubKeySpec);
    }

    public static byte[] sign(byte[] privateKeyBytes, byte[] dataToSign) {
        try {
            Signature signerEngine = Signature.getInstance("SHA256withECDSA");
            PrivateKey privateKey = loadPrivateKey(privateKeyBytes);
            signerEngine.initSign(privateKey);
            signerEngine.update(dataToSign);
            return signerEngine.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        for (int i = 0; i < 1000; i++) {
            var pair = CryptoUtil.generateKeyPair();
            System.out.println(pair.getPrivate());
            System.out.println("Private key:" + Arrays.toString(pair.getPrivate().getEncoded()));
            System.out.println("Private key format:" + pair.getPrivate().getFormat());
            System.out.println("Private key algoritm:" + pair.getPrivate().getAlgorithm());
            System.out.println("Private key size:" + pair.getPrivate().getEncoded().length);
            System.out.println(pair.getPublic());
            System.out.println("Public key:" + Arrays.toString(pair.getPublic().getEncoded()));
            System.out.println("Public key format:" + pair.getPublic().getFormat());
            System.out.println("Public key algoritm:" + pair.getPublic().getAlgorithm());
            System.out.println("Public key size:" + pair.getPublic().getEncoded().length);

        }
    }
}
