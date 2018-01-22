package won.cryptography.service.keystore;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

/**
 * User: fsalcher Date: 12.06.2014
 */
public class FileBasedKeyStoreService extends AbstractKeyStoreService {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final String PROVIDER_BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
	private static final String KEY_STORE_TYPE = "UBER";
	// 'UBER' is more secure, 'PKCS12' is supported by all tools, easier for
	// debugging, e.g. when importing keys,
	// therefore temporarily we can use 'PKCS12':
	// private static final String KEY_STORE_TYPE = "PKCS12";

	private String storePW;

	private File storeFile;

	private java.security.KeyStore store;

	private final Ehcache ehcache;

	public FileBasedKeyStoreService(String filePath, String storePW) {
		this(new File(filePath), storePW);
	}

	public FileBasedKeyStoreService(File storeFile, String storePW) {
		this.storeFile = storeFile;
		this.storePW = storePW;
		logger.info("Using key store file {} with key store type {}, provider {}",
				new Object[] { storeFile, KEY_STORE_TYPE, PROVIDER_BC });

		CacheManager manager = CacheManager.getInstance();
		ehcache = new Cache("keyCache" + storeFile.hashCode(), 100, false, false, 60, 60);
		manager.addCache(ehcache);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see won.cryptography.service.KeyStoreService#getPrivateKey(java.lang.String)
	 */
	@Override
	public PrivateKey getPrivateKey(String alias) {

		Element cachedElement = ehcache.get("KEY++" + alias);
		if (cachedElement != null)
			return (PrivateKey) cachedElement.getObjectValue();
		PrivateKey retrieved = null;
		try {
			retrieved = (PrivateKey) store.getKey(alias, storePW.toCharArray());
		} catch (Exception e) {
			logger.warn("Could not retrieve key for " + alias + " from ks " + storeFile.getName(), e);
		}
		if (retrieved != null) {
			ehcache.put(new Element("KEY++" + alias, retrieved));
		}
		return retrieved;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see won.cryptography.service.KeyStoreService#getPublicKey(java.lang.String)
	 */
	@Override
	public PublicKey getPublicKey(String alias) {
		Certificate cert = getCertificate(alias);
		if (cert == null) {
			logger.warn("No certificate found for alias {}", alias);
			return null;
		}
		return cert.getPublicKey();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see won.cryptography.service.KeyStoreService#getPassword()
	 */
	@Override
	public String getPassword() {
		return storePW;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * won.cryptography.service.KeyStoreService#getCertificate(java.lang.String)
	 */
	@Override
	public Certificate getCertificate(String alias) {
		Element cachedElement = ehcache.get("CERT++" + alias);
		if (cachedElement != null)
			return (Certificate) cachedElement.getObjectValue();
		Certificate retrieved = null;

		try {
			retrieved = store.getCertificate(alias);
		} catch (Exception e) {
			logger.warn("No certificate found for alias " + alias, e);
		}
		ehcache.put(new Element("CERT++" + alias, retrieved));
		return retrieved;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * won.cryptography.service.KeyStoreService#getCertificateAlias(java.security.
	 * cert.Certificate)
	 */
	@Override
	public String getCertificateAlias(Certificate cert) {

		String retrieved = null;

		try {
			retrieved = store.getCertificateAlias(cert);
		} catch (Exception e) {
			logger.warn("No alias found for certificate", e);
		}

		return retrieved;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see won.cryptography.service.KeyStoreService#getUnderlyingKeyStore()
	 */
	@Override
	public KeyStore getUnderlyingKeyStore() {

		return this.store;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see won.cryptography.service.KeyStoreService#putKey(java.lang.String,
	 * java.security.PrivateKey, java.security.cert.Certificate[], boolean)
	 */
	@Override
	public synchronized void putKey(String alias, PrivateKey key, Certificate[] certificateChain, boolean replace)
			throws IOException {

		putEntry(alias, key, certificateChain, null, replace);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * won.cryptography.service.KeyStoreService#putCertificate(java.lang.String,
	 * java.security.cert.Certificate, boolean)
	 */
	@Override
	public synchronized void putCertificate(String alias, Certificate certificate, boolean replace) throws IOException {

		putEntry(alias, null, null, certificate, replace);
	}

	protected synchronized void persistStore() throws Exception {

		FileOutputStream outputStream = null;

		try {
			outputStream = new FileOutputStream(storeFile);
		} catch (IOException e) {
			logger.error("Could not create key store in file " + storeFile.getName(), e);
			throw e;
		}

		if (outputStream != null) {
			try {
				store.store(outputStream, storePW.toCharArray());
			} catch (Exception e) {
				logger.error("Could not save key store to file" + storeFile.getName(), e);
				throw new IOException(e);
			} finally {
				try {
					outputStream.close();
				} catch (Exception e) {
					logger.error("Error closing stream of file" + storeFile.getName(), e);
					throw e;
				}
			}
		}

	}

	private void loadStoreFromFile() throws Exception {

		FileInputStream inputStream = null;

		try {
			inputStream = new FileInputStream(storeFile);
		} catch (FileNotFoundException e) {
			logger.error("Could not load key store from file" + storeFile.getName(), e);
			throw e;
		}

		if (inputStream != null) {
			try {
				store.load(inputStream, storePW.toCharArray());
			} catch (Exception e) {
				logger.error("Could not load key store from file " + storeFile.getName(), e);
				throw e;
			} finally {
				try {
					inputStream.close();
				} catch (Exception e) {
					logger.error("Error closing stream of file " + storeFile.getName(), e);
					throw e;
				}

			}
		}
	}

	

	public void init() throws Exception {
		try {
			store = java.security.KeyStore.getInstance(KEY_STORE_TYPE, PROVIDER_BC);
			logger.debug("KEYSTORE: " + store);

			if (storeFile == null || !storeFile.exists() || !storeFile.isFile())
				store.load(null, null);
			else {
				loadStoreFromFile();
			}
		} catch (Exception e) {
			logger.error("Error initializing key store " + storeFile.getName(), e);
			throw e;
		}
	}
}
