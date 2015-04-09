package io.hops.integration;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author André & Ermias
 */
@XmlRootElement
public class UserDTO {
    private String email;
    private String telephoneNum;
    private String firstName;
    private String lastName; 
    private int status;
    private String securityQuestion;
    private String securityAnswer;
    private String secret;   
    private String chosenPassword;
    private String repeatedPassword;

    public UserDTO() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephoneNum() {
        return telephoneNum;
    }

    public void setTelephoneNum(String telephoneNum) {
        this.telephoneNum = telephoneNum;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(String securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    public String getSecurityAnswer() {
        return securityAnswer;
    }

    public void setSecurityAnswer(String securityAnswer) {
        this.securityAnswer = securityAnswer;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getChosenPassword() {
        return chosenPassword;
    }

    public void setChosenPassword(String chosenPassword) {
        this.chosenPassword = chosenPassword;
    }

    public String getRepeatedPassword() {
        return repeatedPassword;
    }

    public void setRepeatedPassword(String repeatedPassword) {
        this.repeatedPassword = repeatedPassword;
    }

    
    @Override
    public String toString() {
        return "UserDTO{" + "email=" + email + ", telephoneNum=" + telephoneNum + 
               ", firstName=" + firstName + ", lastName=" + lastName + 
               ", status=" + status + ", securityQuestion=" + securityQuestion + 
               ", securityAnswer=" + securityAnswer + ", secret=" + secret + 
               ", chosenPassword=" + chosenPassword + ", repeatedPassword=" + repeatedPassword + '}';
    }

}