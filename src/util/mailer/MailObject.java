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

public class MailObject {
    // message contents and email information
    protected String m_senderAddress;
    protected String m_recipientAddress;
    protected String m_mailSubject;
    protected String m_mailMessage;

    /**
     * 
     * @param senderAddress
     *            : the sender's email address.
     * @param recipientAddress
     *            : recipient's email address.
     * @param mailSubject
     *            : the message subject.
     * @param mailMessage
     *            : the actual message.
     */
    public MailObject(String senderAddress, String recipientAddress,
            String mailSubject, String mailMessage) {
        super();
        m_senderAddress = senderAddress;
        m_recipientAddress = recipientAddress;
        m_mailSubject = mailSubject;
        m_mailMessage = mailMessage;
    }

    /**
     * @return the senderAddress
     */
    public String getSenderAddress() {
        return m_senderAddress;
    }

    /**
     * @param senderAddress
     *            the senderAddress to set
     */
    public void setSenderAddress(String senderAddress) {
        m_senderAddress = senderAddress;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "MailObject [senderAddress=" + m_senderAddress
                + ", recipientAddress=" + m_recipientAddress + ", mailSubject="
                + m_mailSubject + ", mailMessage=" + m_mailMessage + "]";
    }

    /**
     * @return the recipientAddress
     */
    public String getRecipientAddress() {
        return m_recipientAddress;
    }

    /**
     * @param recipientAddress
     *            the recipientAddress to set
     */
    public void setRecipientAddress(String recipientAddress) {
        m_recipientAddress = recipientAddress;
    }

    /**
     * @return the mailSubject
     */
    public String getMailSubject() {
        return m_mailSubject;
    }

    /**
     * @param mailSubject
     *            the mailSubject to set
     */
    public void setMailSubject(String mailSubject) {
        m_mailSubject = mailSubject;
    }

    /**
     * Sets template for missing submissions files to be send to a student.
     * 
     * @param exerciseName
     *            the name of the exercise
     * @param studentName
     *            the name of the student
     * @param problems
     *            the output of the compiler run
     * @param deadline
     *            the deadline of the exercise
     */
    public void setMailStudentProblems(String exerciseName, String studentName,
            String problems, String deadline) {
        m_mailSubject = "Probleme mit Abgabe " + exerciseName;
        m_mailMessage = "Hallo " + studentName + ", \n\n" + "Deine Abgabe "
                + exerciseName + " funktioniert nicht. Probleme sind:" + "\n"
                + problems + "\n\n" + "Du hast bis " + deadline
                + " Zeit, dies noch zu korrigieren." + "\n\n" + "Viel Erfolg"
                + "\n" + "Dein freundliches Abgabesystem";

    }

    /**
     * Sets template for assignment information to be send to a professor.
     * 
     * @param courseName
     *            the name of the course
     * @param exerciseNumber
     *            the name of the exercise
     * @param professorName
     *            the name of the professor
     */
    public void setMailProfessorInformation(String courseName,
            int exerciseNumber, String professorName) {
        m_mailSubject = courseName + " Übung " + exerciseNumber
                + " ist abgeschlossen.";
        m_mailMessage = "Hallo " + professorName + ", \n\n"
                + "Die Verarbeitung der Ergebnisse der Übung " + exerciseNumber
                + " des Kurses " + courseName
                + " ist beendet. Sie können nun das Ergebnis " + "einsehen. "
                + "\n\n" + "Ihr freundliches Abgabesystem";
    }

    /**
     * @return the mailMessage
     */
    public String getMailMessage() {
        return m_mailMessage;
    }

    /**
     * @param mailMessage
     *            the mailMessage to set
     */
    public void setMailMessage(String mailMessage) {
        m_mailMessage = mailMessage;
    }
}
