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

package preprocess;

/**
 * The Class Student.
 *
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 */

public class Student {

    private String m_name;
    private String m_email;

    /**
     * Instantiates a new student.
     *
     * @param name
     *            the name of the student
     */
    public Student(String name) {
        setName(name);
    }

    /**
     * Gets the name of the student.
     *
     * @return the current name of the student
     */
    public String getName() {
        return m_name;
    }

    /**
     * Sets the name of the student.
     *
     * @param name
     *            the new name of the student
     */
    public void setName(String name) {
        this.m_name = name;
    }

    /**
     * Sets the email of the student.
     *
     * @param email
     *            the new email adress of the student
     */
    public void setEmail(String email) {
        this.m_email = email;
    }

    /**
     * Gets the email of the student.
     *
     * @return the email adress
     */
    public String getEmail() {
        return m_email;
    }

    @Override
    public int hashCode() {
        return m_email.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Student other = (Student) obj;
        if (m_email == null) {
            if (other.m_email != null) {
                return false;
            }
        } else if (!m_email.equals(other.m_email)) {
            return false;
        }
        return true;
    }

}
