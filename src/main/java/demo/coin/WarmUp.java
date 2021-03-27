package demo.coin;

import demo.coin.core.Utxo;
import demo.coin.util.CryptoUtil;

import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashSet;
import java.util.Set;

public class WarmUp {

    public final int numPartitions;
    public final Set<KeyPair> users;
    public final Set<Utxo> tokens;


    public WarmUp(int numPartitions, Set<KeyPair> users, Set<Utxo> tokens) {
        this.numPartitions = numPartitions;
        this.users = users;
        this.tokens = tokens;
    }

    public static WarmUp loadFrom(String filePath) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        try (var fis = new FileInputStream(filePath);
             var dis = new DataInputStream(fis)) {
            int auxPartitions = dis.readInt();
            Set<KeyPair> auxUsers = readUsers(dis);
            Set<Utxo> auxTokens = readTokens(dis);
            return new WarmUp(auxPartitions, auxUsers, auxTokens);
        }
    }

    private static Set<Utxo> readTokens(DataInputStream dis) throws IOException {
        var numTokens = dis.readInt();
        Set<Utxo> tokens = new HashSet<>(numTokens);
        for (int i = 0; i < numTokens; i++) {
            tokens.add(Utxo.readFrom(dis));
        }
        return tokens;
    }

    private static Set<KeyPair> readUsers(DataInputStream dis) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        var numUsuarios = dis.readInt();
        Set<KeyPair> users = new HashSet<>(numUsuarios);
        for (int i = 0; i < numUsuarios; i++) {
            var publicKeyBytes = dis.readNBytes(dis.readInt());
            var privateKeyBytes = dis.readNBytes(dis.readInt());
            var publicKey = CryptoUtil.loadPublicKey(publicKeyBytes);
            var privateKey = CryptoUtil.loadPrivateKey(privateKeyBytes);
            users.add(new KeyPair(publicKey, privateKey));
        }
        return users;
    }


    private static void write(int numUsuarios, int numParticoes, int numTokens, String outputFilePath) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, IOException {
        try (var fos = new FileOutputStream(outputFilePath);
             var dos = new DataOutputStream(fos)) {
            dos.write(numParticoes);
            writeUsers(numUsuarios, dos);
            writeTokens(numTokens, dos);
        }
    }

    private static void writeUsers(int numUsuarios, DataOutputStream dos) throws IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        dos.writeInt(numUsuarios);
        for (int i = 0; i < numUsuarios; i++) {
            var keyPair = CryptoUtil.generateKeyPair();
            var pub = keyPair.getPublic().getEncoded();
            dos.writeInt(pub.length);
            dos.write(pub);
            var pri = keyPair.getPrivate().getEncoded();
            dos.writeInt(pri.length);
            dos.write(pri);
        }
    }

    private static void writeTokens(int numTokens, DataOutputStream dos) throws IOException {
        dos.writeInt(numTokens);
        for (int i = 0; i < numTokens; i++) {
            var hash = CryptoUtil.hash(("transaction " + i).getBytes());
            var token = new Utxo(hash, 0, 1L);
            token.writeTo(dos);
        }
    }

    enum PARAMS {
        NUM_USUARIOS, NUM_TOKENS_USUARIO, NUM_PARTICOES, OUTPUT_FILE_PATH
    }

    public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException {
        //@formatter:off
        if (args.length != PARAMS.values().length) throw new IllegalArgumentException("Modo de uso:  java WarmUp  NUM_USUARIOS NUM_TOKENS_USUARIO NUM_PARTICOES OUTPUT_FILE_PATH");
        //@formatter:on

        int numUsuarios = Integer.parseInt(args[PARAMS.NUM_USUARIOS.ordinal()]);
        int numParticoes = Integer.parseInt(args[PARAMS.NUM_PARTICOES.ordinal()]);
        int numTokens = Integer.parseInt(args[PARAMS.NUM_TOKENS_USUARIO.ordinal()]);
        String filepath = args[PARAMS.OUTPUT_FILE_PATH.ordinal()];
        WarmUp.write(numUsuarios, numParticoes, numTokens, filepath);
    }
}
