package demo.coin.util;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class CryptoUtil {


    private static final String EC_Gen_Parameter_Std_name = "secp256r1";
    private static final String PKI_Algorithm_Name = "EC";
    private static final String HASH_Algorithm_Name = "SHA-256";
    private static final String Signature_Algorithm_Name = "SHA256withECDSA";


    public static KeyPair generateKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        KeyPairGenerator g = KeyPairGenerator.getInstance(PKI_Algorithm_Name);
        ECGenParameterSpec ecSpec = new ECGenParameterSpec(EC_Gen_Parameter_Std_name);
        g.initialize(ecSpec, new SecureRandom());
        return g.generateKeyPair();
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

    public static byte[] sign(byte[] privateKeyBytes, byte[] dataToSign) {
        try {
            Signature signerEngine = Signature.getInstance(Signature_Algorithm_Name);
            PrivateKey privateKey = loadPrivateKey(privateKeyBytes);
            signerEngine.initSign(privateKey);
            byte[] hashOfDataToSign = hash(dataToSign);
            signerEngine.update(hashOfDataToSign);
            return signerEngine.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] hash(byte[] data) {
        try {
            return MessageDigest.getInstance(HASH_Algorithm_Name).digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkSignature(byte[] publicKeyBytes, byte[] signedData, byte[] signature) {
        try {
            Signature signerEngine = Signature.getInstance(Signature_Algorithm_Name);
            PublicKey publicKey = loadPublicKey(publicKeyBytes);
            signerEngine.initVerify(publicKey);
            byte[] hashOfSignedData = hash(signedData);
            signerEngine.update(hashOfSignedData);
            return signerEngine.verify(signature);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Função para teste local. Remover!
     */
    public static void main(String[] args) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var keypair = generateKeyPair();
        var data = "arruma a mala aearruma a mala aearruma a mala aearruma a mala aearruma a mala aearruma a mala aearruma a mala aearruma a mala ae".getBytes();
        var priKeyBytes = keypair.getPrivate().getEncoded();
        var pubKeyBytes = keypair.getPublic().getEncoded();
        var signature = sign(priKeyBytes, data);
        var hash = hash(data);
        System.out.println(checkSignature(pubKeyBytes, data, signature));

        System.out.println("Hash " + hash.length + ": " + Arrays.toString(hash));
        System.out.println("Dat " + data.length + ": " + Arrays.toString(data));
        System.out.println("Pri " + priKeyBytes.length + ": " + Arrays.toString(priKeyBytes));
        System.out.println("Pub " + pubKeyBytes.length + ": " + Arrays.toString(pubKeyBytes));
        System.out.println("Sig " + signature.length + ": " + Arrays.toString(signature));
    }
}
