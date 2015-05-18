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

package de.teamgrit.grit.util.mailer;

/**
 * 
 * @author <a href="mailto:fabian.marquart@uni-konstanz.de">Fabian Marquart</a>
 * 
 */

public class MailObjectWithConnection extends MailObjectWithPassword {

    // connection information
    private String m_smtpHost;
    private int m_port;
    private boolean m_smtpAuth;
    private int m_smtpPort;

    /**
     * Instantiates a new mail object with connection.
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
     * @param smtpHost
     *            the smtp host server
     * @param port
     *            the port of the server
     * @param smtpAuth
     *            the smtp authentication
     * @param smtpPort
     *            the smto port
     */
    public MailObjectWithConnection(String senderAddress,
            String recipientAddress, String mailSubject, String mailMessage,
            String encryptedPassword, String smtpHost, int port,
            boolean smtpAuth, int smtpPort) {
        super(senderAddress, recipientAddress, mailSubject, mailMessage,
                encryptedPassword);
        m_smtpHost = smtpHost;
        m_port = port;
        m_smtpAuth = smtpAuth;
        m_smtpPort = smtpPort;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "MailObjectWithConnection [smtpHost=" + m_smtpHost + ", port="
                + m_port + ", smtpAuth=" + m_smtpAuth + ", smtpPort="
                + m_smtpPort + ", senderPassword=" + m_senderPassword
                + ", senderAddress=" + m_senderAddress + ", recipientAddress="
                + m_recipientAddress + ", mailSubject=" + m_mailSubject
                + ", mailMessage=" + m_mailMessage + "]";
    }

    /**
     * @return the smtpHost
     */
    public String getSmtpHost() {
        return m_smtpHost;
    }

    /**
     * @param smtpHost
     *            the smtpHost to set
     */
    public void setSmtpHost(String smtpHost) {
        m_smtpHost = smtpHost;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return m_port;
    }

    /**
     * @param port
     *            the port to set
     */
    public void setPort(int port) {
        m_port = port;
    }

    /**
     * @return the smtpAuth
     */
    public boolean getSmtpAuth() {
        return m_smtpAuth;
    }

    /**
     * @param smtpAuth
     *            the smtpAuth to set
     */
    public void setSmtpAuth(boolean smtpAuth) {
        m_smtpAuth = smtpAuth;
    }

    /**
     * @return the smtpPort
     */
    public int getSmtpPort() {
        return m_smtpPort;
    }

    /**
     * @param smtpPort
     *            the smtpPort to set
     */
    public void setSmtpPort(int smtpPort) {
        m_smtpPort = smtpPort;
    }

}
