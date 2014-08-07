/*
 * Copyright (C) 2014 Team GRIT
 * 
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package util.mailer;

/**
 * 
 * @author <a href="mailto:fabian.marquart@uni-konstanz.de">Fabian Marquart</a>
 * 
 */

public class MailObjectWithPassword extends MailObject {

    // encrypted password
    protected String m_senderPassword;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "MailObjectWithPassword [senderPassword=" + m_senderPassword
                + ", senderAddress=" + m_senderAddress + ", recipientAddress="
                + m_recipientAddress + ", mailSubject=" + m_mailSubject
                + ", mailMessage=" + m_mailMessage + "]";
    }

    /**
     * @return the encryptedPassword
     */
    public String getEncryptedPassword() {
        return m_senderPassword;
    }

    /**
     * @param encryptedPassword
     *            the encryptedPassword to set
     */
    public void setEncryptedPassword(String encryptedPassword) {
        m_senderPassword = encryptedPassword;
    }

    /**
     * Instantiates a new mail object with password.
     * 
     * @param senderAddress
     *            the address of the sender
     * @param recipientAddress
     *            the address of the recipient
     * @param mailSubject
     *            the subject of the mail
     * @param mailMessage
     *            the message of the mail
     * @param encryptedPassword
     *            the encrypted password
     */
    public MailObjectWithPassword(String senderAddress,
            String recipientAddress, String mailSubject, String mailMessage,
            String encryptedPassword) {
        super(senderAddress, recipientAddress, mailSubject, mailMessage);
        m_senderPassword = encryptedPassword;
    }

}
