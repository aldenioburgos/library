package demo.coin.core;

import demo.coin.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import static demo.coin.util.CryptoUtil.generateKeyPair;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.*;

public class CoinGlobalStateTest {


    @BeforeEach
    void setUp() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {

    }

    @Test
    void isMinterNoMinter() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var globalState = new CoinGlobalState();
        var keypair = generateKeyPair();
        assertFalse(globalState.isMinter(null));
        assertFalse(globalState.isMinter(new byte[]{}));
        assertFalse(globalState.isMinter(keypair.getPublic().getEncoded()));
    }

    @Test
    void isMinterWithMinter() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var keypair = generateKeyPair();
        var otherKeypair = generateKeyPair();
        var minter = keypair.getPublic().getEncoded();
        var globalState = new CoinGlobalState(Set.of(minter), emptySet(), (byte) 0);

        assertFalse(globalState.isMinter(new byte[]{}));
        assertFalse(globalState.isMinter(otherKeypair.getPublic().getEncoded()));
        assertTrue(globalState.isMinter(minter));
    }


    @Test
    void isUserNoUser() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var globalState = new CoinGlobalState();
        var keypair = generateKeyPair();
        assertFalse(globalState.isUser(null));
        assertFalse(globalState.isUser(new byte[]{}));
        assertFalse(globalState.isUser(keypair.getPublic().getEncoded()));
    }


    @Test
    void isUserWithUser() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var keypair = generateKeyPair();
        var otherKeypair = generateKeyPair();

        var globalState = new CoinGlobalState(Set.of(keypair.getPublic().getEncoded()), emptySet(), (byte) 0);

        assertFalse(globalState.isUser(new byte[]{}));
        assertFalse(globalState.isUser(otherKeypair.getPublic().getEncoded()));
        assertTrue(globalState.isUser(keypair.getPublic().getEncoded()));
    }

    @Test
    void isCurrencyWithCurrency() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var keypair = generateKeyPair();
        var globalState = new CoinGlobalState(Set.of(keypair.getPublic().getEncoded()), emptySet(), (byte) 0);
        assertFalse(globalState.isCurrency((byte) -1));
        assertFalse(globalState.isCurrency((byte) 1));
        assertTrue(globalState.isCurrency((byte) 0));
    }

    @Test
    void isCurrencyNoCurrency() {
        var emptyGlobalState = new CoinGlobalState();
        assertFalse(emptyGlobalState.isCurrency((byte) 0));
    }

    @Test
    void addListAndListUtxo() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var keypair = generateKeyPair();
        var globalState = new CoinGlobalState(Set.of(keypair.getPublic().getEncoded()), emptySet(), (byte) 0, (byte) 1);
        assertEquals(0, globalState.listUtxos(keypair.getPublic().getEncoded(), 0).size());

        globalState.addUtxo(0, keypair.getPublic().getEncoded(), CryptoUtil.hash(keypair.getPublic().getEncoded()),0, 1L);
        assertEquals(0, globalState.listUtxos(keypair.getPublic().getEncoded(), 1).size());
        assertEquals(1, globalState.listUtxos(keypair.getPublic().getEncoded(), 0).size());

        globalState.removeUtxos(1, keypair.getPublic().getEncoded(), Set.of(new UtxoAddress(CryptoUtil.hash(keypair.getPublic().getEncoded()), 0)));
        assertEquals(0, globalState.listUtxos(keypair.getPublic().getEncoded(), 1).size());
    }



}
