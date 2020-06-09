package com.klaytn.caver.transaction;

import com.klaytn.caver.Klay;
import com.klaytn.caver.account.AccountKeyRoleBased;
import com.klaytn.caver.crypto.KlaySignatureData;
import com.klaytn.caver.transaction.type.LegacyTransaction;
import com.klaytn.caver.transaction.type.TransactionType;
import com.klaytn.caver.utils.Utils;
import com.klaytn.caver.wallet.keyring.Keyring;
import org.web3j.crypto.Hash;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

abstract public class AbstractTransaction {

    /**
     * Klay RPC instance
     */
    private Klay klaytnCall = null;

    /**
     * Transaction's type string
     */
    private String type;

    /**
     * The address of the sender.
     */
    private String from;

    /**
     * A value used to uniquely identify a sender’s transaction.
     * If two transactions with the same nonce are generated by a sender, only one is executed.
     */
    private String nonce = "";

    /**
     * The maximum amount of gas the transaction is allowed to use.
     */
    private String gas;

    /**
     * A unit price of gas in peb the sender will pay for a transaction fee.
     */
    private String gasPrice = "";

    /**
     * Network ID
     */
    private String chainId = "";

    /**
     * A Signature list
     */
    private List<KlaySignatureData> signatures = new ArrayList<>();

    /**
     * Represents a AbstractTransaction class builder.
     * @param <B> An generic extends to AbstractTransaction.Builder
     */
    public static class Builder<B extends AbstractTransaction.Builder> {
        private String type;
        private String gas;

        private String from;
        private String nonce = "";
        private String gasPrice = "";
        private String chainId = "";
        private Klay klaytnCall = null;
        private List<KlaySignatureData> signList = new ArrayList<>();

        public Builder(String type) {
            this.type = type;
        }

        public B setFrom(String from) {
            this.from = from;
            return (B) this;
        }

        public B setNonce(String nonce) {
            this.nonce = nonce;
            return (B) this;
        }

        public B setNonce(BigInteger nonce) {
            setNonce(Numeric.toHexStringWithPrefix(nonce));
            return (B) this;
        }

        public B setGas(String gas) {
            this.gas = gas;
            return (B) this;
        }

        public B setGas(BigInteger gas) {
            setGas(Numeric.toHexStringWithPrefix(gas));
            return (B) this;
        }

        public B setGasPrice(String gasPrice) {
            this.gasPrice = gasPrice;
            return (B) this;
        }

        public B setGasPrice(BigInteger gasPrice) {
            setGasPrice(Numeric.toHexStringWithPrefix(gasPrice));
            return (B) this;
        }

        public B setChainId(String chainId) {
            this.chainId = chainId;
            return (B) this;
        }

        public B setChainId(BigInteger chainId) {
            setChainId(Numeric.toHexStringWithPrefix(chainId));
            return (B) this;
        }

        public B setKlaytnCall(Klay klaytnCall) {
            this.klaytnCall = klaytnCall;
            return (B) this;
        }

        public B setSignList(List<KlaySignatureData> signList) {
            this.signList.addAll(signList);
            return (B) this;
        }

        public B setSignList(KlaySignatureData sign) {
            this.signList.add(sign);
            return (B) this;
        }
    }

    /**
     * Create AbstractTransaction instance
     * @param builder AbstractTransaction.builder
     */
    public AbstractTransaction(AbstractTransaction.Builder builder) {
        this(builder.klaytnCall,
                builder.type,
                builder.from,
                builder.nonce,
                builder.gas,
                builder.gasPrice,
                builder.chainId,
                builder.signList
        );
    }

    /**
     * Create AbstractTransaction instance
     * @param klaytnCall Klay RPC instance
     * @param type Transaction's type string
     * @param from The address of the sender.
     * @param nonce A value used to uniquely identify a sender’s transaction.
     * @param gas The maximum amount of gas the transaction is allowed to use.
     * @param gasPrice A unit price of gas in peb the sender will pay for a transaction fee.
     * @param chainId Network ID
     * @param signatures A Signature list
     */
    public AbstractTransaction(Klay klaytnCall, String type, String from, String nonce, String gas, String gasPrice, String chainId, List<KlaySignatureData> signatures) {
        setKlaytnCall(klaytnCall);
        setType(type);
        setFrom(from);
        setNonce(nonce);
        setGasPrice(gasPrice);
        setGas(gas);
        setChainId(chainId);

        if(signatures != null) {
            this.signatures.addAll(signatures);
        }
    }

    /**
     * Returns the RLP-encoded string of this transaction (i.e., rawTransaction).
     * @return String
     */
    public abstract String getRLPEncoding();

    /**
     * Returns the RLP-encoded string to make the signature of this transaction.
     * @return String
     */
    public abstract String getCommonRLPEncodingForSignature();

    /**
     * Signs to the transaction with a single private key.
     * It sets index and Hasher default value.
     *   - index : 0
     *   - signer : TransactionHasher.getHashForSignature()
     * @param keyString The private key string.
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction signWithKey(String keyString) throws IOException {
        Keyring keyring = Keyring.createFromPrivateKey(keyString);
        return this.signWithKey(keyring, 0, TransactionHasher::getHashForSignature);
    }

    /**
     * Signs to the transaction with a single private key.
     * It sets signer to TransactionHasher.getHashForSignature()
     * @param keyString The private key string.
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction signWithKey(String keyString, int index) throws IOException {
        Keyring keyring = Keyring.createFromPrivateKey(keyString);
        return this.signWithKey(keyring, index, TransactionHasher::getHashForSignature);
    }


    /**
     * Signs to the transaction with a single private key.
     * It sets index 0.
     * @param keyString The private key string
     * @param signer The function to get hash of transaction.
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction signWithKey(String keyString, Function<AbstractTransaction, String> signer) throws IOException {
        Keyring keyring = Keyring.createFromPrivateKey(keyString);
        return this.signWithKey(keyring, 0, signer);
    }

    /**
     * Signs to the transaction with a single private key.
     * @param keyString The private key string
     * @param index The index of private key to use in Keyring instance.
     * @param signer The function to get hash of transaction.
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction signWithKey(String keyString, int index, Function<AbstractTransaction, String> signer) throws IOException {
        Keyring keyring = Keyring.createFromPrivateKey(keyString);
        return this.signWithKey(keyring, index, signer);
    }

    /**
     * Signs to the transaction with a single private key in the Keyring instance.
     * It sets index and Hasher default value.
     *   - index : 0
     *   - signer : TransactionHasher.getHashForSignature()
     * @param keyring The Keyring instance.
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction signWithKey(Keyring keyring) throws IOException  {
        return this.signWithKey(keyring, 0, TransactionHasher::getHashForSignature);
    }

    /**
     * Signs to the transaction with a single private key in the Keyring instance.
     * It sets index 0.
     * @param keyring The Keyring instance.
     * @param signer The function to get hash of transaction.
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction signWithKey(Keyring keyring, Function<AbstractTransaction, String> signer) throws IOException  {
        return this.signWithKey(keyring, 0, signer);
    }

    /**
     * Signs to the transaction with a single private key in the Keyring instance.
     * It sets signer to TransactionHasher.getHashForSignature()
     * @param keyring The Keyring instance.
     * @param index The index of private key to use in Keyring instance.
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction signWithKey(Keyring keyring, int index) throws IOException {
        return this.signWithKey(keyring, index, TransactionHasher::getHashForSignature);
    }

    /**
     * Signs to the transaction with a single private key in the Keyring instance.
     * @param keyring The Keyring instance.
     * @param index The index of private key to use in Keyring instance.
     * @param signer The function to get hash of transaction.
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction signWithKey(Keyring keyring, int index, Function<AbstractTransaction, String> signer) throws IOException {
        if(this.getType().equals(TransactionType.TxTypeLegacyTransaction.toString()) && keyring.isDecoupled()) {
            throw new IllegalArgumentException("A legacy transaction cannot be signed with a decoupled keyring.");
        }

        if(this.from.equals("0x")){
            this.from = keyring.getAddress();
        }

        if(!this.from.toLowerCase().equals(keyring.getAddress().toLowerCase())) {
            throw new IllegalArgumentException("The from address of the transaction is different with the address of the keyring to use");
        }

        this.fillTransaction();
        int role = this.type.startsWith("AccountUpdate") ? AccountKeyRoleBased.RoleGroup.ACCOUNT_UPDATE.getIndex() : AccountKeyRoleBased.RoleGroup.TRANSACTION.getIndex();

        String hash = signer.apply(this);
        KlaySignatureData sig = keyring.signWithKey(hash, Numeric.toBigInt(this.chainId).intValue(), role, index);

        this.appendSignatures(sig);

        return this;
    }

    /**
     * Signs to the transaction using all private keys in Keyring instance.
     * It sets signer to TransactionHasher.getHashForSignature()
     * @param keyString The private key string
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction signWithKeys(String keyString) throws IOException {
        Keyring keyring = Keyring.createFromPrivateKey(keyString);

        return this.signWithKeys(keyring, TransactionHasher::getHashForSignature);
    }


    /**
     * Signs to the transaction using all private keys in Keyring instance.
     * @param keyString The private key string
     * @param signer The function to get hash of transaction.
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction signWithKeys(String keyString, Function<AbstractTransaction, String> signer) throws IOException {
        Keyring keyring = Keyring.createFromPrivateKey(keyString);

        return this.signWithKeys(keyring, signer);
    }

    /**
     * Signs to the transaction using all private keys in Keyring instance.
     * It sets singer to TransactionHasher.getHashForSignature().
     * @param keyring The Keyring instance
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction signWithKeys(Keyring keyring) throws IOException {
        return this.signWithKeys(keyring, TransactionHasher::getHashForSignature);
    }

    /**
     * Signs to the transaction using all private keys in Keyring
     * @param keyring The Keyring instance.
     * @param signer The function to get hash of transaction.
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction signWithKeys(Keyring keyring, Function<AbstractTransaction, String> signer) throws IOException {
        if(this.getType().equals(TransactionType.TxTypeLegacyTransaction.toString()) && keyring.isDecoupled()) {
            throw new IllegalArgumentException("A legacy transaction cannot be signed with a decoupled keyring.");
        }

        if(this.from.equals("0x")){
            this.from = keyring.getAddress();
        }

        if(!this.from.toLowerCase().equals(keyring.getAddress().toLowerCase())) {
            throw new IllegalArgumentException("The from address of the transaction is different with the address of the keyring to use");
        }

        this.fillTransaction();
        int role = this.type.startsWith("AccountUpdate") ? AccountKeyRoleBased.RoleGroup.ACCOUNT_UPDATE.getIndex() : AccountKeyRoleBased.RoleGroup.TRANSACTION.getIndex();

        String hash = signer.apply(this);
        List<KlaySignatureData> sigList = keyring.signWithKeys(hash, Numeric.toBigInt(this.chainId).intValue(), role);

        this.appendSignatures(sigList);

        return this;
    }

    /**
     * Appends signatures to the transaction.
     * @param signatureData KlaySignatureData instance contains ECDSA signature data
     */
    public void appendSignatures(KlaySignatureData signatureData) {
        List<KlaySignatureData> signList = new ArrayList<>();
        signList.add(signatureData);
        appendSignatures(signList);
    }

    /**
     * Appends signatures to the transaction.
     * @param signatureData List of KlaySignatureData contains ECDSA signature data
     */
    public void appendSignatures(List<KlaySignatureData> signatureData) {
        this.signatures.addAll(signatureData);
        this.signatures = refineSignature(this.getSignatures());
    }

    /**
     * Combines signatures to the transaction from RLP-encoded transaction strings and returns a single transaction with all signatures combined.
     * When combining the signatures into a transaction instance,
     * an error is thrown if the decoded transaction contains different value except signatures.
     * @param rlpEncoded An array of RLP-encoded transaction strings.
     * @return String
     */
    public String combineSignatures(List<String> rlpEncoded) {
        boolean fillVariable = false;

        // If the signatures are empty, there may be an undefined member variable.
        // In this case, the empty information is filled with the decoded result.
        boolean isContainsEmptySig = this.getSignatures().stream().anyMatch(Utils::isEmptySig);
        if(this.getSignatures().size() == 0 || isContainsEmptySig) fillVariable = true;

        for(String encodedStr : rlpEncoded) {
            AbstractTransaction txObj = TransactionDecoder.decode(encodedStr);

            if(fillVariable) {
                if(this.getNonce().equals("")) this.setNonce(txObj.getNonce());
                if(this.getGasPrice().equals("")) this.setGasPrice(txObj.getGasPrice());
                fillVariable = false;
            }

            // Signatures can only be combined for the same transaction.
            // Therefore, compare whether the decoded transaction is the same as this.
            if(!this.compareTxField(txObj, false)) {
                throw new RuntimeException("Transactions containing different information cannot be combined.");
            }

            this.appendSignatures(txObj.getSignatures());
        }

        return this.getRLPEncoding();
    }

    /**
     * Returns a RawTransaction(RLP-encoded transaction string)
     * @return String
     */
    public String getRawTransaction() {
        return this.getRLPEncoding();
    }

    /**
     * Returns a hash string of transaction
     * @return String
     */
    public String getTransactionHash() {
        return Hash.sha3(this.getRLPEncoding());
    }

    /**
     * Returns a senderTxHash of transaction
     * @return String
     */
    public String getSenderTxHash() {
        return this.getTransactionHash();
    }

    /**
     * Returns an RLP-encoded transaction string for making signature.
     * @return String
     */
    public String getRLPEncodingForSignature() {
        validateOptionalValues(true);

        byte[] txRLP = Numeric.hexStringToByteArray(getCommonRLPEncodingForSignature());

        List<RlpType> rlpTypeList = new ArrayList<>();
        rlpTypeList.add(RlpString.create(txRLP));
        rlpTypeList.add(RlpString.create(Numeric.toBigInt(this.getChainId())));
        rlpTypeList.add(RlpString.create(0));
        rlpTypeList.add(RlpString.create(0));
        byte[] encoded = RlpEncoder.encode(new RlpList(rlpTypeList));
        return Numeric.toHexString(encoded);
    }

    /**
     * Fills empty optional transaction field.(nonce, gasPrice, chainId)
     * @throws IOException
     */
    public void fillTransaction() throws IOException{
        if(klaytnCall != null) {
            this.nonce = klaytnCall.getTransactionCount(this.from, DefaultBlockParameterName.PENDING).send().getResult();
            this.chainId = klaytnCall.getChainID().send().getResult();
            this.gasPrice = klaytnCall.getGasPrice().send().getResult();
        }

        if(this.nonce.isEmpty() || this.chainId.isEmpty() || this.gasPrice.isEmpty()) {
            throw new RuntimeException("Cannot fill transaction data.(nonce, chainId, gasPrice");
        }
    }

    /**
     * Check equals txObj passed parameter and Current instance.
     * @param txObj The AbstractTransaction Object to compare
     * @param checkSig Check whether signatures field is equal.
     * @return boolean
     */
    public boolean compareTxField(AbstractTransaction txObj, boolean checkSig) {
        if(!this.getType().equals(txObj.getType())) return false;
        if(!this.getFrom().toLowerCase().equals(txObj.getFrom().toLowerCase())) return false;
        if(!Numeric.toBigInt(this.getNonce()).equals(Numeric.toBigInt(txObj.getNonce()))) return false;
        if(!Numeric.toBigInt(this.getGas()).equals(Numeric.toBigInt(txObj.getGas()))) return false;
        if(!Numeric.toBigInt(this.getGasPrice()).equals(Numeric.toBigInt(txObj.getGasPrice()))) return false;

        if(checkSig) {
            List<KlaySignatureData> dataList = this.getSignatures();
            if(dataList.size() != txObj.getSignatures().size()) return false;

            for(int i=0; i< dataList.size(); i++) {
                if(!dataList.get(i).equals(txObj.getSignatures().get(i))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks that member variables that can be defined by the user are defined.
     * If there is an undefined variable, an error occurs.
     */
    public void validateOptionalValues(boolean checkChainID) {
        if(this.getNonce() == null || this.getNonce().isEmpty() || this.getNonce().equals("0x")) {
            throw new RuntimeException("nonce is undefined. Define nonce in transaction or use 'transaction.fillTransaction' to fill values.");
        }

        if(this.getGasPrice() == null || this.getGasPrice().isEmpty() || this.getGasPrice().equals("0x")) {
            throw new RuntimeException("gasPrice is undefined. Define gasPrice in transaction or use 'transaction.fillTransaction' to fill values.");
        }

        if(checkChainID) {
            if(this.getChainId() == null || this.getChainId().isEmpty() || this.getChainId().equals("0x")) {
                throw new RuntimeException("chainId is undefined. Define chainId in transaction or use 'transaction.fillTransaction' to fill values.");
            }
        }
    }

    /**
     * Refines the array containing signatures
     *   - Removes duplicate signatures
     *   - Removes the default empty signature("0x01", "0x", "0x")
     *   - For an empty signature array, return an array containing the default empty signature("0x01", "0x", "0x")
     * @param signatureDataList
     * @return
     */
    public List<KlaySignatureData> refineSignature(List<KlaySignatureData> signatureDataList) {
        boolean isLegacy = this.getType().equals(TransactionType.TxTypeLegacyTransaction.toString());
        KlaySignatureData emptySig = KlaySignatureData.getEmptySignature();

        List<KlaySignatureData> refinedList = new ArrayList<>();

        for(KlaySignatureData signData : signatureDataList) {
            if(!Utils.isEmptySig(signData)) {
                if(!refinedList.contains(signData)) {
                    refinedList.add(signData);
                }
            }
        }

        if(refinedList.size() == 0) {
            refinedList.add(emptySig);
        }

        if(isLegacy && refinedList.size() > 1) {
            throw new RuntimeException("LegacyTransaction cannot have multiple signature.");
        }

        return refinedList;
    }

    /**
     * Getter function for klaytnRPC
     * @return Klay
     */
    public Klay getKlaytnCall() {
        return klaytnCall;
    }

    /**
     * Setter function for klaytnRPC
     * @param klaytnCall Klay RPC Instance.
     */
    public void setKlaytnCall(Klay klaytnCall) {
        this.klaytnCall = klaytnCall;
    }

    /**
     * Getter function for type.
     * @return String
     */
    public String getType() {
        return type;
    }

    /**
     * Getter function for from
     * @return String
     */
    public String getFrom() {
        return from;
    }

    /**
     * Getter function for nonce
     * @return String
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * Getter function for gas
     * @return String
     */
    public String getGas() {
        return gas;
    }

    /**
     * Getter function for gas price
     * @return String
     */
    public String getGasPrice() {
        return gasPrice;
    }

    /**
     * Getter function for chain id
     * @return String
     */
    public String getChainId() {
        return chainId;
    }

    /**
     * Getter function for signatures
     * @return String
     */
    public List<KlaySignatureData> getSignatures() {
        return signatures;
    }

    private void setType(String type) {
        this.type = type;
    }

    private void setFrom(String from) {
        //"From" field in LegacyTransaction allows null
        if(this instanceof LegacyTransaction) {
            if(from == null || from.isEmpty() || from.equals("0x")) from = "0x";
        } else {
            if(from == null) {
                throw new IllegalArgumentException("from is missing.");
            }

            if(!Utils.isAddress(from)) {
                throw new IllegalArgumentException("Invalid address.");
            }
        }

        this.from = from;
    }

    private void setGas(String gas) {
        //Gas value must be set.
        if(gas == null || gas.isEmpty() || gas.equals("0x")) {
            throw new IllegalArgumentException("gas is missing.");
        }

        if(!Utils.isNumber(gas)) {
            throw new IllegalArgumentException("Invalid gas.");
        }
        this.gas = gas;
    }

    /**
     * Setter function for nonce.
     * @param nonce A value used to uniquely identify a sender’s transaction.
     */
    private void setNonce(String nonce) {
        this.nonce = nonce;
    }

    /**
     * Setter function for gas price.
     * @param gasPrice A unit price of gas in peb the sender will pay for a transaction fee.
     */
    private void setGasPrice(String gasPrice) {
        this.gasPrice = gasPrice;
    }

    /**
     * Setter function for chain id.
     * @param chainId A network id.
     */
    private void setChainId(String chainId) {
        this.chainId = chainId;
    }
}
