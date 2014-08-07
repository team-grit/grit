package util.hashing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * This class provides the ability to generate md5 hashes of a directory.
 * 
 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
 * 
 */
public final class SHA1Generator {
    private static final Logger LOGGER = Logger.getLogger("systemlog");
    private static MessageDigest s_MessageDigest = null;

    /**
     * This is a static class, so no constructore is needed.
     * 
     */
    private SHA1Generator() {

    }

    /**
     * Calculates the SHA-1 hash of a folder.
     * 
     * @param folder
     *            the folder of which the hash will be calculated
     * @return the hash in a string
     * @throws IOException
     *             if a file can not be read
     */
    public static String calculateSHA1Hash(Path folder) throws IOException {
        File[] files = folder.toFile().listFiles();
        String md5 = "";
        StringBuffer md5Buffer = new StringBuffer();
        try {
            for (File file : files) {
                if (file.isDirectory()) {
                    md5 += calculateSHA1Hash(file.toPath());
                } else {
                    md5 += getSH1OfFile(file);
                }
            }

            s_MessageDigest = MessageDigest.getInstance("SHA-1");
            s_MessageDigest.update(md5.getBytes());
            byte[] digest = s_MessageDigest.digest();

            for (byte b : digest) {
                md5Buffer.append(String.format("%02x", b & 0xff));
            }

        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("Could not find MD5 algorithm");
        }
        return md5Buffer.toString();
    }

    /**
     * Calculates the SHA-1 hash of a file.
     * 
     * @param file
     *            the file of which the hash will be calculated
     * @return the hash in a string
     * @throws NoSuchAlgorithmException
     *             if the SHA-1 is not available
     * @throws IOException
     *             if a file can not be read
     */
    private static String getSH1OfFile(File file)
            throws NoSuchAlgorithmException, IOException {
        StringBuffer md5 = null;
        try {
            s_MessageDigest = MessageDigest.getInstance("SHA-1");
            FileInputStream fileInput = new FileInputStream(file);

            byte[] dataBytes = new byte[1024];

            int nread = 0;
            while ((nread = fileInput.read(dataBytes)) != -1) {
                s_MessageDigest.update(dataBytes, 0, nread);
            }
            byte[] mdbytes = s_MessageDigest.digest();
            md5 = new StringBuffer();
            for (byte mdbyte : mdbytes) {
                md5.append(Integer.toString((mdbyte & 0xff) + 0x100, 16)
                        .substring(1));
            }

            fileInput.close();

        } catch (IOException e) {
            LOGGER.severe("Couldn't read from file: " + file.getAbsolutePath()
                    + ". " + e.getMessage());
            throw e;
        }

        return md5.toString();

    }
}
