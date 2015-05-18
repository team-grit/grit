package de.teamgrit.grit.util.mailer;

/**
 * 
 * @author Fabian Marquart <fabian.marquart@uni-konstanz.de>
 * 
 */
public class SecretKey {

    private byte[] m_key;

    /**
     * Constructor takes a String.
     * 
     * @param key
     *            the secret key
     */
    public SecretKey(String key) {
        m_key = key.getBytes();
    }

    /**
     * @return the key
     */
    public byte[] getKey() {
        return m_key;
    }

    /**
     * @param key
     *            the key to set
     */
    public void setKey(byte[] key) {
        m_key = key;
    }

}
