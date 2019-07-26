package com.acyumi.utils;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * 加解密工具类. <br>
 * 为保证加解密的正确性，此工具类输入和输出的String默认都是BASE64编码格式. <br>
 * 如果不想使用Base64编码格式（如: 十六进制编码格式），可使用以byte[]为输入输出的方法. <br><br>
 * 十六进制编码可使用工具类: <br>
 * {@link ParameterUtils#fromHexString(String)} <br>
 * {@link ParameterUtils#decodeFromBase64Str(String)} <br><br>
 * 非对称加密算法(如RSA、EC)加密数据比较慢，但解密挺快，而对称加密算法(如AES)加觖密数据都快 <br>
 * 所以一般使用对称加密算法来加密数据，使用非对称加密算法来对数据进行签名 <br>
 * 另外如果要传输AES密钥，可经过RSA/EC加密后再传输 <br><br>
 * <p>
 * 加解密流程举例：
 * <pre>
 * 加密：
 * 1、准备“数据d”
 * 2、生成随机的“AES密钥ak”
 * 3、用“AES密钥ak”对“数据d”进行加密，得到“密文m”
 * 4、根据“摘要规则dr”对“数据d”截取“数据摘要ds”
 * 5、生成或复用已存的自己的RSA密钥对“RSA私钥mpr”“RSA公钥mpu”
 * 6、定义“签名算法rs”
 * 7、使用“数据摘要ds”“RSA私钥mpr”“签名算法rs”进行签名得到“签名数据sd”
 * 8、获取“数据接收方R”的“RSA公钥opu”
 * 9、使用“RSA公钥opu”对“AES密钥ak”进行加密，得到“加密密钥mak”
 * 10、把“密文m”“签名数据sd”“摘要规则dr”“签名算法rs”“RSA公钥mpu”“加密密钥mak”发送给“数据接收方R”
 *
 * 解密：
 * 11、“数据接收方R”使用他自己的“RSA私钥opr”对“加密密钥mak”进行解密，得到“AES密钥ak”
 * 12、使用“AES密钥ak”对“密文m”解密，得到“数据d”
 * 13、根据“摘要规则dr”截取“数据摘要ds1”备用
 * 14、使用“数据摘要ds1”“RSA公钥mpu”“签名算法rs”“签名数据sd”进行校验，如果校验失败，说明内容被篡改了
 * 15、如果校验成功，说明“数据摘要ds1”与“数据摘要ds”完全相同，接收到的“密文m”没被篡改
 * </pre>
 * <p>
 * 加解密使用举例：
 * <pre>
 * // 加密：
 * // 1、准备“数据d”
 * String d = "这是测试明文|这是测试明文|这是测试明文";
 * System.out.println("明文数据d: " + d);
 *
 * // 2、生成随机的“AES密钥ak”
 * SecretKey aesKey = CryptoUtils.genAesSecretKey();
 * String ak = CryptoUtils.getBase64Key(aesKey);
 *
 * // 3、用“AES密钥ak”对“数据d”进行加密，得到“密文m”
 * byte[] dBytes = d.getBytes(StandardCharsets.UTF_8);
 * long start = System.currentTimeMillis();
 * String m = CryptoUtils.encryptToEncodedStr(dBytes, aesKey);
 * long end = System.currentTimeMillis();
 * System.out.println("AES加密数据d用时: " + (end - start));
 *
 * // 4、根据“摘要规则dr”对“数据d”截取“数据摘要ds”
 * //假设摘要规则为截取明文前10个字符
 * String ds = d.substring(0, 10);
 *
 * // 5、生成或复用已存的自己的RSA密钥对“RSA私钥mpr”“RSA公钥mpu”
 * KeyPair mKeyPair = CryptoUtils.genRsaKeyPair();
 * String mpr = CryptoUtils.getBase64Key(mKeyPair.getPrivate());
 * String mpu = CryptoUtils.getBase64Key(mKeyPair.getPublic());
 *
 * // 6、定义“签名算法rs”
 * String rs = "SHA256withRSA";
 *
 * // 7、使用“数据摘要ds”“RSA私钥mpr”“签名算法rs”进行签名得到“签名数据sd”
 * byte[] dsBytes = ds.getBytes(StandardCharsets.UTF_8);
 * //模拟取出字符串格式的私钥后将其解析成PrivateKey对象
 * PrivateKey privateKey = CryptoUtils.parsePrivateKey(mpr);
 * start = System.currentTimeMillis();
 * String sd = CryptoUtils.sign(dsBytes, privateKey, rs);
 * end = System.currentTimeMillis();
 * System.out.println("SHA256withRSA签名用时: " + (end - start));
 *
 * // 8、获取“数据接收方R”的“RSA公钥opu”
 * //模拟获取数据接收方的公钥，他的私钥当然不会给我们不知道
 * KeyPair oKeyPair = CryptoUtils.genRsaKeyPair();
 * String opr = CryptoUtils.getBase64Key(oKeyPair.getPrivate());
 * String opu = CryptoUtils.getBase64Key(oKeyPair.getPublic());
 *
 * // 9、使用“RSA公钥opu”对“AES密钥ak”进行加密，得到“加密密钥mak”
 * byte[] akBytes = ak.getBytes(StandardCharsets.UTF_8);
 * //模拟取出字符串格式的接收方公钥后将其解析成PublicKey对象
 * PublicKey oPublicKey = CryptoUtils.parsePublicKey(opu);
 * start = System.currentTimeMillis();
 * String mak = CryptoUtils.encryptToEncodedStr(akBytes, oPublicKey);
 * end = System.currentTimeMillis();
 * System.out.println("RSA加密AES密钥ak用时: " + (end - start));
 *
 * // 10、把“密文m”“签名数据sd”“摘要规则dr”“签名算法rs”“RSA公钥mpu”“加密密钥mak”发送给“数据接收方R”
 *
 * // 解密：
 * // 11、“数据接收方R”使用他自己的“RSA私钥opr”对“加密密钥mak”进行解密，得到“AES密钥ak1”
 * //模拟取出字符串格式的私钥后将其解析成PrivateKey对象
 * PrivateKey oPrivateKey = CryptoUtils.parsePrivateKey(opr);
 * start = System.currentTimeMillis();
 * byte[] decrypt = CryptoUtils.decryptFromEncodedStr(mak, oPrivateKey);
 * end = System.currentTimeMillis();
 * System.out.println("RSA解密AES密钥mak用时: " + (end - start));
 * String ak1 = new String(decrypt, StandardCharsets.UTF_8);
 * boolean akEquals = ak.equals(ak1);
 * System.out.println("AES密钥ak和ak1比对结果: " + akEquals);
 *
 * // 12、使用“AES密钥ak1”对“密文m”解密，得到“数据d1”
 * SecretKey secretKey = CryptoUtils.parseSecretKey(ak1);
 * start = System.currentTimeMillis();
 * byte[] mDecrypt = CryptoUtils.decryptFromEncodedStr(m, secretKey);
 * end = System.currentTimeMillis();
 * System.out.println("AES解密数据d1用时: " + (end - start));
 * String d1 = new String(mDecrypt, StandardCharsets.UTF_8);
 * System.out.println("明文数据d1: " + d1);
 * boolean dEquals = d.equals(d1);
 * System.out.println("数据d和d1比对结果: " + dEquals);
 *
 * // 13、根据“摘要规则dr”截取“数据摘要ds1”备用
 * String ds1 = d1.substring(0, 10);
 *
 * // 14、使用“数据摘要ds1”“RSA公钥mpu”“签名算法rs”“签名数据sd”进行校验，如果校验失败，说明内容被篡改了
 * // 15、如果校验成功，说明“数据摘要ds1”与“数据摘要ds”完全相同，接收到的“密文m”没被篡改
 * byte[] d1Bytes = ds1.getBytes(StandardCharsets.UTF_8);
 * //模拟取出字符串格式的公钥后将其解析成PublicKey对象
 * PublicKey mPublicKey = CryptoUtils.parsePublicKey(mpu);
 * start = System.currentTimeMillis();
 * boolean verify = CryptoUtils.verify(d1Bytes, sd, mPublicKey, rs);
 * end = System.currentTimeMillis();
 * System.out.println("SHA256withRSA校验签名数据sd用时: " + (end - start));
 * System.out.println("校验签名结果: " + verify);
 * </pre>
 *
 * @author Mr.XiHui
 * @date 2019/7/23
 */
public abstract class CryptoUtils {

    // ~ AES algorithm ----------------------------------------------------------------------------------------

    /*** AES加解密算法. */
    private static final String AES_ALGORITHM = "AES";

    /*** AES密钥长度. */
    private static final int AES_KEY_SIZE = 256;

    // ~ RSA algorithm ----------------------------------------------------------------------------------------

    /**
     * RSA加解密算法. <br>
     * 由Ron (R)ivest、Adi (S)hamir、Leonard (A)dleman三人于1977年在麻省理工学院工作时提出. <br>
     * 对极大整数做因数分解的难度决定了RSA算法的可靠性。 <br>
     * 换言之，对一极大整数做因数分解愈困难，RSA算法愈可靠。 <br>
     * 假如有人找到一种快速因数分解的算法的话，那么用RSA加密的信息的可靠性就肯定会极度下降。 <br>
     * 但找到这样的算法的可能性是非常小的。今天只有短的RSA钥匙才可能被强力方式解破。 <br>
     * 到目前为止，世界上还没有任何可靠的攻击RSA算法的方式。 <br>
     * 只要其钥匙的长度足够长，用RSA加密的信息实际上是不能被解破的。
     */
    private static final String RSA_ALGORITHM = "RSA";

    /*** RSA密钥长度(1024/2048/3072/4096). */
    private static final int RSA_KEY_SIZE = 2048;

    /**
     * RSA最大解密密文大小. <br>
     * 因为要保证大于{@link #RSA_MAX_ENCRYPT_BLOCK_SIZE}， <br>
     * 否则解密失败javax.crypto.BadPaddingException: Decryption error <br>
     * 所以这里先初始化以供下面运算使用
     */
    private static final int RSA_MAX_DECRYPT_BLOCK_SIZE = RSA_KEY_SIZE >> 3;

    /**
     * RSA最大加密明文大小. <br>
     * 如果RSA_KEY_SIZE是1024，因为padding占用了11个字节，计算后要减11， <br>
     * 否则javax.crypto.IllegalBlockSizeException: Data must not be longer than 117 bytes <br>
     * 如果RSA_KEY_SIZE是2048，可减可不减 <br>
     * 因为要保证小于{@link #RSA_MAX_DECRYPT_BLOCK_SIZE}， <br>
     * 所以这里使用{@link #RSA_MAX_DECRYPT_BLOCK_SIZE}作为被减数
     */
    private static final int RSA_MAX_ENCRYPT_BLOCK_SIZE = RSA_MAX_DECRYPT_BLOCK_SIZE - 11;

    /**
     * RSA签名算法. <br>
     * Verify操作频度高，而Sign操作频度低的应用场景。比如，分布式系统中基于capability的访问控制就是这样的一种场景。
     */
    private static final String RSA_SIGNATURE_ALGORITHM = "SHA" + RSA_MAX_DECRYPT_BLOCK_SIZE + "withRSA";

    // ~ EC algorithm -----------------------------------------------------------------------------------------

    /**
     * EC加解密算法. <br>
     * EC/ECC: Elliptic curve (cryptography) <br>
     * 椭圆加密算法（ECC）是一种公钥加密体制，最初由Koblitz和Miller两人于1985年提出， <br>
     * 其数学基础是利用椭圆曲线上的有理点构成Abel加法群上椭圆离散对数的计算困难性。 <br>
     * 公钥密码体制根据其所依据的难题一般分为三类： <br>
     * 大素数分解问题类、离散对数问题类、椭圆曲线类。 <br>
     * 有时也把椭圆曲线类归为离散对数类。 <br>
     * <p>
     * 相比于RSA的优势: <br>
     * 抗攻击性强、CPU占用少、内容使用少、网络消耗低、加密速度快 <br>
     * 随着安全等级的增加，RSA加密法的密钥长度也会成指数增加，而ECC密钥长度却只是成线性增加。
     */
    private static final String EC_ALGORITHM = "EC";

    /*** EC密钥长度(128/256/384). */
    private static final int EC_KEY_SIZE = 384;

    /**
     * ECDSA签名算法. <br>
     * Sign和Verify操作频度相当的应用场景。比如，点对点的安全信道建立。
     */
    private static final String EC_SIGNATURE_ALGORITHM = "SHA" + EC_KEY_SIZE + "withECDSA";

    // ~ public/private key prefix/suffix ---------------------------------------------------------------------

    private static final String LF_SYMBOL = "\n";

    private static final String RSA_X509_PUBLIC_KEY_PREFIX = "-----BEGIN PUBLIC KEY-----";
    private static final String RSA_X509_PUBLIC_KEY_SUFFIX = "-----END PUBLIC KEY-----";
    private static final String RSA_PKCS8_PRIVATE_KEY_PREFIX = "-----BEGIN PRIVATE KEY-----";
    private static final String RSA_PKCS8_PRIVATE_KEY_SUFFIX = "-----END PRIVATE KEY-----";
    private static final String RSA_PKCS1_PUBLIC_KEY_PREFIX = "-----BEGIN RSA PUBLIC KEY-----";
    private static final String RSA_PKCS1_PRIVATE_KEY_PREFIX = "-----BEGIN RSA PRIVATE KEY-----";

    private static final String EC_X509_PUBLIC_KEY_PREFIX = "-----BEGIN EC X.509 PUBLIC KEY-----";
    private static final String EC_X509_PUBLIC_KEY_SUFFIX = "-----END EC X.509 PUBLIC KEY-----";
    private static final String EC_PKCS8_PRIVATE_KEY_PREFIX = "-----BEGIN EC PKCS#8 PRIVATE KEY-----";
    private static final String EC_PKCS8_PRIVATE_KEY_SUFFIX = "-----END EC PKCS#8 PRIVATE KEY-----";

    // ~ public method ----------------------------------------------------------------------------------------

    /**
     * 生成AES密钥.
     *
     * @return SecretKey
     */
    public static SecretKey genAesSecretKey() {
        KeyGenerator keyGen;
        try {
            keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("无此算法: " + AES_ALGORITHM, e);
        }
        keyGen.init(AES_KEY_SIZE);
        return keyGen.generateKey();
    }

    /**
     * 生成RSA密钥对(公钥和私钥).
     *
     * @return KeyPair
     */
    public static KeyPair genRsaKeyPair() {
        return genKeyPair(RSA_ALGORITHM);
    }

    /**
     * 生成EC密钥对(公钥和私钥).
     *
     * @return KeyPair
     */
    public static KeyPair genEcKeyPair() {
        return genKeyPair(EC_ALGORITHM);
    }

    /**
     * 生成密钥对(公钥和私钥).
     *
     * @return KeyPair
     */
    public static KeyPair genKeyPair(String algorithm) {
        KeyPairGenerator keyPairGen;
        try {
            keyPairGen = KeyPairGenerator.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("无此算法: " + algorithm, e);
        }

        int keySize = RSA_ALGORITHM.equalsIgnoreCase(algorithm) ? RSA_KEY_SIZE :
                (EC_ALGORITHM.equalsIgnoreCase(algorithm) ? EC_KEY_SIZE : 256);

        keyPairGen.initialize(keySize);
        return keyPairGen.generateKeyPair();
    }

    /**
     * 根据PrivateKey对象获取(Base64编码的)私钥字符串.
     *
     * @param key 密钥对象
     * @return (BASE64编码的)私钥字符串
     * @see #genKeyPair(String)
     */
    public static String getBase64Key(Key key) {
        byte[] kEncoded = key.getEncoded();
        String str = ParameterUtils.encodeToBase64Str(kEncoded);
        if (key instanceof PublicKey) {
            str = split64CharsPerLine(str);
            if (EC_ALGORITHM.equalsIgnoreCase(key.getAlgorithm())) {
                str = EC_X509_PUBLIC_KEY_PREFIX + str;
                str += EC_X509_PUBLIC_KEY_SUFFIX;
            } else {
                str = RSA_X509_PUBLIC_KEY_PREFIX + str;
                str += RSA_X509_PUBLIC_KEY_SUFFIX;
            }
        } else if (key instanceof PrivateKey) {
            str = split64CharsPerLine(str);
            if (EC_ALGORITHM.equalsIgnoreCase(key.getAlgorithm())) {
                str = EC_PKCS8_PRIVATE_KEY_PREFIX + str;
                str += EC_PKCS8_PRIVATE_KEY_SUFFIX;
            } else {
                str = RSA_PKCS8_PRIVATE_KEY_PREFIX + str;
                str += RSA_PKCS8_PRIVATE_KEY_SUFFIX;
            }
        }
        return str;
    }

    /**
     * 根据(BASE64编码的)AES密钥字符串解析成SecretKey对象.
     *
     * @param base64SecretKey (Base64编码的)密钥字符串
     * @return SecretKey
     */
    public static SecretKey parseSecretKey(String base64SecretKey) {
        byte[] bytes = ParameterUtils.decodeFromBase64Str(base64SecretKey);
        return new SecretKeySpec(bytes, AES_ALGORITHM);
    }

    /**
     * 根据(BASE64编码的)私钥字符串解析成PrivateKey对象.
     *
     * @param base64PrivateKey (BASE64编码的)私钥字符串
     * @return PrivateKey对象
     */
    public static PrivateKey parsePrivateKey(String base64PrivateKey) {
        //如果是PKCS#1私钥
        if (base64PrivateKey.contains(RSA_PKCS1_PRIVATE_KEY_PREFIX)) {
            throw new IllegalArgumentException("不支持PKCS#1格式的私钥，请通过其他途径或自行找java依赖转成PKCS#8格式的私钥再使用");
        }

        String[] ka = trimBase64KeyAndGetAlgorithm(base64PrivateKey);
        byte[] pkBytes = ParameterUtils.decodeFromBase64Str(ka[0]);

        //如果是pkcs8私钥
        //通过KeyPairGenerator生成的RSA和EC算法密钥对中，得到的PrivateKey都是其实现类PKCS8Key的子类
        //即 RSAPrivateKeyImpl.class 和 ECPrivateKeyImpl.class
        KeySpec keySpec = new PKCS8EncodedKeySpec(pkBytes);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance(ka[1]);
            return keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("无此算法: " + ka[1], e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("私钥字符串非法，将私钥字符串解析成PrivateKey对象失败", e);
        }
    }

    /**
     * 根据(BASE64编码的)公钥字符串解析成PublicKey对象.
     *
     * @param base64PublicKey (BASE64编码的)公钥字符串
     * @return PublicKey对象
     */
    public static PublicKey parsePublicKey(String base64PublicKey) {
        //如果是PKCS#1公钥
        if (base64PublicKey.contains(RSA_PKCS1_PUBLIC_KEY_PREFIX)) {
            throw new IllegalArgumentException("不支持PKCS#1格式的公钥，请通过其他途径或自行找java依赖转成PKCS#8格式的公钥再使用");
        }

        String[] ka = trimBase64KeyAndGetAlgorithm(base64PublicKey);
        byte[] pkBytes = ParameterUtils.decodeFromBase64Str(ka[0]);

        //通过KeyPairGenerator生成的RSA和EC算法密钥对中，得到的PublicKey都是其实现类X509Key的子类
        //即 RSAPublicKeyImpl.class 和 ECPublicKeyImpl.class
        KeySpec keySpec = new X509EncodedKeySpec(pkBytes);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance(ka[1]);
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("无此算法: " + ka[1], e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("公钥字符串非法，将公钥字符串解析成PublicKey对象失败", e);
        }
    }

    /**
     * 加密. <br>
     * 若需要用于网络传输，请将加密字节数组进行Base64Encode操作，或者转成十六进制字符串再传输， <br>
     * 否则如果直接new String(byte[])将极可能解密失败，因为加密字节数组里包含不可见字符， <br>
     * 使用utf-8/ASCII/GBK直接转成字符串都有问题
     *
     * @param plainTextBytes 明文字节数组
     * @param key            密钥对象
     * @return 加密字节数组
     * @see ParameterUtils#toHexString(byte[])
     * @see ParameterUtils#encodeToBase64Str(byte[])
     * @see #encryptToEncodedStr(byte[], Key)
     */
    public static byte[] encrypt(byte[] plainTextBytes, Key key) {
        return doCrypto(plainTextBytes, key, Cipher.ENCRYPT_MODE);
    }

    /**
     * 加密并编码成Base64字符串.
     *
     * @param plainTextBytes 明文字节数组
     * @param key            密钥对象
     * @return Base64编码的加密字符串
     */
    public static String encryptToEncodedStr(byte[] plainTextBytes, Key key) {
        byte[] bs = doCrypto(plainTextBytes, key, Cipher.ENCRYPT_MODE);
        return ParameterUtils.encodeToBase64Str(bs);
    }

    /**
     * 解密.
     *
     * @param encryptedBytes 已加密的字节数组
     * @param key            密钥对象
     * @return 明文字节数组
     * @see ParameterUtils#fromHexString(String)
     * @see ParameterUtils#decodeFromBase64Str(String)
     * @see #decryptFromEncodedStr(String, Key)
     */
    public static byte[] decrypt(byte[] encryptedBytes, Key key) {
        return doCrypto(encryptedBytes, key, Cipher.DECRYPT_MODE);
    }

    /**
     * 解密Base64编码的已加密字符串.
     *
     * @param base64EncryptedStr Base64编码的已加密字符串
     * @param key                密钥对象
     * @return 明文字节数组
     */
    public static byte[] decryptFromEncodedStr(String base64EncryptedStr, Key key) {
        byte[] encryptedBytes = ParameterUtils.decodeFromBase64Str(base64EncryptedStr);
        return doCrypto(encryptedBytes, key, Cipher.DECRYPT_MODE);
    }

    /**
     * 用私钥对明文签名.
     *
     * @param plainTextBytes 明文字节数组
     * @param privateKey     PrivateKey对象
     * @return 签名字符串
     */
    public static String sign(byte[] plainTextBytes, PrivateKey privateKey) {
        String algorithm = privateKey.getAlgorithm();
        algorithm = EC_ALGORITHM.equalsIgnoreCase(algorithm) ? EC_SIGNATURE_ALGORITHM : RSA_SIGNATURE_ALGORITHM;
        return sign(plainTextBytes, privateKey, algorithm);
    }

    /**
     * 用私钥对明文签名.
     *
     * @param plainTextBytes 明文字节数组
     * @param privateKey     PrivateKey对象
     * @param algorithm      签名算法，必须与PrivateKey相对应
     * @return 签名字符串
     */
    public static String sign(byte[] plainTextBytes, PrivateKey privateKey, String algorithm) {
        try {
            Signature signature = Signature.getInstance(algorithm);
            signature.initSign(privateKey);
            signature.update(plainTextBytes);
            byte[] signatureBytes = signature.sign();
            return ParameterUtils.encodeToBase64Str(signatureBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("无此算法: " + algorithm, e);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("私钥非法", e);
        } catch (SignatureException e) {
            throw new IllegalArgumentException("签名失败", e);
        }
    }

    /**
     * 用公钥校验签名.
     *
     * @param plainTextBytes  已加密数据
     * @param base64Signature 签名字符串
     * @param publicKey       PublicKey对象
     * @return 校验是否通过
     */
    public static boolean verify(byte[] plainTextBytes, String base64Signature, PublicKey publicKey) {
        String algorithm = publicKey.getAlgorithm();
        algorithm = EC_ALGORITHM.equalsIgnoreCase(algorithm) ? EC_SIGNATURE_ALGORITHM : RSA_SIGNATURE_ALGORITHM;
        return verify(plainTextBytes, base64Signature, publicKey, algorithm);
    }

    /**
     * 用公钥校验签名.
     *
     * @param plainTextBytes  已加密数据
     * @param base64Signature 签名字符串
     * @param publicKey       PublicKey对象
     * @param algorithm       签名算法，必须与PublicKey相对应
     * @return 校验是否通过
     */
    public static boolean verify(byte[] plainTextBytes, String base64Signature, PublicKey publicKey, String algorithm) {
        try {
            byte[] signatureBytes = ParameterUtils.decodeFromBase64Str(base64Signature);
            Signature signature = Signature.getInstance(algorithm);
            signature.initVerify(publicKey);
            signature.update(plainTextBytes);
            return signature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("无此算法: " + algorithm, e);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("公钥非法", e);
        } catch (SignatureException e) {
            throw new IllegalArgumentException("签名字符串非法", e);
        }
    }

    /**
     * 加密或解密.
     *
     * @param inputBytes 字节数组
     * @param key        密钥对象
     * @param cryptoMode 加解密模式，参考{@link Cipher#ENCRYPT_MODE}和{@link Cipher#DECRYPT_MODE}
     * @return 加密或解密后的字节数组
     */
    private static byte[] doCrypto(byte[] inputBytes, Key key, int cryptoMode) {

        Cipher cipher = getInitializedCipher(key, cryptoMode);
        String algorithm = cipher.getAlgorithm();

        if (RSA_ALGORITHM.equalsIgnoreCase(algorithm)) {
            return doBlockCrypto(inputBytes, cipher, cryptoMode);
        }

        try {
            return cipher.doFinal(inputBytes);
        } catch (IllegalBlockSizeException e) {
            String msg = Cipher.ENCRYPT_MODE == cryptoMode ? "明文块长度非法" : "密文块长度非法";
            throw new IllegalArgumentException(msg, e);
        } catch (BadPaddingException e) {
            String msg = Cipher.ENCRYPT_MODE == cryptoMode ? "明文数据已损坏" : "密文数据已损坏";
            throw new IllegalArgumentException(msg, e);
        }
    }

    // ~ private method ---------------------------------------------------------------------------------------

    /**
     * 获取已经初始化好的Cipher密码器对象.
     *
     * @param key        密钥对象
     * @param cryptoMode 加解密模式，参考{@link Cipher#ENCRYPT_MODE}和{@link Cipher#DECRYPT_MODE}
     * @return Cipher
     */
    private static Cipher getInitializedCipher(Key key, int cryptoMode) {
        String algorithm = key.getAlgorithm();
        Cipher cipher;
        try {
            //目前JDK1.8里Cipher.getInstance(algorithm)并没有对应EC算法的实现，所以这里使用NullCipher
            //至于以后的JDK版本如何，再说吧
            if (EC_ALGORITHM.equalsIgnoreCase(algorithm)) {
                cipher = new NullCipher();
            } else {
                cipher = Cipher.getInstance(algorithm);
            }
            cipher.init(cryptoMode, key);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("无此算法: " + algorithm, e);
        } catch (NoSuchPaddingException e) {
            throw new IllegalArgumentException(e);
        } catch (InvalidKeyException e) {
            String msg = key instanceof PrivateKey ? "私钥非法" : (key instanceof PublicKey ? "公钥非法" : "密钥非法");
            throw new IllegalArgumentException(msg, e);
        }
        return cipher;
    }

    /**
     * 分块加密或解密.
     *
     * @param inputBytes 字节数组
     * @param cipher     Cipher对象
     * @param cryptoMode 加解密模式，参考{@link Cipher#ENCRYPT_MODE}和{@link Cipher#DECRYPT_MODE}
     * @return 加密或解密后的字节数组
     */
    private static byte[] doBlockCrypto(byte[] inputBytes, Cipher cipher, int cryptoMode) {

        int ibsLength = inputBytes.length;
        boolean isEncrypting = Cipher.ENCRYPT_MODE == cryptoMode;
        int blockSize = isEncrypting ? RSA_MAX_ENCRYPT_BLOCK_SIZE : RSA_MAX_DECRYPT_BLOCK_SIZE;
        int splitLength = ibsLength % blockSize == 0 ? ibsLength / blockSize : (ibsLength / blockSize) + 1;

        //初始化一个空的二维数组用于暂存分块加密/解密得到的字节数组
        byte[][] outputBytesBlocks = new byte[splitLength][];
        int outputBytesLength = 0;
        byte[] outputBytesBlock;

        //根据blockSize分块加密/解密
        try {
            for (int i = 0; i < splitLength; i++) {
                int offset = i * blockSize;
                int limit = ibsLength - offset;
                limit = limit < blockSize ? limit : blockSize;

                outputBytesBlock = cipher.doFinal(inputBytes, offset, limit);
                outputBytesBlocks[i] = outputBytesBlock;
                outputBytesLength += outputBytesBlock.length;
            }
        } catch (IllegalBlockSizeException e) {
            String msg = isEncrypting ? "明文块长度非法" : "密文块长度非法";
            throw new IllegalArgumentException(msg, e);
        } catch (BadPaddingException e) {
            String msg = isEncrypting ? "明文数据已损坏" : "密文数据已损坏";
            throw new IllegalArgumentException(msg, e);
        }

        //初始化一个空的一维数组用于顺序合并二维数组，然后最终输出结果
        byte[] outputBytes = new byte[outputBytesLength];
        outputBytesLength = 0;
        for (int i = 0; i < splitLength; i++) {
            outputBytesBlock = outputBytesBlocks[i];
            System.arraycopy(outputBytesBlock, 0, outputBytes, outputBytesLength, outputBytesBlock.length);
            outputBytesLength += outputBytesBlock.length;
        }

        return outputBytes;
    }

    /**
     * 给密钥字符串前后及每间隔64个字符中间插入换行字符'\n'.
     *
     * @param base64Key base64密钥字符串
     * @return 插入了多个换行字符'\n'的多行密钥字符串
     */
    private static String split64CharsPerLine(String base64Key) {
        int length = base64Key.length();
        int splitLength = length % 64 == 0 ? length >> 6 : (length >> 6) + 1;
        char[] chars = base64Key.toCharArray();
        StringBuilder builder = new StringBuilder(splitLength << 6);
        builder.append(LF_SYMBOL);
        for (int i = 0; i < splitLength; i++) {
            int offset = i << 6;
            int limit = length - offset;
            limit = limit < 64 ? limit : 64;
            char[] copy = new char[limit];
            System.arraycopy(chars, offset, copy, 0, limit);
            builder.append(copy).append(LF_SYMBOL);
        }
        return builder.toString();
    }

    /**
     * 去掉密钥换行字符'\n'及前后修饰字符串，同时根据修饰字符串解析出密钥的所属算法. <br>
     * 注意: 如果没有修饰字符串用来解析密钥的所属算法，则默认当成RSA算法返回
     *
     * @param base64Key base64密钥字符串
     * @return 字符串数组，索引0的值为处理后的base64Key，索引1为解析出来的algorithm
     */
    private static String[] trimBase64KeyAndGetAlgorithm(String base64Key) {
        String algorithm = RSA_ALGORITHM;
        if (base64Key.contains(RSA_X509_PUBLIC_KEY_PREFIX)) {
            base64Key = base64Key.replace(LF_SYMBOL, "")
                    .replace(RSA_X509_PUBLIC_KEY_PREFIX, "")
                    .replace(RSA_X509_PUBLIC_KEY_SUFFIX, "");
        } else if (base64Key.contains(EC_X509_PUBLIC_KEY_PREFIX)) {
            base64Key = base64Key.replace(LF_SYMBOL, "")
                    .replace(EC_X509_PUBLIC_KEY_PREFIX, "")
                    .replace(EC_X509_PUBLIC_KEY_SUFFIX, "");
            algorithm = EC_ALGORITHM;
        } else if (base64Key.contains(RSA_PKCS8_PRIVATE_KEY_PREFIX)) {
            base64Key = base64Key.replace(LF_SYMBOL, "")
                    .replace(RSA_PKCS8_PRIVATE_KEY_PREFIX, "")
                    .replace(RSA_PKCS8_PRIVATE_KEY_SUFFIX, "");
        } else if (base64Key.contains(EC_PKCS8_PRIVATE_KEY_PREFIX)) {
            base64Key = base64Key.replace(LF_SYMBOL, "")
                    .replace(EC_PKCS8_PRIVATE_KEY_PREFIX, "")
                    .replace(EC_PKCS8_PRIVATE_KEY_SUFFIX, "");
            algorithm = EC_ALGORITHM;
        }
        return new String[]{base64Key, algorithm};
    }
}