package com.sparrowwallet.drongo.protocol;

import com.sparrowwallet.drongo.KeyDerivation;
import com.sparrowwallet.drongo.Utils;
import com.sparrowwallet.drongo.address.*;
import com.sparrowwallet.drongo.crypto.ChildNumber;
import com.sparrowwallet.drongo.crypto.ECKey;
import com.sparrowwallet.drongo.policy.PolicyType;

import java.util.*;
import java.util.stream.Collectors;

import static com.sparrowwallet.drongo.policy.PolicyType.*;
import static com.sparrowwallet.drongo.protocol.Script.decodeFromOpN;
import static com.sparrowwallet.drongo.protocol.ScriptOpCodes.*;

public enum ScriptType {
    P2PK("P2PK", "m/44'/0'/0'") {
        @Override
        public Address getAddress(byte[] pubKey) {
            return new P2PKAddress(pubKey);
        }

        @Override
        public Address getAddress(ECKey key) {
            return getAddress(key.getPubKey());
        }

        @Override
        public Address getAddress(Script script) {
            throw new ProtocolException("No script derived address for non pay to script type");
        }

        @Override
        public Address[] getAddresses(Script script) {
            return new Address[] { getAddress(getPublicKeyFromScript(script).getPubKey()) };
        }

        @Override
        public Script getOutputScript(byte[] pubKey) {
            List<ScriptChunk> chunks = new ArrayList<>();
            chunks.add(new ScriptChunk(pubKey.length, pubKey));
            chunks.add(new ScriptChunk(ScriptOpCodes.OP_CHECKSIG, null));

            return new Script(chunks);
        }

        @Override
        public Script getOutputScript(ECKey key) {
            return getOutputScript(key.getPubKey());
        }

        @Override
        public Script getOutputScript(Script script) {
            throw new ProtocolException("No script derived output script for non pay to script type");
        }

        @Override
        public String getOutputDescriptor(ECKey key) {
            return "pk(" + Utils.bytesToHex(key.getPubKey()) + ")";
        }

        @Override
        public String getOutputDescriptor(Script script) {
            throw new ProtocolException("No script derived output descriptor for non pay to script type");
        }

        @Override
        public boolean isScriptType(Script script) {
            List<ScriptChunk> chunks = script.chunks;
            if (chunks.size() != 2)
                return false;
            if (!chunks.get(0).equalsOpCode(0x21) && !chunks.get(0).equalsOpCode(0x41))
                return false;
            byte[] chunk2data = chunks.get(0).data;
            if (chunk2data == null)
                return false;
            if (chunk2data.length != 33 && chunk2data.length != 65)
                return false;
            if (!chunks.get(1).equalsOpCode(OP_CHECKSIG))
                return false;
            return true;
        }

        @Override
        public byte[] getHashFromScript(Script script) {
            throw new ProtocolException("P2PK script does contain hash, use getPublicKeyFromScript(script) to retreive public key");
        }

        @Override
        public ECKey getPublicKeyFromScript(Script script) {
            return ECKey.fromPublicOnly(script.chunks.get(0).data);
        }

        @Override
        public List<PolicyType> getAllowedPolicyTypes() {
            return List.of(SINGLE);
        }
    },
    P2PKH("P2PKH", "m/44'/0'/0'") {
        @Override
        public Address getAddress(byte[] pubKeyHash) {
            return new P2PKHAddress(pubKeyHash);
        }

        @Override
        public Address getAddress(ECKey key) {
            return getAddress(key.getPubKeyHash());
        }

        @Override
        public Address getAddress(Script script) {
            throw new ProtocolException("No script derived address for non pay to script type");
        }

        @Override
        public Script getOutputScript(byte[] pubKeyHash) {
            List<ScriptChunk> chunks = new ArrayList<>();
            chunks.add(new ScriptChunk(ScriptOpCodes.OP_DUP, null));
            chunks.add(new ScriptChunk(ScriptOpCodes.OP_HASH160, null));
            chunks.add(new ScriptChunk(pubKeyHash.length, pubKeyHash));
            chunks.add(new ScriptChunk(ScriptOpCodes.OP_EQUALVERIFY, null));
            chunks.add(new ScriptChunk(ScriptOpCodes.OP_CHECKSIG, null));

            return new Script(chunks);
        }

        @Override
        public Script getOutputScript(ECKey key) {
            return getOutputScript(key.getPubKeyHash());
        }

        @Override
        public Script getOutputScript(Script script) {
            throw new ProtocolException("No script derived output script for non pay to script type");
        }

        @Override
        public String getOutputDescriptor(ECKey key) {
            return "pkh(" + Utils.bytesToHex(key.getPubKey()) + ")";
        }

        @Override
        public String getOutputDescriptor(Script script) {
            throw new ProtocolException("No script derived output descriptor for non pay to script type");
        }

        @Override
        public boolean isScriptType(Script script) {
            List<ScriptChunk> chunks = script.chunks;
            if (chunks.size() != 5)
                return false;
            if (!chunks.get(0).equalsOpCode(OP_DUP))
                return false;
            if (!chunks.get(1).equalsOpCode(OP_HASH160))
                return false;
            byte[] chunk2data = chunks.get(2).data;
            if (chunk2data == null)
                return false;
            if (chunk2data.length != 20)
                return false;
            if (!chunks.get(3).equalsOpCode(OP_EQUALVERIFY))
                return false;
            if (!chunks.get(4).equalsOpCode(OP_CHECKSIG))
                return false;
            return true;
        }

        @Override
        public byte[] getHashFromScript(Script script) {
            return script.chunks.get(2).data;
        }

        @Override
        public List<PolicyType> getAllowedPolicyTypes() {
            return List.of(SINGLE);
        }
    },
    MULTISIG("Bare Multisig", "m/44'/0'/0'") {
        @Override
        public Address getAddress(byte[] bytes) {
            throw new ProtocolException("No single address for multisig script type");
        }

        @Override
        public Address getAddress(Script script) {
            throw new ProtocolException("No single address for multisig script type");
        }

        @Override
        public Address getAddress(ECKey key) {
            throw new ProtocolException("No single key address for multisig script type");
        }

        @Override
        public Address[] getAddresses(Script script) {
            return Arrays.stream(getPublicKeysFromScript(script)).map(pubKey -> new P2PKAddress(pubKey.getPubKey())).toArray(Address[]::new);
        }

        @Override
        public Script getOutputScript(byte[] bytes) {
            throw new ProtocolException("Output script for multisig script type must be constructed with method getOutputScript(int threshold, List<ECKey> pubKeys)");
        }

        @Override
        public Script getOutputScript(ECKey key) {
            throw new ProtocolException("Output script for multisig script type must be constructed with method getOutputScript(int threshold, List<ECKey> pubKeys)");
        }

        @Override
        public Script getOutputScript(Script script) {
            if(isScriptType(script)) {
                return script;
            }

            throw new ProtocolException("No script derived output script for non pay to script type");
        }

        @Override
        public Script getOutputScript(int threshold, List<ECKey> pubKeys) {
            List<byte[]> pubKeyBytes = new ArrayList<>();
            for(ECKey key : pubKeys) {
                pubKeyBytes.add(key.getPubKey());
            }
            pubKeyBytes.sort(new Utils.LexicographicByteArrayComparator());

            List<ScriptChunk> chunks = new ArrayList<>();
            chunks.add(new ScriptChunk(Script.encodeToOpN(threshold), null));
            for(byte[] pubKey : pubKeyBytes) {
                chunks.add(new ScriptChunk(pubKey.length, pubKey));
            }
            chunks.add(new ScriptChunk(Script.encodeToOpN(pubKeys.size()), null));
            chunks.add(new ScriptChunk(ScriptOpCodes.OP_CHECKMULTISIG, null));
            return new Script(chunks);
        }

        @Override
        public String getOutputDescriptor(ECKey key) {
            throw new ProtocolException("No single key output descriptor for multisig script type");
        }

        @Override
        public String getOutputDescriptor(Script script) {
            if(!isScriptType(script)) {
                throw new IllegalArgumentException("Can only create output descriptor from multisig script");
            }

            int threshold = getThreshold(script);
            ECKey[] pubKeys = getPublicKeysFromScript(script);

            List<byte[]> pubKeyBytes = new ArrayList<>();
            for(ECKey key : pubKeys) {
                pubKeyBytes.add(key.getPubKey());
            }
            pubKeyBytes.sort(new Utils.LexicographicByteArrayComparator());

            StringJoiner joiner = new StringJoiner(",");
            for(byte[] pubKey : pubKeyBytes) {
                joiner.add(Utils.bytesToHex(pubKey));
            }

            return "multi(" + threshold + "," + joiner.toString() + ")";
        }

        @Override
        public boolean isScriptType(Script script) {
            List<ScriptChunk> chunks = script.chunks;
            if (chunks.size() < 4) return false;
            ScriptChunk chunk = chunks.get(chunks.size() - 1);
            // Must end in OP_CHECKMULTISIG[VERIFY].
            if (!chunk.isOpCode()) return false;
            if (!(chunk.equalsOpCode(OP_CHECKMULTISIG) || chunk.equalsOpCode(OP_CHECKMULTISIGVERIFY))) return false;
            try {
                // Second to last chunk must be an OP_N opcode and there should be that many data chunks (keys).
                ScriptChunk m = chunks.get(chunks.size() - 2);
                if (!m.isOpCode()) return false;
                int numKeys = Script.decodeFromOpN(m.opcode);
                if (numKeys < 1 || chunks.size() != 3 + numKeys) return false;
                for (int i = 1; i < chunks.size() - 2; i++) {
                    if (chunks.get(i).isOpCode()) return false;
                }
                // First chunk must be an OP_N opcode too.
                if (Script.decodeFromOpN(chunks.get(0).opcode) < 1) return false;
            } catch (IllegalStateException e) {
                return false;   // Not an OP_N opcode.
            }
            return true;
        }

        @Override
        public byte[] getHashFromScript(Script script) {
            throw new ProtocolException("Public keys for bare multisig script type must be retrieved with method getPublicKeysFromScript(Script script)");
        }

        @Override
        public ECKey[] getPublicKeysFromScript(Script script) {
            List<ECKey> pubKeys = new ArrayList<>();

            List<ScriptChunk> chunks = script.chunks;
            for (int i = 1; i < chunks.size() - 2; i++) {
                byte[] pubKey = chunks.get(i).data;
                pubKeys.add(ECKey.fromPublicOnly(pubKey));
            }

            return pubKeys.toArray(new ECKey[pubKeys.size()]);
        }

        @Override
        public int getThreshold(Script script) {
            return decodeFromOpN(script.chunks.get(0).opcode);
        }

        @Override
        public List<PolicyType> getAllowedPolicyTypes() {
            return List.of(MULTI);
        }
    },
    P2SH("P2SH", "m/45'/0'/0'") {
        @Override
        public Address getAddress(byte[] scriptHash) {
            return new P2SHAddress(scriptHash);
        }

        @Override
        public Address getAddress(ECKey key) {
            throw new ProtocolException("No single key address for script hash type");
        }

        @Override
        public Address getAddress(Script script) {
            return getAddress(Utils.sha256hash160(script.getProgram()));
        }

        @Override
        public Script getOutputScript(byte[] scriptHash) {
            List<ScriptChunk> chunks = new ArrayList<>();
            chunks.add(new ScriptChunk(ScriptOpCodes.OP_HASH160, null));
            chunks.add(new ScriptChunk(scriptHash.length, scriptHash));
            chunks.add(new ScriptChunk(ScriptOpCodes.OP_EQUAL, null));

            return new Script(chunks);
        }

        @Override
        public Script getOutputScript(ECKey key) {
            throw new ProtocolException("No single key output script for script hash type");
        }

        @Override
        public Script getOutputScript(Script script) {
            return getOutputScript(Utils.sha256hash160(script.getProgram()));
        }

        @Override
        public String getOutputDescriptor(ECKey key) {
            throw new ProtocolException("No single key output descriptor for script hash type");
        }

        @Override
        public String getOutputDescriptor(Script script) {
            if(!MULTISIG.isScriptType(script)) {
                throw new IllegalArgumentException("Can only create output descriptor from multisig script");
            }

            return "sh(" + MULTISIG.getOutputDescriptor(script) + ")";
        }

        @Override
        public boolean isScriptType(Script script) {
            List<ScriptChunk> chunks = script.chunks;
            // We check for the effective serialized form because BIP16 defines a P2SH output using an exact byte
            // template, not the logical program structure. Thus you can have two programs that look identical when
            // printed out but one is a P2SH script and the other isn't! :(
            // We explicitly test that the op code used to load the 20 bytes is 0x14 and not something logically
            // equivalent like {@code OP_HASH160 OP_PUSHDATA1 0x14 <20 bytes of script hash> OP_EQUAL}
            if (chunks.size() != 3)
                return false;
            if (!chunks.get(0).equalsOpCode(OP_HASH160))
                return false;
            ScriptChunk chunk1 = chunks.get(1);
            if (chunk1.opcode != 0x14)
                return false;
            byte[] chunk1data = chunk1.data;
            if (chunk1data == null)
                return false;
            if (chunk1data.length != 20)
                return false;
            if (!chunks.get(2).equalsOpCode(OP_EQUAL))
                return false;
            return true;
        }

        @Override
        public byte[] getHashFromScript(Script script) {
            return script.chunks.get(1).data;
        }

        @Override
        public List<PolicyType> getAllowedPolicyTypes() {
            return List.of(MULTI);
        }
    },
    P2SH_P2WPKH("P2SH-P2WPKH", "m/49'/0'/0'") {
        @Override
        public Address getAddress(byte[] scriptHash) {
            return P2SH.getAddress(scriptHash);
        }

        @Override
        public Address getAddress(ECKey key) {
            Script p2wpkhScript = P2WPKH.getOutputScript(key.getPubKeyHash());
            return P2SH.getAddress(p2wpkhScript);
        }

        @Override
        public Address getAddress(Script script) {
            if(P2WPKH.isScriptType(script)) {
                return P2SH.getAddress(script);
            }

            throw new ProtocolException("Provided script is not a P2WPKH script");
        }

        @Override
        public Script getOutputScript(byte[] scriptHash) {
            return P2SH.getOutputScript(scriptHash);
        }

        @Override
        public Script getOutputScript(ECKey key) {
            Script p2wpkhScript = P2WPKH.getOutputScript(key.getPubKeyHash());
            return P2SH.getOutputScript(p2wpkhScript);
        }

        @Override
        public Script getOutputScript(Script script) {
            if(P2WPKH.isScriptType(script)) {
                return P2SH.getOutputScript(script);
            }

            throw new ProtocolException("Provided script is not a P2WPKH script");
        }

        @Override
        public String getOutputDescriptor(ECKey key) {
            return "sh(wpkh(" + Utils.bytesToHex(key.getPubKey()) + "))";
        }

        @Override
        public String getOutputDescriptor(Script script) {
            throw new ProtocolException("No script derived output descriptor for non pay to script type");
        }

        @Override
        public boolean isScriptType(Script script) {
            return P2SH.isScriptType(script);
        }

        @Override
        public byte[] getHashFromScript(Script script) {
            return P2SH.getHashFromScript(script);
        }

        @Override
        public List<PolicyType> getAllowedPolicyTypes() {
            return List.of(SINGLE);
        }
    },
    P2SH_P2WSH("P2SH-P2WSH", "m/48'/0'/0'/1'") {
        @Override
        public Address getAddress(byte[] scriptHash) {
            return P2SH.getAddress(scriptHash);
        }

        @Override
        public Address getAddress(ECKey key) {
            throw new ProtocolException("No single key address for wrapped witness script hash type");
        }

        @Override
        public Address getAddress(Script script) {
            Script p2wshScript = P2WSH.getOutputScript(script);
            return P2SH.getAddress(p2wshScript);
        }

        @Override
        public Script getOutputScript(byte[] scriptHash) {
            return P2SH.getOutputScript(scriptHash);
        }

        @Override
        public Script getOutputScript(ECKey key) {
            throw new ProtocolException("No single key output script for wrapped witness script hash type");
        }

        @Override
        public Script getOutputScript(Script script) {
            Script p2wshScript = P2WSH.getOutputScript(script);
            return P2SH.getOutputScript(p2wshScript);
        }

        @Override
        public String getOutputDescriptor(ECKey key) {
            throw new ProtocolException("No single key output descriptor for script hash type");
        }

        @Override
        public String getOutputDescriptor(Script script) {
            if(!MULTISIG.isScriptType(script)) {
                throw new IllegalArgumentException("Can only create output descriptor from multisig script");
            }

            return "sh(wsh(" + MULTISIG.getOutputDescriptor(script) + "))";
        }

        @Override
        public boolean isScriptType(Script script) {
            return P2SH.isScriptType(script);
        }

        @Override
        public byte[] getHashFromScript(Script script) {
            return P2SH.getHashFromScript(script);
        }

        @Override
        public List<PolicyType> getAllowedPolicyTypes() {
            return List.of(MULTI, CUSTOM);
        }
    },
    P2WPKH("P2WPKH", "m/84'/0'/0'") {
        @Override
        public Address getAddress(byte[] pubKeyHash) {
            return new P2WPKHAddress(pubKeyHash);
        }

        @Override
        public Address getAddress(ECKey key) {
            return getAddress(key.getPubKeyHash());
        }

        @Override
        public Address getAddress(Script script) {
            throw new ProtocolException("No script derived address for non pay to script type");
        }

        @Override
        public Script getOutputScript(byte[] pubKeyHash) {
            List<ScriptChunk> chunks = new ArrayList<>();
            chunks.add(new ScriptChunk(OP_0, null));
            chunks.add(new ScriptChunk(pubKeyHash.length, pubKeyHash));

            return new Script(chunks);
        }

        @Override
        public Script getOutputScript(ECKey key) {
            return getOutputScript(key.getPubKeyHash());
        }

        @Override
        public Script getOutputScript(Script script) {
            throw new ProtocolException("No script derived output script for non pay to script type");
        }

        @Override
        public String getOutputDescriptor(ECKey key) {
            return "wpkh(" + Utils.bytesToHex(key.getPubKey()) + ")";
        }

        @Override
        public String getOutputDescriptor(Script script) {
            throw new ProtocolException("No script derived output descriptor for non pay to script type");
        }

        @Override
        public boolean isScriptType(Script script) {
            List<ScriptChunk> chunks = script.chunks;
            if (chunks.size() != 2)
                return false;
            if (!chunks.get(0).equalsOpCode(OP_0))
                return false;
            byte[] chunk1data = chunks.get(1).data;
            if (chunk1data == null)
                return false;
            if (chunk1data.length != 20)
                return false;
            return true;
        }

        @Override
        public byte[] getHashFromScript(Script script) {
            return script.chunks.get(1).data;
        }

        @Override
        public List<PolicyType> getAllowedPolicyTypes() {
            return List.of(SINGLE);
        }
    },
    P2WSH("P2WSH", "m/48'/0'/0'/2'") {
        @Override
        public Address getAddress(byte[] scriptHash) {
            return new P2WSHAddress(scriptHash);
        }

        @Override
        public Address getAddress(ECKey key) {
            throw new ProtocolException("No single key address for witness script hash type");
        }

        @Override
        public Address getAddress(Script script) {
            return getAddress(Sha256Hash.hash(script.getProgram()));
        }

        @Override
        public Script getOutputScript(byte[] scriptHash) {
            List<ScriptChunk> chunks = new ArrayList<>();
            chunks.add(new ScriptChunk(OP_0, null));
            chunks.add(new ScriptChunk(scriptHash.length, scriptHash));

            return new Script(chunks);
        }

        @Override
        public Script getOutputScript(ECKey key) {
            throw new ProtocolException("No single key output script for witness script hash type");
        }

        @Override
        public Script getOutputScript(Script script) {
            return getOutputScript(Sha256Hash.hash(script.getProgram()));
        }

        @Override
        public String getOutputDescriptor(ECKey key) {
            throw new ProtocolException("No single key output descriptor for script hash type");
        }

        @Override
        public String getOutputDescriptor(Script script) {
            if(!MULTISIG.isScriptType(script)) {
                throw new IllegalArgumentException("Can only create output descriptor from multisig script");
            }

            return "wsh(" + MULTISIG.getOutputDescriptor(script) + ")";
        }

        @Override
        public boolean isScriptType(Script script) {
            List<ScriptChunk> chunks = script.chunks;
            if (chunks.size() != 2)
                return false;
            if (!chunks.get(0).equalsOpCode(OP_0))
                return false;
            byte[] chunk1data = chunks.get(1).data;
            if (chunk1data == null)
                return false;
            if (chunk1data.length != 32)
                return false;
            return true;
        }

        @Override
        public byte[] getHashFromScript(Script script) {
            return script.chunks.get(1).data;
        }

        @Override
        public List<PolicyType> getAllowedPolicyTypes() {
            return List.of(MULTI, CUSTOM);
        }
    };

    private final String name;
    private final String defaultDerivationPath;

    ScriptType(String name, String defaultDerivationPath) {
        this.name = name;
        this.defaultDerivationPath = defaultDerivationPath;
    }

    public String getName() {
        return name;
    }

    public String getDefaultDerivationPath() {
        return defaultDerivationPath;
    }

    public List<ChildNumber> getDefaultDerivation() {
        return KeyDerivation.parsePath(defaultDerivationPath);
    }

    public List<ChildNumber> getDefaultDerivation(int account) {
        List<ChildNumber> copy = new ArrayList<>(KeyDerivation.parsePath(defaultDerivationPath));
        ChildNumber accountChildNumber = new ChildNumber(account, true);
        copy.set(2, accountChildNumber);
        return Collections.unmodifiableList(copy);
    }

    public int getAccount(String derivationPath) {
        if(KeyDerivation.isValid(derivationPath)) {
            List<ChildNumber> derivation = new ArrayList<>(KeyDerivation.parsePath(derivationPath));
            if(derivation.size() > 2) {
                int account = derivation.get(2).num();
                List<ChildNumber> defaultDerivation = getDefaultDerivation(account);
                if(defaultDerivation.equals(derivation)) {
                    return account;
                }
            }
        }

        return -1;
    }

    public abstract List<PolicyType> getAllowedPolicyTypes();

    public boolean isAllowed(PolicyType policyType) {
        return getAllowedPolicyTypes().contains(policyType);
    }

    public abstract Address getAddress(byte[] bytes);

    public abstract Address getAddress(ECKey key);

    public abstract Address getAddress(Script script);

    public abstract Script getOutputScript(byte[] bytes);

    public abstract Script getOutputScript(ECKey key);

    public abstract Script getOutputScript(Script script);

    public Script getOutputScript(int threshold, List<ECKey> pubKeys) {
        throw new UnsupportedOperationException("Only defined for MULTISIG script type");
    }

    public abstract String getOutputDescriptor(ECKey key);

    public abstract String getOutputDescriptor(Script script);

    public abstract boolean isScriptType(Script script);

    public abstract byte[] getHashFromScript(Script script);

    public Address[] getAddresses(Script script) {
        return new Address[] { getAddress(getHashFromScript(script)) };
    }

    public ECKey getPublicKeyFromScript(Script script) {
        throw new ProtocolException("Script type " + this + " does not contain a public key");
    }

    public ECKey[] getPublicKeysFromScript(Script script) {
        throw new ProtocolException("Script type " + this + " does not contain public keys");
    }

    public int getThreshold(Script script) {
        throw new ProtocolException("Script type " + this + " is not a multisig script");
    }

    public static final ScriptType[] SINGLE_HASH_TYPES = {P2PKH, P2SH, P2SH_P2WPKH, P2SH_P2WSH, P2WPKH, P2WSH};

    public static List<ScriptType> getScriptTypesForPolicyType(PolicyType policyType) {
        return Arrays.stream(values()).filter(scriptType -> scriptType.isAllowed(policyType)).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return name;
    }
}